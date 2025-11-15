package rto.intelfit.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import rto.intelfit.domain.FitnessExerciseCategorySave;
import rto.intelfit.domain.User;
import rto.intelfit.dto.FitnessExerciseCategorySaveDto;
import rto.intelfit.exception.BusinessException;
import rto.intelfit.exception.ErrorCode;
import rto.intelfit.repository.FitnessExerciseCategorySaveRepository;
import rto.intelfit.repository.UserRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class FitnessExerciseCategorySaveService {

    private final FitnessExerciseCategorySaveRepository saveRepository;
    private final UserRepository userRepository;
    private final DailyProgressService dailyProgressService;

    /**
     * 1️⃣ 특정 유저의 운동 기록을 세션 단위로 그룹핑하여 조회
     */
    @Transactional(readOnly = true)
    public List<FitnessExerciseCategorySaveDto.SessionResponse> getUserGroupedSessions(Long userId) {
        log.info("🔍 유저 ID={} 의 운동 세션 기록 조회 시작", userId);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        List<FitnessExerciseCategorySave> records =
                saveRepository.findByUserOrderByWorkoutDateDesc(user);

        // sessionId 기준 그룹핑
        Map<String, List<FitnessExerciseCategorySave>> grouped =
                records.stream().collect(Collectors.groupingBy(FitnessExerciseCategorySave::getSessionId));

        List<FitnessExerciseCategorySaveDto.SessionResponse> result = grouped.entrySet().stream()
                .map(entry -> FitnessExerciseCategorySaveDto.SessionResponse.from(entry.getKey(), entry.getValue()))
                .collect(Collectors.toList());

        log.info("✅ 유저 ID={} 의 운동 세션 {}개 반환", userId, result.size());
        return result;
    }

    /**
     * 2️⃣ 세션 ID 기준으로 삭제
     * ✅ 삭제 후 해당 날짜의 달성률 재계산
     */
    public FitnessExerciseCategorySaveDto.DeleteResponse deleteBySessionId(String sessionId) {
        log.info("🗑 세션 ID={} 삭제 요청", sessionId);

        List<FitnessExerciseCategorySave> sessionRecords = saveRepository.findBySessionId(sessionId);
        if (sessionRecords.isEmpty()) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "해당 세션 ID에 해당하는 기록이 없습니다.");
        }

        User user = sessionRecords.get(0).getUser();
        LocalDate workoutDate = sessionRecords.get(0).getWorkoutDate().toLocalDate();
        String externalId = sessionRecords.get(0).getExternalId();   // ✅ 운동 ID 추출

        saveRepository.deleteAll(sessionRecords);
        log.info("✅ 세션 ID={} 삭제 완료 ({}개 세트)", sessionId, sessionRecords.size());

        dailyProgressService.recalculateProgress(user, workoutDate);

        return FitnessExerciseCategorySaveDto.DeleteResponse.builder()
                .sessionId(sessionId)
                .externalId(externalId)
                .deletedCount(sessionRecords.size())
                .build();
    }

    /**
     * 3️⃣ 운동 세션 추가 (세트 여러 개 포함)
     * ✅ 저장 후 해당 날짜의 달성률 자동 재계산
     */
    public String addWorkoutSession(FitnessExerciseCategorySaveDto.CreateRequest request) {
        log.info("💪 운동 세션 추가 요청 - userId={}, exerciseId={}, exerciseName={}, sets={}",
                request.getUserId(), request.getExternalId(), request.getExerciseName(), request.getSets().size());

        User user = userRepository.findById(request.getUserId())
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        String sessionId = "S-" + System.currentTimeMillis();

        // ✅ 운동 ID 포함
        List<FitnessExerciseCategorySave> entities = request.getSets().stream()
                .map(set -> FitnessExerciseCategorySave.builder()
                        .user(user)
                        .sessionId(sessionId)
                        .externalId(request.getExternalId())        // 추가
                        .exerciseName(request.getExerciseName())
                        .category(request.getCategory())
                        .setNumber(set.getSetNumber())
                        .weight(set.getWeight())
                        .reps(set.getReps())
                        .workoutDate(request.getWorkoutDate())
                        .build())
                .collect(Collectors.toList());

        saveRepository.saveAll(entities);
        log.info("✅ 세션ID={} 운동 '{}' 저장 완료 (미완료 상태)", sessionId, request.getExerciseName());

        LocalDate workoutDate = request.getWorkoutDate().toLocalDate();
        dailyProgressService.recalculateProgress(user, workoutDate);

        return sessionId;
    }

    /**
     * 4️⃣ 세션 완료 상태 토글 (완료 ↔ 미완료)
     * ✅ 해당 세션의 모든 세트를 완료/미완료 처리
     * ✅ 토글 후 해당 날짜의 달성률 재계산
     */
    public FitnessExerciseCategorySaveDto.ToggleResponse toggleSessionCompletion(String sessionId) {
        log.info("🔄 세션 완료 상태 토글 요청 - sessionId={}", sessionId);

        List<FitnessExerciseCategorySave> sessionRecords = saveRepository.findBySessionId(sessionId);
        if (sessionRecords.isEmpty()) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "해당 세션 ID에 해당하는 기록이 없습니다.");
        }

        // 현재 상태 확인 (하나라도 미완료면 전체를 완료로, 모두 완료면 전체를 미완료로)
        boolean hasIncomplete = sessionRecords.stream().anyMatch(record -> !record.isCompleted());
        boolean newCompletionStatus = hasIncomplete;  // 미완료가 있으면 → 완료로, 모두 완료면 → 미완료로

        // 모든 세트의 완료 상태 변경
        sessionRecords.forEach(record -> record.setCompleted(newCompletionStatus));
        saveRepository.saveAll(sessionRecords);

        log.info("✅ 세션 ID={} 완료 상태 변경: {} ({}개 세트)", 
                sessionId, newCompletionStatus ? "완료" : "미완료", sessionRecords.size());

        // 달성률 재계산
        User user = sessionRecords.get(0).getUser();
        LocalDate workoutDate = sessionRecords.get(0).getWorkoutDate().toLocalDate();
        dailyProgressService.recalculateProgress(user, workoutDate);

        return FitnessExerciseCategorySaveDto.ToggleResponse.builder()
                .sessionId(sessionId)
                .completed(newCompletionStatus)
                .affectedSets(sessionRecords.size())
                .build();
    }
}
