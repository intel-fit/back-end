package rto.intelfit.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import rto.intelfit.domain.WorkoutPlanDetail;

@Repository
public interface WorkoutPlanDetailRepository extends JpaRepository<WorkoutPlanDetail, Long> {

    // ✅ 서비스에서 호출하는 이름 그대로
    @Query("SELECT COALESCE(SUM(w.sets), 0) " +
            "FROM WorkoutPlanDetail w " +
            "WHERE w.user.id = :userId AND w.dayOfWeek = :dayOfWeek")
    int sumSetsByUserAndDayOfWeek(@Param("userId") Long userId,
                                  @Param("dayOfWeek") String dayOfWeek);

    // (선택) 혹시 다른 곳에서 쓸 수도 있으니, 기존 별칭도 함께 두고 싶으면 유지해도 됩니다.
    @Query("SELECT COALESCE(SUM(w.sets), 0) " +
            "FROM WorkoutPlanDetail w " +
            "WHERE w.user.id = :userId AND w.dayOfWeek = :dayOfWeek")
    int countPlannedSetsByDay(@Param("userId") Long userId,
                              @Param("dayOfWeek") String dayOfWeek);
}
