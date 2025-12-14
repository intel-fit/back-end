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

    /**
     * Removes all entities that reference the given user to avoid FK violations during deletion.
     */
    @Transactional
    public void cleanup(User user) {
        Long userSeq = user.getId();
        String loginId = user.getUserId();

        log.info("회원 데이터 정리 시작 - userId: {}", loginId);

        workoutRecordRepository.deleteAllByUser_Id(userSeq);
        logDeletion(loginId, "workout records");
        dailyProgressRepository.deleteAllByUser_Id(userSeq);
        logDeletion(loginId, "daily progress");
        tempExerciseSummaryRepository.deleteAllByUser(user);
        logDeletion(loginId, "temp exercise summary");
        userRecommendedExerciseRepository.deleteAllByUser(user);
        logDeletion(loginId, "user recommended exercises");
        fitnessExerciseCategorySaveRepository.deleteAllByUser(user);
        logDeletion(loginId, "saved fitness exercises");
        workoutPlanDetailRepository.deleteAllByUser_Id(userSeq);
        logDeletion(loginId, "workout plan details");
        exerciseGoalRepository.deleteAllByUser(user);
        logDeletion(loginId, "exercise goals");

        recommendedExercisePlanRepository.deleteAllByUser(user);
        logDeletion(loginId, "recommended exercise plans");
        exerciseRepository.deleteAllByUser(user);
        logDeletion(loginId, "exercise logs");

        inBodyAnalysisResultRepository.deleteAllByUser(user);
        logDeletion(loginId, "inbody analysis results");
        inBodyRepository.deleteAllByUser(user);
        logDeletion(loginId, "inbody records");

        userFoodPreferenceRepository.deleteAllByUser(user);
        logDeletion(loginId, "food preferences");
        dailyNutritionGoalRepository.deleteAllByUser(user);
        logDeletion(loginId, "daily nutrition goals");
        mealRepository.deleteAllByUser(user);
        logDeletion(loginId, "meals");
        recommendedMealPlanRepository.deleteAllByUser(user);
        logDeletion(loginId, "recommended meal plans");
        tempBundleRepo.deleteAllByUser(user);
        logDeletion(loginId, "temp meal bundles");

        userBadgeRepository.deleteAllByUser(user);
        logDeletion(loginId, "user badges");
        paymentHistoryRepository.deleteAllByUser_Id(userSeq);
        logDeletion(loginId, "payment history");
        subscriptionRepository.deleteAllByUserId(loginId);
        logDeletion(loginId, "subscriptions");
        aiChatMessageRepository.deleteAllByUser_UserId(loginId);
        logDeletion(loginId, "AI chat messages");

        log.info("회원 데이터 정리 완료 - userId: {}", loginId);
    }

    private void logDeletion(String userId, String target) {
        log.info("데이터 삭제 완료 - {} (userId={})", target, userId);
    }
}
