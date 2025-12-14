package rto.intelfit.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import rto.intelfit.domain.User;
import rto.intelfit.repository.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserCleanupService {

    private final InBodyRepository inBodyRepository;
    private final InBodyAnalysisResultRepository inBodyAnalysisResultRepository;
    private final UserFoodPreferenceRepository userFoodPreferenceRepository;
    private final DailyNutritionGoalRepository dailyNutritionGoalRepository;
    private final MealRepository mealRepository;
    private final RecommendedMealPlanRepository recommendedMealPlanRepository;
    private final TempBundleRepo tempBundleRepo;
    private final ExerciseRepository exerciseRepository;
    private final ExerciseGoalRepository exerciseGoalRepository;
    private final FitnessExerciseCategorySaveRepository fitnessExerciseCategorySaveRepository;
    private final WorkoutPlanDetailRepository workoutPlanDetailRepository;
    private final WorkoutRecordRepository workoutRecordRepository;
    private final DailyProgressRepository dailyProgressRepository;
    private final TempExerciseSummaryRepository tempExerciseSummaryRepository;
    private final UserRecommendedExerciseRepository userRecommendedExerciseRepository;
    private final RecommendedExercisePlanRepository recommendedExercisePlanRepository;
    private final UserBadgeRepository userBadgeRepository;
    private final PaymentHistoryRepository paymentHistoryRepository;
    private final SubscriptionRepository subscriptionRepository;
    private final AIChatMessageRepository aiChatMessageRepository;
    private final AIServerService aiServerService;

    /**
     * Removes all entities that reference the given user to avoid FK violations during deletion.
     */
    @Transactional
    public void cleanup(User user) {
        Long userSeq = user.getId();
        String loginId = user.getUserId();

        log.info("회원 데이터 정리 시작 - userId: {}", loginId);

        // ✅ AI 서버 사용자 데이터 삭제 (가장 먼저 호출)
        try {
            aiServerService.deleteUserOnAI(user);
            log.info("AI 서버 사용자 데이터 삭제 완료 - userId: {}", loginId);
        } catch (Exception e) {
            log.warn("AI 서버 사용자 삭제 실패 (계속 진행) - userId: {}, error: {}", loginId, e.getMessage());
        }

        workoutRecordRepository.deleteAllByUser_Id(userSeq);
        dailyProgressRepository.deleteAllByUser_Id(userSeq);
        tempExerciseSummaryRepository.deleteAllByUser(user);
        userRecommendedExerciseRepository.deleteAllByUser(user);
        fitnessExerciseCategorySaveRepository.deleteAllByUser(user);
        workoutPlanDetailRepository.deleteAllByUser_Id(userSeq);
        exerciseGoalRepository.deleteAllByUser(user);

        recommendedExercisePlanRepository.deleteAllByUser(user);
        exerciseRepository.deleteAllByUser(user);

        inBodyAnalysisResultRepository.deleteAllByUser(user);
        inBodyRepository.deleteAllByUser(user);

        userFoodPreferenceRepository.deleteAllByUser(user);
        dailyNutritionGoalRepository.deleteAllByUser(user);

        long mealCountBefore = mealRepository.countByUser(user);
        if (mealCountBefore > 0) {
            log.info("탈퇴 사용자 식단 기록 {}건 삭제 예정 - userId: {}", mealCountBefore, loginId);
        }
        mealRepository.deleteAllByUser(user);
        long mealCountAfter = mealRepository.countByUser(user);
        log.info("탈퇴 사용자 식단 삭제 결과 - userId: {}, 잔여 {}건", loginId, mealCountAfter);

        recommendedMealPlanRepository.deleteAllByUser(user);
        tempBundleRepo.deleteAllByUser(user);

        userBadgeRepository.deleteAllByUser(user);
        paymentHistoryRepository.deleteAllByUser_Id(userSeq);
        subscriptionRepository.deleteAllByUserId(loginId);
        aiChatMessageRepository.deleteAllByUser_UserId(loginId);

        log.info("회원 데이터 정리 완료 - userId: {}", loginId);
    }
}
