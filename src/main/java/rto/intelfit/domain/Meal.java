package rto.intelfit.domain;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "meals")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Meal {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private User user;

    @Column(name = "meal_date", nullable = false)
    private LocalDate mealDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "meal_type", nullable = false)
    private MealType mealType;

    // 총 영양소 정보 (해당 식사의 모든 음식 합계)..
    @Column(name = "total_calories", precision = 8, scale = 2)
    @Builder.Default
    private BigDecimal totalCalories = BigDecimal.ZERO;

    @Column(name = "total_carbs", precision = 8, scale = 2)
    @Builder.Default
    private BigDecimal totalCarbs = BigDecimal.ZERO;

    @Column(name = "total_protein", precision = 8, scale = 2)
    @Builder.Default
    private BigDecimal totalProtein = BigDecimal.ZERO;

    @Column(name = "total_fat", precision = 8, scale = 2)
    @Builder.Default
    private BigDecimal totalFat = BigDecimal.ZERO;

    // 식사에 포함된 음식들
    @OneToMany(mappedBy = "meal", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<MealFood> mealFoods = new ArrayList<>();

    @Column(name = "memo", length = 500)
    private String memo;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    // 식사 타입 열거형
    public enum MealType {
        BREAKFAST("아침"),
        LUNCH("점심"),
        DINNER("저녁"),
        SNACK("간식");

        private final String description;

        MealType(String description) {
            this.description = description;
        }

        public String getDescription() {
            return description;
        }
    }

    // 편의 메서드
    public void addMealFood(MealFood mealFood) {
        mealFoods.add(mealFood);
        mealFood.setMeal(this);
        recalculateTotals();
    }

    public void removeMealFood(MealFood mealFood) {
        mealFoods.remove(mealFood);
        mealFood.setMeal(null);
        recalculateTotals();
    }

    // 총 영양소 재계산
    public void recalculateTotals() {
        this.totalCalories = mealFoods.stream()
                .map(MealFood::getCalories)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        this.totalCarbs = mealFoods.stream()
                .map(MealFood::getCarbs)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        this.totalProtein = mealFoods.stream()
                .map(MealFood::getProtein)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        this.totalFat = mealFoods.stream()
                .map(MealFood::getFat)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}