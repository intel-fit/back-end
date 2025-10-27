package rto.intelfit.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import rto.intelfit.domain.*;
import rto.intelfit.dto.ExerciseDto;
import rto.intelfit.exception.BusinessException;
import rto.intelfit.exception.ErrorCode;
import rto.intelfit.repository.ExerciseRepository;
import rto.intelfit.repository.InBodyRepository;
import rto.intelfit.repository.UserRepository;
import rto.intelfit.security.CustomUserPrincipal;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 운동 기록 관리 서비스
 *
 * 주요 기능:
 * 1. 운동 기록 CRUD
 * 2. 일별/주간 운동 통계 조회
 * 3. 체지방 감량 분석
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ExerciseService {

    private final ExerciseRepository exerciseRepository;
    private final UserRepository userRepository;
    private final InBodyRepository inBodyRepository;

    /**
     * 운동 기록 추가
     */
    @Transactional
    public ExerciseDto.ExerciseCreateResponse createExercise(
            CustomUserPrincipal userPrincipal,
            ExerciseDto.ExerciseCreateRequest request) {

        User user = getUserById(userPrincipal.getUserId());

        // Exercise 엔티티 생성
        Exercise exercise = Exercise.builder()
                .user(user)
                .exerciseDate(request.getExerciseDate())
                .exerciseCategory(request.getExerciseCategory())
                .totalDurationMinutes(request.getTotalDurationMinutes())
                .memo(request.getMemo())
                .build();

        // ExerciseSet 추가
        for (ExerciseDto.ExerciseSetRequest setRequest : request.getExerciseSets()) {
            ExerciseSet exerciseSet = createExerciseSet(setRequest, request.getExerciseCategory());
            exercise.addExerciseSet(exerciseSet);
        }

        Exercise savedExercise = exerciseRepository.save(exercise);

        log.info("운동 기록 추가 완료 - ID: {}, 사용자: {}, 날짜: {}",
                savedExercise.getId(), user.getUserId(), savedExercise.getExerciseDate());

        return ExerciseDto.ExerciseCreateResponse.builder()
                .success(true)
                .message("운동 기록이 성공적으로 추가되었습니다")
                .exercise(ExerciseDto.ExerciseDetailResponse.from(savedExercise))
                .build();
    }

    /**
     * 운동 기록 수정
     */
    @Transactional
    public ExerciseDto.ExerciseCreateResponse updateExercise(
            CustomUserPrincipal userPrincipal,
            Long exerciseId,
            ExerciseDto.ExerciseCreateRequest request) {

        Exercise exercise = getExerciseByIdAndUserId(exerciseId, userPrincipal.getUserId());

        // 기본 정보 수정
        exercise.setExerciseDate(request.getExerciseDate());
        exercise.setExerciseCategory(request.getExerciseCategory());
        exercise.setTotalDurationMinutes(request.getTotalDurationMinutes());
        exercise.setMemo(request.getMemo());

        // 기존 세트 삭제
        exercise.getExerciseSets().clear();

        // 새로운 세트 추가
        for (ExerciseDto.ExerciseSetRequest setRequest : request.getExerciseSets()) {
            ExerciseSet exerciseSet = createExerciseSet(setRequest, request.getExerciseCategory());
            exercise.addExerciseSet(exerciseSet);
        }

        log.info("운동 기록 수정 완료 - ID: {}, 사용자: {}", exerciseId, userPrincipal.getUserId());

        return ExerciseDto.ExerciseCreateResponse.builder()
                .success(true)
                .message("운동 기록이 성공적으로 수정되었습니다")
                .exercise(ExerciseDto.ExerciseDetailResponse.from(exercise))
                .build();
    }

    /**
     * 운동 기록 삭제
     */
    @Transactional
    public void deleteExercise(CustomUserPrincipal userPrincipal, Long exerciseId) {
        Exercise exercise = getExerciseByIdAndUserId(exerciseId, userPrincipal.getUserId());

        exerciseRepository.delete(exercise);

        log.info("운동 기록 삭제 완료 - ID: {}, 사용자: {}", exerciseId, userPrincipal.getUserId());
    }

    /**
     * 특정 날짜의 운동 기록 조회
     */
    public ExerciseDto.DailyExercisesResponse getDailyExercises(
            CustomUserPrincipal userPrincipal,
            LocalDate date) {

        User user = getUserById(userPrincipal.getUserId());

        List<Exercise> exercises = exerciseRepository.findByUserAndExerciseDate(user, date);

        List<ExerciseDto.ExerciseDetailResponse> exerciseResponses = exercises.stream()
                .map(ExerciseDto.ExerciseDetailResponse::from)
                .collect(Collectors.toList());

        // 일일 총 운동 시간 계산
        int dailyTotalDuration = exercises.stream()
                .map(Exercise::getTotalDurationMinutes)
                .filter(duration -> duration != null)
                .mapToInt(Integer::intValue)
                .sum();

        // 일일 총 칼로리 소모 계산 (유산소 운동)
        BigDecimal dailyTotalCalories = exercises.stream()
                .flatMap(exercise -> exercise.getExerciseSets().stream())
                .filter(ExerciseSet::isCardio)
                .map(ExerciseSet::getCaloriesBurned)
                .filter(calories -> calories != null)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return ExerciseDto.DailyExercisesResponse.builder()
                .date(date)
                .exercises(exerciseResponses)
                .dailyTotalDurationMinutes(dailyTotalDuration)
                .dailyTotalCalories(dailyTotalCalories)
                .build();
    }

    /**
     * 특정 운동 기록 상세 조회
     */
    public ExerciseDto.ExerciseDetailResponse getExerciseDetail(
            CustomUserPrincipal userPrincipal,
            Long exerciseId) {

        Exercise exercise = getExerciseByIdAndUserId(exerciseId, userPrincipal.getUserId());

        return ExerciseDto.ExerciseDetailResponse.from(exercise);
    }

    /**
     * 주간 운동 통계 조회
     */
    public ExerciseDto.WeeklyExerciseStatsResponse getWeeklyStats(
            CustomUserPrincipal userPrincipal,
            LocalDate startDate,
            LocalDate endDate) {

        User user = getUserById(userPrincipal.getUserId());

        List<Exercise> exercises = exerciseRepository.findByUserAndExerciseDateBetween(user, startDate, endDate);

        // 주간 총 운동 시간
        int weeklyTotalDuration = exercises.stream()
                .map(Exercise::getTotalDurationMinutes)
                .filter(duration -> duration != null)
                .mapToInt(Integer::intValue)
                .sum();

        // 주간 운동 일수
        long weeklyExerciseDays = exercises.stream()
                .map(Exercise::getExerciseDate)
                .distinct()
                .count();

        // 주간 총 칼로리 소모
        BigDecimal weeklyTotalCalories = exercises.stream()
                .flatMap(exercise -> exercise.getExerciseSets().stream())
                .filter(ExerciseSet::isCardio)
                .map(ExerciseSet::getCaloriesBurned)
                .filter(calories -> calories != null)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // 일일 평균 운동 시간
        int dailyAvgDuration = weeklyExerciseDays > 0
                ? (int) (weeklyTotalDuration / weeklyExerciseDays)
                : 0;

        // 지난주 대비 변화율 계산
        LocalDate previousStartDate = startDate.minusWeeks(1);
        LocalDate previousEndDate = endDate.minusWeeks(1);
        List<Exercise> previousExercises = exerciseRepository.findByUserAndExerciseDateBetween(
                user, previousStartDate, previousEndDate);

        int previousWeeklyDuration = previousExercises.stream()
                .map(Exercise::getTotalDurationMinutes)
                .filter(duration -> duration != null)
                .mapToInt(Integer::intValue)
                .sum();

        BigDecimal weeklyChangeRate = calculateChangeRate(weeklyTotalDuration, previousWeeklyDuration);

        // 일별 상세 통계
        List<ExerciseDto.DailyExerciseStatItem> dailyStats = createDailyStats(exercises, startDate, endDate);

        return ExerciseDto.WeeklyExerciseStatsResponse.builder()
                .startDate(startDate)
                .endDate(endDate)
                .weeklyTotalDurationMinutes(weeklyTotalDuration)
                .weeklyExerciseDays((int) weeklyExerciseDays)
                .weeklyTotalCalories(weeklyTotalCalories)
                .dailyAvgDurationMinutes(dailyAvgDuration)
                .weeklyChangeRate(weeklyChangeRate)
                .dailyStats(dailyStats)
                .build();
    }

    /**
     * 체지방 감량 분석
     */
    public ExerciseDto.FatLossAnalysisResponse getFatLossAnalysis(CustomUserPrincipal userPrincipal) {
        User user = getUserById(userPrincipal.getUserId());

        // 가장 최근 인바디 데이터 조회
        InBody latestInBody = inBodyRepository.findTopByUserOrderByMeasurementDateDesc(user)
                .orElseThrow(() -> new BusinessException(ErrorCode.INBODY_NOT_FOUND));

        BigDecimal currentBodyFatPercentage = latestInBody.getBodyFatPercentage();

        // 목표 체지방률 설정 (건강 목표에 따라)
        BigDecimal targetBodyFatPercentage = getTargetBodyFatPercentage(user, currentBodyFatPercentage);

        // 감량 필요 체지방량 계산
        BigDecimal currentBodyFatMass = latestInBody.getBodyFatMass();
        BigDecimal targetBodyFatMass = BigDecimal.valueOf(user.getWeight())
                .multiply(targetBodyFatPercentage)
                .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
        BigDecimal requiredFatLoss = currentBodyFatMass.subtract(targetBodyFatMass);

        // 주간 권장 운동 시간 계산 (체지방 1kg 감량 = 약 7700kcal)
        // 일반적으로 유산소 운동 1시간 = 약 400~600kcal 소모
        // 주간 0.5kg 감량 목표 = 3850kcal = 약 7시간 운동
        int recommendedWeeklyMinutes = calculateRecommendedWeeklyMinutes(requiredFatLoss);

        // 예상 소요 기간 계산 (주당 0.5kg 감량 가정)
        int estimatedWeeks = requiredFatLoss.compareTo(BigDecimal.ZERO) > 0
                ? requiredFatLoss.divide(BigDecimal.valueOf(0.5), 0, RoundingMode.UP).intValue()
                : 0;

        // 권장 운동 타입
        List<String> recommendedExerciseTypes = getRecommendedExerciseTypes(user);

        // 분석 메시지
        String analysisMessage = createFatLossAnalysisMessage(
                requiredFatLoss, estimatedWeeks, recommendedWeeklyMinutes);

        return ExerciseDto.FatLossAnalysisResponse.builder()
                .currentBodyFatPercentage(currentBodyFatPercentage)
                .targetBodyFatPercentage(targetBodyFatPercentage)
                .requiredFatLoss(requiredFatLoss)
                .estimatedWeeks(estimatedWeeks)
                .recommendedWeeklyExerciseMinutes(recommendedWeeklyMinutes)
                .recommendedExerciseTypes(recommendedExerciseTypes)
                .analysisMessage(analysisMessage)
                .build();
    }

    // ========== Private Helper Methods ==========

    private User getUserById(String userId) {
        return userRepository.findByUserId(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
    }

    private Exercise getExerciseByIdAndUserId(Long exerciseId, String userId) {
        Exercise exercise = exerciseRepository.findById(exerciseId)
                .orElseThrow(() -> new BusinessException(ErrorCode.EXERCISE_NOT_FOUND,
                        "해당 운동 기록을 찾을 수 없습니다"));

        if (!exercise.getUser().getUserId().equals(userId)) {
            throw new BusinessException(ErrorCode.ACCESS_DENIED,
                    "해당 운동 기록에 접근할 권한이 없습니다");
        }

        return exercise;
    }

    private ExerciseSet createExerciseSet(
            ExerciseDto.ExerciseSetRequest request,
            Exercise.ExerciseCategory category) {

        ExerciseSet.ExerciseSetBuilder builder = ExerciseSet.builder();

        if (category == Exercise.ExerciseCategory.CARDIO) {
            // 유산소 운동
            builder.cardioType(request.getCardioType())
                    .distance(request.getDistance())
                    .durationMinutes(request.getDurationMinutes())
                    .caloriesBurned(request.getCaloriesBurned())
                    .averageHeartRate(request.getAverageHeartRate());

            // 평균 속도 자동 계산
            if (request.getDistance() != null && request.getDurationMinutes() != null
                    && request.getDurationMinutes() > 0) {
                BigDecimal avgSpeed = request.getDistance()
                        .multiply(BigDecimal.valueOf(60))
                        .divide(BigDecimal.valueOf(request.getDurationMinutes()), 2, RoundingMode.HALF_UP);
                builder.averageSpeed(avgSpeed);
            }
        } else {
            // 무산소 운동
            builder.resistanceExerciseType(request.getResistanceExerciseType())
                    .muscleGroup(request.getMuscleGroup())
                    .setNumber(request.getSetNumber())
                    .weight(request.getWeight())
                    .reps(request.getReps())
                    .restSeconds(request.getRestSeconds());
        }

        builder.memo(request.getMemo());

        return builder.build();
    }

    private BigDecimal calculateChangeRate(int current, int previous) {
        if (previous == 0) {
            return BigDecimal.ZERO;
        }

        return BigDecimal.valueOf(current - previous)
                .multiply(BigDecimal.valueOf(100))
                .divide(BigDecimal.valueOf(previous), 2, RoundingMode.HALF_UP);
    }

    private List<ExerciseDto.DailyExerciseStatItem> createDailyStats(
            List<Exercise> exercises,
            LocalDate startDate,
            LocalDate endDate) {

        List<ExerciseDto.DailyExerciseStatItem> dailyStats = new ArrayList<>();

        for (LocalDate date = startDate; !date.isAfter(endDate); date = date.plusDays(1)) {
            LocalDate currentDate = date;

            List<Exercise> dayExercises = exercises.stream()
                    .filter(e -> e.getExerciseDate().equals(currentDate))
                    .collect(Collectors.toList());

            int dailyDuration = dayExercises.stream()
                    .map(Exercise::getTotalDurationMinutes)
                    .filter(duration -> duration != null)
                    .mapToInt(Integer::intValue)
                    .sum();

            BigDecimal dailyCalories = dayExercises.stream()
                    .flatMap(exercise -> exercise.getExerciseSets().stream())
                    .filter(ExerciseSet::isCardio)
                    .map(ExerciseSet::getCaloriesBurned)
                    .filter(calories -> calories != null)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            dailyStats.add(ExerciseDto.DailyExerciseStatItem.builder()
                    .date(currentDate)
                    .dailyDurationMinutes(dailyDuration)
                    .dailyCalories(dailyCalories)
                    .exerciseCount(dayExercises.size())
                    .build());
        }

        return dailyStats;
    }

    private BigDecimal getTargetBodyFatPercentage(User user, BigDecimal currentBodyFat) {
        // 사용자의 건강 목표에 따른 목표 체지방률 설정
        if (user.getHealthGoal() == null) {
            return currentBodyFat.subtract(BigDecimal.valueOf(3)); // 기본: 현재보다 3% 감량
        }

        switch (user.getHealthGoal()) {
            case DIET:
                // 남성: 10~15%, 여성: 18~23%
                return User.Gender.M.equals(user.getGender())
                        ? BigDecimal.valueOf(12)
                        : BigDecimal.valueOf(20);
            case MUSCLE_GAIN:
                // 근육 증가 목표는 체지방률 유지
                return currentBodyFat;
            case MAINTENANCE:
                // 현재 체지방률 유지
                return currentBodyFat;
            default:
                return currentBodyFat.subtract(BigDecimal.valueOf(3));
        }
    }

    private int calculateRecommendedWeeklyMinutes(BigDecimal requiredFatLoss) {
        if (requiredFatLoss.compareTo(BigDecimal.ZERO) <= 0) {
            return 150; // 최소 권장 운동량 (WHO 기준)
        }

        // 주당 0.5kg 감량 목표 = 약 300분 운동
        // 최대 450분 권장
        int minutes = requiredFatLoss
                .multiply(BigDecimal.valueOf(300))
                .divide(BigDecimal.valueOf(0.5), 0, RoundingMode.HALF_UP)
                .intValue();

        return Math.min(minutes, 450);
    }

    private List<String> getRecommendedExerciseTypes(User user) {
        List<String> types = new ArrayList<>();

        if (user.getHealthGoal() == null || user.getHealthGoal() == User.HealthGoal.DIET) {
            types.add("유산소 운동 (런닝, 사이클 등)");
            types.add("HIIT (고강도 인터벌 트레이닝)");
            types.add("저중량 고반복 근력 운동");
        } else if (user.getHealthGoal() == User.HealthGoal.MUSCLE_GAIN) {
            types.add("고중량 저반복 근력 운동");
            types.add("복합 운동 (스쿼트, 데드리프트, 벤치프레스)");
            types.add("적절한 유산소 운동 (주 2~3회)");
        } else {
            types.add("균형 잡힌 유산소 및 근력 운동");
            types.add("주 3~4회 운동");
        }

        return types;
    }

    private String createFatLossAnalysisMessage(
            BigDecimal requiredFatLoss,
            int estimatedWeeks,
            int recommendedWeeklyMinutes) {

        if (requiredFatLoss.compareTo(BigDecimal.ZERO) <= 0) {
            return "현재 체지방률이 목표 범위 내에 있습니다. 현재 상태를 유지하기 위해 주 " +
                    recommendedWeeklyMinutes + "분의 운동을 권장합니다.";
        }

        return String.format(
                "목표 체지방률 달성을 위해 약 %.1fkg의 체지방 감량이 필요합니다. " +
                        "예상 소요 기간은 %d주이며, 주 %d분 이상의 운동을 권장합니다.",
                requiredFatLoss.doubleValue(),
                estimatedWeeks,
                recommendedWeeklyMinutes
        );
    }
}
