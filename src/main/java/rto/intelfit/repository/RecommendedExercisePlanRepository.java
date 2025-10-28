package rto.intelfit.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import rto.intelfit.domain.RecommendedExercisePlan;
import rto.intelfit.domain.User;

import java.util.List;

@Repository
public interface RecommendedExercisePlanRepository extends JpaRepository<RecommendedExercisePlan, Long> {

    // 사용자별 저장된 추천 플랜 조회
    List<RecommendedExercisePlan> findByUserAndIsSavedTrueOrderByCreatedAtDesc(User user);

    // 사용자별 모든 추천 플랜 조회 (최신순)
    List<RecommendedExercisePlan> findByUserOrderByCreatedAtDesc(User user);

    // 사용자의 저장된 추천 플랜 개수
    long countByUserAndIsSavedTrue(User user);

    // 사용자의 모든 추천 플랜 삭제 (회원 탈퇴 시)
    void deleteAllByUser(User user);

    // 사용자별 숙련도에 맞는 추천 플랜 조회
    List<RecommendedExercisePlan> findByUserAndTargetLevelOrderByCreatedAtDesc(
            User user, User.ExperienceLevel targetLevel);

    // 사용자별 피트니스 목표에 맞는 추천 플랜 조회
    List<RecommendedExercisePlan> findByUserAndFitnessGoalOrderByCreatedAtDesc(
            User user, User.HealthGoal fitnessGoal);
}