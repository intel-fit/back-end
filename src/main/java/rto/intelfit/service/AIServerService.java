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
import rto.intelfit.exception.BusinessException;
import rto.intelfit.exception.ErrorCode;
import rto.intelfit.repository.RecommendedMealPlanRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import org.springframework.web.util.UriComponentsBuilder;
import rto.intelfit.dto.RecommendedMealDto;
import rto.intelfit.dto.ExerciseFeedbackDto;


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
@Transactional
public class AIServerService {

    private final RestTemplate restTemplate;
    private final UserRepository userRepository;
    private final RecommendedMealPlanRepository recommendedMealPlanRepository;
    private final UserFoodPreferenceService preferenceService;
    private final ObjectMapper objectMapper;

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
            case BULK:
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
    // -----------------------------
    @Transactional
    public void createUserOnAI(User user) {
        String endpoint = aiServerUrl + "/user/create";

        AIUserCreateRequest payload = AIUserCreateRequest.builder()
                .id(user.getUserId())
                .name(user.getName())
                .age(calcAge(user.getBirthDate()))
                .sex(mapSex(user.getGender()))
                .height(user.getHeight() == null ? null : user.getHeight().floatValue())
                .weight(user.getWeight() == null ? null : user.getWeight().floatValue())
                .body_fat(null)
                .skeletal_muscle(null)
                .activity_level(1.2f)
                .goal(mapGoal(user.getHealthGoal()))
                .build();

        try {
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
            throw e;
        } catch (Exception e) {
            log.error("AI user sync failed: {}", e.getMessage(), e);
            throw new RuntimeException(e);
        }
    }
    //1.5 ai 서버로 피드백 전송
    public void sendExerciseFeedback(ExerciseFeedbackDto.Request request) {
        String url = aiServerUrl + "/exercise/feedback";

        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            HttpEntity<ExerciseFeedbackDto.Request> entity =
                    new HttpEntity<>(request, headers);

            ResponseEntity<String> response =
                    restTemplate.postForEntity(url, entity, String.class);

            log.info("✅ AI 운동 피드백 전송 완료 userId={}, sessionName={}, status={}",
                    request.getUser_id(), request.getSession_name(), response.getStatusCode());

        } catch (Exception e) {
            log.warn("⚠️ AI 운동 피드백 전송 실패 userId={}, sessionName={}",
                    request.getUser_id(), request.getSession_name(), e);
            // 여기서 예외를 다시 던지지 않는 이유:
            //  → AI 서버 장애 때문에 메인 트랜잭션(운동 저장)이 롤백되면 안 되기 때문.
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

    // =========================================================
    // 3) 추천 식단 요청
    // =========================================================
    // A) 🔹 일일 추천을 가져오되, 아직 DB에 저장하지 않는 헬퍼

    @Transactional(noRollbackFor = HttpClientErrorException.class)
    protected RecommendedMealPlan fetchDailyRecommendedMealPlan(User user) {
        try {
            String goalParam = mapGoalForRecommender(user.getHealthGoal());

            // 1) 비선호 음식 리스트 로드
            List<String> dislikedFoods = preferenceService.getDislikedFoods(user);

            // 2) 기본 URL Builder 생성
            UriComponentsBuilder ub = UriComponentsBuilder
                    .fromHttpUrl(aiServerUrl + "/recommend/recommend_daily_meal")
                    .queryParam("user_id", user.getUserId())
                    .queryParam("meals_per_day", 3)
                    .queryParam("goal", goalParam);

            // 3) 비선호 음식 쿼리스트링 추가
            if (dislikedFoods != null && !dislikedFoods.isEmpty()) {
                for (String df : dislikedFoods) {
                    ub.queryParam("excluded_foods", df);
                }
            }

            String url = ub.toUriString();

            ResponseEntity<Map> response = restTemplate.exchange(
                    url,
                    HttpMethod.POST,
                    new HttpEntity<>(jsonHeaders()),
                    Map.class
            );

            if (response.getStatusCode() != HttpStatus.OK || response.getBody() == null) {
                log.warn("AI 추천 실패 - status={}, body={}", response.getStatusCode(), response.getBody());
                return createSampleRecommendedMealPlan(user);
            }

            Map<String, Object> body = response.getBody();

            BigDecimal targetDailyCalories = toBD(body.get("target_daily_calories"));
            BigDecimal targetProtein       = toBD(body.get("target_protein"));
            BigDecimal targetFat           = toBD(body.get("target_fat"));
            BigDecimal targetCarbs         = toBD(body.get("target_carbs"));
            String comment                 = str(body.get("comment"));

            Map<String, Object> aiPlan = safeMap(body.get("ai_meal_plan"));
            BigDecimal totalKcalFromPlan = toBD(aiPlan.get("total_kcal"));
            if (totalKcalFromPlan == null || BigDecimal.ZERO.compareTo(totalKcalFromPlan) == 0) {
                totalKcalFromPlan = targetDailyCalories;
            }

            List<Map<String, Object>> meals = safeListMap(aiPlan.get("meals"));

            RecommendedMealPlan plan = RecommendedMealPlan.builder()
                    .user(user)
                    .planName("AI Daily Plan")
                    .description("AI 서버에서 생성한 하루 식단")
                    .totalCalories(totalKcalFromPlan)
                    .totalCarbs(targetCarbs)
                    .totalProtein(targetProtein)
                    .totalFat(targetFat)
                    .recommendationReason(
                            (comment == null || comment.isBlank())
                                    ? "사용자 프로필과 목표 기반 자동 생성"
                                    : comment
                    )
                    .isSaved(false)
                    .build();

            for (Map<String, Object> m : meals) {
                String typeRaw = str(m.get("meal_type"));

                RecommendedMeal rm = RecommendedMeal.builder()
                        .mealType(mapMealTypeGuess(typeRaw))
                        .build();

                BigDecimal mealCalories = BigDecimal.ZERO;
                BigDecimal mealCarbs    = BigDecimal.ZERO;
                BigDecimal mealProtein  = BigDecimal.ZERO;
                BigDecimal mealFat      = BigDecimal.ZERO;

                List<Map<String, Object>> foods = safeListMap(m.get("foods"));
                for (Map<String, Object> f : foods) {
                    String foodName = str(f.get("name"));
                    BigDecimal amountG = toBD(f.get("amount_g"));
                    if (amountG == null || BigDecimal.ZERO.compareTo(amountG) == 0) {
                        amountG = new BigDecimal("100");
                    }

                    BigDecimal calories = toBD(f.get("calories"));
                    BigDecimal protein  = toBD(f.get("protein"));
                    BigDecimal fat      = toBD(f.get("fat"));
                    BigDecimal carbs    = toBD(f.get("carb"));

                    mealCalories = mealCalories.add(calories);
                    mealProtein  = mealProtein.add(protein);
                    mealFat      = mealFat.add(fat);
                    mealCarbs    = mealCarbs.add(carbs);

                    rm.addRecommendedFood(
                            RecommendedFood.builder()
                                    .foodName(foodName)
                                    .servingSize(amountG)
                                    .calories(calories)
                                    .carbs(carbs)
                                    .protein(protein)
                                    .fat(fat)
                                    .build()
                    );
                }

                rm.setTotalCalories(mealCalories);
                rm.setTotalProtein(mealProtein);
                rm.setTotalFat(mealFat);
                rm.setTotalCarbs(mealCarbs);

                plan.addRecommendedMeal(rm);
            }

            log.info("AI 일간 식단 생성 성공 - userId={}, goal={}", user.getUserId(), goalParam);
            return plan;

        } catch (HttpClientErrorException e) {
            log.warn("AI 추천 4xx/5xx - status={}, body={}", e.getStatusCode(), e.getResponseBodyAsString(), e);
            return createSampleRecommendedMealPlan(user);
        } catch (Exception e) {
            log.error("AI 추천 호출 실패: {}", e.getMessage(), e);
            return createSampleRecommendedMealPlan(user);
        }
    }

    // == NEW: 0) 유저 비선호 음식 조회 ==
    private List<String> loadDislikedFoods(User user) {
        return preferenceService.getDislikedFoods(user);
    }


    public List<RecommendedMealPlan> requestWeeklyRecommendedMealPlansWithoutSave(CustomUserPrincipal principal) {

        User user = userRepository.findByUserId(principal.getUserId())
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        // ✔ fetchDailyRecommendedMealPlan() 기반으로 7일 생성
        List<RecommendedMealPlan> weekly = new ArrayList<>();

        for (int day = 1; day <= 7; day++) {
            RecommendedMealPlan daily = fetchDailyRecommendedMealPlan(user);

            daily.setUser(user);
            daily.setBundleId(null); // temp 단계이므로 번들 없음
            daily.setBundleDay(day);
            daily.setPlanDate(LocalDate.now().plusDays(day - 1));
            daily.setIsSaved(false);

            if (daily.getRecommendedMeals() != null) {
                daily.getRecommendedMeals().forEach(m -> m.setRecommendedMealPlan(daily));
            }

            weekly.add(daily);
        }

        return weekly;
    }


    public RecommendedMealPlan requestDailyRecommendedMealPlanWithoutSave(CustomUserPrincipal principal) {
        User user = userRepository.findByUserId(principal.getUserId())
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        return fetchDailyRecommendedMealPlan(user);
    }

    @Transactional
    public RecommendedMealPlan saveRecommendedMealPlan(RecommendedMealPlan plan) {
        return recommendedMealPlanRepository.save(plan);
    }







    // 샘플 플랜 생성기 (AI 실패 시 대체용)
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
                .foodName("통밀빵 토스트")
                .servingSize(new BigDecimal("60"))
                .calories(new BigDecimal("150"))
                .carbs(new BigDecimal("28"))
                .protein(new BigDecimal("6"))
                .fat(new BigDecimal("2"))
                .build());

