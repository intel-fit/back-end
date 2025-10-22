package rto.intelfit.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;
import rto.intelfit.domain.*;
import rto.intelfit.dto.MealDto;
import rto.intelfit.dto.UserFoodPreferenceDto;
import rto.intelfit.exception.BusinessException;
import rto.intelfit.exception.ErrorCode;
import rto.intelfit.repository.RecommendedMealPlanRepository;
import rto.intelfit.repository.UserRepository;
import rto.intelfit.security.CustomUserPrincipal;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * AI 서버와 통신하는 서비스
 *
 * 주요 기능:
 * 1. 음식 이미지를 AI 서버에 전송하여 영양 성분 분석
 * 2. 사용자 정보를 기반으로 AI 서버에서 추천 식단 받아오기
 * 3. 사용자 선호 음식 데이터를 AI 추천에 포함
 * 4. AI 서버 응답을 도메인 객체로 변환
 *
 * Note: AI 서버의 실제 API 스펙에 맞춰 수정이 필요합니다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AIServerService {

    private final RestTemplate restTemplate;
    private final UserRepository userRepository;
    private final RecommendedMealPlanRepository recommendedMealPlanRepository;
    private final UserFoodPreferenceService preferenceService; // 추가

    @Value("${ai.server.url:http://localhost:8000}")
    private String aiServerUrl;

    @Value("${ai.server.api-key:}")
    private String aiServerApiKey;

    /**
     * 음식 이미지 분석 요청 (AI 서버)
     *
     * @param image 음식 이미지 파일
     * @return AI가 분석한 음식 정보 리스트
     */
    public List<MealDto.FoodItemRequest> analyzeFoodImage(MultipartFile image) {
        try {
            String endpoint = aiServerUrl + "/api/analyze-food";

            // HTTP 헤더 설정
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.MULTIPART_FORM_DATA);
            if (!aiServerApiKey.isEmpty()) {
                headers.set("X-API-Key", aiServerApiKey);
            }

            // 멀티파트 요청 생성
            // MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
            // body.add("image", image.getResource());

            // HttpEntity<MultiValueMap<String, Object>> requestEntity = new HttpEntity<>(body, headers);

            // AI 서버 호출
            // ResponseEntity<AIFoodAnalysisResponse> response = restTemplate.exchange(
            //     endpoint,
            //     HttpMethod.POST,
            //     requestEntity,
            //     AIFoodAnalysisResponse.class
            // );

            // TODO: AI 서버의 실제 응답 형식에 맞춰 파싱
            // 여기서는 예시 데이터 반환
            log.info("AI 서버 음식 이미지 분석 요청 - 파일명: {}", image.getOriginalFilename());

            return createSampleFoodAnalysisResult();

        } catch (Exception e) {
            log.error("AI 서버 음식 분석 실패: {}", e.getMessage(), e);
            throw new BusinessException(ErrorCode.INTERNAL_SERVER_ERROR,
                    "음식 이미지 분석 중 오류가 발생했습니다");
        }
    }

    /**
     * AI 추천 식단 요청 (선호 음식 데이터 포함)
     *
     * @param userPrincipal 사용자 정보
     * @return AI가 추천한 식단 플랜
     */
    @Transactional
    public RecommendedMealPlan requestRecommendedMealPlan(CustomUserPrincipal userPrincipal) {
        try {
            User user = userRepository.findByUserId(userPrincipal.getUserId())
                    .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

            String endpoint = aiServerUrl + "/api/recommend-meal-plan";

            // HTTP 헤더 설정
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            if (!aiServerApiKey.isEmpty()) {
                headers.set("X-API-Key", aiServerApiKey);
            }

            // 사용자 선호 음식 데이터 조회
            UserFoodPreferenceDto.PreferenceDataForAI preferenceData =
                    preferenceService.getPreferenceDataForAI(userPrincipal);

            // 요청 바디 생성 (사용자 정보 + 선호 음식)
            Map<String, Object> requestBody = buildRecommendationRequest(user, preferenceData);

            HttpEntity<Map<String, Object>> requestEntity = new HttpEntity<>(requestBody, headers);

            // AI 서버 호출
            // ResponseEntity<AIRecommendedMealPlanResponse> response = restTemplate.exchange(
            //     endpoint,
            //     HttpMethod.POST,
            //     requestEntity,
            //     AIRecommendedMealPlanResponse.class
            // );

            // TODO: AI 서버의 실제 응답 형식에 맞춰 파싱
            // 여기서는 예시 데이터로 RecommendedMealPlan 생성
            log.info("AI 서버 추천 식단 요청 - 사용자 ID: {}, 선호 음식 포함", user.getUserId());
            log.debug("선호 데이터 - 좋아하는 음식: {}, 싫어하는 음식: {}",
                    preferenceData.getLikedFoods().size(),
                    preferenceData.getDislikedFoods().size());

            RecommendedMealPlan plan = createSampleRecommendedMealPlan(user);
            return recommendedMealPlanRepository.save(plan);

        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("AI 서버 추천 식단 요청 실패: {}", e.getMessage(), e);
            throw new BusinessException(ErrorCode.INTERNAL_SERVER_ERROR,
                    "추천 식단 생성 중 오류가 발생했습니다");
        }
    }

    // ========== Private Helper Methods ==========

    /**
     * AI 추천 요청 바디 생성 (선호 음식 데이터 포함)
     */
    private Map<String, Object> buildRecommendationRequest(
            User user,
            UserFoodPreferenceDto.PreferenceDataForAI preferenceData) {

        Map<String, Object> requestBody = new HashMap<>();

        // 기본 사용자 정보
        requestBody.put("userId", user.getId());
        requestBody.put("age", calculateAge(user));
        requestBody.put("gender", user.getGender());
        requestBody.put("weight", user.getWeight());
        requestBody.put("height", user.getHeight());
        requestBody.put("healthGoal", user.getHealthGoal());
        requestBody.put("workoutDaysPerWeek", user.getWorkoutDaysPerWeek());

        // 선호 음식 데이터 (신규)
        Map<String, Object> preferences = new HashMap<>();
        preferences.put("likedFoods", preferenceData.getLikedFoods());
        preferences.put("dislikedFoods", preferenceData.getDislikedFoods());
        preferences.put("frequentFoods", preferenceData.getFrequentFoods());
        preferences.put("preferredCategories", preferenceData.getPreferredCategories());
        preferences.put("preferredTags", preferenceData.getPreferredTags());

        requestBody.put("preferences", preferences);

        return requestBody;
    }

    private int calculateAge(User user) {
        if (user.getBirthDate() == null) {
            return 30;
        }
        return java.time.Period.between(user.getBirthDate(), java.time.LocalDate.now()).getYears();
    }

    /**
     * 샘플 음식 분석 결과 생성 (AI 서버 연동 전 테스트용)
     */
    private List<MealDto.FoodItemRequest> createSampleFoodAnalysisResult() {
        List<MealDto.FoodItemRequest> foods = new ArrayList<>();

        foods.add(MealDto.FoodItemRequest.builder()
                .foodName("닭가슴살")
                .servingSize(new BigDecimal("100"))
                .calories(new BigDecimal("165"))
                .carbs(new BigDecimal("0"))
                .protein(new BigDecimal("31"))
                .fat(new BigDecimal("3.6"))
                .sodium(new BigDecimal("74"))
                .aiConfidenceScore(new BigDecimal("95.5"))
                .build());

        foods.add(MealDto.FoodItemRequest.builder()
                .foodName("현미밥")
                .servingSize(new BigDecimal("150"))
                .calories(new BigDecimal("180"))
                .carbs(new BigDecimal("38"))
                .protein(new BigDecimal("4"))
                .fat(new BigDecimal("1"))
                .aiConfidenceScore(new BigDecimal("92.3"))
                .build());

        return foods;
    }

    /**
     * 샘플 추천 식단 플랜 생성 (AI 서버 연동 전 테스트용)
     */
    private RecommendedMealPlan createSampleRecommendedMealPlan(User user) {
        RecommendedMealPlan plan = RecommendedMealPlan.builder()
                .user(user)
                .planName("다이어트를 위한 균형 잡힌 식단")
                .description("하루 1800kcal 목표의 저칼로리 고단백 식단입니다")
                .totalCalories(new BigDecimal("1800"))
                .totalCarbs(new BigDecimal("200"))
                .totalProtein(new BigDecimal("120"))
                .totalFat(new BigDecimal("60"))
                .recommendationReason("사용자의 목표 체중 감량에 최적화된 영양 비율입니다")
                .isSaved(false)
                .build();

        // 아침 식사
        RecommendedMeal breakfast = RecommendedMeal.builder()
                .mealType(Meal.MealType.BREAKFAST)
                .totalCalories(new BigDecimal("450"))
                .totalCarbs(new BigDecimal("50"))
                .totalProtein(new BigDecimal("30"))
                .totalFat(new BigDecimal("15"))
                .build();

        breakfast.addRecommendedFood(RecommendedFood.builder()
                .foodName("현미밥")
                .servingSize(new BigDecimal("150"))
                .calories(new BigDecimal("180"))
                .carbs(new BigDecimal("38"))
                .protein(new BigDecimal("4"))
                .fat(new BigDecimal("1"))
                .build());

        breakfast.addRecommendedFood(RecommendedFood.builder()
                .foodName("계란 2개")
                .servingSize(new BigDecimal("100"))
                .calories(new BigDecimal("155"))
                .carbs(new BigDecimal("1.1"))
                .protein(new BigDecimal("13"))
                .fat(new BigDecimal("11"))
                .build());

        plan.addRecommendedMeal(breakfast);

        // 점심 식사
        RecommendedMeal lunch = RecommendedMeal.builder()
                .mealType(Meal.MealType.LUNCH)
                .totalCalories(new BigDecimal("600"))
                .totalCarbs(new BigDecimal("70"))
                .totalProtein(new BigDecimal("40"))
                .totalFat(new BigDecimal("20"))
                .build();

        lunch.addRecommendedFood(RecommendedFood.builder()
                .foodName("닭가슴살")
                .servingSize(new BigDecimal("150"))
                .calories(new BigDecimal("248"))
                .carbs(new BigDecimal("0"))
                .protein(new BigDecimal("46.5"))
                .fat(new BigDecimal("5.4"))
                .build());

        plan.addRecommendedMeal(lunch);

        return plan;
    }

    /**
     * AI 서버 요청/응답 DTO 예시 (실제 AI 서버 스펙에 맞춰 수정 필요)
     */

    // ==================== 요청 DTO ====================
    /*
    private static class AIRecommendationRequest {
        private Long userId;
        private Integer age;
        private String gender;
        private BigDecimal weight;
        private BigDecimal height;
        private String healthGoal;
        private Integer workoutDaysPerWeek;
        private PreferenceData preferences;  // 신규 추가

        private static class PreferenceData {
            private List<String> likedFoods;
            private List<String> dislikedFoods;
            private List<String> frequentFoods;
            private List<String> preferredCategories;
            private List<String> preferredTags;
        }
    }
    */

    // ==================== 응답 DTO ====================
    /*
    private static class AIFoodAnalysisResponse {
        private List<FoodItem> foods;
        private String message;

        private static class FoodItem {
            private String name;
            private Double servingSize;
            private Double calories;
            private Double carbs;
            private Double protein;
            private Double fat;
            private Double confidenceScore;
        }
    }

    private static class AIRecommendedMealPlanResponse {
        private String planName;
        private String description;
        private List<MealPlan> meals;
        private NutritionSummary nutrition;
        private String recommendationReason;

        private static class MealPlan {
            private String mealType;
            private List<FoodItem> foods;
        }

        private static class NutritionSummary {
            private Double totalCalories;
            private Double totalCarbs;
            private Double totalProtein;
            private Double totalFat;
        }

        private static class FoodItem {
            private String name;
            private Double servingSize;
            private Double calories;
            private Double carbs;
            private Double protein;
            private Double fat;
        }
    }
    */
}