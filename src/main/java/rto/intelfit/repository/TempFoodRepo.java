package rto.intelfit.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import rto.intelfit.domain.TempMealDomain.TempMealFood;

public interface TempFoodRepo extends JpaRepository<TempMealFood, Long> {
    void deleteByTempMeal_Id(Long mealId);
}


