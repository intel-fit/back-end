package rto.intelfit.domain;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "recommended_meal_plans")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RecommendedMealPlan {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)

    private User user;

    @Column(name = "plan_name", nullable = false, length = 200)
    private String planName;

    @Column(name = "description", length = 1000)
    private String description;

    // 추천 식단의 총 영양소
    @Column(name = "total_calories", precision = 8, scale = 2)
    private BigDecimal totalCalories;

    @Column(name = "total_carbs", precision = 8, scale = 2)
    private BigDecimal totalCarbs;

    @Column(name = "total_protein", precision = 8, scale = 2)
    private BigDecimal totalProtein;

    @Column(name = "total_fat", precision = 8, scale = 2)
    private BigDecimal totalFat;

    // 추천 식단에 포함된 식사들
    @OneToMany(mappedBy = "recommendedMealPlan", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<RecommendedMeal> recommendedMeals = new ArrayList<>();

    // AI 추천 이유/근거
    @Column(name = "recommendation_reason", length = 1000)
    private String recommendationReason;

    @Column(name = "is_saved", nullable = false)
    @Builder.Default
    private Boolean isSaved = false; // 사용자가 저장했는지 여부

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    // 편의 메서드
    public void addRecommendedMeal(RecommendedMeal recommendedMeal) {
        recommendedMeals.add(recommendedMeal);
        recommendedMeal.setRecommendedMealPlan(this);
    }

    public void removeRecommendedMeal(RecommendedMeal recommendedMeal) {
        recommendedMeals.remove(recommendedMeal);
        recommendedMeal.setRecommendedMealPlan(null);
    }
}