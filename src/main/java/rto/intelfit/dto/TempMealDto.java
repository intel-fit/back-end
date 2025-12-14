package rto.intelfit.dto;

import lombok.*;
import java.math.BigDecimal;
import java.util.List;
import rto.intelfit.domain.RecommendedMealPlan;

public class TempMealDto {

    // 1) 하루/주간 끼니수 요청 DTO
    @Getter @Setter
    public static class DailyRequest {
        private int mealsPerDay;

    }

    @Getter @Setter
    public static class WeeklyRequest {
        private int mealsPerDay;
    }

    // 2) 음식 정보 DTO
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

    // 3) 끼니 정보 DTO
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

    // 4) 하루 식단 응답 DTO
    @Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
    public static class PlanResponse {
        private Integer dayIndex;
        private List<MealResponse> meals;
        public static PlanResponse fromRecommendedPlan(RecommendedMealPlan plan) {
            return PlanResponse.builder()
                    .dayIndex(plan.getBundleDay())
                    .meals(
                            plan.getRecommendedMeals().stream()
                                    .map(rm -> MealResponse.builder()
                                            .id(null) // TEMP가 아니므로 원본 RecommendedMeal 에 id를 안 보낼 수도 있음
                                            .mealType(rm.getMealType().name())
                                            .totalCalories(rm.getTotalCalories())
                                            .totalCarbs(rm.getTotalCarbs())
                                            .totalProtein(rm.getTotalProtein())
                                            .totalFat(rm.getTotalFat())
                                            .foods(
                                                    rm.getRecommendedFoods().stream()
                                                            .map(rf -> FoodResponse.builder()
                                                                    .id(null)
                                                                    .foodName(rf.getFoodName())
                                                                    .servingSize(rf.getServingSize())
                                                                    .calories(rf.getCalories())
                                                                    .carbs(rf.getCarbs())
                                                                    .protein(rf.getProtein())
                                                                    .fat(rf.getFat())
                                                                    .build())
                                                            .toList()
                                            )
                                            .build())
                                    .toList()
                    )
                    .build();
        }

    }
}
