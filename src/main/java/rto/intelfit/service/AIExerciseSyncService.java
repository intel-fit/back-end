package rto.intelfit.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import rto.intelfit.domain.FitnessExerciseCategorySave;
import rto.intelfit.domain.User;
import rto.intelfit.exception.BusinessException;
import rto.intelfit.exception.ErrorCode;
import rto.intelfit.repository.FitnessExerciseCategorySaveRepository;
import rto.intelfit.repository.UserRepository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class AIExerciseSyncService {

    private final AIServerClient aiServerClient;
    private final UserRepository userRepository;
    private final FitnessExerciseCategorySaveRepository saveRepository;

    /**
     *  AI 추천 운동을 받아서 DB(FitnessExerciseCategorySave)에 자동 저장
     */
    public void syncAIRecommendedExercises(Long userId) {
        log.info("🤖 AI 추천 운동 DB 저장 시작 - userId={}", userId);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        //  AI 서버에서 추천 운동 받아오기
        Map<String, Object> aiResult = aiServerClient.generateExercisePlan(Map.of("user_id", userId));
        if (aiResult == null || aiResult.isEmpty()) {
            log.warn("⚠️ AI 추천 결과 없음 (userId={})", userId);
            return;
        }

        List<Map<String, Object>> exercises = (List<Map<String, Object>>) aiResult.get("exercises");
        if (exercises == null || exercises.isEmpty()) {
            log.warn("⚠️ 추천 운동이 비어 있음 (userId={})", userId);
            return;
        }

        //  DB 저장용 엔티티 변환
        String sessionId = "AI-" + System.currentTimeMillis();

        List<FitnessExerciseCategorySave> entities = exercises.stream()
                .map(ex -> FitnessExerciseCategorySave.builder()
                        .user(user)
                        .sessionId(sessionId)
                        .externalId(String.valueOf(ex.getOrDefault("id", "AI-EX")))
                        .exerciseName((String) ex.getOrDefault("name", "알 수 없는 운동"))
                        .category((String) ex.getOrDefault("category", "기타"))
                        .setNumber(1)
                        .reps(10)
                        .workoutDate(LocalDateTime.now())
                        .build())
                .toList();

        saveRepository.saveAll(entities);

        log.info("✅ AI 추천 운동 {}개 저장 완료 - sessionId={}, userId={}",
                entities.size(), sessionId, userId);
    }

    public void syncAIRecommendedExercises(String userId) {
    }
}
