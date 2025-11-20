package rto.intelfit.repository.temp;

import org.springframework.data.jpa.repository.JpaRepository;
import rto.intelfit.domain.temp.TempMealDomain.TempMealPlan;
import java.util.Optional;
import java.util.List;

public interface TempPlanRepo extends JpaRepository<TempMealPlan, Long> {
    List<TempMealPlan> findByTempBundleId(Long id);

    Optional<TempMealPlan> findByTempBundleIdAndDayIndex(Long bundleId, int dayIndex);
    void deleteByTempBundle_Id(Long bundleId);

}
