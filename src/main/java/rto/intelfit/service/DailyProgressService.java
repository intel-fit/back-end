package rto.intelfit.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import rto.intelfit.domain.DailyProgress;
import rto.intelfit.domain.FitnessExerciseCategorySave;
import rto.intelfit.domain.Meal;
import rto.intelfit.domain.User;
import rto.intelfit.dto.DailyProgressDto;
import rto.intelfit.repository.*;

import java.math.BigDecimal;
import java.time.*;
import java.time.temporal.TemporalAdjusters;
import java.util.*;
import java.util.stream.Collectors;

/**
 * ✅ DailyProgressService (FitnessExerciseCategorySave 기준)
 * - 운동 달성률(%) = 완료된 세트 수 / 계획된 세트 수 * 100
 * - 실시간으로 세트를 완료할 때마다 달성률 업데이트
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DailyProgressService {

    private final DailyProgressRepository dailyProgressRepository;
    private final WorkoutPlanDetailRepository workoutPlanDetailRepository;
    private final FitnessExerciseCategorySaveRepository saveRepository;
    private final MealRepository mealRepository;

    /**
     * ✅ 오늘의 운동 달성률 & 칼로리 계산
     */
    @Transactional
    public DailyProgressDto calculateTodayProgress(User user) {
        LocalDate today = LocalDate.now();
        return calculateProgressByDate(user, today);
    }

    /**
     * ✅ 특정 날짜의 운동 달성률 계산 및 저장 (전체 갱신용)
     * ⚠️ 운동 + 식단 모두 반영 (기존 유지)
     */
    @Transactional
    public DailyProgressDto calculateProgressByDate(User user, LocalDate date) {
        String dayOfWeek = date.getDayOfWeek().name();

        // 🏋️ 계획된 세트 수 (없으면 0)
        int plannedSets = Optional.ofNullable(
                workoutPlanDetailRepository.sumSetsByUserAndDayOfWeek(user.getId(), dayOfWeek)
        ).orElse(0);

        // ✅ 완료된 운동 세트 수 계산
        List<FitnessExerciseCategorySave> allRecords =
                saveRepository.findByUserOrderByWorkoutDateDesc(user);

        int completedSets = (int) allRecords.stream()
                .filter(record -> record.getWorkoutDate() != null)
                .filter(record -> record.getWorkoutDate().toLocalDate().isEqual(date))
                .count();

        log.debug("운동 달성률 계산 - 날짜: {}, 계획: {}세트, 완료: {}세트",
                date, plannedSets, completedSets);

        // 📊 운동 달성률 계산
        double exerciseRate;
        if (plannedSets == 0) {
            exerciseRate = (completedSets > 0) ? 100.0 : 0.0;
        } else {
            exerciseRate = Math.min(
                    Math.round(((double) completedSets / plannedSets) * 1000.0) / 10.0,
                    100.0
            );
        }

        // 🍽️ 섭취 칼로리 합산
        BigDecimal totalCalories = mealRepository
                .findByUserAndMealDateOrderByMealTypeAsc(user, date)
                .stream()
                .map(Meal::getTotalCalories)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // 🧾 저장 또는 업데이트
        DailyProgress progress = dailyProgressRepository.findByUserIdAndDate(user.getId(), date)
                .orElse(DailyProgress.builder()
                        .user(user)
                        .date(date)
                        .exerciseRate(0.0)
                        .totalCalorie(0.0)
                        .build());

        progress.setExerciseRate(exerciseRate);
        progress.setTotalCalorie(totalCalories.doubleValue());
        dailyProgressRepository.save(progress);

        log.info("✅ DailyProgress 저장 - userId: {}, date: {}, rate: {}%, kcal: {}, 완료세트: {}/{}",
                user.getUserId(), date, exerciseRate, totalCalories, completedSets, plannedSets);

        return DailyProgressDto.builder()
                .date(date)
                .exerciseRate(exerciseRate)
                .totalCalorie(totalCalories.doubleValue())
                .build();
    }

    /**
     * ✅ (신규) 운동 기록 변경 시 호출 - 운동율만 갱신
     */
    @Transactional
    public void updateExerciseOnly(User user, LocalDate date) {
        String dayOfWeek = date.getDayOfWeek().name();

        int plannedSets = Optional.ofNullable(
                workoutPlanDetailRepository.sumSetsByUserAndDayOfWeek(user.getId(), dayOfWeek)
        ).orElse(0);

        List<FitnessExerciseCategorySave> allRecords =
                saveRepository.findByUserOrderByWorkoutDateDesc(user);

        int completedSets = (int) allRecords.stream()
                .filter(record -> record.getWorkoutDate() != null)
                .filter(record -> record.getWorkoutDate().toLocalDate().isEqual(date))
                .count();

        double exerciseRate;
        if (plannedSets == 0) {
            exerciseRate = (completedSets > 0) ? 100.0 : 0.0;
        } else {
            exerciseRate = Math.min(((double) completedSets / plannedSets) * 100.0, 100.0);
        }

        DailyProgress progress = dailyProgressRepository.findByUserIdAndDate(user.getId(), date)
                .orElse(DailyProgress.builder()
                        .user(user)
                        .date(date)
                        .exerciseRate(0.0)
                        .totalCalorie(0.0)
                        .build());

        progress.setExerciseRate(exerciseRate);
        dailyProgressRepository.save(progress);

        log.info("✅ [운동] DailyProgress(운동율만) 갱신 - userId: {}, date: {}, rate: {}%",
                user.getUserId(), date, exerciseRate);
    }

    /**
     * ✅ (신규) 식단 변경 시 호출 - 칼로리만 갱신
     */
    @Transactional
    public void updateCalorieOnly(User user, LocalDate date) {
        BigDecimal totalCalories = mealRepository
                .findByUserAndMealDateOrderByMealTypeAsc(user, date)
                .stream()
                .map(Meal::getTotalCalories)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        DailyProgress progress = dailyProgressRepository.findByUserIdAndDate(user.getId(), date)
                .orElse(DailyProgress.builder()
                        .user(user)
                        .date(date)
                        .exerciseRate(0.0)
                        .totalCalorie(0.0)
                        .build());

        progress.setTotalCalorie(totalCalories.doubleValue());
        dailyProgressRepository.save(progress);

        log.info("✅ [식단] DailyProgress(칼로리만) 갱신 - userId: {}, date: {}, kcal: {}",
                user.getUserId(), date, totalCalories);
    }

    /**
     * ✅ 특정 날짜의 운동 달성률 조회 (DB에서, 없으면 0)
     */
    public DailyProgressDto getProgressByDate(User user, LocalDate date) {
        return dailyProgressRepository.findByUserIdAndDate(user.getId(), date)
                .map(p -> DailyProgressDto.builder()
                        .date(p.getDate())
                        .exerciseRate(p.getExerciseRate())
                        .totalCalorie(p.getTotalCalorie())
                        .build())
                .orElseGet(() -> DailyProgressDto.builder()
                        .date(date)
                        .exerciseRate(0.0)
                        .totalCalorie(0.0)
                        .build());
    }

    /**
     * ✅ 이번 주 (일~토) 운동 달성률 조회
     */
    public List<DailyProgressDto> getWeeklyProgress(User user) {
        LocalDate today = LocalDate.now();
        LocalDate startOfWeek = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.SUNDAY));
        LocalDate endOfWeek = today.with(TemporalAdjusters.nextOrSame(DayOfWeek.SATURDAY));

        return getProgressInRange(user, startOfWeek, endOfWeek);
    }

    /**
     * ✅ 월별 운동 달성률 조회
     */
    public List<DailyProgressDto> getMonthlyProgress(User user, YearMonth month) {
        LocalDate start = month.atDay(1);
        LocalDate end = month.atEndOfMonth();

        return getProgressInRange(user, start, end);
    }

    /**
     * ✅ 최근 N일간 운동 달성률 조회
     */
    public List<DailyProgressDto> getRecentProgress(User user, int days) {
        LocalDate end = LocalDate.now();
        LocalDate start = end.minusDays(days - 1);

        return getProgressInRange(user, start, end);
    }

    /**
     * ✅ 기간별 운동 달성률 조회 (공통 로직)
     */
    private List<DailyProgressDto> getProgressInRange(User user, LocalDate start, LocalDate end) {
        List<DailyProgress> progressList =
                dailyProgressRepository.findByUserIdAndDateBetween(user.getId(), start, end);

        Map<LocalDate, DailyProgress> progressMap = progressList.stream()
                .collect(Collectors.toMap(DailyProgress::getDate, p -> p));

        return start.datesUntil(end.plusDays(1))
                .map(date -> {
                    DailyProgress progress = progressMap.get(date);
                    if (progress != null) {
                        return DailyProgressDto.builder()
                                .date(progress.getDate())
                                .exerciseRate(progress.getExerciseRate())
                                .totalCalorie(progress.getTotalCalorie())
                                .build();
                    } else {
                        return DailyProgressDto.builder()
                                .date(date)
                                .exerciseRate(0.0)
                                .totalCalorie(0.0)
                                .build();
                    }
                })
                .collect(Collectors.toList());
    }

    /**
     * ✅ 운동 기록 추가/수정 시 호출 - 해당 날짜의 달성률 재계산
     */
    @Transactional
    public void recalculateProgress(User user, LocalDate date) {
        log.info("운동 달성률 재계산 - userId: {}, date: {}", user.getUserId(), date);
        calculateProgressByDate(user, date);
    }
}
