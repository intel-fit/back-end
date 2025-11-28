package rto.intelfit.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import rto.intelfit.domain.TempMealDomain.TempMeal;

public interface TempMealRepo extends JpaRepository<TempMeal, Long> {
    void deleteByTempMealPlan_Id(Long tempMealPlanId);
}

