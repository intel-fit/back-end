package rto.intelfit.repository.temp;

import org.springframework.data.jpa.repository.JpaRepository;
import rto.intelfit.domain.temp.TempMealDomain.TempMealFood;

public interface TempFoodRepo extends JpaRepository<TempMealFood, Long> {
    void deleteByTempMeal_Id(Long mealId);
}


