package rto.intelfit.domain;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "recommended_meals")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RecommendedMeal {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "recommended_meal_plan_id", nullable = false)
    private RecommendedMealPlan recommendedMealPlan;

    @Enumerated(EnumType.STRING)
    @Column(name = "meal_type", nullable = false)
    private Meal.MealType mealType;

    // 해당 식사의 총 영양소
    @Column(name = "total_calories", precision = 8, scale = 2)
    private BigDecimal totalCalories;

    @Column(name = "total_carbs", precision = 8, scale = 2)
    private BigDecimal totalCarbs;

    @Column(name = "total_protein", precision = 8, scale = 2)
    private BigDecimal totalProtein;

    @Column(name = "total_fat", precision = 8, scale = 2)
    private BigDecimal totalFat;

    // 추천 식사에 포함된 음식들
    @OneToMany(mappedBy = "recommendedMeal", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<RecommendedFood> recommendedFoods = new ArrayList<>();

    // 편의 메서드
    public void addRecommendedFood(RecommendedFood recommendedFood) {
        recommendedFoods.add(recommendedFood);
        recommendedFood.setRecommendedMeal(this);
    }

    public void removeRecommendedFood(RecommendedFood recommendedFood) {
        recommendedFoods.remove(recommendedFood);
        recommendedFood.setRecommendedMeal(null);
    }
}