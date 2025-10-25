package rto.intelfit.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import rto.intelfit.domain.DailyProgressExercisePlan; // ✅ 새 엔티티 import

import java.time.LocalDate;

/**
 * ✅ DailyProgress 전용 Repository
 * DailyProgressExercisePlan 데이터를 조회하여 "오늘 계획된 운동 수" 계산
 */
@Repository
public interface DailyProgressExercisePlanRepository extends JpaRepository<DailyProgressExercisePlan, Long> {

    @Query("SELECT COUNT(p) FROM DailyProgressExercisePlan p WHERE p.user.id = :userId AND p.planDate = :date")
    int countPlannedExercises(@Param("userId") Long userId,
                              @Param("date") LocalDate date);
}
