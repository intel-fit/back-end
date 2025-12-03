package rto.intelfit.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import rto.intelfit.domain.DailyNutritionGoal;
import rto.intelfit.domain.User;

import java.util.Optional;

@Repository
public interface DailyNutritionGoalRepository extends JpaRepository<DailyNutritionGoal, Long> {

    // 사용자별 영양 목표 조회
    Optional<DailyNutritionGoal> findByUser(User user);

    // 사용자별 영양 목표 존재 여부
    boolean existsByUser(User user);

    // 사용자의 영양 목표 삭제 (회원 탈퇴 시)
    void deleteAllByUser(User user);
}