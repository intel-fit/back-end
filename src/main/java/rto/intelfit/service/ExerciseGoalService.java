package rto.intelfit.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import rto.intelfit.domain.ExerciseGoal;
import rto.intelfit.domain.User;
import rto.intelfit.dto.ExerciseGoalDto;
import rto.intelfit.exception.BusinessException;
import rto.intelfit.exception.ErrorCode;
import rto.intelfit.repository.DailyProgressRepository;

import rto.intelfit.repository.ExerciseGoalRepository;
import rto.intelfit.repository.UserRepository;
import rto.intelfit.security.CustomUserPrincipal;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;

import rto.intelfit.domain.DailyProgress;
import rto.intelfit.repository.DailyProgressRepository;



@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class ExerciseGoalService {

    private final ExerciseGoalRepository exerciseGoalRepository;
    private final UserRepository userRepository;
    private final DailyProgressRepository dailyProgressRepository;

    /** 운동 목표 저장 */
    public ExerciseGoalDto.Response saveGoal(CustomUserPrincipal userPrincipal, ExerciseGoalDto.Request request) {

        User user = userRepository.findByUserId(userPrincipal.getUserId())
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        exerciseGoalRepository.findByUser(user).ifPresent(oldGoal -> {
            exerciseGoalRepository.delete(oldGoal);
            log.info("기존 목표 삭제 userId={}, goalId={}", user.getUserId(), oldGoal.getId());
        });

        ExerciseGoal newGoal = ExerciseGoal.builder()
                .user(user)
                .weeklyFrequency(request.getWeeklyFrequency())
                .durationPerSession(request.getDurationPerSession())
                .exerciseType(request.getExerciseType())
                .weeklyCalorieGoal(request.getWeeklyCalorieGoal())
                .build();

        ExerciseGoal saved = exerciseGoalRepository.save(newGoal);

        return ExerciseGoalDto.Response.builder()
                .id(saved.getId())
                .weeklyFrequency(saved.getWeeklyFrequency())
                .durationPerSession(saved.getDurationPerSession())
                .exerciseType(saved.getExerciseType())
                .weeklyCalorieGoal(saved.getWeeklyCalorieGoal())
                .build();
    }


    /** 운동 목표 요약 계산 + 반환 */
    @Transactional
    public ExerciseGoalDto.SummaryResponse getGoalSummary(CustomUserPrincipal userPrincipal) {

        User user = userRepository.findByUserId(userPrincipal.getUserId())
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        ExerciseGoal goal = exerciseGoalRepository.findByUser(user)
                .orElseThrow(() -> new BusinessException(ErrorCode.INVALID_INPUT_VALUE));

        int weeklyCount = parseWeeklyFrequency(goal.getWeeklyFrequency());
        long requiredSeconds = parseDurationToSeconds(goal.getDurationPerSession());

        LocalDate now = LocalDate.now();
        LocalDate goalStartDate = goal.getCreatedAt().toLocalDate();

        long daysPassed = ChronoUnit.DAYS.between(goalStartDate, now);
        long weekIndex = daysPassed / 7;

        LocalDate weekStart = goalStartDate.plusDays(weekIndex * 7);
        LocalDate weekEnd = weekStart.plusDays(6);

        List<DailyProgress> weekProgress =
                dailyProgressRepository.findByUserIdAndDateBetween(user.getId(), weekStart, weekEnd);

        double sumRates = weekProgress.stream()
                .mapToDouble(dp -> {
                    double rate = (double) dp.getTotalExerciseSeconds() / requiredSeconds * 100.0;
                    return Math.min(rate, 100.0);
                })
                .sum();

        double progressPercent = sumRates / weeklyCount;
        progressPercent = Math.round(progressPercent * 10) / 10.0;

        // ⭐ ExerciseGoal.progress 갱신
        goal.setProgress((long) progressPercent);
        exerciseGoalRepository.save(goal);

        // ⭐ 반드시 return 필요!!!
        return ExerciseGoalDto.SummaryResponse.builder()
                .weeklyFrequency(goal.getWeeklyFrequency())
                .durationPerSession(goal.getDurationPerSession())
                .progress(progressPercent)
                .build();
    }





    /** 문자열 → 주간 횟수 숫자 파싱 */
    private int parseWeeklyFrequency(String weeklyFrequency) {

        // 1. weeklyFrequency null or 빈 문자열 검증
        if (weeklyFrequency == null || weeklyFrequency.isBlank()) {
            throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE, "주간 운동 횟수 형식이 올바르지 않습니다.");
        }

        // 2. 숫자만 추출 ("주 3회" → 3, "주 10회" → 10)
        String numeric = weeklyFrequency.replaceAll("\\D", "");

        // 3. 숫자가 전혀 없는 경우 (예: "주 회", "abc", "")
        if (numeric.isBlank()) {
            throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE, "주간 운동 횟수에서 숫자를 찾을 수 없습니다.");
        }

        int weeklyCount = Integer.parseInt(numeric);

        // 4. 0 이하일 경우 (이건 말이 안 되니까 예외)
        if (weeklyCount <= 0) {
            throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE, "주간 운동 횟수는 1 이상이어야 합니다.");
        }

        return weeklyCount;
    }


    /** 문자열 → 초 단위 운동시간 파싱 ("30분 이상") → 1800 초 */
    private long parseDurationToSeconds(String durationPerSession) {
        int minutes = Integer.parseInt(durationPerSession.replaceAll("\\D", ""));
        return minutes * 60L;
    }
//

    /** 목표 삭제 */
    public void deleteGoal(CustomUserPrincipal userPrincipal) {
        User user = userRepository.findByUserId(userPrincipal.getUserId())
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        ExerciseGoal goal = exerciseGoalRepository.findByUser(user)
                .orElseThrow(() -> new BusinessException(ErrorCode.INVALID_INPUT_VALUE, "삭제할 운동 목표가 없습니다."));

        exerciseGoalRepository.delete(goal);
    }
}