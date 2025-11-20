package rto.intelfit.domain;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import java.math.BigDecimal;

@Entity
@Table(name = "recommended_foods")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RecommendedFood {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "recommended_meal_id", nullable = false)
    private RecommendedMeal recommendedMeal;

    @Column(name = "food_name", nullable = false, length = 200)
    private String foodName;

    @Column(name = "serving_size", precision = 8, scale = 2, nullable = false)
    private BigDecimal servingSize;

    @Column(name = "calories", precision = 8, scale = 2, nullable = false)
    private BigDecimal calories;

    @Column(name = "carbs", precision = 8, scale = 2, nullable = false)
    private BigDecimal carbs;

    @Column(name = "protein", precision = 8, scale = 2, nullable = false)
    private BigDecimal protein;

    @Column(name = "fat", precision = 8, scale = 2, nullable = false)
    private BigDecimal fat;

    @Column(name = "sodium", precision = 8, scale = 2)
    private BigDecimal sodium;

    @Column(name = "sugar", precision = 8, scale = 2)
    private BigDecimal sugar;
}