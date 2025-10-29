package rto.intelfit.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import rto.intelfit.domain.*;
import rto.intelfit.domain.User.Gender;
import rto.intelfit.dto.RecommendedExerciseDto;
import rto.intelfit.exception.BusinessException;
import rto.intelfit.exception.ErrorCode;
import rto.intelfit.repository.*;
import rto.intelfit.security.CustomUserPrincipal;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * AI 기반 운동 추천 서비스
 * <p>
 * 주요 기능:
 * 1. 사용자 맞춤 운동 추천 생성
 * 2. 식단 데이터 기반 운동 추천
 * 3. 추천 플랜 저장 및 관리
 * 4. 추천 플랜을 실제 운동 기록으로 적용
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ExerciseRecommendationService {

    private final RecommendedExercisePlanRepository recommendedExercisePlanRepository;
    private final UserRepository userRepository;
    private final InBodyRepository inBodyRepository;
    private final MealRepository mealRepository;
    private final DailyNutritionGoalRepository dailyNutritionGoalRepository;
    private final ExerciseRepository exerciseRepository;
    private final AIServerService aiServerService;

    /**
     * AI 기반 맞춤 운동 추천 생성
     */
    @Transactional
    public RecommendedExerciseDto.GenerateRecommendationResponse generateRecommendation(
            CustomUserPrincipal userPrincipal,
            LocalDate baseDate) {

        User user = getUserById(userPrincipal.getUserId());

        // 사용자 건강 정보 조회
        InBody latestInBody = inBodyRepository.findTopByUserOrderByMeasurementDateDesc(user)
                .orElseThrow(() -> new BusinessException(ErrorCode.INBODY_NOT_FOUND));

        // AI 추천 플랜 생성 (실제로는 AI 서버 호출)
        RecommendedExercisePlan plan = createAIRecommendedPlan(user, latestInBody, baseDate);

        RecommendedExercisePlan savedPlan = recommendedExercisePlanRepository.save(plan);

        log.info("AI 운동 추천 생성 완료 - 사용자: {}, 플랜 ID: {}",
                user.getUserId(), savedPlan.getId());

        return RecommendedExerciseDto.GenerateRecommendationResponse.builder()
                .success(true)
                .message("맞춤 운동 플랜이 생성되었습니다")
                .plan(RecommendedExerciseDto.ExercisePlanDetailResponse.from(savedPlan))
                .build();
    }

    /**
     * 식단 연동 운동 추천 생성
     */
    @Transactional
    public RecommendedExerciseDto.MealBasedRecommendationResponse generateMealBasedRecommendation(
            CustomUserPrincipal userPrincipal,
            LocalDate mealDate) {

        User user = getUserById(userPrincipal.getUserId());

        // 해당 날짜의 식단 데이터 조회
        List<Meal> meals = mealRepository.findByUserAndMealDate(user, mealDate);

        if (meals.isEmpty()) {
            throw new BusinessException(ErrorCode.MEAL_NOT_FOUND,
                    "해당 날짜의 식단 기록이 없습니다");
        }

        // 총 섭취 칼로리 계산
        BigDecimal totalIntakeCalories = meals.stream()
                .map(Meal::getTotalCalories)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // 목표 칼로리 조회
        DailyNutritionGoal nutritionGoal = dailyNutritionGoalRepository
                .findByUser(user)
                .orElse(null);

        BigDecimal targetCalories = nutritionGoal != null
                ? nutritionGoal.getTargetCalories()
                : calculateDefaultTargetCalories(user);

        // 초과 칼로리 계산
        BigDecimal excessCalories = totalIntakeCalories.subtract(targetCalories);

        // 권장 칼로리 소모량 계산
        BigDecimal recommendedCaloriesBurn = excessCalories.compareTo(BigDecimal.ZERO) > 0
                ? excessCalories.multiply(BigDecimal.valueOf(1.2)) // 20% 여유분
                : BigDecimal.ZERO;

        // 식단 분석 정보 생성
        RecommendedExerciseDto.MealAnalysisInfo mealAnalysis = createMealAnalysisInfo(
                mealDate, totalIntakeCalories, targetCalories, excessCalories, recommendedCaloriesBurn);

        // 식단 기반 운동 추천 플랜 생성
        InBody latestInBody = inBodyRepository.findTopByUserOrderByMeasurementDateDesc(user)
                .orElseThrow(() -> new BusinessException(ErrorCode.INBODY_NOT_FOUND));

        RecommendedExercisePlan plan = createMealBasedRecommendedPlan(
                user, latestInBody, mealDate, recommendedCaloriesBurn);

        RecommendedExercisePlan savedPlan = recommendedExercisePlanRepository.save(plan);

        log.info("식단 연동 운동 추천 생성 완료 - 사용자: {}, 플랜 ID: {}, 초과 칼로리: {}kcal",
                user.getUserId(), savedPlan.getId(), excessCalories);

        return RecommendedExerciseDto.MealBasedRecommendationResponse.builder()
                .success(true)
                .message("식단 기반 운동 플랜이 생성되었습니다")
                .mealAnalysis(mealAnalysis)
                .plan(RecommendedExerciseDto.ExercisePlanDetailResponse.from(savedPlan))
                .build();
    }

    /**
     * 저장된 추천 플랜 목록 조회
     */
    public RecommendedExerciseDto.SavedPlansResponse getSavedPlans(CustomUserPrincipal userPrincipal) {
        User user = getUserById(userPrincipal.getUserId());

        List<RecommendedExercisePlan> plans = recommendedExercisePlanRepository
                .findByUserAndIsSavedTrueOrderByCreatedAtDesc(user);

        List<RecommendedExerciseDto.ExercisePlanSummary> planSummaries = plans.stream()
                .map(RecommendedExerciseDto.ExercisePlanSummary::from)
                .collect(Collectors.toList());

        return RecommendedExerciseDto.SavedPlansResponse.builder()
                .plans(planSummaries)
                .totalCount(planSummaries.size())
                .build();
    }

    /**
     * 특정 추천 플랜 상세 조회
     */
    public RecommendedExerciseDto.ExercisePlanDetailResponse getPlanDetail(
            CustomUserPrincipal userPrincipal,
            Long planId) {

        RecommendedExercisePlan plan = getPlanByIdAndUserId(planId, userPrincipal.getUserId());

        return RecommendedExerciseDto.ExercisePlanDetailResponse.from(plan);
    }

    /**
     * 추천 플랜 저장
     */
    @Transactional
    public RecommendedExerciseDto.SavePlanResponse savePlan(
            CustomUserPrincipal userPrincipal,
            Long planId) {

        RecommendedExercisePlan plan = getPlanByIdAndUserId(planId, userPrincipal.getUserId());

        plan.setIsSaved(true);

        log.info("추천 플랜 저장 완료 - 사용자: {}, 플랜 ID: {}",
                userPrincipal.getUserId(), planId);

        return RecommendedExerciseDto.SavePlanResponse.builder()
                .success(true)
                .message("추천 플랜이 저장되었습니다")
                .planId(planId)
                .build();
    }

    /**
     * 추천 플랜 삭제
     */
    @Transactional
    public void deletePlan(CustomUserPrincipal userPrincipal, Long planId) {
        RecommendedExercisePlan plan = getPlanByIdAndUserId(planId, userPrincipal.getUserId());

        recommendedExercisePlanRepository.delete(plan);

        log.info("추천 플랜 삭제 완료 - 사용자: {}, 플랜 ID: {}",
                userPrincipal.getUserId(), planId);
    }

    /**
     * 추천 플랜을 실제 운동 기록으로 적용
     */
    @Transactional
    public RecommendedExerciseDto.ApplyPlanResponse applyPlan(
            CustomUserPrincipal userPrincipal,
            Long planId,
            LocalDate exerciseDate) {

        RecommendedExercisePlan plan = getPlanByIdAndUserId(planId, userPrincipal.getUserId());
        User user = plan.getUser();

        List<Long> createdExerciseIds = new ArrayList<>();

        // 각 루틴을 실제 운동 기록으로 변환
        for (RecommendedExerciseRoutine routine : plan.getRoutines()) {
            Exercise exercise = convertRoutineToExercise(routine, user, exerciseDate);
            Exercise savedExercise = exerciseRepository.save(exercise);
            createdExerciseIds.add(savedExercise.getId());
        }

        log.info("추천 플랜 적용 완료 - 사용자: {}, 플랜 ID: {}, 생성된 운동 수: {}",
                userPrincipal.getUserId(), planId, createdExerciseIds.size());

        return RecommendedExerciseDto.ApplyPlanResponse.builder()
                .success(true)
                .message("추천 플랜이 운동 기록으로 저장되었습니다")
                .createdExerciseIds(createdExerciseIds)
                .build();
    }

    // ========== Private Helper Methods ==========

    private User getUserById(String userId) {
        return userRepository.findByUserId(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
    }

    private RecommendedExercisePlan getPlanByIdAndUserId(Long planId, String userId) {
        RecommendedExercisePlan plan = recommendedExercisePlanRepository.findById(planId)
                .orElseThrow(() -> new BusinessException(ErrorCode.RECOMMENDED_EXERCISE_PLAN_NOT_FOUND,
                        "해당 추천 플랜을 찾을 수 없습니다"));

        if (!plan.getUser().getUserId().equals(userId)) {
            throw new BusinessException(ErrorCode.RECOMMENDED_EXERCISE_PLAN_ACCESS_DENIED,
                    "해당 추천 플랜에 접근할 권한이 없습니다");
        }

        return plan;
    }

    /**
     * AI 추천 운동 플랜 생성 (샘플 - 실제로는 AI 서버 호출)
     */
    private RecommendedExercisePlan createAIRecommendedPlan(
            User user,
            InBody inBody,
            LocalDate baseDate) {

        // 사용자 목표에 따른 플랜 생성
        String planName = getPlanNameByGoal(user.getHealthGoal());
        String description = getPlanDescriptionByGoal(user.getHealthGoal());
        int targetWeeklyMinutes = getTargetWeeklyMinutes(user.getHealthGoal());

        RecommendedExercisePlan plan = RecommendedExercisePlan.builder()
                .user(user)
                .planName(planName)
                .description(description)
                .targetWeeklyMinutes(targetWeeklyMinutes)
                .recommendationReason(createRecommendationReason(user, inBody))
                .isSaved(false)
                .build();

        // 루틴 생성
        if (user.getHealthGoal() == User.HealthGoal.DIET) {
            createDietRoutines(plan);
        } else if (user.getHealthGoal() == User.HealthGoal.MUSCLE_GAIN) {
            createMuscleGainRoutines(plan);
        } else {
            createMaintenanceRoutines(plan);
        }

        return plan;
    }

    /**
     * 식단 기반 추천 운동 플랜 생성
     */
    private RecommendedExercisePlan createMealBasedRecommendedPlan(
            User user,
            InBody inBody,
            LocalDate mealDate,
            BigDecimal recommendedCaloriesBurn) {

        String planName = "식단 조절을 위한 칼로리 소모 플랜";
        String description = String.format("목표 칼로리 소모: %.0fkcal", recommendedCaloriesBurn.doubleValue());

        RecommendedExercisePlan plan = RecommendedExercisePlan.builder()
                .user(user)
                .planName(planName)
                .description(description)
                .targetWeeklyMinutes(calculateMinutesFromCalories(recommendedCaloriesBurn))
                .recommendationReason("오늘 섭취한 칼로리를 고려한 맞춤 운동 플랜입니다")
                .isSaved(false)
                .build();

        // 칼로리 소모 중심 유산소 루틴 생성
        createCalorieBurnRoutine(plan, recommendedCaloriesBurn);

        return plan;
    }

    /**
     * 다이어트 목표 루틴 생성
     */
    private void createDietRoutines(RecommendedExercisePlan plan) {
        // 유산소 루틴
        RecommendedExerciseRoutine cardioRoutine = RecommendedExerciseRoutine.builder()
                .routineName("유산소 집중 데이")
                .dayOfWeek("월요일, 수요일, 금요일")
                .exerciseCategory(Exercise.ExerciseCategory.CARDIO)
                .estimatedDurationMinutes(45)
                .build();

        cardioRoutine.addItem(RecommendedExerciseItem.builder()
                .cardioType(Exercise.CardioType.TREADMILL)
                .targetDistance(new BigDecimal("5.0"))
                .targetDurationMinutes(30)
                .targetCaloriesBurn(new BigDecimal("300"))
                .description("중강도 러닝으로 지방 연소")
                .exerciseOrder(1)
                .build());

        plan.addRoutine(cardioRoutine);

        // 근력 루틴
        RecommendedExerciseRoutine resistanceRoutine = RecommendedExerciseRoutine.builder()
                .routineName("전신 근력 운동")
                .dayOfWeek("화요일, 목요일")
                .exerciseCategory(Exercise.ExerciseCategory.RESISTANCE)
                .estimatedDurationMinutes(60)
                .build();

        resistanceRoutine.addItem(RecommendedExerciseItem.builder()
                .resistanceExerciseType(Exercise.ResistanceExerciseType.BARBELL_SQUAT)
                .muscleGroup(Exercise.MuscleGroup.LEGS)
                .recommendedSets(3)
                .recommendedReps("12-15")
                .recommendedWeight("체중의 60-70%")
                .recommendedRestSeconds(60)
                .description("하체 근력 유지 및 대사량 증가")
                .exerciseOrder(1)
                .build());

        plan.addRoutine(resistanceRoutine);
    }

    /**
     * 근육 증가 목표 루틴 생성
     */
    private void createMuscleGainRoutines(RecommendedExercisePlan plan) {
        // 상체 루틴
        RecommendedExerciseRoutine upperBodyRoutine = RecommendedExerciseRoutine.builder()
                .routineName("상체 집중 데이")
                .dayOfWeek("월요일, 목요일")
                .exerciseCategory(Exercise.ExerciseCategory.RESISTANCE)
                .estimatedDurationMinutes(75)
                .build();

        upperBodyRoutine.addItem(RecommendedExerciseItem.builder()
                .resistanceExerciseType(Exercise.ResistanceExerciseType.BENCH_PRESS)
                .muscleGroup(Exercise.MuscleGroup.CHEST)
                .recommendedSets(4)
                .recommendedReps("6-8")
                .recommendedWeight("1RM의 80-85%")
                .recommendedRestSeconds(120)
                .description("가슴 근육 발달을 위한 고중량 훈련")
                .exerciseOrder(1)
                .build());

        plan.addRoutine(upperBodyRoutine);

        // 하체 루틴
        RecommendedExerciseRoutine lowerBodyRoutine = RecommendedExerciseRoutine.builder()
                .routineName("하체 집중 데이")
                .dayOfWeek("화요일, 금요일")
                .exerciseCategory(Exercise.ExerciseCategory.RESISTANCE)
                .estimatedDurationMinutes(75)
                .build();

        lowerBodyRoutine.addItem(RecommendedExerciseItem.builder()
                .resistanceExerciseType(Exercise.ResistanceExerciseType.BARBELL_SQUAT)
                .muscleGroup(Exercise.MuscleGroup.LEGS)
                .recommendedSets(5)
                .recommendedReps("5-8")
                .recommendedWeight("1RM의 85-90%")
                .recommendedRestSeconds(180)
                .description("하체 근육량 증가를 위한 고중량 스쿼트")
                .exerciseOrder(1)
                .build());

        plan.addRoutine(lowerBodyRoutine);
    }

    /**
     * 유지 목표 루틴 생성
     */
    private void createMaintenanceRoutines(RecommendedExercisePlan plan) {
        RecommendedExerciseRoutine mixedRoutine = RecommendedExerciseRoutine.builder()
                .routineName("균형 잡힌 전신 운동")
                .dayOfWeek("월요일, 수요일, 금요일")
                .exerciseCategory(Exercise.ExerciseCategory.RESISTANCE)
                .estimatedDurationMinutes(60)
                .build();

        mixedRoutine.addItem(RecommendedExerciseItem.builder()
                .resistanceExerciseType(Exercise.ResistanceExerciseType.BARBELL_SQUAT)
                .muscleGroup(Exercise.MuscleGroup.LEGS)
                .recommendedSets(3)
                .recommendedReps("10-12")
                .recommendedWeight("1RM의 70-75%")
                .recommendedRestSeconds(90)
                .description("전신 근력 유지")
                .exerciseOrder(1)
                .build());

        plan.addRoutine(mixedRoutine);
    }

    /**
     * 칼로리 소모 중심 루틴 생성
     */
    private void createCalorieBurnRoutine(RecommendedExercisePlan plan, BigDecimal targetCalories) {
        int minutes = calculateMinutesFromCalories(targetCalories);

        RecommendedExerciseRoutine routine = RecommendedExerciseRoutine.builder()
                .routineName("칼로리 소모 집중 운동")
                .dayOfWeek("오늘")
                .exerciseCategory(Exercise.ExerciseCategory.CARDIO)
                .estimatedDurationMinutes(minutes)
                .build();

        routine.addItem(RecommendedExerciseItem.builder()
                .cardioType(Exercise.CardioType.TREADMILL)
                .targetDurationMinutes(minutes)
                .targetCaloriesBurn(targetCalories)
                .description("초과 섭취 칼로리 소모를 위한 유산소 운동")
                .exerciseOrder(1)
                .build());

        plan.addRoutine(routine);
    }

    private String getPlanNameByGoal(User.HealthGoal goal) {
        if (goal == null) return "균형 잡힌 운동 플랜";
        switch (goal) {
            case DIET:
                return "체지방 감량을 위한 유산소 중심 플랜";
            case MUSCLE_GAIN:
                return "근육 증가를 위한 고강도 근력 플랜";
            case MAINTENANCE:
                return "건강 유지를 위한 균형 운동 플랜";
            default:
                return "맞춤 운동 플랜";
        }
    }

    private String getPlanDescriptionByGoal(User.HealthGoal goal) {
        if (goal == null) return "주 3~4회, 유산소와 근력 운동 병행";
        switch (goal) {
            case DIET:
                return "주 5회, 유산소 중심 + 근력 운동 보조";
            case MUSCLE_GAIN:
                return "주 4~5회, 고강도 근력 운동 중심";
            case MAINTENANCE:
                return "주 3회, 적절한 강도의 전신 운동";
            default:
                return "개인 맞춤 운동 계획";
        }
    }

    private int getTargetWeeklyMinutes(User.HealthGoal goal) {
        if (goal == null) return 180;
        switch (goal) {
            case DIET:
                return 300;
            case MUSCLE_GAIN:
                return 300;
            case MAINTENANCE:
                return 180;
            default:
                return 180;
        }
    }

    private String createRecommendationReason(User user, InBody inBody) {
        String goalDescription = user.getHealthGoal() != null ?
                user.getHealthGoal().name() : "유지";

        return String.format(
                "사용자의 건강 목표(%s)와 현재 체지방률(%.1f%%)을 고려한 최적화된 플랜입니다",
                goalDescription,
                inBody.getBodyFatPercentage().doubleValue()
        );
    }

    private BigDecimal calculateDefaultTargetCalories(User user) {
        // 기초대사량 계산 (Harris-Benedict 공식)
        // 남성: 66 + (13.7 × 체중) + (5 × 키) - (6.8 × 나이)
        // 여성: 655 + (9.6 × 체중) + (1.8 × 키) - (4.7 × 나이)
        int age = calculateAge(user);
        BigDecimal bmr;

        if (user.getGender() == Gender.M) {
            bmr = BigDecimal.valueOf(66)
                    .add(BigDecimal.valueOf(13.7).multiply(BigDecimal.valueOf(user.getWeight())))
                    .add(BigDecimal.valueOf(5).multiply(BigDecimal.valueOf(user.getHeight())))
                    .subtract(BigDecimal.valueOf(6.8).multiply(BigDecimal.valueOf(age)));
        } else {
            bmr = BigDecimal.valueOf(655)
                    .add(BigDecimal.valueOf(9.6).multiply(BigDecimal.valueOf(user.getWeight())))
                    .add(BigDecimal.valueOf(1.8).multiply(BigDecimal.valueOf(user.getHeight())))
                    .subtract(BigDecimal.valueOf(4.7).multiply(BigDecimal.valueOf(age)));
        }

        // 활동 계수 적용 (보통 활동: 1.55)
        return bmr.multiply(BigDecimal.valueOf(1.55)).setScale(0, RoundingMode.HALF_UP);
    }

    private int calculateAge(User user) {
        if (user.getBirthDate() == null) return 30;
        return java.time.Period.between(user.getBirthDate(), LocalDate.now()).getYears();
    }

    private RecommendedExerciseDto.MealAnalysisInfo createMealAnalysisInfo(
            LocalDate analysisDate,
            BigDecimal totalCalories,
            BigDecimal targetCalories,
            BigDecimal excessCalories,
            BigDecimal recommendedCaloriesBurn) {

        String message;
        if (excessCalories.compareTo(BigDecimal.ZERO) > 0) {
            message = String.format("목표 대비 %.0fkcal 초과 섭취하셨습니다. " +
                            "약 %.0fkcal의 칼로리 소모를 권장합니다.",
                    excessCalories.doubleValue(),
                    recommendedCaloriesBurn.doubleValue());
        } else {
            message = "목표 칼로리 범위 내에서 섭취하셨습니다.";
        }

        return RecommendedExerciseDto.MealAnalysisInfo.builder()
                .analysisDate(analysisDate)
                .totalCalories(totalCalories)
                .targetCalories(targetCalories)
                .excessCalories(excessCalories)
                .recommendedCaloriesBurn(recommendedCaloriesBurn)
                .analysisMessage(message)
                .build();
    }

    private int calculateMinutesFromCalories(BigDecimal calories) {
        // 일반적으로 유산소 운동 1시간 = 약 500kcal 소모
        // minutes = (calories / 500) * 60
        return calories
                .divide(BigDecimal.valueOf(500), 2, RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(60))
                .intValue();
    }

    private Exercise convertRoutineToExercise(
            RecommendedExerciseRoutine routine,
            User user,
            LocalDate exerciseDate) {

        Exercise exercise = Exercise.builder()
                .user(user)
                .exerciseDate(exerciseDate)
                .exerciseCategory(routine.getExerciseCategory())
                .totalDurationMinutes(routine.getEstimatedDurationMinutes())
                .memo("추천 플랜에서 생성됨: " + routine.getRoutineName())
                .build();

        // 각 운동 항목을 ExerciseSet으로 변환
        for (RecommendedExerciseItem item : routine.getItems()) {
            ExerciseSet set = convertItemToExerciseSet(item);
            exercise.addExerciseSet(set);
        }

        return exercise;
    }

    private ExerciseSet convertItemToExerciseSet(RecommendedExerciseItem item) {
        ExerciseSet.ExerciseSetBuilder builder = ExerciseSet.builder();

        if (item.isCardio()) {
            builder.cardioType(item.getCardioType())
                    .distance(item.getTargetDistance())
                    .durationMinutes(item.getTargetDurationMinutes())
                    .caloriesBurned(item.getTargetCaloriesBurn());
        }

        if (item.isResistance()) {
            builder.resistanceExerciseType(item.getResistanceExerciseType())
                    .muscleGroup(item.getMuscleGroup())
                    .setNumber(1) // 첫 번째 세트
                    .restSeconds(item.getRecommendedRestSeconds());

            // 추천 무게와 반복수 파싱 (예: "80-100" -> 평균값 사용)
            if (item.getRecommendedWeight() != null) {
                // 숫자만 추출하여 평균 계산 (간단한 예시)
                try {
                    String[] parts = item.getRecommendedWeight().split("-");
                    if (parts.length > 0) {
                        builder.weight(new BigDecimal(parts[0].replaceAll("[^0-9.]", "")));
                    }
                } catch (Exception e) {
                    log.warn("무게 파싱 실패: {}", item.getRecommendedWeight());
                }
            }

            if (item.getRecommendedReps() != null) {
                try {
                    String[] parts = item.getRecommendedReps().split("-");
                    if (parts.length > 0) {
                        builder.reps(Integer.parseInt(parts[0].replaceAll("[^0-9]", "")));
                    }
                } catch (Exception e) {
                    log.warn("반복수 파싱 실패: {}", item.getRecommendedReps());
                }
            }
        }

        builder.memo(item.getDescription());

        return builder.build();
    }
}
