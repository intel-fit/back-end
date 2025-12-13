package rto.intelfit.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import rto.intelfit.domain.DailyProgress;
import rto.intelfit.domain.Meal;
import rto.intelfit.domain.User;
import rto.intelfit.domain.ExerciseGoal;
import rto.intelfit.dto.DailyProgressDto;
import rto.intelfit.repository.*;

import java.math.BigDecimal;
import java.time.*;
import java.time.temporal.TemporalAdjusters;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DailyProgressService {

    private final DailyProgressRepository dailyProgressRepository;
    private final WorkoutPlanDetailRepository workoutPlanDetailRepository;
    private final FitnessExerciseCategorySaveRepository saveRepository;
    private final MealRepository mealRepository;

    // ✅ 추가
    private final ExerciseGoalRepository exerciseGoalRepository;

    @Transactional
    public DailyProgressDto calculateTodayProgress(User user) {
        LocalDate today = LocalDate.now();
        return calculateProgressByDate(user, today);
    }

    @Transactional
    public DailyProgressDto calculateProgressByDate(User user, LocalDate date) {

        // ✅ 1) DailyProgress를 락 걸고 가져오기 (없으면 생성)
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

        // ✅ 2) 하루 누적 운동 시간(초) - 다른 서비스에서 최신값으로 유지된다고 가정
        long totalExerciseSeconds = progress.getTotalExerciseSeconds();

        // ✅ 3) 운동목표가 있을 때만 목표시간 기반으로 rate 계산
        double exerciseRate = 0.0;

        Optional<ExerciseGoal> optionalGoal = exerciseGoalRepository.findByUser(user);
        if (optionalGoal.isPresent()) {

            long targetSeconds = parseTargetSeconds(optionalGoal.get());

            if (targetSeconds > 0) {
                // rate = min(100, total / target * 100), 소수 1자리
                exerciseRate = Math.min(
                        100.0,
                        Math.round((totalExerciseSeconds * 1000.0) / targetSeconds) / 10.0
                );
            } else {
                // 목표는 있는데 파싱이 실패했거나 목표가 0이면 0%
                exerciseRate = 0.0;
                log.warn("ExerciseGoal durationPerSession 파싱 실패 또는 목표시간 0 - userId={}, durationPerSession={}",
                        user.getUserId(), optionalGoal.get().getDurationPerSession());
            }

        } else {
            // 운동 목표 자체가 없으면 계산 안 함(0%)
            exerciseRate = 0.0;
        }

        // ✅ 4) 해당 날짜 섭취 칼로리 합산 (기존 그대로)
        BigDecimal totalCalories = mealRepository
                .findByUserAndMealDateOrderByMealTypeAsc(user, date)
                .stream()
                .map(Meal::getTotalCalories)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // ✅ 5) 업데이트 후 저장
        progress.setExerciseRate(exerciseRate);
        progress.setTotalCalorie(totalCalories.doubleValue());

        dailyProgressRepository.save(progress);

        log.info("DailyProgress 저장 - userId: {}, date: {}, totalSeconds: {}, rate: {}%, kcal: {}",
                user.getUserId(), date, totalExerciseSeconds, exerciseRate, totalCalories);

        return DailyProgressDto.builder()
                .date(date)
                .exerciseRate(exerciseRate)
                .totalCalorie(totalCalories.doubleValue())
                .build();
    }

    /**
     * durationPerSession 문자열에서 목표 시간을 "초"로 변환
     * 예: "30분 이상" -> 1800
     * 예: "1시간 이상" -> 3600
     */
    private long parseTargetSeconds(ExerciseGoal goal) {
        String raw = goal.getDurationPerSession();
        if (raw == null) return 0L;

        // 숫자 추출
        Matcher m = Pattern.compile("(\\d+)").matcher(raw);
        if (!m.find()) return 0L;

        long n = Long.parseLong(m.group(1));

        // 단위 판별
        if (raw.contains("시간")) return n * 3600L;
        if (raw.contains("분")) return n * 60L;

        // 단위가 없으면 기본 분으로 처리(원하면 0으로 처리해도 됨)
        return n * 60L;
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
