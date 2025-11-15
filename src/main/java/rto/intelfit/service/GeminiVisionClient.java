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
            You are an OCR specialist that extracts InBody result tables. Follow the rules strictly:
            1. Even when the photo has distortions, shadows, or noise, reconstruct the table values faithfully.
            2. If a value is blurry or unreadable, do NOT guess; return null.
            3. Respect the units shown in the table (kg, %, kcal, L) to decide each metric.
            4. Output pure JSON only. No natural language, no markdown or code fences.
            5. Compare both images (original and preprocessed). Choose the number that is more reliable.
            6. Fields order: measurementDate(yyyy-MM-dd), weight, muscleMass, bodyFatMass, skeletalMuscleMass, bodyFatPercentage,
               leftArmMuscle, rightArmMuscle, trunkMuscle, leftLegMuscle, rightLegMuscle,
               leftArmFat, rightArmFat, trunkFat, leftLegFat, rightLegFat,
               totalBodyWater, protein, mineral, bmi, bodyFatPercentageStandard, obesityDegree,
               visceralFatLevel, basalMetabolicRate.
            7. Return JSON with those exact keys. Null for missing values. Do not add comments.
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

    public InBodyOcrResult analyze(byte[] primaryImage, byte[] secondaryImage, FocusTarget focusTarget) {
        if (!StringUtils.hasText(geminiApiKey)) {
            throw new BusinessException(ErrorCode.SERVICE_UNAVAILABLE, "Gemini API 키가 설정되지 않았습니다");
        }

        try {
            String response = callGeminiApi(primaryImage, secondaryImage, focusTarget);
            String extractedText = extractTextContent(response);
            String cleanJson = cleanResponse(extractedText);
            return objectMapper.readValue(cleanJson, InBodyOcrResult.class);
        } catch (Exception e) {
            throw new BusinessException(ErrorCode.SERVICE_UNAVAILABLE, "Gemini OCR 처리 중 오류가 발생했습니다", e);
        }
    }

    private String callGeminiApi(byte[] primaryImage, byte[] secondaryImage, FocusTarget focusTarget) throws JsonProcessingException {
        String url = String.format("%s/%s:generateContent?key=%s", geminiBaseUrl, geminiModel, geminiApiKey);
        List<Map<String, Object>> parts = new ArrayList<>();
        parts.add(Map.of("text", focusTargetPrompt(focusTarget)));

        parts.add(Map.of("inline_data", Map.of(
                "mime_type", "image/jpeg",
                "data", Base64.getEncoder().encodeToString(primaryImage)
        )));
        if (secondaryImage != null && secondaryImage.length > 0) {
            parts.add(Map.of("inline_data", Map.of(
                    "mime_type", "image/jpeg",
                    "data", Base64.getEncoder().encodeToString(secondaryImage)
            )));
        }

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

    private String focusTargetPrompt(FocusTarget focusTarget) {
        String focusText = focusTarget == FocusTarget.ORIGINAL_PRIMARY
                ? "Image #1 is the original photo. Image #2 is a preprocessed version. Use Image #1 as the primary source and verify with Image #2."
                : "Image #1 is the enhanced/preprocessed photo. Image #2 is the original. Use Image #1 as the primary source and cross-check with Image #2.";
        return BASE_PROMPT + System.lineSeparator() + focusText;
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

    public enum FocusTarget {
        ORIGINAL_PRIMARY,
        PREPROCESSED_PRIMARY
    }
}
