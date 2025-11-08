package rto.intelfit.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
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

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.Period;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Fast API 서버와 통신하는 서비스
 *
 * 주요 기능:
 * 1. 음식 이미지를 Fast API 서버에 전송하여 영양 성분 분석
 * 2. 사용자 정보를 기반으로 Fast API 서버에서 추천 식단 받아오기
 * 3. 사용자 선호 음식 데이터를 AI 추천에 포함
 * 4. Fast API 서버 응답을 도메인 객체로 변환
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AIServerService {

    private final RestTemplate restTemplate;
    private final UserRepository userRepository;
    private final RecommendedMealPlanRepository recommendedMealPlanRepository;
    private final UserFoodPreferenceService preferenceService;
    private final ObjectMapper objectMapper;

    @Value("${ai.server.url:http://localhost:8000}")
    private String aiServerUrl;

    @Value("${ai.server.api-key:}")
    private String aiServerApiKey;

    /**
     * 음식 이미지 분석 요청 (Fast API 서버)
     *
     * @param image 음식 이미지 파일
     * @return AI가 분석한 음식 정보 리스트
     */
    public List<MealDto.FoodItemRequest> analyzeFoodImage(MultipartFile image) {
        try {
            String endpoint = aiServerUrl + "/api/v1/food/analyze";

            // HTTP 헤더 설정
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.MULTIPART_FORM_DATA);
            if (!aiServerApiKey.isEmpty()) {
                headers.set("Authorization", "Bearer " + aiServerApiKey);
            }

            // 멀티파트 요청 생성
            MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();

            // ByteArrayResource를 사용하여 파일 데이터 전송
            ByteArrayResource fileResource = new ByteArrayResource(image.getBytes()) {
                @Override
                public String getFilename() {
                    return image.getOriginalFilename();
                }
            };
            body.add("file", fileResource);

            HttpEntity<MultiValueMap<String, Object>> requestEntity = new HttpEntity<>(body, headers);

            // Fast API 서버 호출
            ResponseEntity<FoodAnalysisResponse> response = restTemplate.exchange(
                    endpoint,
                    HttpMethod.POST,
                    requestEntity,
                    FoodAnalysisResponse.class
            );

            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                log.info("음식 이미지 분석 성공 - 파일명: {}, 분석된 음식 개수: {}",
                        image.getOriginalFilename(),
                        response.getBody().getFoods().size());

                return convertToFoodItemRequests(response.getBody());
            } else {
                log.warn("음식 이미지 분석 실패 - 응답 상태: {}", response.getStatusCode());
                return createSampleFoodAnalysisResult();
            }

        } catch (Exception e) {
            log.error("Fast API 서버 음식 분석 실패: {}", e.getMessage(), e);
            // 실패 시 샘플 데이터 반환 (개발 중 fallback)
            return createSampleFoodAnalysisResult();
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

            String endpoint = aiServerUrl + "/api/v1/meal/recommend";

            // HTTP 헤더 설정
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            if (!aiServerApiKey.isEmpty()) {
                headers.set("Authorization", "Bearer " + aiServerApiKey);
            }

            // 사용자 선호 음식 데이터 조회
            UserFoodPreferenceDto.PreferenceDataForAI preferenceData =
                    preferenceService.getPreferenceDataForAI(userPrincipal);

            // 요청 바디 생성
            MealRecommendationRequest requestBody = buildRecommendationRequest(user, preferenceData);

            HttpEntity<MealRecommendationRequest> requestEntity = new HttpEntity<>(requestBody, headers);

            // Fast API 서버 호출
            ResponseEntity<MealRecommendationResponse> response = restTemplate.exchange(
                    endpoint,
                    HttpMethod.POST,
                    requestEntity,
                    MealRecommendationResponse.class
            );

            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                log.info("AI 추천 식단 생성 성공 - 사용자: {}", user.getUserId());

                RecommendedMealPlan plan = convertToRecommendedMealPlan(response.getBody(), user);
                return recommendedMealPlanRepository.save(plan);
            } else {
                log.warn("AI 추천 식단 생성 실패 - 응답 상태: {}", response.getStatusCode());
                RecommendedMealPlan plan = createSampleRecommendedMealPlan(user);
                return recommendedMealPlanRepository.save(plan);
            }

        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("Fast API 서버 추천 식단 요청 실패: {}", e.getMessage(), e);
            // 실패 시 샘플 데이터 반환 (개발 중 fallback)
            User user = userRepository.findByUserId(userPrincipal.getUserId())
                    .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
            RecommendedMealPlan plan = createSampleRecommendedMealPlan(user);
            return recommendedMealPlanRepository.save(plan);
        }
    }

    // ========== Private Helper Methods ==========

    /**
     * Fast API 추천 요청 객체 생성
     */
    private MealRecommendationRequest buildRecommendationRequest(
            User user,
            UserFoodPreferenceDto.PreferenceDataForAI preferenceData) {

        return MealRecommendationRequest.builder()
                .userId(user.getId())
                .age(calculateAge(user))
                .gender(user.getGender() != null ? user.getGender().toString() : "UNKNOWN")
                .weight(user.getWeight() != null ? user.getWeight().doubleValue() : 65.0)
                .height(user.getHeight() != null ? user.getHeight().doubleValue() : 170.0)
                .healthGoal(user.getHealthGoal() != null ? user.getHealthGoal().toString() : "MAINTAIN")
                .preferences(MealRecommendationRequest.PreferenceData.builder()
                        .likedFoods(preferenceData.getLikedFoods())
                        .dislikedFoods(preferenceData.getDislikedFoods())
                        .frequentFoods(preferenceData.getFrequentFoods())
                        .preferredCategories(preferenceData.getPreferredCategories())
                        .preferredTags(preferenceData.getPreferredTags())
                        .build())
                .build();
    }

    /**
     * 운동 빈도를 활동 수준으로 변환
     */
    private String mapActivityLevel(Integer workoutDaysPerWeek) {
        if (workoutDaysPerWeek == null) return "MODERATE";
        if (workoutDaysPerWeek <= 1) return "SEDENTARY";
        if (workoutDaysPerWeek <= 3) return "LIGHT";
        if (workoutDaysPerWeek <= 5) return "MODERATE";
        return "VERY_ACTIVE";
    }

    /**
     * 나이 계산
     */
    private int calculateAge(User user) {
        if (user.getBirthDate() == null) {
            return 30;
        }
        return Period.between(user.getBirthDate(), LocalDate.now()).getYears();
    }

    /**
     * Fast API 음식 분석 응답을 DTO로 변환
     */
    private List<MealDto.FoodItemRequest> convertToFoodItemRequests(FoodAnalysisResponse response) {
        return response.getFoods().stream()
                .map(food -> MealDto.FoodItemRequest.builder()
                        .foodName(food.getName())
                        .servingSize(BigDecimal.valueOf(food.getServingSize()))
                        .calories(BigDecimal.valueOf(food.getCalories()))
                        .carbs(BigDecimal.valueOf(food.getCarbs()))
                        .protein(BigDecimal.valueOf(food.getProtein()))
                        .fat(BigDecimal.valueOf(food.getFat()))
                        .sodium(food.getSodium() != null ? BigDecimal.valueOf(food.getSodium()) : BigDecimal.ZERO)
                        .sugar(food.getSugar() != null ? BigDecimal.valueOf(food.getSugar()) : BigDecimal.ZERO)
                        .fiber(food.getFiber() != null ? BigDecimal.valueOf(food.getFiber()) : BigDecimal.ZERO)
                        .aiConfidenceScore(BigDecimal.valueOf(food.getConfidenceScore()))
                        .build())
                .collect(Collectors.toList());
    }

    /**
     * Fast API 추천 응답을 도메인 객체로 변환
     */
    private RecommendedMealPlan convertToRecommendedMealPlan(MealRecommendationResponse response, User user) {
        RecommendedMealPlan plan = RecommendedMealPlan.builder()
                .user(user)
                .planName(response.getPlanName())
                .description(response.getDescription())
                .totalCalories(BigDecimal.valueOf(response.getNutrition().getTotalCalories()))
                .totalCarbs(BigDecimal.valueOf(response.getNutrition().getTotalCarbs()))
                .totalProtein(BigDecimal.valueOf(response.getNutrition().getTotalProtein()))
                .totalFat(BigDecimal.valueOf(response.getNutrition().getTotalFat()))
                .recommendationReason(response.getRecommendationReason())
                .isSaved(false)
                .build();

        // 각 끼니별 식사 추가
        for (MealRecommendationResponse.MealPlan mealPlan : response.getMeals()) {
            RecommendedMeal meal = RecommendedMeal.builder()
                    .mealType(mapMealType(mealPlan.getMealType()))
                    .totalCalories(BigDecimal.valueOf(mealPlan.getTotalCalories()))
                    .totalCarbs(BigDecimal.valueOf(mealPlan.getTotalCarbs()))
                    .totalProtein(BigDecimal.valueOf(mealPlan.getTotalProtein()))
                    .totalFat(BigDecimal.valueOf(mealPlan.getTotalFat()))
                    .build();

            // 각 음식 추가
            for (MealRecommendationResponse.FoodItem foodItem : mealPlan.getFoods()) {
                meal.addRecommendedFood(RecommendedFood.builder()
                        .foodName(foodItem.getName())
                        .servingSize(BigDecimal.valueOf(foodItem.getServingSize()))
                        .calories(BigDecimal.valueOf(foodItem.getCalories()))
                        .carbs(BigDecimal.valueOf(foodItem.getCarbs()))
                        .protein(BigDecimal.valueOf(foodItem.getProtein()))
                        .fat(BigDecimal.valueOf(foodItem.getFat()))
                        .build());
            }

            plan.addRecommendedMeal(meal);
        }

        return plan;
    }

    /**
     * 식사 타입 문자열을 Enum으로 변환
     */
    private Meal.MealType mapMealType(String mealType) {
        try {
            return Meal.MealType.valueOf(mealType.toUpperCase());
        } catch (IllegalArgumentException e) {
            log.warn("알 수 없는 식사 타입: {}", mealType);
            return Meal.MealType.SNACK;
        }
    }

    /**
     * 샘플 음식 분석 결과 생성 (Fast API 서버 연동 실패 시 fallback)
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
     * 샘플 추천 식단 플랜 생성 (Fast API 서버 연동 실패 시 fallback)
     */
    private RecommendedMealPlan createSampleRecommendedMealPlan(User user) {
        RecommendedMealPlan plan = RecommendedMealPlan.builder()
                .user(user)
                .planName("균형 잡힌 하루 식단")
                .description("하루 2000kcal 목표의 균형 잡힌 영양 식단입니다")
                .totalCalories(new BigDecimal("2000"))
                .totalCarbs(new BigDecimal("250"))
                .totalProtein(new BigDecimal("100"))
                .totalFat(new BigDecimal("67"))
                .recommendationReason("사용자의 활동량과 목표에 최적화된 영양 비율입니다")
                .isSaved(false)
                .build();

        // 아침 식사
        RecommendedMeal breakfast = RecommendedMeal.builder()
                .mealType(Meal.MealType.BREAKFAST)
                .totalCalories(new BigDecimal("500"))
                .totalCarbs(new BigDecimal("60"))
                .totalProtein(new BigDecimal("25"))
                .totalFat(new BigDecimal("15"))
                .build();

        breakfast.addRecommendedFood(RecommendedFood.builder()
                .foodName("통밀빵 토스트")
                .servingSize(new BigDecimal("60"))
                .calories(new BigDecimal("150"))
                .carbs(new BigDecimal("28"))
                .protein(new BigDecimal("6"))
                .fat(new BigDecimal("2"))
                .build());

        breakfast.addRecommendedFood(RecommendedFood.builder()
                .foodName("스크램블 에그")
                .servingSize(new BigDecimal("100"))
                .calories(new BigDecimal("155"))
                .carbs(new BigDecimal("1.1"))
                .protein(new BigDecimal("13"))
                .fat(new BigDecimal("11"))
                .build());

        plan.addRecommendedMeal(breakfast);

        return plan;
    }

    // ========== Fast API Request/Response DTOs ==========

    /**
     * 음식 분석 응답 DTO
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class FoodAnalysisResponse {
        private List<FoodItem> foods;
        private String message;
        private boolean success;

        @Data
        @NoArgsConstructor
        @AllArgsConstructor
        public static class FoodItem {
            private String name;

            @JsonProperty("serving_size")
            private Double servingSize;

            private Double calories;
            private Double carbs;
            private Double protein;
            private Double fat;
            private Double sodium;
            private Double sugar;
            private Double fiber;

            @JsonProperty("confidence_score")
            private Double confidenceScore;
        }
    }

    /**
     * 식단 추천 요청 DTO
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MealRecommendationRequest {

        @JsonProperty("user_id")
        private Long userId;

        private Integer age;
        private String gender;
        private Double weight;
        private Double height;

        @JsonProperty("activity_level")
        private String activityLevel;

        @JsonProperty("health_goal")
        private String healthGoal;

        private PreferenceData preferences;

        @Data
        @Builder
        @NoArgsConstructor
        @AllArgsConstructor
        public static class PreferenceData {

            @JsonProperty("liked_foods")
            private List<String> likedFoods;

            @JsonProperty("disliked_foods")
            private List<String> dislikedFoods;

            @JsonProperty("frequent_foods")
            private List<String> frequentFoods;

            @JsonProperty("preferred_categories")
            private List<String> preferredCategories;

            @JsonProperty("preferred_tags")
            private List<String> preferredTags;
        }
    }

    /**
     * 식단 추천 응답 DTO
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MealRecommendationResponse {

        @JsonProperty("plan_name")
        private String planName;

        private String description;
        private List<MealPlan> meals;
        private NutritionSummary nutrition;

        @JsonProperty("recommendation_reason")
        private String recommendationReason;

        private boolean success;
        private String message;

        @Data
        @NoArgsConstructor
        @AllArgsConstructor
        public static class MealPlan {

            @JsonProperty("meal_type")
            private String mealType;

            private List<FoodItem> foods;

            @JsonProperty("total_calories")
            private Double totalCalories;

            @JsonProperty("total_carbs")
            private Double totalCarbs;

            @JsonProperty("total_protein")
            private Double totalProtein;

            @JsonProperty("total_fat")
            private Double totalFat;
        }

        @Data
        @NoArgsConstructor
        @AllArgsConstructor
        public static class NutritionSummary {

            @JsonProperty("total_calories")
            private Double totalCalories;

            @JsonProperty("total_carbs")
            private Double totalCarbs;

            @JsonProperty("total_protein")
            private Double totalProtein;

            @JsonProperty("total_fat")
            private Double totalFat;
        }

        @Data
        @NoArgsConstructor
        @AllArgsConstructor
        public static class FoodItem {
            private String name;

            @JsonProperty("serving_size")
            private Double servingSize;

            private Double calories;
            private Double carbs;
            private Double protein;
            private Double fat;
        }
    }
}