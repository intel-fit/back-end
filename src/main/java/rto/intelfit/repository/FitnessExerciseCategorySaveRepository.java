package rto.intelfit.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import rto.intelfit.domain.FitnessExerciseCategorySave;
import rto.intelfit.domain.User;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface FitnessExerciseCategorySaveRepository extends JpaRepository<FitnessExerciseCategorySave, Long> {

    /** 특정 유저의 전체 운동 기록 조회 (최신순) */
    List<FitnessExerciseCategorySave> findByUserOrderByWorkoutDateDesc(User user);

    List<FitnessExerciseCategorySave> findByUserIdAndSessionIdIn(Long userId, List<String> sessionIds);
    /** 특정 세션 ID로 운동 세트 조회 */
    List<FitnessExerciseCategorySave> findBySessionId(String sessionId);

    @Query("SELECT DISTINCT f.sessionId FROM FitnessExerciseCategorySave f WHERE f.user = :user ORDER BY MAX(f.workoutDate) DESC")
    List<String> findDistinctSessionIdsByUser(@Param("user") User user);

    /**
     * 특정 날짜의 전체 세션(운동 종목) 수 카운트
     * - 같은 sessionId를 가진 세트들은 하나의 세션(종목)으로 간주
     */
    @Query("SELECT COUNT(DISTINCT f.sessionId) FROM FitnessExerciseCategorySave f " +
           "WHERE f.user = :user AND DATE(f.workoutDate) = :date")
    long countTotalSessionsByDate(@Param("user") User user, @Param("date") LocalDate date);

    /**
     * 특정 날짜의 완료된 세션(운동 종목) 수 카운트
     * - 한 세션의 모든 세트가 completed=true일 때만 완료된 세션으로 간주
     */
    @Query("SELECT COUNT(DISTINCT f.sessionId) FROM FitnessExerciseCategorySave f " +
           "WHERE f.user = :user AND DATE(f.workoutDate) = :date " +
           "AND f.sessionId NOT IN (" +
           "    SELECT DISTINCT f2.sessionId FROM FitnessExerciseCategorySave f2 " +
           "    WHERE f2.user = :user AND DATE(f2.workoutDate) = :date " +
           "    AND f2.completed = false" +
           ")")
    long countCompletedSessionsByDate(@Param("user") User user, @Param("date") LocalDate date);
    List<FitnessExerciseCategorySave> findByUserIdAndIsSavedFalse(Long userId);
    List<FitnessExerciseCategorySave> findByUserIdAndIsSavedTrueOrderBySaveTitleAsc(Long userId);

}