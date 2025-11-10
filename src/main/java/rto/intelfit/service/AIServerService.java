package rto.intelfit.service;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;
import rto.intelfit.domain.*;
import rto.intelfit.dto.MealDto;
import rto.intelfit.dto.UserFoodPreferenceDto;
import rto.intelfit.exception.BusinessException;
import rto.intelfit.exception.ErrorCode;
import rto.intelfit.repository.RecommendedMealPlanRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import org.springframework.web.util.UriComponentsBuilder;


import rto.intelfit.repository.UserRepository;
import rto.intelfit.security.CustomUserPrincipal;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.Period;
import java.util.*;
import java.util.stream.Collectors;

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

    // ✅ 옥텟 빠진 URL 고쳐서 넣어라 (예: http://43.200.40.140:8000)
    @Value("${ai.server.url:http://43.200.40.140:8000}")
    private String aiServerUrl;

    @Value("${ai.server.api-key:}")
    private String aiServerApiKey;

    // -----------------------------
    // 0) 공통 헬퍼
    // -----------------------------
    private HttpHeaders jsonHeaders() {
        HttpHeaders h = new HttpHeaders();
        h.setContentType(MediaType.APPLICATION_JSON);
        if (aiServerApiKey != null && !aiServerApiKey.isBlank()) {
            h.set("Authorization", "Bearer " + aiServerApiKey);
        }
        return h;
    }

    private HttpHeaders multipartHeaders() {
        HttpHeaders h = new HttpHeaders();
        h.setContentType(MediaType.MULTIPART_FORM_DATA);
        if (aiServerApiKey != null && !aiServerApiKey.isBlank()) {
            h.set("Authorization", "Bearer " + aiServerApiKey);
        }
        return h;
    }

    private int calcAge(LocalDate birthDate) {
        return (birthDate == null) ? 30 : Period.between(birthDate, LocalDate.now()).getYears();
    }

    private String mapSex(User.Gender gender) {
        if (gender == null) return "male";
        return (gender == User.Gender.F) ? "female" : "male";
    }

    private String mapGoal(User.HealthGoal goal) {
        if (goal == null) return "maintenance";
        switch (goal) {
            case DIET:        return "fat_loss";
            case BULK:        // 의도에 맞게 통일
            case MUSCLE_GAIN:
            case LEAN_MASS:   return "hypertrophy";
            case MAINTENANCE: default: return "maintenance";
        }
    }

    private String mapActivityLevel(Integer workoutDaysPerWeek) {
        if (workoutDaysPerWeek == null) return "MODERATE";
        if (workoutDaysPerWeek <= 1) return "SEDENTARY";
        if (workoutDaysPerWeek <= 3) return "LIGHT";
        if (workoutDaysPerWeek <= 5) return "MODERATE";
        return "VERY_ACTIVE";
    }

    // -----------------------------
    // 1) 회원가입 직후 AI 서버 사용자 동기화
    //    FastAPI의 사용자 생성 엔드포인트 경로에 맞춰 path 바꿔라.
    //    현재 FastAPI router가 prefix 없이 @router.post("/create") 이면 "/create"가 맞다.
    // -----------------------------
    @Transactional
    public void createUserOnAI(User user) {
        // FastAPI가 /create 라우트라면:
        String endpoint = aiServerUrl + "/user/create";
        // 만약 prefix "/users"가 붙어 있다면: String endpoint = aiServerUrl + "/users/create";

        AIUserCreateRequest payload = AIUserCreateRequest.builder()
                .id(user.getUserId())
                .name(user.getName())
                .age(calcAge(user.getBirthDate()))
                .sex(mapSex(user.getGender())) // "male"/"female"
                .height(user.getHeight() == null ? null : user.getHeight().floatValue())
                .weight(user.getWeight() == null ? null : user.getWeight().floatValue())
                .body_fat(null)
                .skeletal_muscle(null) //체지방률과 인바디 정보는 회원가입시 자동 업로드 x
                .activity_level(1.2f)
                .goal(mapGoal(user.getHealthGoal()))
                .build();

        try {
            // 요청 바디 디버그 출력 (직렬화 실패는 개별 처리)
            if (log.isDebugEnabled()) {
                try {
                    log.debug("AI user create payload: {}", objectMapper.writeValueAsString(payload));
                } catch (JsonProcessingException jpe) {
                    log.debug("AI user create payload serialization failed; printing as object. payload={}", payload, jpe);
                }
            }

            ResponseEntity<String> res = restTemplate.exchange(
                    endpoint,
                    HttpMethod.POST,
                    new HttpEntity<>(payload, jsonHeaders()),
                    String.class
            );

            log.info("AI user sync: status={}, body={}", res.getStatusCode(), res.getBody());
        } catch (HttpClientErrorException e) {
            log.warn("AI user sync 4xx/5xx: status={}, body={}", e.getStatusCode(), e.getResponseBodyAsString(), e);
            throw e; // 여기서는 런타임 예외라 그대로 던져도 OK
        } catch (Exception e) {
            log.error("AI user sync failed: {}", e.getMessage(), e);
            throw new RuntimeException(e); // ★ 체크 예외를 런타임으로 래핑
        }

    }

    // -----------------------------
    // 2) 음식 이미지 분석
    // -----------------------------
    public List<MealDto.FoodItemRequest> analyzeFoodImage(MultipartFile image) {
        try {
            String endpoint = aiServerUrl + "/api/v1/food/analyze";

            MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
            ByteArrayResource fileResource = new ByteArrayResource(image.getBytes()) {
                @Override public String getFilename() { return image.getOriginalFilename(); }
            };
            body.add("file", fileResource);

            ResponseEntity<FoodAnalysisResponse> response = restTemplate.exchange(
                    endpoint, HttpMethod.POST,
                    new HttpEntity<>(body, multipartHeaders()),
                    FoodAnalysisResponse.class
            );

            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                log.info("음식 이미지 분석 성공 - 파일명: {}, 개수: {}",
                        image.getOriginalFilename(),
                        response.getBody().getFoods() == null ? 0 : response.getBody().getFoods().size());
                return convertToFoodItemRequests(response.getBody());
            }

            log.warn("음식 이미지 분석 실패 - 응답 상태: {}", response.getStatusCode());
            return createSampleFoodAnalysisResult();

        } catch (Exception e) {
            log.error("FastAPI 음식 분석 실패: {}", e.getMessage(), e);
            return createSampleFoodAnalysisResult();
        }
    }

    // -----------------------------
    // 3) 추천 식단 요청
    // -----------------------------
