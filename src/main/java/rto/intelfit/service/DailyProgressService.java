package rto.intelfit.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import rto.intelfit.domain.DailyProgress;
import rto.intelfit.domain.Meal;
import rto.intelfit.domain.User;
import rto.intelfit.dto.DailyProgressDto;
import rto.intelfit.repository.*;

import java.math.BigDecimal;
import java.time.*;
import java.util.*;
import java.util.stream.Collectors;

/**
 * ✅ DailyProgressService (실제 DB 기준 최종 버전)
 * 운동 달성률(%) + 섭취 칼로리 합산
 * - workout_plan_details  → 요일별 계획 세트 합계
 * - workout_records       → 날짜별 수행 세트 합계
 * - meals                 → 섭취 칼로리 합산
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DailyProgressService {

    private final DailyProgressRepository dailyProgressRepository;
    private final WorkoutPlanDetailRepository workoutPlanDetailRepository;
    private final WorkoutRecordRepository workoutRecordRepository;
    private final MealRepository mealRepository;

    /**
     * ✅ 오늘의 운동 달성률 & 칼로리 계산
     * 이미 있으면 업데이트, 없으면 새로 생성
     */
    @Transactional
    public DailyProgressDto calculateTodayProgress(User user) {
        LocalDate today = LocalDate.now();
        String dayOfWeek = today.getDayOfWeek().name(); // e.g. MONDAY

        // 🏋️ 계획된 세트 수
        int plannedSets = workoutPlanDetailRepository.sumSetsByUserAndDayOfWeek(user.getId(), dayOfWeek);

        // ✅ 수행된 세트 수
        int completedSets = workoutRecordRepository.sumSetsCompletedByUserAndDate(user.getId(), today);

        // 📊 운동 달성률 계산
        double exerciseRate = (plannedSets == 0) ? 0.0 : ((double) completedSets / plannedSets) * 100.0;

        // 🍽️ 오늘 섭취 칼로리 합산
        List<Meal> meals = mealRepository.findByUserAndMealDateOrderByMealTypeAsc(user, today);
        BigDecimal totalCalories = meals.stream()
                .map(Meal::getTotalCalories)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // 🧾 DB에 저장 (있으면 업데이트)
        DailyProgress progress = dailyProgressRepository.findByUserIdAndDate(user.getId(), today)
                .orElse(DailyProgress.builder()
                        .user(user)
                        .date(today)
                        .build());

        progress.setExerciseRate(exerciseRate);
        progress.setTotalCalorie(totalCalories.doubleValue());
        dailyProgressRepository.save(progress);

        log.info("✅ DailyProgress 저장 완료 - userId: {}, date: {}, rate: {}%, kcal: {}",
                user.getUserId(), today, exerciseRate, totalCalories);

        return DailyProgressDto.builder()
                .date(today)
                .exerciseRate(exerciseRate)
                .totalCalorie(totalCalories.doubleValue())
                .build();
    }

    /**
     * ✅ 특정 날짜의 운동 달성률 조회
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
     * 홈 화면 표용 (일~토 한 줄)
     */
    public List<DailyProgressDto> getWeeklyProgress(User user) {
        LocalDate today = LocalDate.now();
        LocalDate startOfWeek = today.with(DayOfWeek.SUNDAY);
        LocalDate endOfWeek = today.with(DayOfWeek.SATURDAY);

        List<DailyProgress> progressList =
                dailyProgressRepository.findByUserIdAndDateBetween(user.getId(), startOfWeek, endOfWeek);

        // 없으면 빈 주차 채우기
        Map<LocalDate, DailyProgress> map = progressList.stream()
                .collect(Collectors.toMap(DailyProgress::getDate, p -> p));

        return startOfWeek.datesUntil(endOfWeek.plusDays(1))
                .map(date -> map.containsKey(date)
                        ? DailyProgressDto.builder()
                        .date(date)
                        .exerciseRate(map.get(date).getExerciseRate())
                        .totalCalorie(map.get(date).getTotalCalorie())
                        .build()
                        : DailyProgressDto.builder()
                        .date(date)
                        .exerciseRate(0.0)
                        .totalCalorie(0.0)
                        .build())
                .collect(Collectors.toList());
    }

    /**
     * ✅ 월별 운동 달성률 & 칼로리 조회
     * 캘린더용
     */
    public List<DailyProgressDto> getMonthlyProgress(User user, YearMonth month) {
        LocalDate start = month.atDay(1);
        LocalDate end = month.atEndOfMonth();

        List<DailyProgress> list = dailyProgressRepository.findByUserIdAndDateBetween(user.getId(), start, end);

        return list.stream()
                .map(p -> DailyProgressDto.builder()
                        .date(p.getDate())
                        .exerciseRate(p.getExerciseRate())
                        .totalCalorie(p.getTotalCalorie())
                        .build())
                .collect(Collectors.toList());
    }

    /**
     * ✅ 최근 N일간 운동 달성률 & 칼로리 조회
     * (통계 그래프용)
     */
    public List<DailyProgressDto> getRecentProgress(User user, int days) {
        LocalDate end = LocalDate.now();
        LocalDate start = end.minusDays(days - 1);

        List<DailyProgress> list = dailyProgressRepository.findByUserIdAndDateBetween(user.getId(), start, end);

        return list.stream()
                .map(p -> DailyProgressDto.builder()
                        .date(p.getDate())
                        .exerciseRate(p.getExerciseRate())
                        .totalCalorie(p.getTotalCalorie())
                        .build())
                .collect(Collectors.toList());
    }
}
