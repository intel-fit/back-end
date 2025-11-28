package rto.intelfit.domain;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(
        name = "recommended_meal_plans",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_bundle_day", columnNames = {"bundle_id", "bundle_day"})
        }
)

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RecommendedMealPlan {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 7일 묶음을 나타내는 공통 ID */
    @Column(name = "bundle_id", nullable = false, length = 36)
    private String bundleId;

    /** 번들 내 일자(1~7) */
    @Column(name = "bundle_day", nullable = false)
    private Integer bundleDay;

    /** 실제 날짜(옵션: 주차 조회/정렬용) */
    @Column(name = "plan_date")
    private LocalDate planDate;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private User user;

    @Column(name = "plan_name", nullable = false, length = 200)
    private String planName;

    @Column(name = "description", length = 1000)
    private String description;

    @Column(name = "total_calories", precision = 8, scale = 2)
    private BigDecimal totalCalories;

    @Column(name = "total_carbs", precision = 8, scale = 2)
    private BigDecimal totalCarbs;

    @Column(name = "total_protein", precision = 8, scale = 2)
    private BigDecimal totalProtein;

    @Column(name = "total_fat", precision = 8, scale = 2)
    private BigDecimal totalFat;

    @OneToMany(mappedBy = "recommendedMealPlan", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<RecommendedMeal> recommendedMeals = new ArrayList<>();

    @Column(name = "recommendation_reason", length = 1000)
    private String recommendationReason;

    @Column(name = "is_saved", nullable = false)
    @Builder.Default
    private Boolean isSaved = false;

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