        plan.addRecommendedMeal(breakfast);
        return plan;
    }

    public List<RecommendedMealDto.RecommendedPlanDetailResponse>
    requestWeeklyRecommendedMealForFreeUser(User user) {

        resetWeeklyTokensIfNeeded(user); // lastReset 로직

        if (user.getMealRecommendTokens() <= 0) {
            throw new BusinessException(ErrorCode.NO_MEAL_TOKENS,
                    "무료 식단 추천 토큰이 부족합니다.");
        }

        // 하루치만 생성
        RecommendedMealPlan daily = fetchDailyRecommendedMealPlan(user);
        daily.setUser(user);
        daily.setBundleId(UUID.randomUUID().toString());
        daily.setBundleDay(1);
        daily.setPlanDate(LocalDate.now());

        user.setMealRecommendTokens(user.getMealRecommendTokens() - 1);

        return List.of(
                RecommendedMealDto.RecommendedPlanDetailResponse.from(daily)
        );
    }

    // 🔥 무료 플랜 토큰 자동 리셋 (7일마다)
    private void resetWeeklyTokensIfNeeded(User user) {

        if (user.getMealTokenLastReset() == null) {
            user.setMealTokenLastReset(LocalDate.now());
            user.setMealRecommendTokens(1); // 기본 제공 1개
            return;
        }

        // 7일 경과 시 토큰 리셋
        if (user.getMealTokenLastReset().plusDays(7).isBefore(LocalDate.now())) {
            user.setMealRecommendTokens(1);
            user.setMealTokenLastReset(LocalDate.now());
        }
    }


    // B) 🔹 주간(7일) 추천 생성 및 저장:  bundleId 하나로 1~7일 저장
    @Transactional
    public List<RecommendedMealDto.RecommendedPlanDetailResponse> requestWeeklyRecommendedMealPlans(
            CustomUserPrincipal userPrincipal, LocalDate weekStartDate) {

        User user = userRepository.findByUserId(userPrincipal.getUserId())
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        String bundleId = UUID.randomUUID().toString(); // ✅ 7일 공통 번들 ID
        List<RecommendedMealPlan> weekly = new ArrayList<>(7);

        for (int day = 1; day <= 7; day++) {

            // ✅ 1) 기존 builder 기반 메서드 결과 가져오기
            RecommendedMealPlan daily = fetchDailyRecommendedMealPlan(user);

            // ✅ 2) builder로 만들어진 객체는 JPA 변경감지에서 제외될 수 있으므로 완전히 재귀 세팅
            daily.setUser(user);
            daily.setBundleId(bundleId);
            daily.setBundleDay(day);
            daily.setPlanDate(
                    weekStartDate != null ? weekStartDate.plusDays(day - 1) : LocalDate.now().plusDays(day - 1)
            );

            daily.setIsSaved(true); // ← 이 한 줄 추가!

            // ✅ 3) 식사 목록에도 영속성 연동을 위해 역참조 세팅
            if (daily.getRecommendedMeals() != null) {
                daily.getRecommendedMeals().forEach(meal -> meal.setRecommendedMealPlan(daily));
            }

            weekly.add(daily);
        }


        log.info("주간 식단 생성 완료 - userId={}, bundleId={}, count={}", user.getUserId(), bundleId, weekly.size());

        return weekly.stream()
                .map(RecommendedMealDto.RecommendedPlanDetailResponse::from)
                .toList();
    }


    // C) 🔹 단일(하루) 추천 생성 및 저장: 번들 메타(day=1) 포함
    @Transactional
    public RecommendedMealPlan requestRecommendedMealPlan(CustomUserPrincipal userPrincipal) {
        User user = userRepository.findByUserId(userPrincipal.getUserId())
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        RecommendedMealPlan plan = fetchDailyRecommendedMealPlan(user);
        plan.setBundleId(UUID.randomUUID().toString());
        plan.setBundleDay(1);
        plan.setPlanDate(LocalDate.now());

        return plan;    }

    // -----------------------------
    // 3-보조
    // -----------------------------
    private Meal.MealType mapMealTypeGuess(String mealTypeFromAI) {
        String s = (mealTypeFromAI == null) ? "" : mealTypeFromAI.toLowerCase(Locale.ROOT);
        if (s.contains("1")) return Meal.MealType.BREAKFAST;
        if (s.contains("2")) return Meal.MealType.LUNCH;
        if (s.contains("3")) return Meal.MealType.DINNER;
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
    // 4) 변환/샘플 헬퍼 (기존 유지)
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

    // -----------------------------
    // 5) 내부 DTO (기존 유지)
    // -----------------------------
    @Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
    private static class AIUserCreateRequest {
        private String id;
        private String name;
        private Integer age;
        private String sex;
        private Float height;
        private Float weight;
        private Float body_fat;
        private Float skeletal_muscle;
        private Float activity_level;
        private String goal;
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
