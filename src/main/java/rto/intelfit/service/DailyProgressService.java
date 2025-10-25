package rto.intelfit.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import rto.intelfit.domain.*;
import rto.intelfit.dto.DailyProgressDto;
import rto.intelfit.repository.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.stream.Collectors;

/**
 * ✅ DailyProgressService v2
 * DailyProgress 전용 (운동 달성률 + 칼로리 합산)
 * - DailyProgressExerciseRepository → 실제 수행 운동 수
 * - DailyProgressExercisePlanRepository → 계획된 운동 수
 * - MealRepository → 일별 섭취 칼로리 합산
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DailyProgressService {

    private final DailyProgressRepository dailyProgressRepository;
    private final DailyProgressExerciseRepository dailyProgressExerciseRepository;
    private final DailyProgressExercisePlanRepository dailyProgressExercisePlanRepository;
    private final MealRepository mealRepository;

    /**
     * ✅ 오늘의 DailyProgress 계산 (운동 달성률 + 섭취 칼로리)
     * 이미 존재하면 업데이트, 없으면 새로 생성
     */
    @Transactional
    public DailyProgressDto calculateTodayProgress(User user) {
        LocalDate today = LocalDate.now();

        // 1️⃣ 오늘 계획된 운동 개수
        int plannedCount = dailyProgressExercisePlanRepository.countPlannedExercises(user.getId(), today);

        // 2️⃣ 오늘 실제 수행한 운동 개수
        long doneCount = dailyProgressExerciseRepository.countCompletedExercises(user.getId(), today);

        // 3️⃣ 운동 달성률 계산
        double exerciseRate = (plannedCount == 0)
                ? 0.0
                : ((double) doneCount / plannedCount) * 100.0;

        // 4️⃣ 오늘 섭취한 총 칼로리 계산
        List<Meal> meals = mealRepository.findByUserAndMealDateOrderByMealTypeAsc(user, today);
        BigDecimal totalCalories = meals.stream()
                .map(Meal::getTotalCalories)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // 5️⃣ DailyProgress 엔티티 저장 or 업데이트
        DailyProgress progress = dailyProgressRepository.findByUserIdAndDate(user.getId(), today)
                .orElse(DailyProgress.builder()
                        .user(user)
                        .date(today)
                        .build());

        progress.setExerciseRate(exerciseRate);
        progress.setTotalCalorie(totalCalories.doubleValue());
        dailyProgressRepository.save(progress);

        log.info("✅ DailyProgress 계산 완료 - userId: {}, date: {}, rate: {}%, kcal: {}",
                user.getUserId(), today, exerciseRate, totalCalories);

        // 6️⃣ DTO로 반환
        return DailyProgressDto.builder()
                .date(today)
                .exerciseRate(exerciseRate)
                .totalCalorie(totalCalories.doubleValue())
                .build();
    }

    /**
     * ✅ 특정 날짜의 DailyProgress 조회
     * 홈 화면 또는 캘린더 클릭 시 사용
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
     * ✅ 월별 DailyProgress 목록 조회
     * 홈 화면 캘린더용 (각 날짜별 %와 kcal 표시)
     */
    public List<DailyProgressDto> getMonthlyProgress(User user, YearMonth month) {
        LocalDate startDate = month.atDay(1);
        LocalDate endDate = month.atEndOfMonth();

        List<DailyProgress> progressList =
                dailyProgressRepository.findByUserIdAndDateBetween(user.getId(), startDate, endDate);

        return progressList.stream()
                .map(p -> DailyProgressDto.builder()
                        .date(p.getDate())
                        .exerciseRate(p.getExerciseRate())
                        .totalCalorie(p.getTotalCalorie())
                        .build())
                .collect(Collectors.toList());
    }

    /**
     * ✅ 최근 N일간 DailyProgress 조회 (홈 화면 차트 등)
     */
    public List<DailyProgressDto> getRecentProgress(User user, int days) {
        LocalDate endDate = LocalDate.now();
        LocalDate startDate = endDate.minusDays(days - 1);

        List<DailyProgress> recent = dailyProgressRepository.findByUserIdAndDateBetween(
                user.getId(), startDate, endDate);

        return recent.stream()
                .map(p -> DailyProgressDto.builder()
                        .date(p.getDate())
                        .exerciseRate(p.getExerciseRate())
                        .totalCalorie(p.getTotalCalorie())
                        .build())
                .collect(Collectors.toList());
    }

    /**
     * ✅ 이번 주(일~토) 운동 달성률 & 칼로리 조회
     * 홈 화면 달력 1줄용
     */
    public List<DailyProgressDto> getWeeklyProgress(User user) {
        LocalDate today = LocalDate.now();
        LocalDate startOfWeek = today.with(java.time.DayOfWeek.SUNDAY);
        LocalDate endOfWeek = today.with(java.time.DayOfWeek.SATURDAY);

        List<DailyProgress> progressList =
                dailyProgressRepository.findByUserIdAndDateBetween(user.getId(), startOfWeek, endOfWeek);

        // 누락된 요일(운동 안한 날)은 0%로 채워서 리턴
        return startOfWeek.datesUntil(endOfWeek.plusDays(1))
                .map(date -> progressList.stream()
                        .filter(p -> p.getDate().equals(date))
                        .findFirst()
                        .map(p -> DailyProgressDto.builder()
                                .date(p.getDate())
                                .exerciseRate(p.getExerciseRate())
                                .totalCalorie(p.getTotalCalorie())
                                .build())
                        .orElse(DailyProgressDto.builder()
                                .date(date)
                                .exerciseRate(0.0)
                                .totalCalorie(0.0)
                                .build()))
                .collect(Collectors.toList());
    }

}
