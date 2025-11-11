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
import java.util.Map;

/**
 * FastAPI AI 서버와 통신하는 클라이언트
 * - 식단 추천 (AI 기반)
 * - 운동 루틴 추천 (AI 기반)
 * - 건강 점수 계산
 * - AI 코치 챗봇
 * - 분석/통계 그래프
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AIServerClient {

    private final RestTemplate restTemplate;

    @Value("${ai.server.url}")
    private String aiServerUrl;

    // ========================================
    // 1️⃣ 식단 관련 API
    // ========================================

    /**
     * AI 기반 식단 추천
     * POST /ai-plan/meal_plan
     */
    public Map<String, Object> generateMealPlan(Map<String, Object> request) {
        String url = aiServerUrl + "/ai-plan/meal_plan";
        return postRequest(url, request);
    }

    /**
     * 음식 이미지 업로드 및 AI 분석
     * POST /upload_food
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

    // ========================================
    // 2️⃣ 운동 관련 API
    // ========================================

    /**
     * AI 기반 운동 루틴 추천
     * POST /ai/exercise_plan
     */
    public Map<String, Object> generateExercisePlan(Map<String, Object> request) {
        String url = aiServerUrl + "/ai/exercise_plan";
        return postRequest(url, request);
    }

    /**
     * 운동 루틴 저장 (피드백용)
     * POST /exercise_feedback/log
     */
    public Map<String, Object> logExercisePlan(Map<String, Object> request) {
        String url = aiServerUrl + "/exercise_feedback/log";
        return postRequest(url, request);
    }

    /**
     * 운동 피드백 업데이트
     * PUT /exercise_feedback/{rec_id}
     */
    public Map<String, Object> updateExerciseFeedback(String recId, Map<String, Object> request) {
        String url = aiServerUrl + "/exercise_feedback/" + recId;
        return putRequest(url, request);
    }

    /**
     * 일일 운동 점수 조회
     * GET /exercise_score/daily/{user_id}
     */
    public Map<String, Object> getDailyExerciseScore(String userId, String date) {
        String url = aiServerUrl + "/exercise_score/daily/" + userId;
        if (date != null && !date.isEmpty()) {
            url += "?date=" + date;
        }
        return getRequest(url);
    }

    // ========================================
    // 3️⃣ 건강 점수 API
    // ========================================

    /**
     * 일일 건강 점수 조회
     * GET /score/daily/{user_id}
     */
    public Map<String, Object> getDailyHealthScore(String userId, String date) {
        String url = aiServerUrl + "/score/daily/" + userId;
        if (date != null && !date.isEmpty()) {
            url += "?date=" + date;
        }
        return getRequest(url);
    }

    /**
     * 주간 건강 점수 조회
     * GET /score/weekly/{user_id}
     */
    public Map<String, Object> getWeeklyHealthScore(String userId) {
        String url = aiServerUrl + "/score/weekly/" + userId;
        return getRequest(url);
    }

    /**
     * 월간 건강 점수 조회
     * GET /score/monthly/{user_id}
     */
    public Map<String, Object> getMonthlyHealthScore(String userId, String yearMonth) {
        String url = aiServerUrl + "/score/monthly/" + userId;
        if (yearMonth != null && !yearMonth.isEmpty()) {
            url += "?year_month=" + yearMonth;
        }
        return getRequest(url);
    }

    /**
     * 점수 트렌드 조회
     * GET /score/trend/{user_id}
     */
    public Map<String, Object> getScoreTrend(String userId, int days) {
        String url = aiServerUrl + "/score/trend/" + userId + "?days=" + days;
        return getRequest(url);
    }

    // ========================================
    // 4️⃣ 분석/통계 API
    // ========================================

    /**
     * 일일 요약 조회
     * GET /analytics/daily/{user_id}
     */
    public Map<String, Object> getDailySummary(String userId) {
        String url = aiServerUrl + "/analytics/daily/" + userId;
        return getRequest(url);
    }

    /**
     * 주간 트렌드 그래프 (이미지)
     * GET /analytics/weekly/{user_id}
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

    /**
     * 월간 통계 요약
     * GET /analytics/monthly/{user_id}
     */
    public Map<String, Object> getMonthlyAverage(String userId) {
        String url = aiServerUrl + "/analytics/monthly/" + userId;
        return getRequest(url);
    }

    // ========================================
    // 5️⃣ 코치 API
    // ========================================

    /**
     * 주간 코치 리포트
     * GET /coach/weekly_report/{user_id}
     */
    public Map<String, Object> getWeeklyCoachReport(String userId) {
        String url = aiServerUrl + "/coach/weekly_report/" + userId;
        return getRequest(url);
    }

    /**
     * AI 코치 챗봇
     * POST /chat/coach
     */
    public Map<String, Object> chatWithCoach(String userId, String message) {
        String url = aiServerUrl + "/chat/coach";

        Map<String, Object> request = Map.of(
                "user_id", userId,
                "message", message
        );

        return postRequest(url, request);
    }

    // ========================================
    // 🔧 헬퍼 메서드
    // ========================================

    private Map<String, Object> getRequest(String url) {
        try {
            log.debug("AI 서버 GET 요청: {}", url);
            ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    null,
                    new ParameterizedTypeReference<Map<String, Object>>() {}
            );
            return response.getBody();
        } catch (RestClientException e) {
            log.error("AI 서버 GET 요청 실패: {} - {}", url, e.getMessage());
            throw new RuntimeException("AI 서버 호출 실패: " + e.getMessage(), e);
        }
    }

    private Map<String, Object> postRequest(String url, Map<String, Object> request) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<Map<String, Object>> requestEntity = new HttpEntity<>(request, headers);

        try {
            log.debug("AI 서버 POST 요청: {}", url);
            ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                    url,
                    HttpMethod.POST,
                    requestEntity,
                    new ParameterizedTypeReference<Map<String, Object>>() {}
            );
            return response.getBody();
        } catch (RestClientException e) {
            log.error("AI 서버 POST 요청 실패: {} - {}", url, e.getMessage());
            throw new RuntimeException("AI 서버 호출 실패: " + e.getMessage(), e);
        }
    }

    private Map<String, Object> putRequest(String url, Map<String, Object> request) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<Map<String, Object>> requestEntity = new HttpEntity<>(request, headers);

        try {
            log.debug("AI 서버 PUT 요청: {}", url);
            ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                    url,
                    HttpMethod.PUT,
                    requestEntity,
                    new ParameterizedTypeReference<Map<String, Object>>() {}
            );
            return response.getBody();
        } catch (RestClientException e) {
            log.error("AI 서버 PUT 요청 실패: {} - {}", url, e.getMessage());
            throw new RuntimeException("AI 서버 호출 실패: " + e.getMessage(), e);
        }
    }
}
