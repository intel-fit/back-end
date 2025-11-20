package rto.intelfit.domain.temp;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import rto.intelfit.domain.Meal;
import rto.intelfit.domain.User;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class TempMealDomain {

    // =========================================================
    // 🔹 TempMealBundle
    // =========================================================
    @Getter
    @Setter
    @Entity
    @Table(name = "temp_meal_bundles")
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class TempMealBundle {

        @Id
        @GeneratedValue(strategy = GenerationType.IDENTITY)
        private Long id;

        @ManyToOne(fetch = FetchType.LAZY)
        private User user;

        @CreationTimestamp
        private LocalDateTime createdAt;

        @UpdateTimestamp
        private LocalDateTime updatedAt;

        // ⭐⭐ 핵심 추가 — cascade + orphanRemoval
        @OneToMany(
                mappedBy = "tempBundle",
                cascade = CascadeType.ALL,
                orphanRemoval = true
        )
        @Builder.Default
        private List<TempMealPlan> plans = new ArrayList<>();

        public void addPlan(TempMealPlan plan) {
            plans.add(plan);
            plan.setTempBundle(this);
        }
    }

    // =========================================================
    // 🔹 TempMealPlan
    // =========================================================
    @Getter
    @Setter
    @Entity
    @Table(name = "temp_meal_plans")
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class TempMealPlan {

        @Id
        @GeneratedValue(strategy = GenerationType.IDENTITY)
        private Long id;

        @ManyToOne(fetch = FetchType.LAZY)
        private TempMealBundle tempBundle;

        private Integer dayIndex;

        private BigDecimal totalCalories;
        private BigDecimal totalCarbs;
        private BigDecimal totalProtein;
        private BigDecimal totalFat;

        @OneToMany(mappedBy = "tempMealPlan", cascade = CascadeType.ALL, orphanRemoval = true)
        @Builder.Default
        private List<TempMeal> meals = new ArrayList<>();

        public void addMeal(TempMeal meal) {
            meals.add(meal);
            meal.setTempMealPlan(this);
        }
    }

    // =========================================================
    // 🔹 TempMeal
    // =========================================================
    @Getter
    @Setter
    @Entity
    @Table(name = "temp_meals")
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class TempMeal {

        @Id
        @GeneratedValue(strategy = GenerationType.IDENTITY)
        private Long id;

        @ManyToOne(fetch = FetchType.LAZY)
        private TempMealPlan tempMealPlan;

        @Enumerated(EnumType.STRING)
        private Meal.MealType mealType;

        private BigDecimal totalCalories;
        private BigDecimal totalCarbs;
        private BigDecimal totalProtein;
        private BigDecimal totalFat;

        @OneToMany(mappedBy = "tempMeal", cascade = CascadeType.ALL, orphanRemoval = true)
        @Builder.Default
        private List<TempMealFood> foods = new ArrayList<>();

        public void addFood(TempMealFood food) {
            foods.add(food);
            food.setTempMeal(this);
        }
    }

    // =========================================================
    // 🔹 TempMealFood
    // =========================================================
    @Getter
    @Setter
    @Entity
    @Table(name = "temp_meal_foods")
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class TempMealFood {

        @Id
        @GeneratedValue(strategy = GenerationType.IDENTITY)
        private Long id;

        private String foodName;
        private BigDecimal servingSize;
        private BigDecimal calories;
        private BigDecimal carbs;
        private BigDecimal protein;
        private BigDecimal fat;

        @ManyToOne(fetch = FetchType.LAZY)
        private TempMeal tempMeal;
    }
}
