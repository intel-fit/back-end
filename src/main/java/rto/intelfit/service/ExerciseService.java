package rto.intelfit.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import rto.intelfit.domain.Exercise;
import rto.intelfit.domain.User;
import rto.intelfit.dto.ExerciseDto;
import rto.intelfit.exception.BusinessException;
import rto.intelfit.exception.ErrorCode;
import rto.intelfit.repository.ExerciseRepository;
import rto.intelfit.repository.UserRepository;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ExerciseService {

    private final ExerciseRepository exerciseRepository;
    private final UserRepository userRepository;

    @Transactional
    public ExerciseDto.Response addExercise(ExerciseDto.Request request) {
        User user = userRepository.findById(request.getUserId())
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        Exercise exercise = Exercise.builder()
                .user(user)
                .exerciseName(request.getExerciseName())
                .category(request.getCategory())
                .weight(request.getWeight())
                .reps(request.getReps())
                .sets(request.getSets())
                .build();

        Exercise saved = exerciseRepository.save(exercise);
        log.info("운동 추가 완료 - userId: {}, name: {}", user.getId(), saved.getExerciseName());

        return ExerciseDto.Response.builder()
                .exerciseId(saved.getId())
                .exerciseName(saved.getExerciseName())
                .category(saved.getCategory())
                .weight(saved.getWeight())
                .reps(saved.getReps())
                .sets(saved.getSets())
                .createdAt(saved.getCreatedAt())
                .build();
    }

    public ExerciseDto.ListResponse getUserExercises(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        List<ExerciseDto.Response> exercises = exerciseRepository.findByUser(user).stream()
                .map(e -> ExerciseDto.Response.builder()
                        .exerciseId(e.getId())
                        .exerciseName(e.getExerciseName())
                        .category(e.getCategory())
                        .weight(e.getWeight())
                        .reps(e.getReps())
                        .sets(e.getSets())
                        .createdAt(e.getCreatedAt())
                        .build())
                .collect(Collectors.toList());

        return ExerciseDto.ListResponse.builder()
                .userId(userId)
                .exercises(exercises)
                .build();
    }

    @Transactional
    public ExerciseDto.DeleteResponse deleteExercise(Long exerciseId) {
        Exercise exercise = exerciseRepository.findById(exerciseId)
                .orElseThrow(() -> new BusinessException(ErrorCode.EXERCISE_NOT_FOUND));

        exerciseRepository.delete(exercise);
        log.info("운동 삭제 완료 - exerciseId: {}", exerciseId);

        return ExerciseDto.DeleteResponse.builder()
                .success(true)
                .message("운동이 삭제되었습니다.")
                .build();
    }
}
