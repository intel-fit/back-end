package rto.intelfit.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestTemplate;
import rto.intelfit.dto.InBodyOcrResult;
import rto.intelfit.exception.BusinessException;
import rto.intelfit.exception.ErrorCode;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class GeminiVisionClient {

    private static final String BASE_PROMPT = """
            You are an expert OCR specialist for InBody result data. Extract all values from the single input image into the exact JSON format specified below.
            
            [REQUIRED EXTRACTION RULES]
            1.  Strict Data Extraction: Identify and extract numerical values and the measurement date.
            2.  No Guessing: If any value is ambiguous, blurry, or unreadable, return null for that field. Do not guess or approximate.
            3.  Field Mapping: Use the units (kg, %, kcal, L) and surrounding labels to map the numbers to the exact fields defined in the schema.
            4.  Key Integrity: Use the 24 keys exactly as listed. Keys must not be modified or commented.
            
            [FINAL JSON SCHEMA - PURE JSON ONLY]
            Output a single JSON object (starting with {) containing all fields below.
            DO NOT include any natural language, code fences, titles, or explanations.
            
            {
                "measurementDate": "yyyy-MM-dd" or null,
                "weight": float or null,
                "muscleMass": float or null,
                "bodyFatMass": float or null,
                "skeletalMuscleMass": float or null,
                "bodyFatPercentage": float or null,
            
                "leftArmMuscle": float or null,
                "rightArmMuscle": float or null,
                "trunkMuscle": float or null,
                "leftLegMuscle": float or null,
                "rightLegMuscle": float or null,
            
                "leftArmFat": float or null,
                "rightArmFat": float or null,
                "trunkFat": float or null,
                "leftLegFat": float or null,
                "rightLegFat": float or null,
            
                "totalBodyWater": float or null,
                "protein": float or null,
                "mineral": float or null,
                "bmi": float or null,
                "bodyFatPercentageStandard": float or null,
                "obesityDegree": float or null,
                "visceralFatLevel": float or null,
                "basalMetabolicRate": float or null
            }
            """;

    private static final int MAX_RETRY = 3;
    private static final long INITIAL_DELAY_MS = 1000;

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    @Value("${gemini.api-key:}")
    private String geminiApiKey;

    @Value("${gemini.model:gemini-2.5-flash}")
    private String geminiModel;

    @Value("${gemini.url:https://generativelanguage.googleapis.com/v1beta/models}")
    private String geminiBaseUrl;

    public InBodyOcrResult analyze(byte[] imageBytes) {
        if (!StringUtils.hasText(geminiApiKey)) {
            throw new BusinessException(ErrorCode.SERVICE_UNAVAILABLE, "Gemini API 키가 설정되지 않았습니다");
        }

        try {
            String response = callGeminiApi(imageBytes);
            String extractedText = extractTextContent(response);
            String cleanJson = cleanResponse(extractedText);
            return objectMapper.readValue(cleanJson, InBodyOcrResult.class);
        } catch (Exception e) {
            throw new BusinessException(ErrorCode.SERVICE_UNAVAILABLE, "Gemini OCR 처리 중 오류가 발생했습니다", e);
        }
    }

    private String callGeminiApi(byte[] imageBytes) throws JsonProcessingException {
        String url = String.format("%s/%s:generateContent?key=%s", geminiBaseUrl, geminiModel, geminiApiKey);
        List<Map<String, Object>> parts = new ArrayList<>();
        parts.add(Map.of("text", BASE_PROMPT));

        parts.add(Map.of("inline_data", Map.of(
                "mime_type", "image/jpeg",
                "data", Base64.getEncoder().encodeToString(imageBytes)
        )));

        Map<String, Object> payload = Map.of(
                "contents", List.of(
                        Map.of(
                                "role", "user",
                                "parts", parts
                        )
                )
        );

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setAcceptCharset(List.of(StandardCharsets.UTF_8));

        HttpEntity<String> entity = new HttpEntity<>(objectMapper.writeValueAsString(payload), headers);

        int attempt = 0;
        long delay = INITIAL_DELAY_MS;

        while (true) {
            attempt++;
            try {
                ResponseEntity<String> response = restTemplate.exchange(
                        url,
                        HttpMethod.POST,
                        entity,
                        String.class
                );
                return response.getBody();
            } catch (HttpStatusCodeException e) {
                if (e.getStatusCode() == HttpStatus.SERVICE_UNAVAILABLE && attempt < MAX_RETRY) {
                    log.warn("Gemini API 503 응답, {}ms 후 재시도 (시도 {}/{})", delay, attempt, MAX_RETRY);
                    sleep(delay);
                    delay *= 2;
                    continue;
                }
                log.error("Gemini API 호출 실패 - status {}", e.getStatusCode(), e);
                throw new BusinessException(ErrorCode.SERVICE_UNAVAILABLE, "Gemini API 호출 중 오류가 발생했습니다", e);
            } catch (RestClientException e) {
                log.error("Gemini API 호출 실패", e);
                throw new BusinessException(ErrorCode.SERVICE_UNAVAILABLE, "Gemini API 호출 중 오류가 발생했습니다", e);
            }
        }
    }

    private String extractTextContent(String responseBody) throws JsonProcessingException {
        JsonNode root = objectMapper.readTree(responseBody);
        JsonNode candidates = root.path("candidates");
        if (candidates.isArray() && candidates.size() > 0) {
            JsonNode parts = candidates.get(0).path("content").path("parts");
            if (parts.isArray() && parts.size() > 0) {
                JsonNode textNode = parts.get(0).path("text");
                if (StringUtils.hasText(textNode.asText())) {
                    return textNode.asText();
                }
            }
        }
        throw new BusinessException(ErrorCode.INVALID_INBODY_DATA, "Gemini 응답을 파싱할 수 없습니다");
    }

    private String cleanResponse(String text) {
        if (!StringUtils.hasText(text)) {
            return "{}";
        }
        return text.replace("```json", "")
                .replace("```JSON", "")
                .replace("```", "")
                .trim();
    }

    private void sleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new BusinessException(ErrorCode.SERVICE_UNAVAILABLE, "Gemini API 재시도 중 인터럽트가 발생했습니다", e);
        }
    }
}
