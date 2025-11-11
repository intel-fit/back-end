package rto.intelfit.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import rto.intelfit.domain.*;
import rto.intelfit.dto.MealDto;
import rto.intelfit.exception.BusinessException;
import rto.intelfit.exception.ErrorCode;
import rto.intelfit.repository.DailyNutritionGoalRepository;
import rto.intelfit.repository.MealRepository;
import rto.intelfit.repository.UserRepository;
import rto.intelfit.security.CustomUserPrincipal;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MealService {

    private final MealRepository mealRepository;
    private final UserRepository userRepository;
    private final DailyNutritionGoalRepository nutritionGoalRepository;
    private final UserFoodPreferenceService preferenceService; // 추가
    private final DailyProgressService dailyProgressService; // 추가

    /**
     * 식사 추가
     */
    @Transactional
    public MealDto.MealCreateResponse createMeal(
            CustomUserPrincipal userPrincipal,
            MealDto.MealCreateRequest request) {

        User user = findUserByPrincipal(userPrincipal);

        // 미래 날짜 체크
        if (request.getMealDate().isAfter(LocalDate.now())) {
            throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE,
                    "미래 날짜의 식사는 등록할 수 없습니다");
        }

        // 동일 날짜/타입의 식사가 이미 있는지 확인
        mealRepository.findByUserAndMealDateAndMealType(
                user, request.getMealDate(), request.getMealType()
        ).ifPresent(meal -> {
            throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE,
                    "해당 날짜에 이미 " + request.getMealType().getDescription() + " 식사가 등록되어 있습니다");
        });

        // Meal 엔티티 생성
        Meal meal = Meal.builder()
                .user(user)
                .mealDate(request.getMealDate())
                .mealType(request.getMealType())
                .memo(request.getMemo())
                .build();

        // MealFood 엔티티들 생성 및 추가
        for (MealDto.FoodItemRequest foodRequest : request.getFoods()) {
            MealFood mealFood = MealFood.builder()
                    .foodName(foodRequest.getFoodName())
                    .servingSize(foodRequest.getServingSize())
                    .calories(foodRequest.getCalories())
                    .carbs(foodRequest.getCarbs())
                    .protein(foodRequest.getProtein())
                    .fat(foodRequest.getFat())
                    .sodium(foodRequest.getSodium())
                    .cholesterol(foodRequest.getCholesterol())
                    .sugar(foodRequest.getSugar())
                    .fiber(foodRequest.getFiber())
                    .imageUrl(foodRequest.getImageUrl())
                    .aiConfidenceScore(foodRequest.getAiConfidenceScore())
                    .build();

            meal.addMealFood(mealFood);

            // 선호 음식 섭취 횟수 업데이트 (비동기로 처리 가능)
            try {
                preferenceService.updateFoodConsumption(user, foodRequest.getFoodName());
            } catch (Exception e) {
                log.warn("선호 음식 섭취 횟수 업데이트 실패 - 음식: {}, 오류: {}",
                        foodRequest.getFoodName(), e.getMessage());
                // 실패해도 식사 추가는 계속 진행
            }
        }

        Meal savedMeal = mealRepository.save(meal);

        dailyProgressService.calculateTodayProgress(user); // dailyProgress 갱신

        log.info("식사 추가 완료 - 사용자 ID: {}, 날짜: {}, 타입: {}",
                user.getUserId(), request.getMealDate(), request.getMealType());

        return MealDto.MealCreateResponse.builder()
                .success(true)
                .message("식사가 추가되었습니다")
                .meal(MealDto.MealDetailResponse.from(savedMeal))
                .build();
    }

    /**
     * 일별 식단 조회
     */
    public MealDto.DailyMealsResponse getDailyMeals(
            CustomUserPrincipal userPrincipal,
            LocalDate date) {

        User user = findUserByPrincipal(userPrincipal);

        List<Meal> meals = mealRepository.findByUserAndMealDateOrderByMealTypeAsc(user, date);

        // 일일 총 영양소 계산
        BigDecimal dailyTotalCalories = meals.stream()
                .map(Meal::getTotalCalories)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal dailyTotalCarbs = meals.stream()
                .map(Meal::getTotalCarbs)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal dailyTotalProtein = meals.stream()
                .map(Meal::getTotalProtein)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal dailyTotalFat = meals.stream()
                .map(Meal::getTotalFat)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        List<MealDto.MealDetailResponse> mealResponses = meals.stream()
                .map(MealDto.MealDetailResponse::from)
                .collect(Collectors.toList());

        log.info("일별 식단 조회 - 사용자 ID: {}, 날짜: {}, 식사 개수: {}",
                user.getUserId(), date, meals.size());

        return MealDto.DailyMealsResponse.builder()
                .date(date)
                .meals(mealResponses)
                .dailyTotalCalories(dailyTotalCalories)
                .dailyTotalCarbs(dailyTotalCarbs)
                .dailyTotalProtein(dailyTotalProtein)
                .dailyTotalFat(dailyTotalFat)
                .build();
    }

    /**
     * 주간 식단 통계
     */
    public MealDto.WeeklyMealStatsResponse getWeeklyStats(
            CustomUserPrincipal userPrincipal,
            LocalDate startDate) {

        User user = findUserByPrincipal(userPrincipal);
        LocalDate endDate = startDate.plusDays(6);

        // 주간 영양소 합계
        Double weeklyCalories = mealRepository.sumCaloriesByPeriod(user, startDate, endDate);
        Double weeklyCarbs = mealRepository.sumCarbsByPeriod(user, startDate, endDate);
        Double weeklyProtein = mealRepository.sumProteinByPeriod(user, startDate, endDate);
        Double weeklyFat = mealRepository.sumFatByPeriod(user, startDate, endDate);

        BigDecimal weeklyTotalCalories = BigDecimal.valueOf(weeklyCalories != null ? weeklyCalories : 0);
        BigDecimal weeklyTotalCarbs = BigDecimal.valueOf(weeklyCarbs != null ? weeklyCarbs : 0);
        BigDecimal weeklyTotalProtein = BigDecimal.valueOf(weeklyProtein != null ? weeklyProtein : 0);
        BigDecimal weeklyTotalFat = BigDecimal.valueOf(weeklyFat != null ? weeklyFat : 0);

        // 일평균 칼로리
        BigDecimal dailyAvgCalories = weeklyTotalCalories.divide(
                BigDecimal.valueOf(7), 2, RoundingMode.HALF_UP);

        // 목표 대비 달성률 계산
        DailyNutritionGoal goal = nutritionGoalRepository.findByUser(user).orElse(null);

        BigDecimal calorieAchievementRate = BigDecimal.ZERO;
        BigDecimal carbsAchievementRate = BigDecimal.ZERO;
        BigDecimal proteinAchievementRate = BigDecimal.ZERO;
        BigDecimal fatAchievementRate = BigDecimal.ZERO;

        if (goal != null) {
            BigDecimal weeklyTargetCalories = goal.getTargetCalories().multiply(BigDecimal.valueOf(7));
            BigDecimal weeklyTargetCarbs = goal.getTargetCarbs().multiply(BigDecimal.valueOf(7));
            BigDecimal weeklyTargetProtein = goal.getTargetProtein().multiply(BigDecimal.valueOf(7));
            BigDecimal weeklyTargetFat = goal.getTargetFat().multiply(BigDecimal.valueOf(7));

            if (weeklyTargetCalories.compareTo(BigDecimal.ZERO) > 0) {
                calorieAchievementRate = weeklyTotalCalories
                        .divide(weeklyTargetCalories, 4, RoundingMode.HALF_UP)
                        .multiply(BigDecimal.valueOf(100))
                        .setScale(2, RoundingMode.HALF_UP);
            }

            if (weeklyTargetCarbs.compareTo(BigDecimal.ZERO) > 0) {
                carbsAchievementRate = weeklyTotalCarbs
                        .divide(weeklyTargetCarbs, 4, RoundingMode.HALF_UP)
                        .multiply(BigDecimal.valueOf(100))
                        .setScale(2, RoundingMode.HALF_UP);
            }

            if (weeklyTargetProtein.compareTo(BigDecimal.ZERO) > 0) {
                proteinAchievementRate = weeklyTotalProtein
                        .divide(weeklyTargetProtein, 4, RoundingMode.HALF_UP)
                        .multiply(BigDecimal.valueOf(100))
                        .setScale(2, RoundingMode.HALF_UP);
            }

            if (weeklyTargetFat.compareTo(BigDecimal.ZERO) > 0) {
                fatAchievementRate = weeklyTotalFat
                        .divide(weeklyTargetFat, 4, RoundingMode.HALF_UP)
                        .multiply(BigDecimal.valueOf(100))
                        .setScale(2, RoundingMode.HALF_UP);
            }
        }

        // 일별 통계
        List<MealDto.DailyStatItem> dailyStats = new ArrayList<>();
        for (int i = 0; i < 7; i++) {
            LocalDate date = startDate.plusDays(i);
            List<Meal> dailyMeals = mealRepository.findByUserAndMealDateOrderByMealTypeAsc(user, date);

            BigDecimal dailyCalories = dailyMeals.stream()
                    .map(Meal::getTotalCalories)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            BigDecimal dailyAchievementRate = BigDecimal.ZERO;
            if (goal != null && goal.getTargetCalories().compareTo(BigDecimal.ZERO) > 0) {
                dailyAchievementRate = dailyCalories
                        .divide(goal.getTargetCalories(), 4, RoundingMode.HALF_UP)
                        .multiply(BigDecimal.valueOf(100))
                        .setScale(2, RoundingMode.HALF_UP);
            }

            dailyStats.add(MealDto.DailyStatItem.builder()
                    .date(date)
                    .dailyCalories(dailyCalories)
                    .achievementRate(dailyAchievementRate)
                    .build());
        }

        log.info("주간 식단 통계 조회 - 사용자 ID: {}, 기간: {} ~ {}",
                user.getUserId(), startDate, endDate);

        return MealDto.WeeklyMealStatsResponse.builder()
                .startDate(startDate)
                .endDate(endDate)
                .weeklyTotalCalories(weeklyTotalCalories)
                .weeklyTotalCarbs(weeklyTotalCarbs)
                .weeklyTotalProtein(weeklyTotalProtein)
                .weeklyTotalFat(weeklyTotalFat)
                .dailyAvgCalories(dailyAvgCalories)
                .calorieAchievementRate(calorieAchievementRate)
                .carbsAchievementRate(carbsAchievementRate)
                .proteinAchievementRate(proteinAchievementRate)
                .fatAchievementRate(fatAchievementRate)
                .dailyStats(dailyStats)
                .build();
    }

    /**
     * 지난주 대비 이번주 비교
     */
    public MealDto.WeekComparisonResponse compareWeeks(
            CustomUserPrincipal userPrincipal,
            LocalDate thisWeekStart) {

        User user = findUserByPrincipal(userPrincipal);

        LocalDate thisWeekEnd = thisWeekStart.plusDays(6);
        LocalDate lastWeekStart = thisWeekStart.minusWeeks(1);
        LocalDate lastWeekEnd = lastWeekStart.plusDays(6);

        // 이번주 영양소
        Double thisWeekCaloriesDouble = mealRepository.sumCaloriesByPeriod(user, thisWeekStart, thisWeekEnd);
        Double thisWeekCarbsDouble = mealRepository.sumCarbsByPeriod(user, thisWeekStart, thisWeekEnd);
        Double thisWeekProteinDouble = mealRepository.sumProteinByPeriod(user, thisWeekStart, thisWeekEnd);
        Double thisWeekFatDouble = mealRepository.sumFatByPeriod(user, thisWeekStart, thisWeekEnd);

        BigDecimal thisWeekCalories = BigDecimal.valueOf(thisWeekCaloriesDouble != null ? thisWeekCaloriesDouble : 0);
        BigDecimal thisWeekCarbs = BigDecimal.valueOf(thisWeekCarbsDouble != null ? thisWeekCarbsDouble : 0);
        BigDecimal thisWeekProtein = BigDecimal.valueOf(thisWeekProteinDouble != null ? thisWeekProteinDouble : 0);
        BigDecimal thisWeekFat = BigDecimal.valueOf(thisWeekFatDouble != null ? thisWeekFatDouble : 0);

        // 지난주 영양소
        Double lastWeekCaloriesDouble = mealRepository.sumCaloriesByPeriod(user, lastWeekStart, lastWeekEnd);
        Double lastWeekCarbsDouble = mealRepository.sumCarbsByPeriod(user, lastWeekStart, lastWeekEnd);
        Double lastWeekProteinDouble = mealRepository.sumProteinByPeriod(user, lastWeekStart, lastWeekEnd);
        Double lastWeekFatDouble = mealRepository.sumFatByPeriod(user, lastWeekStart, lastWeekEnd);

        BigDecimal lastWeekCalories = BigDecimal.valueOf(lastWeekCaloriesDouble != null ? lastWeekCaloriesDouble : 0);
        BigDecimal lastWeekCarbs = BigDecimal.valueOf(lastWeekCarbsDouble != null ? lastWeekCarbsDouble : 0);
        BigDecimal lastWeekProtein = BigDecimal.valueOf(lastWeekProteinDouble != null ? lastWeekProteinDouble : 0);
        BigDecimal lastWeekFat = BigDecimal.valueOf(lastWeekFatDouble != null ? lastWeekFatDouble : 0);

        // 변화량 및 변화율 계산
        BigDecimal caloriesDifference = thisWeekCalories.subtract(lastWeekCalories);
        BigDecimal caloriesChangeRate = calculateChangeRate(lastWeekCalories, thisWeekCalories);
        BigDecimal carbsChangeRate = calculateChangeRate(lastWeekCarbs, thisWeekCarbs);
        BigDecimal proteinChangeRate = calculateChangeRate(lastWeekProtein, thisWeekProtein);
        BigDecimal fatChangeRate = calculateChangeRate(lastWeekFat, thisWeekFat);

        // 분석 메시지 생성
        String analysisMessage = generateAnalysisMessage(caloriesChangeRate);

        log.info("주간 비교 조회 - 사용자 ID: {}, 이번주: {} ~ {}, 지난주: {} ~ {}",
                user.getUserId(), thisWeekStart, thisWeekEnd, lastWeekStart, lastWeekEnd);

        return MealDto.WeekComparisonResponse.builder()
                .thisWeekStart(thisWeekStart)
                .thisWeekEnd(thisWeekEnd)
                .thisWeekCalories(thisWeekCalories)
                .lastWeekCalories(lastWeekCalories)
                .caloriesDifference(caloriesDifference)
                .caloriesChangeRate(caloriesChangeRate)
                .carbsChangeRate(carbsChangeRate)
                .proteinChangeRate(proteinChangeRate)
                .fatChangeRate(fatChangeRate)
                .analysisMessage(analysisMessage)
                .build();
    }

    /**
     * 현재까지의 식단 기록 조회 (최근 N일)
     */
    public List<MealDto.MealDetailResponse> getRecentMeals(
            CustomUserPrincipal userPrincipal,
            int days) {

        User user = findUserByPrincipal(userPrincipal);
        LocalDate startDate = LocalDate.now().minusDays(days - 1);

        List<Meal> meals = mealRepository.findRecentMeals(user, startDate);

        log.info("최근 식단 기록 조회 - 사용자 ID: {}, 최근 {}일, 기록 개수: {}",
                user.getUserId(), days, meals.size());

        return meals.stream()
                .map(MealDto.MealDetailResponse::from)
                .collect(Collectors.toList());
    }

    // Private helper methods

    private User findUserByPrincipal(CustomUserPrincipal userPrincipal) {
        return userRepository.findByUserId(userPrincipal.getUserId())
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
    }

    private BigDecimal calculateChangeRate(BigDecimal oldValue, BigDecimal newValue) {
        if (oldValue.compareTo(BigDecimal.ZERO) == 0) {
            return newValue.compareTo(BigDecimal.ZERO) > 0
                    ? BigDecimal.valueOf(100)
                    : BigDecimal.ZERO;
        }

        return newValue.subtract(oldValue)
                .divide(oldValue, 4, RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100))
                .setScale(2, RoundingMode.HALF_UP);
    }

    private String generateAnalysisMessage(BigDecimal changeRate) {
        if (changeRate.compareTo(BigDecimal.ZERO) == 0) {
            return "지난주와 동일한 칼로리를 섭취했습니다";
        } else if (changeRate.compareTo(BigDecimal.ZERO) > 0) {
            return String.format("지난주 대비 칼로리를 %.1f%% 더 섭취했습니다",
                    changeRate.abs().doubleValue());
        } else {
            return String.format("지난주 대비 칼로리를 %.1f%% 줄였습니다",
                    changeRate.abs().doubleValue());
        }
    }

    /**
     * 이번주 월요일 날짜 계산 (주간 통계용 헬퍼 메서드)
     */
    public LocalDate getThisWeekMonday() {
        LocalDate today = LocalDate.now();
        return today.with(DayOfWeek.MONDAY);
    }
}