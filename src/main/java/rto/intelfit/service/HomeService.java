package rto.intelfit.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import rto.intelfit.domain.*;
import rto.intelfit.dto.HomeDto;
import rto.intelfit.exception.BusinessException;
import rto.intelfit.exception.ErrorCode;
import rto.intelfit.repository.*;
import rto.intelfit.security.CustomUserPrincipal;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.TemporalAdjusters;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class HomeService {

    private final UserRepository userRepository;
    private final ExerciseRepository exerciseRepository;
    private final FitnessExerciseCategorySaveRepository fitnessExerciseCategorySaveRepository; // ✅ 추가
    private final MealRepository mealRepository;
    private final InBodyRepository inBodyRepository;
    private final DailyNutritionGoalRepository dailyNutritionGoalRepository;

    /**
     * 홈 화면 메인 정보 조회
     */
    public HomeDto.HomeMainResponse getHomeMain(
            CustomUserPrincipal userPrincipal,
            LocalDate date) {

        User user = getUserById(userPrincipal.getUserId());

        HomeDto.UserSummary userSummary = createUserSummary(user);
        HomeDto.TodayExerciseSummary todayExercise = getTodayExerciseSummary(userPrincipal, date);
        HomeDto.TodayMealSummary todayMeal = getTodayMealSummary(userPrincipal, date);
        HomeDto.LatestInBodySummary latestInBody = getLatestInBodySummary(user);
        HomeDto.WeeklySummary weeklySummary = getWeeklySummary(userPrincipal, date);

        return HomeDto.HomeMainResponse.builder()
                .userSummary(userSummary)
                .todayExercise(todayExercise)
                .todayMeal(todayMeal)
                .latestInBody(latestInBody)
                .weeklySummary(weeklySummary)
                .aiChatbotAvailable(true)
                .build();
    }

    /**
     * ✅ 오늘의 운동 요약 조회 (FitnessExerciseCategorySave 사용)
     */
    public HomeDto.TodayExerciseSummary getTodayExerciseSummary(
            CustomUserPrincipal userPrincipal,
            LocalDate date) {

        User user = getUserById(userPrincipal.getUserId());

        log.info("=== 운동 요약 조회 시작 ===");
        log.info("userId: {}, date: {}", user.getId(), date);

        // ✅ FitnessExerciseCategorySave에서 조회
        LocalDateTime startOfDay = date.atStartOfDay();
        LocalDateTime endOfDay = date.plusDays(1).atStartOfDay();

        List<FitnessExerciseCategorySave> records =
                fitnessExerciseCategorySaveRepository.findByUserOrderByWorkoutDateDesc(user)
                        .stream()
                        .filter(record -> {
                            LocalDateTime workoutDate = record.getWorkoutDate();
                            return !workoutDate.isBefore(startOfDay) && workoutDate.isBefore(endOfDay);
                        })
                        .collect(Collectors.toList());

        log.info("조회된 운동 기록: {}건", records.size());

        if (records.isEmpty()) {
            return createEmptyExerciseSummary(date);
        }

        // sessionId별로 그룹핑 (한 세션 = 한 운동)
        Map<String, List<FitnessExerciseCategorySave>> sessionGroups =
                records.stream()
                        .collect(Collectors.groupingBy(FitnessExerciseCategorySave::getSessionId));

        int exerciseCount = sessionGroups.size();

        log.info("총 세션(운동) 개수: {}", exerciseCount);

        // 세트 수 계산
        int totalSets = records.size();

        // 주요 카테고리 추출 (중복 제거)
        List<String> mainCategories = records.stream()
                .map(FitnessExerciseCategorySave::getCategory)
                .filter(Objects::nonNull)
                .distinct()
                .collect(Collectors.toList());

        log.info("주요 카테고리: {}", mainCategories);

        // 총 운동 시간은 FitnessExerciseCategorySave에 없으므로
        // 세트당 평균 3분으로 추정 (휴식 포함)
        int estimatedDuration = totalSets * 3;

        String message = createExerciseMessage(exerciseCount, estimatedDuration);

        return HomeDto.TodayExerciseSummary.builder()
                .date(date)
                .completed(true)
                .totalDurationMinutes(estimatedDuration)
                .totalCaloriesBurned(BigDecimal.ZERO) // 칼로리 데이터 없음
                .exerciseCount(exerciseCount)
                .mainCategories(mainCategories)
                .message(message)
                .build();
    }

    /**
     * 오늘의 식단 요약 조회 (기존 코드 유지)
     */
    public HomeDto.TodayMealSummary getTodayMealSummary(
            CustomUserPrincipal userPrincipal,
            LocalDate date) {

        User user = getUserById(userPrincipal.getUserId());

        List<Meal> meals = mealRepository.findByUserAndMealDate(user, date);

        if (meals.isEmpty()) {
            return createEmptyMealSummary(date, user);
        }

        BigDecimal totalCalories = meals.stream()
                .map(Meal::getTotalCalories)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalCarbs = meals.stream()
                .map(Meal::getTotalCarbs)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalProtein = meals.stream()
                .map(Meal::getTotalProtein)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalFat = meals.stream()
                .map(Meal::getTotalFat)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal targetCalories = getTargetCalories(user);

        BigDecimal achievementRate = BigDecimal.ZERO;
        if (targetCalories.compareTo(BigDecimal.ZERO) > 0) {
            achievementRate = totalCalories
                    .divide(targetCalories, 4, RoundingMode.HALF_UP)
                    .multiply(BigDecimal.valueOf(100))
                    .setScale(1, RoundingMode.HALF_UP);
        }

        String message = createMealMessage(totalCalories, targetCalories, achievementRate);

        return HomeDto.TodayMealSummary.builder()
                .date(date)
                .totalCalories(totalCalories)
                .targetCalories(targetCalories)
                .calorieAchievementRate(achievementRate)
                .totalCarbs(totalCarbs)
                .totalProtein(totalProtein)
                .totalFat(totalFat)
                .mealCount(meals.size())
                .message(message)
                .build();
    }

    /**
     * ✅ 주간 요약 조회 (FitnessExerciseCategorySave 사용)
     */
    public HomeDto.WeeklySummary getWeeklySummary(
            CustomUserPrincipal userPrincipal,
            LocalDate date) {

        User user = getUserById(userPrincipal.getUserId());

        LocalDate weekStart = date.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        LocalDate weekEnd = date.with(TemporalAdjusters.nextOrSame(DayOfWeek.SUNDAY));

        log.info("주간 요약 조회 - 기간: {} ~ {}", weekStart, weekEnd);

        // ✅ FitnessExerciseCategorySave에서 이번주 데이터 조회
        LocalDateTime weekStartTime = weekStart.atStartOfDay();
        LocalDateTime weekEndTime = weekEnd.plusDays(1).atStartOfDay();

        List<FitnessExerciseCategorySave> weekRecords =
                fitnessExerciseCategorySaveRepository.findByUserOrderByWorkoutDateDesc(user)
                        .stream()
                        .filter(record -> {
                            LocalDateTime workoutDate = record.getWorkoutDate();
                            return !workoutDate.isBefore(weekStartTime) && workoutDate.isBefore(weekEndTime);
                        })
                        .collect(Collectors.toList());

        log.info("이번주 운동 기록: {}건", weekRecords.size());

        // 운동 일수 계산
        int weeklyExerciseDays = (int) weekRecords.stream()
                .map(r -> r.getWorkoutDate().toLocalDate())
                .distinct()
                .count();

        // 총 세트 수
        int totalSets = weekRecords.size();

        // 추정 운동 시간 (세트당 3분)
        int weeklyTotalDuration = totalSets * 3;

        log.info("주간 운동 일수: {}일, 총 세트: {}개, 추정 시간: {}분",
                weeklyExerciseDays, totalSets, weeklyTotalDuration);

        // 지난주 데이터 (비교용)
        LocalDate lastWeekStart = weekStart.minusWeeks(1);
        LocalDate lastWeekEnd = weekEnd.minusWeeks(1);
        LocalDateTime lastWeekStartTime = lastWeekStart.atStartOfDay();
        LocalDateTime lastWeekEndTime = lastWeekEnd.plusDays(1).atStartOfDay();

        List<FitnessExerciseCategorySave> lastWeekRecords =
                fitnessExerciseCategorySaveRepository.findByUserOrderByWorkoutDateDesc(user)
                        .stream()
                        .filter(record -> {
                            LocalDateTime workoutDate = record.getWorkoutDate();
                            return !workoutDate.isBefore(lastWeekStartTime) && workoutDate.isBefore(lastWeekEndTime);
                        })
                        .collect(Collectors.toList());

        int lastWeekTotalDuration = lastWeekRecords.size() * 3;

        // 식단 데이터
        List<Meal> weekMeals = mealRepository
                .findByUserAndMealDateBetween(user, weekStart, weekEnd);

        List<Meal> lastWeekMeals = mealRepository
                .findByUserAndMealDateBetween(user, lastWeekStart, lastWeekEnd);

        // 주간 평균 칼로리
        BigDecimal weeklyAvgCalories = BigDecimal.ZERO;
        if (!weekMeals.isEmpty()) {
            BigDecimal totalCalories = weekMeals.stream()
                    .map(Meal::getTotalCalories)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            long mealDays = weekMeals.stream()
                    .map(Meal::getMealDate)
                    .distinct()
                    .count();

            if (mealDays > 0) {
                weeklyAvgCalories = totalCalories
                        .divide(BigDecimal.valueOf(mealDays), 0, RoundingMode.HALF_UP);
            }
        }

        // 지난주 대비 변화율
        BigDecimal exerciseChangeRate = calculateChangeRate(
                weeklyTotalDuration,
                lastWeekTotalDuration
        );

        BigDecimal calorieChangeRate = calculateCalorieChangeRate(weekMeals, lastWeekMeals);

        // 목표 달성률 (주 3회 운동 기준)
        BigDecimal goalAchievementRate = BigDecimal.valueOf(weeklyExerciseDays)
                .divide(BigDecimal.valueOf(3), 4, RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100))
                .min(BigDecimal.valueOf(100))
                .setScale(0, RoundingMode.HALF_UP);

        String message = createWeeklyMessage(weeklyExerciseDays, goalAchievementRate);

        return HomeDto.WeeklySummary.builder()
                .weekStartDate(weekStart)
                .weekEndDate(weekEnd)
                .weeklyExerciseDays(weeklyExerciseDays)
                .weeklyTotalDurationMinutes(weeklyTotalDuration)
                .weeklyAvgCalories(weeklyAvgCalories)
                .exerciseChangeRate(exerciseChangeRate)
                .calorieChangeRate(calorieChangeRate)
                .weeklyGoalAchievementRate(goalAchievementRate)
                .message(message)
                .build();
    }

    // ========== Private Helper Methods ==========

    private User getUserById(String userId) {
        return userRepository.findByUserId(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
    }

    private HomeDto.UserSummary createUserSummary(User user) {
        String experienceLevelName = getExperienceLevelName(user.getExperienceLevel());
        String healthGoalName = getHealthGoalName(user.getHealthGoal());

        return HomeDto.UserSummary.builder()
                .name(user.getName())
                .experienceLevel(user.getExperienceLevel() != null ?
                        user.getExperienceLevel().name() : null)
                .experienceLevelName(experienceLevelName)
                .healthGoal(user.getHealthGoal() != null ?
                        user.getHealthGoal().name() : null)
                .healthGoalName(healthGoalName)
                .currentWeight(user.getWeight())
                .targetWeight(user.getWeightGoal())
                .workoutDaysPerWeek(user.getWorkoutDaysPerWeek())
                .build();
    }

    private HomeDto.LatestInBodySummary getLatestInBodySummary(User user) {
        return inBodyRepository.findTopByUserOrderByMeasurementDateDesc(user)
                .map(inBody -> {
                    InBody previousInBody = inBodyRepository
                            .findTopByUserAndMeasurementDateBeforeOrderByMeasurementDateDesc(
                                    user, inBody.getMeasurementDate())
                            .orElse(null);

                    BigDecimal weightChange = BigDecimal.ZERO;
                    if (previousInBody != null) {
                        weightChange = inBody.getWeight().subtract(previousInBody.getWeight());
                    }

                    String badge = determineAchievementBadge(weightChange, user.getHealthGoal());
                    String message = createInBodyMessage(weightChange);

                    return HomeDto.LatestInBodySummary.builder()
                            .measurementDate(inBody.getMeasurementDate())
                            .weight(inBody.getWeight())
                            .skeletalMuscleMass(inBody.getSkeletalMuscleMass())
                            .bodyFatPercentage(inBody.getBodyFatPercentage())
                            .bmi(inBody.getBmi())
                            .weightChange(weightChange)
                            .achievementBadge(badge)
                            .message(message)
                            .build();
                })
                .orElse(null);
    }

    private HomeDto.TodayExerciseSummary createEmptyExerciseSummary(LocalDate date) {
        return HomeDto.TodayExerciseSummary.builder()
                .date(date)
                .completed(false)
                .totalDurationMinutes(0)
                .totalCaloriesBurned(BigDecimal.ZERO)
                .exerciseCount(0)
                .mainCategories(new ArrayList<>())
                .message("오늘 운동을 시작해보세요! 💪")
                .build();
    }

    private HomeDto.TodayMealSummary createEmptyMealSummary(LocalDate date, User user) {
        BigDecimal targetCalories = getTargetCalories(user);

        return HomeDto.TodayMealSummary.builder()
                .date(date)
                .totalCalories(BigDecimal.ZERO)
                .targetCalories(targetCalories)
                .calorieAchievementRate(BigDecimal.ZERO)
                .totalCarbs(BigDecimal.ZERO)
                .totalProtein(BigDecimal.ZERO)
                .totalFat(BigDecimal.ZERO)
                .mealCount(0)
                .message("오늘의 식단을 기록해보세요! 🍽️")
                .build();
    }

    private BigDecimal getTargetCalories(User user) {
        return dailyNutritionGoalRepository.findByUser(user)
                .map(DailyNutritionGoal::getTargetCalories)
                .orElse(BigDecimal.valueOf(2000));
    }

    private String getExperienceLevelName(User.ExperienceLevel level) {
        if (level == null) return "설정 안 됨";
        switch (level) {
            case BEGINNER: return "초보자";
            case INTERMEDIATE: return "중급자";
            case ADVANCED: return "숙련자";
            default: return level.name();
        }
    }

    private String getHealthGoalName(User.HealthGoal goal) {
        if (goal == null) return "설정 안 됨";
        switch (goal) {
            case DIET: return "체중 감량";
            case BULK: return "벌크업";
            case LEAN_MASS: return "린매스";
            case MUSCLE_GAIN: return "근육 증가";
            case MAINTENANCE: return "유지";
            default: return goal.name();
        }
    }

    private String createExerciseMessage(int count, int duration) {
        if (count == 0) {
            return "오늘 운동을 시작해보세요! 💪";
        } else if (duration >= 60) {
            return "훌륭합니다! 오늘 운동을 완료했어요! 💪";
        } else if (duration >= 30) {
            return "좋아요! 조금만 더 힘내보세요! 🔥";
        } else if (duration > 0) {
            return "시작이 반이에요! 계속 이어가세요! 👍";
        } else {
            return String.format("오늘 %d개의 운동을 완료했습니다! 💪", count);
        }
    }

    private String createMealMessage(BigDecimal total, BigDecimal target, BigDecimal rate) {
        if (total.compareTo(BigDecimal.ZERO) == 0) {
            return "오늘의 식단을 기록해보세요! 🍽️";
        } else if (rate.compareTo(BigDecimal.valueOf(90)) >= 0 &&
                rate.compareTo(BigDecimal.valueOf(110)) <= 0) {
            return String.format("목표 칼로리의 %.0f%%를 달성했어요! 완벽해요! 👍",
                    rate.doubleValue());
        } else if (rate.compareTo(BigDecimal.valueOf(110)) > 0) {
            return String.format("목표보다 %.0fkcal 초과했어요. 운동으로 소모해볼까요? 💪",
                    total.subtract(target).doubleValue());
        } else {
            return String.format("목표 칼로리의 %.0f%%를 섭취했어요! 👍",
                    rate.doubleValue());
        }
    }

    private String createInBodyMessage(BigDecimal weightChange) {
        if (weightChange.compareTo(BigDecimal.ZERO) == 0) {
            return "체중을 잘 유지하고 있어요! 👍";
        } else if (weightChange.compareTo(BigDecimal.ZERO) < 0) {
            return String.format("지난 측정 대비 %.1fkg 감량에 성공했어요! 🎉",
                    weightChange.abs().doubleValue());
        } else {
            return String.format("지난 측정 대비 %.1fkg 증가했어요. 💪",
                    weightChange.doubleValue());
        }
    }

    private String createWeeklyMessage(int exerciseDays, BigDecimal achievementRate) {
        if (exerciseDays == 0) {
            return "이번주 첫 운동을 시작해보세요! 💪";
        } else if (achievementRate.compareTo(BigDecimal.valueOf(100)) >= 0) {
            return "이번주 목표를 달성했어요! 정말 대단해요! 🎉";
        } else {
            return String.format("이번주 목표의 %.0f%%를 달성했어요! 조금만 더 화이팅! 💪",
                    achievementRate.doubleValue());
        }
    }

    private String determineAchievementBadge(BigDecimal weightChange, User.HealthGoal goal) {
        if (goal == User.HealthGoal.DIET &&
                weightChange.compareTo(BigDecimal.valueOf(-1)) <= 0) {
            return "GOLD";
        } else if (goal == User.HealthGoal.MUSCLE_GAIN &&
                weightChange.compareTo(BigDecimal.valueOf(0.5)) >= 0) {
            return "GOLD";
        } else if (weightChange.abs().compareTo(BigDecimal.valueOf(0.5)) < 0) {
            return "SILVER";
        }
        return "BRONZE";
    }

    private BigDecimal calculateChangeRate(int current, int previous) {
        if (previous == 0) {
            return current > 0 ? BigDecimal.valueOf(100) : BigDecimal.ZERO;
        }

        return BigDecimal.valueOf(current - previous)
                .divide(BigDecimal.valueOf(previous), 4, RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100))
                .setScale(1, RoundingMode.HALF_UP);
    }

    private BigDecimal calculateCalorieChangeRate(List<Meal> currentWeek, List<Meal> lastWeek) {
        BigDecimal currentTotal = currentWeek.stream()
                .map(Meal::getTotalCalories)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal lastTotal = lastWeek.stream()
                .map(Meal::getTotalCalories)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        if (lastTotal.compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.ZERO;
        }

        return currentTotal.subtract(lastTotal)
                .divide(lastTotal, 4, RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100))
                .setScale(1, RoundingMode.HALF_UP);
    }
}