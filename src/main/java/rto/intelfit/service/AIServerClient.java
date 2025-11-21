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
import rto.intelfit.domain.User;
import rto.intelfit.domain.InBody;

import java.io.IOException;
import java.time.LocalDate;
import java.time.Period;
import java.util.HashMap;
import java.util.Map;

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

    public Map<String, Object> generateMealPlan(Map<String, Object> request) {
        String url = aiServerUrl + "/ai-plan/meal_plan";
        return postRequest(url, request);
    }

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
    // 2️⃣ 운동 관련 API - 수정됨 ✅
    // ========================================

    /**
     * AI 기반 운동 루틴 추천 - User 엔티티 기반
     * POST /ai/exercise_plan
     */
    public Map<String, Object> generateExercisePlan(User user, InBody latestInBody) {
        String url = aiServerUrl + "/ai/exercise_plan";

        // UserExerciseContext 형식으로 요청 바디 구성
        Map<String, Object> request = new HashMap<>();
        request.put("user_id", String.valueOf(user.getUserId()));
        request.put("age", calculateAge(user.getBirthDate()));
        request.put("sex", mapGenderToSex(user.getGender()));
        request.put("height", user.getHeight() != null ? user.getHeight().floatValue() : 170.0f);
        request.put("weight", user.getWeight() != null ? user.getWeight().floatValue() : 70.0f);

        // InBody 데이터가 있으면 추가
        if (latestInBody != null) {
            request.put("body_fat", latestInBody.getBodyFatPercentage() != null
                    ? latestInBody.getBodyFatPercentage().floatValue() : null);
            request.put("skeletal_muscle", latestInBody.getSkeletalMuscleMass() != null
                    ? latestInBody.getSkeletalMuscleMass().floatValue() : null);
        } else {
            request.put("body_fat", null);
            request.put("skeletal_muscle", null);
        }

        request.put("activity_level", mapActivityLevel(parseWorkoutDays(user.getWorkoutDaysPerWeek())));
        request.put("goal", mapHealthGoal(user.getHealthGoal()));

        log.info("🤖 AI 운동 추천 요청: userId={}, goal={}, activity_level={}",
                user.getUserId(), request.get("goal"), request.get("activity_level"));

        return postRequest(url, request);
    }

    /**
     * AI 기반 운동 루틴 추천 - Map 기반 (기존 호환성 유지)
     * POST /ai/exercise_plan
     */
    public Map<String, Object> generateExercisePlan(Map<String, Object> request) {
        String url = aiServerUrl + "/ai/exercise_plan";

        // 필수 필드 검증
        validateExercisePlanRequest(request);

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

    public Map<String, Object> getDailyHealthScore(String userId, String date) {
        String url = aiServerUrl + "/score/daily/" + userId;
        if (date != null && !date.isEmpty()) {
            url += "?date=" + date;
        }
        return getRequest(url);
    }

    public Map<String, Object> getWeeklyHealthScore(String userId) {
        String url = aiServerUrl + "/score/weekly/" + userId;
        return getRequest(url);
    }

    public Map<String, Object> getMonthlyHealthScore(String userId, String yearMonth) {
        String url = aiServerUrl + "/score/monthly/" + userId;
        if (yearMonth != null && !yearMonth.isEmpty()) {
            url += "?year_month=" + yearMonth;
        }
        return getRequest(url);
    }

    public Map<String, Object> getScoreTrend(String userId, int days) {
        String url = aiServerUrl + "/score/trend/" + userId + "?days=" + days;
        return getRequest(url);
    }

    // ========================================
    // 4️⃣ 분석/통계 API
    // ========================================

    public Map<String, Object> getDailySummary(String userId) {
        String url = aiServerUrl + "/analytics/daily/" + userId;
        return getRequest(url);
    }

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

    public Map<String, Object> getMonthlyAverage(String userId) {
        String url = aiServerUrl + "/analytics/monthly/" + userId;
        return getRequest(url);
    }

    // ========================================
    // 5️⃣ 코치 API
    // ========================================

    public Map<String, Object> getWeeklyCoachReport(String userId) {
        String url = aiServerUrl + "/coach/weekly_report/" + userId;
        return getRequest(url);
    }

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
            log.debug("AI 서버 POST 요청: {} - Body: {}", url, request);
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

    // ========================================
    // 📊 데이터 매핑 헬퍼
    // ========================================

    private int calculateAge(LocalDate birthDate) {
        if (birthDate == null) {
            return 30; // 기본값
        }
        return Period.between(birthDate, LocalDate.now()).getYears();
    }

    private String mapGenderToSex(User.Gender gender) {
        if (gender == null) return "male";
        return gender == User.Gender.F ? "female" : "male";
    }

    private String mapHealthGoal(User.HealthGoal healthGoal) {
        if (healthGoal == null) return "maintenance";

        switch (healthGoal) {
            case DIET:
                return "fat_loss";
            case MUSCLE_GAIN:
            case BULK:
            case LEAN_MASS:
                return "hypertrophy";
            case MAINTENANCE:
            default:
                return "maintenance";
        }
    }

    private float mapActivityLevel(Integer workoutDaysPerWeek) {
        if (workoutDaysPerWeek == null || workoutDaysPerWeek <= 0) {
            return 1.55f; // 보통 활동 (기본값)
        }

        if (workoutDaysPerWeek <= 1) return 1.2f;  // 거의 운동 안함
        if (workoutDaysPerWeek <= 3) return 1.375f; // 가벼운 운동
        if (workoutDaysPerWeek <= 5) return 1.55f;  // 보통 운동
        return 1.725f; // 매우 활동적
    }

    /**
     * workoutDaysPerWeek 문자열을 Integer로 파싱
     * 예: "3-4일" -> 3, "5" -> 5, null -> 기본값 3
     */
    private Integer parseWorkoutDays(String workoutDaysPerWeek) {
        if (workoutDaysPerWeek == null || workoutDaysPerWeek.trim().isEmpty()) {
            return 3; // 기본값
        }

        try {
            // 순수 숫자인 경우
            return Integer.parseInt(workoutDaysPerWeek.trim());
        } catch (NumberFormatException e) {
            // "3-4일", "3~4일" 같은 형식 파싱
            String cleaned = workoutDaysPerWeek.replaceAll("[^0-9-~]", ""); // 숫자, -, ~ 만 남김
            
            if (cleaned.contains("-") || cleaned.contains("~")) {
                // 범위에서 첫 번째 숫자 추출 (예: "3-4" -> 3)
                String[] parts = cleaned.split("[-~]");
                if (parts.length > 0 && !parts[0].isEmpty()) {
                    try {
                        return Integer.parseInt(parts[0]);
                    } catch (NumberFormatException ex) {
                        log.warn("운동 일수 파싱 실패: {}, 기본값 사용", workoutDaysPerWeek);
                        return 3;
                    }
                }
            }
            
            log.warn("운동 일수 파싱 실패: {}, 기본값 사용", workoutDaysPerWeek);
            return 3; // 파싱 실패 시 기본값
        }
    }

    private void validateExercisePlanRequest(Map<String, Object> request) {
        if (!request.containsKey("user_id")) {
            throw new IllegalArgumentException("user_id is required");
        }
        if (!request.containsKey("age")) {
            throw new IllegalArgumentException("age is required");
        }
        if (!request.containsKey("sex")) {
            throw new IllegalArgumentException("sex (male/female) is required");
        }
        if (!request.containsKey("height")) {
            throw new IllegalArgumentException("height is required");
        }
        if (!request.containsKey("weight")) {
            throw new IllegalArgumentException("weight is required");
        }
        if (!request.containsKey("activity_level")) {
            throw new IllegalArgumentException("activity_level is required");
        }
        if (!request.containsKey("goal")) {
            throw new IllegalArgumentException("goal (fat_loss/hypertrophy/maintenance) is required");
        }
    }
}