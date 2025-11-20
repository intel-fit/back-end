package rto.intelfit.repository.temp;

import org.springframework.data.jpa.repository.JpaRepository;
import rto.intelfit.domain.temp.TempMealDomain.TempMeal;

public interface TempMealRepo extends JpaRepository<TempMeal, Long> {
    void deleteByTempMealPlan_Id(Long tempMealPlanId);
}

