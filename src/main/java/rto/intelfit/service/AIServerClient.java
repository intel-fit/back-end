// src/main/java/rto/intelfit/service/AIServerClient.java
package rto.intelfit.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class AIServerClient {

    private final RestTemplate restTemplate;

    @Value("${ai.server.url}")
    private String aiServerUrl;

    /**
     * 음식 이미지 업로드 및 AI 분석
     */
    public Map<String, Object> uploadFoodImage(MultipartFile file) throws IOException {
        String url = aiServerUrl + "/upload_food";

        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        body.add("file", new ByteArrayResource(file.getBytes()) {
            @Override
            public String getFilename() {
                return file.getOriginalFilename();
            }
        });

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);

        HttpEntity<MultiValueMap<String, Object>> requestEntity = new HttpEntity<>(body, headers);

        try {
            ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                    url,
                    HttpMethod.POST,
                    requestEntity,
                    new ParameterizedTypeReference<Map<String, Object>>() {}
            );
            return response.getBody();
        } catch (RestClientException e) {
            log.error("AI 서버 호출 실패: {}", e.getMessage());
            throw new RuntimeException("AI 서버와 통신 중 오류 발생", e);
        }
    }

    /**
     * 일일 식단 추천
     */
    public Map<String, Object> recommendDailyMeal(String userId, int mealsPerDay, String goal) {
        String url = aiServerUrl + "/recommend_daily_meal";

        Map<String, Object> request = Map.of(
                "user_id", userId,
                "meals_per_day", mealsPerDay,
                "goal", goal
        );

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        HttpEntity<Map<String, Object>> requestEntity = new HttpEntity<>(request, headers);

        try {
            ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                    url,
                    HttpMethod.POST,
                    requestEntity,
                    new ParameterizedTypeReference<Map<String, Object>>() {}
            );
            return response.getBody();
        } catch (RestClientException e) {
            log.error("식단 추천 API 호출 실패: {}", e.getMessage());
            throw new RuntimeException("식단 추천 중 오류 발생", e);
        }
    }

    /**
     * 건강 점수 조회 (주간)
     */
    public Map<String, Object> getWeeklyHealthScore(String userId) {
        String url = aiServerUrl + "/score/weekly/" + userId;

        try {
            ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    null,
                    new ParameterizedTypeReference<Map<String, Object>>() {}
            );
            return response.getBody();
        } catch (RestClientException e) {
            log.error("건강 점수 조회 실패: {}", e.getMessage());
            throw new RuntimeException("건강 점수 조회 중 오류 발생", e);
        }
    }

    /**
     * AI 코치 채팅
     */
    public Map<String, Object> chatWithCoach(String userId, String message) {
        String url = aiServerUrl + "/chat/coach";

        Map<String, Object> request = Map.of(
                "user_id", userId,
                "message", message
        );

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        HttpEntity<Map<String, Object>> requestEntity = new HttpEntity<>(request, headers);

        try {
            ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                    url,
                    HttpMethod.POST,
                    requestEntity,
                    new ParameterizedTypeReference<Map<String, Object>>() {}
            );
            return response.getBody();
        } catch (RestClientException e) {
            log.error("AI 코치 채팅 실패: {}", e.getMessage());
            throw new RuntimeException("AI 코치 채팅 중 오류 발생", e);
        }
    }

    /**
     * 주간 트렌드 이미지 조회
     */
    public byte[] getWeeklyTrendImage(String userId) {
        String url = aiServerUrl + "/analytics/weekly/" + userId;

        try {
            ResponseEntity<byte[]> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    null,
                    byte[].class
            );
            return response.getBody();
        } catch (RestClientException e) {
            log.error("주간 트렌드 이미지 조회 실패: {}", e.getMessage());
            throw new RuntimeException("트렌드 이미지 조회 중 오류 발생", e);
        }
    }
}