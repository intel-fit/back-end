package rto.intelfit.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import rto.intelfit.domain.Exercise;

import java.time.LocalDate;

/**
 * ✅ DailyProgress 전용 Repository
 * 실제 운동 기록 (Exercise) 데이터를 조회
 */
@Repository
public interface DailyProgressExerciseRepository extends JpaRepository<Exercise, Long> {

    /**
     * 오늘 사용자가 실제로 수행한 운동 개수를 반환합니다.
     */
    @Query("SELECT COUNT(e) FROM Exercise e WHERE e.user.id = :userId AND e.exerciseDate = :date")
    long countCompletedExercises(@Param("userId") Long userId,
                                 @Param("date") LocalDate date);
}
