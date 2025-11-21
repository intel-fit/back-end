package rto.intelfit.dto.tempmeal;

import lombok.*;
import java.math.BigDecimal;
import java.util.List;

public class TempMealDto {

    @Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
    public static class FoodResponse {
        private Long id;
        private String foodName;
        private BigDecimal servingSize;
        private BigDecimal calories;
        private BigDecimal carbs;
        private BigDecimal protein;
        private BigDecimal fat;
    }

    @Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
    public static class MealResponse {
        private Long id;
        private String mealType;
        private BigDecimal totalCalories;
        private BigDecimal totalCarbs;
        private BigDecimal totalProtein;
        private BigDecimal totalFat;
        private List<FoodResponse> foods;
    }

    @Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
    public static class PlanResponse {
        private Integer dayIndex;
        private List<MealResponse> meals;
    }
}
