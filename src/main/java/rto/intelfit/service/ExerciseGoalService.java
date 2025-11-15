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
import rto.intelfit.repository.ExerciseGoalRepository;
import rto.intelfit.repository.UserRepository;
import rto.intelfit.security.CustomUserPrincipal;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class ExerciseGoalService {

    private final ExerciseGoalRepository exerciseGoalRepository;
    private final UserRepository userRepository;

    /** 운동 목표 저장 (기존 목표 삭제 후 새로 삽입) */
    public ExerciseGoalDto.Response saveGoal(CustomUserPrincipal userPrincipal, ExerciseGoalDto.Request request) {

        User user = userRepository.findByUserId(userPrincipal.getUserId())
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        // 기존 목표 삭제
        exerciseGoalRepository.findByUser(user).ifPresent(oldGoal -> {
            exerciseGoalRepository.delete(oldGoal);
            log.info("기존 운동 목표 삭제 - userId={}, oldGoalId={}", user.getUserId(), oldGoal.getId());
        });

        // 새 목표 생성 (progress = 0 기본값)
        ExerciseGoal newGoal = ExerciseGoal.builder()
                .user(user)
                .weeklyFrequency(request.getWeeklyFrequency())
                .durationPerSession(request.getDurationPerSession())
                .exerciseType(request.getExerciseType())
                .weeklyCalorieGoal(request.getWeeklyCalorieGoal())
                .progress(0L)
                .build();

        ExerciseGoal saved = exerciseGoalRepository.save(newGoal);

        log.info("운동 목표 새로 저장 완료 - userId={}, goalId={}", user.getUserId(), saved.getId());

        return ExerciseGoalDto.Response.builder()
                .id(saved.getId())
                .weeklyFrequency(saved.getWeeklyFrequency())
                .durationPerSession(saved.getDurationPerSession())
                .exerciseType(saved.getExerciseType())
                .weeklyCalorieGoal(saved.getWeeklyCalorieGoal())
                .progress(saved.getProgress())
                .build();
    }

    /** 운동 목표 요약 조회 */
    @Transactional(readOnly = true)
    public ExerciseGoalDto.SummaryResponse getGoalSummary(CustomUserPrincipal userPrincipal) {
        User user = userRepository.findByUserId(userPrincipal.getUserId())
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        ExerciseGoal goal = exerciseGoalRepository.findByUser(user)
                .orElseThrow(() -> new BusinessException(ErrorCode.INVALID_INPUT_VALUE, "운동 목표가 설정되어 있지 않습니다."));

        return ExerciseGoalDto.SummaryResponse.builder()
                .weeklyFrequency(goal.getWeeklyFrequency())
                .durationPerSession(goal.getDurationPerSession())
                .progress(goal.getProgress())
                .build();
    }
    @Transactional
    public void deleteGoal(CustomUserPrincipal userPrincipal) {
        User user = userRepository.findByUserId(userPrincipal.getUserId())
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        ExerciseGoal goal = exerciseGoalRepository.findByUser(user)
                .orElseThrow(() -> new BusinessException(ErrorCode.INVALID_INPUT_VALUE, "삭제할 운동 목표가 없습니다."));

        exerciseGoalRepository.delete(goal);
        log.info("운동 목표 삭제 완료 - userId={}, goalId={}", user.getUserId(), goal.getId());
    }

}
