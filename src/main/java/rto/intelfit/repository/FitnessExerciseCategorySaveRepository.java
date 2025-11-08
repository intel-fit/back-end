package rto.intelfit.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import rto.intelfit.domain.FitnessExerciseCategorySave;
import rto.intelfit.domain.User;

import java.util.List;

@Repository
public interface FitnessExerciseCategorySaveRepository extends JpaRepository<FitnessExerciseCategorySave, Long> {

    /** 특정 유저의 전체 운동 기록 조회 (최신순) */
    List<FitnessExerciseCategorySave> findByUserOrderByWorkoutDateDesc(User user);

    /** 특정 세션 ID로 운동 세트 조회 */
    List<FitnessExerciseCategorySave> findBySessionId(String sessionId);

    /** 특정 유저의 세션 ID 목록 (중복 제거) */
    @Query("SELECT DISTINCT f.sessionId FROM FitnessExerciseCategorySave f WHERE f.user = :user ORDER BY MAX(f.workoutDate) DESC")
    List<String> findDistinctSessionIdsByUser(@Param("user") User user);
}