// 3) 추천 식단 요청  (기존 POST /api/v1/meal/recommend -> AI의 GET /generate_daily_plan로 변경)
    @Transactional
    // AIServerService.java 안, 기존 requestRecommendedMealPlan(...) 전체 교체


    public RecommendedMealPlan requestRecommendedMealPlan(CustomUserPrincipal userPrincipal) {
        User user = userRepository.findByUserId(userPrincipal.getUserId())
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        try {
            // FastAPI 스펙: POST /recommend/recommend_daily_meal
            // params: user_id, meals_per_day(기본 3), goal(diet|bulk|lean|maintain)
            String goalParam = mapGoalForRecommender(user.getHealthGoal());

            UriComponentsBuilder ub = UriComponentsBuilder
                    .fromHttpUrl(aiServerUrl + "/recommend/recommend_daily_meal")
                    .queryParam("user_id", user.getUserId())
                    .queryParam("meals_per_day", 3)
                    .queryParam("goal", goalParam);

            // preferred_foods / excluded_foods 는 현재 백엔드에 ID 정보가 없으므로 생략
            String url = ub.toUriString();

            // POST 이지만 본문은 비우고, 쿼리스트링으로 전달 (FastAPI 시그니처와 합치)
            ResponseEntity<Map> response = restTemplate.exchange(
                    url,
                    HttpMethod.POST,
                    new HttpEntity<>(jsonHeaders()),
                    Map.class
            );

            if (response.getStatusCode() != HttpStatus.OK || response.getBody() == null) {
                log.warn("AI 추천 실패 - status={}, body={}", response.getStatusCode(), response.getBody());
                return recommendedMealPlanRepository.save(createSampleRecommendedMealPlan(user));
            }

            Map<String, Object> body = response.getBody();

            // ------ AI 응답 해석 (recommendation.py 기준) ------
            // {
            //   "date": "2025-11-09",
            //   "user_id": "user030",
            //   "goal": "diet",
            //   "meals_per_day": 3,
            //   "target_daily_calories": 1987.5,
            //   "target_protein": 170.0,
            //   "target_fat": 55.0,
            //   "target_carbs": 210.0,
            //   "meals": [
            //     {
            //       "meal_type": "meal_1",
            //       "target_calories": ...,
            //       "actual_calories": ...,
            //       "target_protein": ...,
            //       "actual_protein": ...,
            //       "target_fat": ...,
            //       "actual_fat": ...,
            //       "target_carbs": ...,
            //       "actual_carbs": ...,
            //       "foods": [ { "id": ..., "name": "...", "calories": ..., "protein": ..., "fat": ..., "carbs": ... }, ... ]
            //     }, ...
            //   ]
            // }

            BigDecimal totalKcal = toBD(body.get("target_daily_calories"));  // 목표 기준으로 저장
            // 실제 합계가 필요한 경우: meals[].actual_* 합산해서 교체해도 됨

            List<Map<String, Object>> meals = safeListMap(body.get("meals"));

            RecommendedMealPlan plan = RecommendedMealPlan.builder()
                    .user(user)
                    .planName("AI Daily Plan")
                    .description("AI 서버에서 생성한 하루 식단")
                    .totalCalories(totalKcal)
                    .totalCarbs(toBD(body.get("target_carbs")))
                    .totalProtein(toBD(body.get("target_protein")))
                    .totalFat(toBD(body.get("target_fat")))
                    .recommendationReason("사용자 프로필과 목표 기반 자동 생성")
                    .isSaved(false)
                    .build();

            for (Map<String, Object> m : meals) {
                String type = str(m.get("meal_type")); // meal_1, meal_2, ...
                RecommendedMeal rm = RecommendedMeal.builder()
                        .mealType(mapMealTypeGuess(type)) // 아래 보조 매핑 사용
                        .totalCalories(toBD(m.get("actual_calories")))
                        .totalCarbs(toBD(m.get("actual_carbs")))
                        .totalProtein(toBD(m.get("actual_protein")))
                        .totalFat(toBD(m.get("actual_fat")))
                        .build();


                List<Map<String, Object>> foods = safeListMap(m.get("foods"));
                for (Map<String, Object> f : foods) {
                    BigDecimal serving = toBD(f.get("serving_size")); // 응답에 없으면 ZERO

                    if (serving == null || BigDecimal.ZERO.compareTo(serving) == 0) {
                        serving = new BigDecimal("100");
                    }
                    rm.addRecommendedFood(RecommendedFood.builder()
                            .foodName(str(f.get("name")))
                            .servingSize(serving) // AI 응답에 g 정보가 없으니 비움(추후 확장)
                            .calories(toBD(f.get("calories")))
                            .carbs(toBD(f.get("carbs")))
                            .protein(toBD(f.get("protein")))
                            .fat(toBD(f.get("fat")))
                            .build());
                }
                plan.addRecommendedMeal(rm);
            }

            log.info("AI 일간 식단 생성 성공 - userId={}, goal={}", user.getUserId(), goalParam);
            return recommendedMealPlanRepository.save(plan);

        } catch (HttpClientErrorException e) {
            log.warn("AI 추천 4xx/5xx - status={}, body={}", e.getStatusCode(), e.getResponseBodyAsString(), e);
            return recommendedMealPlanRepository.save(createSampleRecommendedMealPlan(user));
        } catch (Exception e) {
            log.error("AI 추천 호출 실패: {}", e.getMessage(), e);
            return recommendedMealPlanRepository.save(createSampleRecommendedMealPlan(user));
        }
    }
    // AIServerService.java 하단 보조 메서드 추가

    private Meal.MealType mapMealTypeGuess(String mealTypeFromAI) {
        String s = (mealTypeFromAI == null) ? "" : mealTypeFromAI.toLowerCase(Locale.ROOT);
        if (s.contains("1")) return Meal.MealType.BREAKFAST;
        if (s.contains("2")) return Meal.MealType.LUNCH;
        if (s.contains("3")) return Meal.MealType.DINNER;
        // 그 외는 간식 처리
        return Meal.MealType.SNACK;
    }
    @SuppressWarnings("unchecked")
    private Map<String, Object> safeMap(Object o) {
        return (o instanceof Map) ? (Map<String, Object>) o : Collections.emptyMap();
    }
    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> safeListMap(Object o) {
        return (o instanceof List) ? (List<Map<String, Object>>) o : Collections.emptyList();
    }
    private String str(Object o) { return o == null ? "" : String.valueOf(o); }
    private BigDecimal toBD(Object o) {
        if (o == null) return BigDecimal.ZERO;
        try { return new BigDecimal(String.valueOf(o)); } catch (Exception ignore) { return BigDecimal.ZERO; }
    }
    // AIServerService.java 안

    private String mapGoalForRecommender(User.HealthGoal goal) {
        if (goal == null) return "maintain";
        switch (goal) {
            case DIET:        return "diet";
            case BULK:
            case MUSCLE_GAIN: return "bulk";
            case LEAN_MASS:   return "lean";
            case MAINTENANCE:
            default:          return "maintain";
        }
    }






    // -----------------------------
    // 4) 변환/샘플 헬퍼
    // -----------------------------
    private List<MealDto.FoodItemRequest> convertToFoodItemRequests(FoodAnalysisResponse response) {
        if (response.getFoods() == null) return Collections.emptyList();
        return response.getFoods().stream()
                .map(food -> MealDto.FoodItemRequest.builder()
                        .foodName(food.getName())
                        .servingSize(BigDecimal.valueOf(nz(food.getServingSize())))
                        .calories(BigDecimal.valueOf(nz(food.getCalories())))
                        .carbs(BigDecimal.valueOf(nz(food.getCarbs())))
                        .protein(BigDecimal.valueOf(nz(food.getProtein())))
                        .fat(BigDecimal.valueOf(nz(food.getFat())))
                        .sodium(BigDecimal.valueOf(nz(food.getSodium())))
                        .sugar(BigDecimal.valueOf(nz(food.getSugar())))
                        .fiber(BigDecimal.valueOf(nz(food.getFiber())))
                        .aiConfidenceScore(BigDecimal.valueOf(nz(food.getConfidenceScore())))
                        .build())
                .collect(Collectors.toList());
    }

    private double nz(Double v) { return v == null ? 0.0 : v; }

    private RecommendedMealPlan convertToRecommendedMealPlan(MealRecommendationResponse res, User user) {
        RecommendedMealPlan plan = RecommendedMealPlan.builder()
                .user(user)
                .planName(res.getPlanName())
                .description(res.getDescription())
                .totalCalories(BigDecimal.valueOf(nz(res.getNutrition().getTotalCalories())))
                .totalCarbs(BigDecimal.valueOf(nz(res.getNutrition().getTotalCarbs())))
                .totalProtein(BigDecimal.valueOf(nz(res.getNutrition().getTotalProtein())))
                .totalFat(BigDecimal.valueOf(nz(res.getNutrition().getTotalFat())))
                .recommendationReason(res.getRecommendationReason())
                .isSaved(false)
                .build();

        if (res.getMeals() != null) {
            for (MealRecommendationResponse.MealPlan mp : res.getMeals()) {
                RecommendedMeal meal = RecommendedMeal.builder()
                        .mealType(mapMealType(mp.getMealType()))
                        .totalCalories(BigDecimal.valueOf(nz(mp.getTotalCalories())))
                        .totalCarbs(BigDecimal.valueOf(nz(mp.getTotalCarbs())))
                        .totalProtein(BigDecimal.valueOf(nz(mp.getTotalProtein())))
                        .totalFat(BigDecimal.valueOf(nz(mp.getTotalFat())))
                        .build();

                if (mp.getFoods() != null) {
                    for (MealRecommendationResponse.FoodItem f : mp.getFoods()) {
                        meal.addRecommendedFood(RecommendedFood.builder()
                                .foodName(f.getName())
                                .servingSize(BigDecimal.valueOf(nz(f.getServingSize())))
                                .calories(BigDecimal.valueOf(nz(f.getCalories())))
                                .carbs(BigDecimal.valueOf(nz(f.getCarbs())))
                                .protein(BigDecimal.valueOf(nz(f.getProtein())))
                                .fat(BigDecimal.valueOf(nz(f.getFat())))
                                .build());
                    }
                }
                plan.addRecommendedMeal(meal);
            }
        }
        return plan;
    }

    private Meal.MealType mapMealType(String mealType) {
        try { return Meal.MealType.valueOf(mealType.toUpperCase()); }
        catch (Exception e) { return Meal.MealType.SNACK; }
    }

    private List<MealDto.FoodItemRequest> createSampleFoodAnalysisResult() {
        List<MealDto.FoodItemRequest> foods = new ArrayList<>();
        foods.add(MealDto.FoodItemRequest.builder()
                .foodName("닭가슴살").servingSize(new BigDecimal("100"))
                .calories(new BigDecimal("165")).carbs(new BigDecimal("0"))
                .protein(new BigDecimal("31")).fat(new BigDecimal("3.6"))
                .sodium(new BigDecimal("74")).aiConfidenceScore(new BigDecimal("95.5"))
                .build());
        foods.add(MealDto.FoodItemRequest.builder()
                .foodName("현미밥").servingSize(new BigDecimal("150"))
                .calories(new BigDecimal("180")).carbs(new BigDecimal("38"))
                .protein(new BigDecimal("4")).fat(new BigDecimal("1"))
                .aiConfidenceScore(new BigDecimal("92.3"))
                .build());
        return foods;
    }

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
        RecommendedMeal breakfast = RecommendedMeal.builder()
                .mealType(Meal.MealType.BREAKFAST)
                .totalCalories(new BigDecimal("500"))
                .totalCarbs(new BigDecimal("60"))
                .totalProtein(new BigDecimal("25"))
                .totalFat(new BigDecimal("15"))
                .build();
        breakfast.addRecommendedFood(RecommendedFood.builder()
                .foodName("통밀빵 토스트").servingSize(new BigDecimal("60"))
                .calories(new BigDecimal("150")).carbs(new BigDecimal("28"))
                .protein(new BigDecimal("6")).fat(new BigDecimal("2"))
                .build());
        plan.addRecommendedMeal(breakfast);
        return plan;
    }

    // -----------------------------
    // 5) 내부 DTO (한 파일 내 정리)
    // -----------------------------
    @Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
    private static class AIUserCreateRequest {
        private String id;
        private String name;
        private Integer age;
        private String sex;            // "male" | "female"
        private Float height;
        private Float weight;
        private Float body_fat;
        private Float skeletal_muscle;
        private Float activity_level;  // ex) 1.2
        private String goal;           // "fat_loss" | "hypertrophy" | "maintenance"
    }

    @Data @NoArgsConstructor @AllArgsConstructor
    public static class FoodAnalysisResponse {
        private List<FoodItem> foods;
        private String message;
        private boolean success;

        @Data @NoArgsConstructor @AllArgsConstructor
        public static class FoodItem {
            private String name;
            @JsonProperty("serving_size") private Double servingSize;
            private Double calories;
            private Double carbs;
            private Double protein;
            private Double fat;
            private Double sodium;
            private Double sugar;
            private Double fiber;
            @JsonProperty("confidence_score") private Double confidenceScore;
        }
    }

    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class MealRecommendationRequest {
        @JsonProperty("user_id") private String userId;
        private Integer age;
        private String gender;
        private Double weight;
        private Double height;
        @JsonProperty("activity_level") private String activityLevel;
        @JsonProperty("health_goal") private String healthGoal;
        private PreferenceData preferences;

        @Data @Builder @NoArgsConstructor @AllArgsConstructor
        public static class PreferenceData {
            @JsonProperty("liked_foods") private List<String> likedFoods;
            @JsonProperty("disliked_foods") private List<String> dislikedFoods;
            @JsonProperty("frequent_foods") private List<String> frequentFoods;
            @JsonProperty("preferred_categories") private List<String> preferredCategories;
            @JsonProperty("preferred_tags") private List<String> preferredTags;
        }
    }

    @Data @NoArgsConstructor @AllArgsConstructor
    public static class MealRecommendationResponse {
        @JsonProperty("plan_name") private String planName;
        private String description;
        private List<MealPlan> meals;
        private NutritionSummary nutrition;
        @JsonProperty("recommendation_reason") private String recommendationReason;
        private boolean success;
        private String message;

        @Data @NoArgsConstructor @AllArgsConstructor
        public static class MealPlan {
            @JsonProperty("meal_type") private String mealType;
            private List<FoodItem> foods;
            @JsonProperty("total_calories") private Double totalCalories;
            @JsonProperty("total_carbs") private Double totalCarbs;
            @JsonProperty("total_protein") private Double totalProtein;
            @JsonProperty("total_fat") private Double totalFat;
        }

        @Data @NoArgsConstructor @AllArgsConstructor
        public static class NutritionSummary {
            @JsonProperty("total_calories") private Double totalCalories;
            @JsonProperty("total_carbs") private Double totalCarbs;
            @JsonProperty("total_protein") private Double totalProtein;
            @JsonProperty("total_fat") private Double totalFat;
        }

        @Data @NoArgsConstructor @AllArgsConstructor
        public static class FoodItem {
            private String name;
            @JsonProperty("serving_size") private Double servingSize;
            private Double calories;
            private Double carbs;
            private Double protein;
            private Double fat;
        }
    }
}
