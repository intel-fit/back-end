package rto.intelfit.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import rto.intelfit.domain.RecommendedMealPlan;
import rto.intelfit.domain.User;

import java.util.List;

@Repository
public interface RecommendedMealPlanRepository extends JpaRepository<RecommendedMealPlan, Long> {

    // 사용자별 저장된 추천 식단 조회
    List<RecommendedMealPlan> findByUserAndIsSavedTrueOrderByCreatedAtDesc(User user);

    // 사용자별 모든 추천 식단 조회 (최신순)
    List<RecommendedMealPlan> findByUserOrderByCreatedAtDesc(User user);

    // 사용자의 저장된 추천 식단 개수
    long countByUserAndIsSavedTrue(User user);
}