package rto.intelfit.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import rto.intelfit.domain.*;
import rto.intelfit.dto.DailyProgressDto;
import rto.intelfit.repository.*;

import java.math.BigDecimal;
import java.time.*;
import java.util.*;
import java.util.stream.Collectors;

import rto.intelfit.domain.DailyProgress;
import rto.intelfit.domain.Meal;
import rto.intelfit.domain.User;

import java.time.temporal.TemporalAdjusters;


/**
 * ✅ DailyProgressService (리팩토링 최종버전 + 100% 상한 적용)
 * - null-safe 강화
 * - 주/월/최근 공통 메서드 추출
 * - 운동 달성률은 최대 100%로 제한
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DailyProgressService {

    private final DailyProgressRepository dailyProgressRepository;
    private final ExerciseRepository exerciseRepository;
    private final RecommendedExercisePlanRepository recommendedExercisePlanRepository;
    private final MealRepository mealRepository;

    /**
     * ✅ 오늘의 운동 달성률 & 칼로리 계산
     */
    @Transactional
    public DailyProgressDto calculateTodayProgress(User user) {
        LocalDate today = LocalDate.now();
        String dayOfWeek = today.getDayOfWeek().name();

        // 🧩 오늘 실제 운동 기록 (AI 외 자발적 운동 포함)
        List<Exercise> exercises = Optional.ofNullable(
                exerciseRepository.findByUserAndExerciseDate(user, today)
        ).orElse(Collections.emptyList());

        int completedSets = exercises.stream()
                .flatMap(e -> Optional.ofNullable(e.getExerciseSets())
                        .orElse(Collections.emptyList()).stream())
                .mapToInt(set -> (set.getReps() != null && set.getReps() > 0) ? 1 : 0)
                .sum();

        // 🧠 오늘 요일 기준 AI 추천 세트 수
        int plannedSets = recommendedExercisePlanRepository
                .findByUserAndIsSavedTrueOrderByCreatedAtDesc(user)
                .stream()
                .flatMap(plan -> Optional.ofNullable(plan.getRoutines())
                        .orElse(Collections.emptyList()).stream())
                .filter(r -> Optional.ofNullable(r.getDayOfWeek())
                        .map(d -> d.toUpperCase().contains(dayOfWeek))
                        .orElse(false))
                .mapToInt(r -> Optional.ofNullable(r.getItems())
                        .orElse(Collections.emptyList()).size())
                .sum();

        // ✨ 계획에 없던 자발적 운동 세트 수
        int extraManualSets = Math.max(0, exercises.size() - plannedSets);

        // 📊 총 계획 세트 = AI 추천 + 자발적 운동
        int totalPlannedSets = plannedSets + extraManualSets;

        // ✅ 달성률 계산 (100% 초과 방지)
        double exerciseRate = totalPlannedSets == 0 ? 0.0 :
                Math.min(100.0, ((double) completedSets / totalPlannedSets) * 100.0);

        // 🍽️ 오늘 섭취 칼로리 합산
        List<Meal> meals = Optional.ofNullable(
                mealRepository.findByUserAndMealDateOrderByMealTypeAsc(user, today)
        ).orElse(Collections.emptyList());

        BigDecimal totalCalories = meals.stream()
                .map(m -> Optional.ofNullable(m.getTotalCalories()).orElse(BigDecimal.ZERO))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // 🧾 daily_progress 저장 or 갱신
        DailyProgress progress = dailyProgressRepository.findByUserIdAndDate(user.getId(), today)
                .orElse(DailyProgress.builder().user(user).date(today).build());

        progress.setExerciseRate(exerciseRate);
        progress.setTotalCalorie(totalCalories.doubleValue());
        dailyProgressRepository.save(progress);

        log.info("✅ DailyProgress 업데이트 - userId: {}, date: {}, rate: {}%, kcal: {}",
                user.getUserId(), today, exerciseRate, totalCalories);

        return DailyProgressDto.builder()
                .date(today)
                .exerciseRate(exerciseRate)
                .totalCalorie(totalCalories.doubleValue())
                .build();
    }


    /**
     * ✅ 특정 날짜
     */
    public DailyProgressDto getProgressByDate(User user, LocalDate date) {
        return dailyProgressRepository.findByUserIdAndDate(user.getId(), date)
                .map(this::toDto)
                .orElse(zeroDto(date));
    }

    /**
     * ✅ 이번 주 (일~토)
     */
    public List<DailyProgressDto> getWeeklyProgress(User user) {
        LocalDate today = LocalDate.now();
        LocalDate startOfWeek = today.minusDays(today.getDayOfWeek().getValue() % 7);
        LocalDate endOfWeek = startOfWeek.plusDays(6);
        return getProgressInRange(user, startOfWeek, endOfWeek);
    }

    /**
     * ✅ 월별
     */
    public List<DailyProgressDto> getMonthlyProgress(User user, YearMonth month) {
        LocalDate start = month.atDay(1);
        LocalDate end = month.atEndOfMonth();
        return getProgressInRange(user, start, end);
    }

    /**
     * ✅ 최근 N일간
     */
    public List<DailyProgressDto> getRecentProgress(User user, int days) {
        LocalDate end = LocalDate.now();
        LocalDate start = end.minusDays(days - 1);
        return getProgressInRange(user, start, end);
    }

    // ✅ 공통 구간 조회 메서드 (중복 제거)
    private List<DailyProgressDto> getProgressInRange(User user, LocalDate start, LocalDate end) {
        List<DailyProgress> list =
                dailyProgressRepository.findByUserIdAndDateBetween(user.getId(), start, end);

        Map<LocalDate, DailyProgress> map = list.stream()
                .collect(Collectors.toMap(DailyProgress::getDate, p -> p));

        return start.datesUntil(end.plusDays(1))
                .map(date -> map.containsKey(date)
                        ? toDto(map.get(date))
                        : zeroDto(date))
                .collect(Collectors.toList());
    }

    // ✅ 변환 헬퍼
    private DailyProgressDto toDto(DailyProgress progress) {
        return DailyProgressDto.builder()
                .date(progress.getDate())
                .exerciseRate(progress.getExerciseRate())
                .totalCalorie(progress.getTotalCalorie())
                .build();
    }

    // ✅ 0으로 채우는 기본 DTO
    private DailyProgressDto zeroDto(LocalDate date) {
        return DailyProgressDto.builder()
                .date(date)
                .exerciseRate(0.0)
                .totalCalorie(0.0)
                .build();
    }
}
