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

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DailyProgressService {

    private final DailyProgressRepository dailyProgressRepository;
    private final FitnessExerciseCategorySaveRepository saveRepository;
    private final MealRepository mealRepository;

    @Transactional
    public DailyProgressDto calculateTodayProgress(User user) {
        LocalDate today = LocalDate.now();
        return calculateProgressByDate(user, today);
    }

    @Transactional
    public DailyProgressDto calculateProgressByDate(User user, LocalDate date) {

        // 1) DailyProgress를 락 걸고 가져오기 (없으면 생성)
        DailyProgress progress =
                dailyProgressRepository.findByUserIdAndDateForUpdate(user.getId(), date)
                        .orElseGet(() -> {
                            DailyProgress p = new DailyProgress();
                            p.setUser(user);
                            p.setDate(date);
                            p.setExerciseRate(0.0);
                            p.setTotalCalorie(0.0);
                            p.setTotalExerciseSeconds(0L);
                            return p;
                        });

        // 2) 종목 기반 운동 달성률 계산 (1/n 방식)
        double exerciseRate = calculateExerciseRateBySession(user, date);

        // 3) 해당 날짜 섭취 칼로리 합산
        BigDecimal totalCalories = mealRepository
                .findByUserAndMealDateOrderByMealTypeAsc(user, date)
                .stream()
                .map(Meal::getTotalCalories)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // 4) 업데이트 후 저장
        progress.setExerciseRate(exerciseRate);
        progress.setTotalCalorie(totalCalories.doubleValue());

        dailyProgressRepository.save(progress);

        log.info("DailyProgress 저장 - userId: {}, date: {}, exerciseRate: {}%, kcal: {}",
                user.getUserId(), date, exerciseRate, totalCalories);

        return DailyProgressDto.builder()
                .date(date)
                .exerciseRate(exerciseRate)
                .totalCalorie(totalCalories.doubleValue())
                .build();
    }

    /**
     * 종목 기반 운동 달성률 계산
     * - 하루에 n개의 운동 종목(세션)이 있으면, 각 종목 완료 시 1/n(%)씩 증가
     * - 한 세션의 모든 세트가 completed=true일 때 해당 세션이 완료된 것으로 간주
     * - 추천운동 + 사용자 추가 운동 모두 포함
     */
    private double calculateExerciseRateBySession(User user, LocalDate date) {
        // 전체 세션(종목) 수
        long totalSessions = saveRepository.countTotalSessionsByDate(user, date);
        
        if (totalSessions == 0) {
            return 0.0;
        }

        // 완료된 세션(종목) 수
        long completedSessions = saveRepository.countCompletedSessionsByDate(user, date);

        // 달성률 계산: (완료된 종목 / 전체 종목) * 100, 소수점 1자리
        double rate = Math.round((completedSessions * 1000.0) / totalSessions) / 10.0;
        
        log.info("운동 달성률 계산 - userId: {}, date: {}, 완료: {}/{} 종목, rate: {}%",
                user.getUserId(), date, completedSessions, totalSessions, rate);

        return Math.min(100.0, rate);
    }

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

    public List<DailyProgressDto> getWeeklyProgress(User user) {
        LocalDate today = LocalDate.now();
        LocalDate startOfWeek = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.SUNDAY));
        LocalDate endOfWeek = today.with(TemporalAdjusters.nextOrSame(DayOfWeek.SATURDAY));
        return getProgressInRange(user, startOfWeek, endOfWeek);
    }

    public List<DailyProgressDto> getMonthlyProgress(User user, YearMonth month) {
        LocalDate start = month.atDay(1);
        LocalDate end = month.atEndOfMonth();
        return getProgressInRange(user, start, end);
    }

    public List<DailyProgressDto> getRecentProgress(User user, int days) {
        LocalDate end = LocalDate.now();
        LocalDate start = end.minusDays(days - 1);
        return getProgressInRange(user, start, end);
    }

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

    @Transactional
    public void recalculateProgress(User user, LocalDate date) {
        log.info("운동 달성률 재계산 - userId: {}, date: {}", user.getUserId(), date);
        calculateProgressByDate(user, date);
    }
}
