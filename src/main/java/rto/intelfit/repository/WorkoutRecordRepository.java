package rto.intelfit.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import rto.intelfit.domain.WorkoutRecord;

import java.time.LocalDate;
import java.util.Optional;

@Repository
public interface WorkoutRecordRepository extends JpaRepository<WorkoutRecord, Long> {

    /**
     * ✅ 사용자 + 날짜 기준으로 WorkoutRecord 1개 찾기
     * (존재 여부 확인 및 업데이트용)
     */
    Optional<WorkoutRecord> findByUserIdAndWorkoutDate(Long userId, LocalDate workoutDate);

    /**
     * ✅ 사용자 + 날짜 기준으로 세트 합계 구하기
     * (달성률 계산용)
     */
    @Query("SELECT COALESCE(SUM(r.setsCompleted), 0) " +
            "FROM WorkoutRecord r " +
            "WHERE r.user.id = :userId AND r.workoutDate = :date")
    int sumSetsCompletedByUserAndDate(@Param("userId") Long userId,
                                      @Param("date") LocalDate date);

    void deleteAllByUser_Id(Long userId);
}
