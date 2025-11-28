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
    private final UserFoodPreferenceService preferenceService;

    // ===================================================================
    //  ✔ 1) 식단 생성 (정상 버전)
    // ===================================================================
    @Transactional
    public MealDto.MealCreateResponse createMeal(
            CustomUserPrincipal userPrincipal,
            MealDto.MealCreateRequest request) {

        User user = findUserByPrincipal(userPrincipal);


        // 2) 음식 → 영양소 합산
        BigDecimal totalCalories = BigDecimal.ZERO;
        BigDecimal totalCarbs = BigDecimal.ZERO;
        BigDecimal totalProtein = BigDecimal.ZERO;
        BigDecimal totalFat = BigDecimal.ZERO;

        for (MealDto.FoodItemRequest fi : request.getFoods()) {
            totalCalories = totalCalories.add(nz(fi.getCalories()));
            totalCarbs = totalCarbs.add(nz(fi.getCarbs()));
            totalProtein = totalProtein.add(nz(fi.getProtein()));
            totalFat = totalFat.add(nz(fi.getFat()));
        }

        // 3) Meal 생성
        Meal meal = Meal.builder()
                .user(user)
                .mealDate(request.getMealDate())
                .mealType(request.getMealType())
                .totalCalories(totalCalories)
                .totalCarbs(totalCarbs)
                .totalProtein(totalProtein)
                .totalFat(totalFat)
                .build();

        // 4) 음식 리스트 저장
        request.getFoods().forEach(fi -> {

            meal.addMealFood(
                    MealFood.builder()
                            .meal(meal)
                            .foodName(fi.getFoodName())
                            .servingSize(fi.getServingSize())
                            .calories(fi.getCalories())
                            .carbs(fi.getCarbs())
                            .protein(fi.getProtein())
                            .fat(fi.getFat())
                            .sodium(fi.getSodium())
                            .cholesterol(fi.getCholesterol())
                            .sugar(fi.getSugar())
                            .fiber(fi.getFiber())
                            .imageUrl(fi.getImageUrl())
                            .aiConfidenceScore(fi.getAiConfidenceScore())
                            .build()
            );


            // 선호/비선호 학습 데이터 반영용 (필요)
            preferenceService.addFoodConsumption(user, fi.getFoodName());
        });

        // 5) 저장
        mealRepository.save(meal);

        log.info("식사 추가 완료 - user={}, date={}, type={}",
                user.getUserId(), request.getMealDate(), request.getMealType());

        return MealDto.MealCreateResponse.from(meal);
    }

    private BigDecimal nz(BigDecimal v) {
        return v == null ? BigDecimal.ZERO : v;
    }

    // ===================================================================
    //  ✔ 2) 일별 식단 조회
    // ===================================================================
    public MealDto.DailyMealsResponse getDailyMeals(
            CustomUserPrincipal userPrincipal,
            LocalDate date) {

        User user = findUserByPrincipal(userPrincipal);

        List<Meal> meals =
                mealRepository.findByUserAndMealDateOrderByMealTypeAsc(user, date);

        BigDecimal dailyTotalCalories = meals.stream()
                .map(Meal::getTotalCalories).reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal dailyTotalCarbs = meals.stream()
                .map(Meal::getTotalCarbs).reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal dailyTotalProtein = meals.stream()
                .map(Meal::getTotalProtein).reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal dailyTotalFat = meals.stream()
                .map(Meal::getTotalFat).reduce(BigDecimal.ZERO, BigDecimal::add);

        return MealDto.DailyMealsResponse.builder()
                .date(date)
                .meals(meals.stream()
                        .map(MealDto.MealDetailResponse::from)
                        .collect(Collectors.toList()))
                .dailyTotalCalories(dailyTotalCalories)
                .dailyTotalCarbs(dailyTotalCarbs)
                .dailyTotalProtein(dailyTotalProtein)
                .dailyTotalFat(dailyTotalFat)
                .build();
    }

    // ===================================================================
    // ✔ 3) 주간 통계
    // ===================================================================
    public MealDto.WeeklyMealStatsResponse getWeeklyStats(
            CustomUserPrincipal userPrincipal,
            LocalDate startDate) {

        User user = findUserByPrincipal(userPrincipal);
        LocalDate endDate = startDate.plusDays(6);

        Double cal = mealRepository.sumCaloriesByPeriod(user, startDate, endDate);
        Double carb = mealRepository.sumCarbsByPeriod(user, startDate, endDate);
        Double pro = mealRepository.sumProteinByPeriod(user, startDate, endDate);
        Double fat = mealRepository.sumFatByPeriod(user, startDate, endDate);

        BigDecimal weeklyCal = BigDecimal.valueOf(cal != null ? cal : 0);
        BigDecimal weeklyCarb = BigDecimal.valueOf(carb != null ? carb : 0);
        BigDecimal weeklyPro = BigDecimal.valueOf(pro != null ? pro : 0);
        BigDecimal weeklyFat = BigDecimal.valueOf(fat != null ? fat : 0);

        BigDecimal dailyAvg = weeklyCal.divide(BigDecimal.valueOf(7), 2, RoundingMode.HALF_UP);

        DailyNutritionGoal goal = nutritionGoalRepository.findByUser(user).orElse(null);

        return MealDto.WeeklyMealStatsResponse.builder()
                .startDate(startDate)
                .endDate(endDate)
                .weeklyTotalCalories(weeklyCal)
                .weeklyTotalCarbs(weeklyCarb)
                .weeklyTotalProtein(weeklyPro)
                .weeklyTotalFat(weeklyFat)
                .dailyAvgCalories(dailyAvg)
                .calorieAchievementRate(calcRate(weeklyCal, goal != null ? goal.getTargetCalories().multiply(BigDecimal.valueOf(7)) : null))
                .carbsAchievementRate(calcRate(weeklyCarb, goal != null ? goal.getTargetCarbs().multiply(BigDecimal.valueOf(7)) : null))
                .proteinAchievementRate(calcRate(weeklyPro, goal != null ? goal.getTargetProtein().multiply(BigDecimal.valueOf(7)) : null))
                .fatAchievementRate(calcRate(weeklyFat, goal != null ? goal.getTargetFat().multiply(BigDecimal.valueOf(7)) : null))
                .dailyStats(makeDailyStats(user, startDate, goal))
                .build();
    }

    // ===================================================================
    // ✔ 4) 지난주 대비 이번주 비교
    // ===================================================================
    public MealDto.WeekComparisonResponse compareWeeks(
            CustomUserPrincipal userPrincipal,
            LocalDate thisWeekStart) {

        User user = findUserByPrincipal(userPrincipal);

        LocalDate thisWeekEnd = thisWeekStart.plusDays(6);
        LocalDate lastWeekStart = thisWeekStart.minusWeeks(1);
        LocalDate lastWeekEnd = lastWeekStart.plusDays(6);

        Double thisC = mealRepository.sumCaloriesByPeriod(user, thisWeekStart, thisWeekEnd);
        Double lastC = mealRepository.sumCaloriesByPeriod(user, lastWeekStart, lastWeekEnd);

        BigDecimal thisCal = BigDecimal.valueOf(thisC != null ? thisC : 0);
        BigDecimal lastCal = BigDecimal.valueOf(lastC != null ? lastC : 0);

        BigDecimal diff = thisCal.subtract(lastCal);
        BigDecimal rate = calculateChangeRate(lastCal, thisCal);

        return MealDto.WeekComparisonResponse.builder()
                .thisWeekStart(thisWeekStart)
                .thisWeekEnd(thisWeekEnd)
                .thisWeekCalories(thisCal)
                .lastWeekCalories(lastCal)
                .caloriesDifference(diff)
                .caloriesChangeRate(rate)
                .analysisMessage(generateAnalysisMessage(rate))
                .build();
    }

    // ===================================================================
    // ✔ 5) 최근 N일 조회
    // ===================================================================
    public List<MealDto.MealDetailResponse> getRecentMeals(
            CustomUserPrincipal userPrincipal,
            int days) {

        User user = findUserByPrincipal(userPrincipal);
        LocalDate start = LocalDate.now().minusDays(days - 1);

        return mealRepository.findRecentMeals(user, start)
                .stream()
                .map(MealDto.MealDetailResponse::from)
                .collect(Collectors.toList());
    }

    // ===================================================================
    // ✔ 헬퍼
    // ===================================================================
    private User findUserByPrincipal(CustomUserPrincipal principal) {
        return userRepository.findByUserId(principal.getUserId())
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
    }

    private BigDecimal calcRate(BigDecimal actual, BigDecimal target) {
        if (target == null || target.compareTo(BigDecimal.ZERO) == 0) return BigDecimal.ZERO;
        return actual.divide(target, 4, RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100))
                .setScale(2, RoundingMode.HALF_UP);
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

    private String generateAnalysisMessage(BigDecimal rate) {
        if (rate.compareTo(BigDecimal.ZERO) == 0) return "지난주와 동일한 칼로리를 섭취했습니다";
        if (rate.compareTo(BigDecimal.ZERO) > 0) return "지난주 대비 칼로리를 더 섭취했습니다";
        return "지난주 대비 칼로리를 줄였습니다";
    }

    public LocalDate getThisWeekMonday() {
        return LocalDate.now().with(DayOfWeek.MONDAY);
    }

    private List<MealDto.DailyStatItem> makeDailyStats(User user, LocalDate start, DailyNutritionGoal goal) {
        List<MealDto.DailyStatItem> stats = new ArrayList<>();

        for (int i = 0; i < 7; i++) {
            LocalDate date = start.plusDays(i);
            BigDecimal cal = mealRepository.findByUserAndMealDateOrderByMealTypeAsc(user, date)
                    .stream()
                    .map(Meal::getTotalCalories)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            BigDecimal rate = BigDecimal.ZERO;
            if (goal != null && goal.getTargetCalories().compareTo(BigDecimal.ZERO) > 0) {
                rate = cal.divide(goal.getTargetCalories(), 4, RoundingMode.HALF_UP)
                        .multiply(BigDecimal.valueOf(100))
                        .setScale(2, RoundingMode.HALF_UP);
            }

            stats.add(MealDto.DailyStatItem.builder()
                    .date(date)
                    .dailyCalories(cal)
                    .achievementRate(rate)
                    .build());
        }

        return stats;
    }


}
