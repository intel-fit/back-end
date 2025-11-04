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
import java.time.temporal.TemporalAdjusters;
import java.util.*;
import java.util.stream.Collectors;

/**
 * ✅ DailyProgressService (최종 개선 버전)
 * - 운동 달성률(%) + 섭취 칼로리 합산
 * - 주간/월간 데이터가 없을 때도 0으로 안전하게 반환
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
        String dayOfWeek = today.getDayOfWeek().name();

        // 🏋️ 계획된 세트 수 (없으면 0)
        int plannedSets = Optional.ofNullable(
                workoutPlanDetailRepository.sumSetsByUserAndDayOfWeek(user.getId(), dayOfWeek)
        ).orElse(0);

        // ✅ 수행된 세트 수 (없으면 0)
        int completedSets = Optional.ofNullable(
                workoutRecordRepository.sumSetsCompletedByUserAndDate(user.getId(), today)
        ).orElse(0);

        // 📊 운동 달성률 계산
        double exerciseRate = (plannedSets == 0)
                ? 0.0
                : Math.round(((double) completedSets / plannedSets) * 1000.0) / 10.0;

        // 🍽️ 오늘 섭취 칼로리 합산 (데이터 없으면 0)
        BigDecimal totalCalories = mealRepository
                .findByUserAndMealDateOrderByMealTypeAsc(user, today)
                .stream()
                .map(Meal::getTotalCalories)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // 🧾 DB에 저장 (없으면 새로 생성)
        DailyProgress progress = dailyProgressRepository.findByUserIdAndDate(user.getId(), today)
                .orElse(DailyProgress.builder()
                        .user(user)
                        .date(today)
                        .exerciseRate(0.0)
                        .totalCalorie(0.0)
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
     * ✅ 특정 날짜의 운동 달성률 조회 (없으면 0으로 반환)
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
     * ✅ 이번 주 (일~토) 운동 달성률 조회 (없으면 0으로 채움)
     */
    public List<DailyProgressDto> getWeeklyProgress(User user) {
        LocalDate today = LocalDate.now();

        // ✅ 이번 주 시작: 이번 주 일요일 / 끝: 이번 주 토요일
        LocalDate startOfWeek = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.SUNDAY));
        LocalDate endOfWeek = today.with(TemporalAdjusters.nextOrSame(DayOfWeek.SATURDAY));

        log.info("🧩 [getWeeklyProgress] now={}, start={}, end={}", today, startOfWeek, endOfWeek);

        List<DailyProgress> progressList =
                dailyProgressRepository.findByUserIdAndDateBetween(user.getId(), startOfWeek, endOfWeek);

        Map<LocalDate, DailyProgress> map = progressList.stream()
                .collect(Collectors.toMap(DailyProgress::getDate, p -> p));

        // ✅ 일~토까지 모두 채우기
        return startOfWeek.datesUntil(endOfWeek.plusDays(1))
                .map(date -> map.containsKey(date)
                        ? DailyProgressDto.builder()
                        .date(date)
                        .exerciseRate(Optional.ofNullable(map.get(date).getExerciseRate()).orElse(0.0))
                        .totalCalorie(Optional.ofNullable(map.get(date).getTotalCalorie()).orElse(0.0))
                        .build()
                        : DailyProgressDto.builder()
                        .date(date)
                        .exerciseRate(0.0)
                        .totalCalorie(0.0)
                        .build())
                .collect(Collectors.toList());
    }

    /**
     * ✅ 월별 운동 달성률 & 칼로리 조회 (없으면 0으로 채움)
     */
    public List<DailyProgressDto> getMonthlyProgress(User user, YearMonth month) {
        LocalDate start = month.atDay(1);
        LocalDate end = month.atEndOfMonth();

        log.info("🧩 [getMonthlyProgress] month={}, start={}, end={}", month, start, end);

        List<DailyProgress> list =
                dailyProgressRepository.findByUserIdAndDateBetween(user.getId(), start, end);

        Map<LocalDate, DailyProgress> map = list.stream()
                .collect(Collectors.toMap(DailyProgress::getDate, p -> p));

        // ✅ 해당 월의 모든 날짜를 0으로 채우기
        return start.datesUntil(end.plusDays(1))
                .map(date -> map.containsKey(date)
                        ? DailyProgressDto.builder()
                        .date(date)
                        .exerciseRate(Optional.ofNullable(map.get(date).getExerciseRate()).orElse(0.0))
                        .totalCalorie(Optional.ofNullable(map.get(date).getTotalCalorie()).orElse(0.0))
                        .build()
                        : DailyProgressDto.builder()
                        .date(date)
                        .exerciseRate(0.0)
                        .totalCalorie(0.0)
                        .build())
                .collect(Collectors.toList());
    }

    /**
     * ✅ 최근 N일간 운동 달성률 & 칼로리 조회 (없으면 0으로 채움)
     */
    public List<DailyProgressDto> getRecentProgress(User user, int days) {
        LocalDate end = LocalDate.now();
        LocalDate start = end.minusDays(days - 1);

        List<DailyProgress> list =
                dailyProgressRepository.findByUserIdAndDateBetween(user.getId(), start, end);

        Map<LocalDate, DailyProgress> map = list.stream()
                .collect(Collectors.toMap(DailyProgress::getDate, p -> p));

        // ✅ N일간 데이터 누락 보완
        return start.datesUntil(end.plusDays(1))
                .map(date -> map.containsKey(date)
                        ? DailyProgressDto.builder()
                        .date(date)
                        .exerciseRate(Optional.ofNullable(map.get(date).getExerciseRate()).orElse(0.0))
                        .totalCalorie(Optional.ofNullable(map.get(date).getTotalCalorie()).orElse(0.0))
                        .build()
                        : DailyProgressDto.builder()
                        .date(date)
                        .exerciseRate(0.0)
                        .totalCalorie(0.0)
                        .build())
                .collect(Collectors.toList());
    }
}
