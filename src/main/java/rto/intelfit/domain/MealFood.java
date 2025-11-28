package rto.intelfit.domain;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "meal_foods")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MealFood {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "meal_id", nullable = false)
    private Meal meal;

    // AI 서버로부터 받은 음식 정보
    @Column(name = "food_name", nullable = false, length = 200)
    private String foodName;

    @Column(name = "serving_size", precision = 8, scale = 2, nullable = false)
    private BigDecimal servingSize; // 1인분 기준량 (g)

    @Column(name = "calories", precision = 8, scale = 2, nullable = false)
    private BigDecimal calories;

    @Column(name = "carbs", precision = 8, scale = 2, nullable = false)
    private BigDecimal carbs; // 탄수화물 (g)

    @Column(name = "protein", precision = 8, scale = 2, nullable = false)
    private BigDecimal protein; // 단백질 (g)

    @Column(name = "fat", precision = 8, scale = 2, nullable = false)
    private BigDecimal fat; // 지방 (g)

    // 선택적 영양소
    @Column(name = "sodium", precision = 8, scale = 2)
    private BigDecimal sodium; // 나트륨 (mg)

    @Column(name = "cholesterol", precision = 8, scale = 2)
    private BigDecimal cholesterol; // 콜레스테롤 (mg)

    @Column(name = "sugar", precision = 8, scale = 2)
    private BigDecimal sugar; // 당 (g)

    @Column(name = "fiber", precision = 8, scale = 2)
    private BigDecimal fiber; // 식이섬유 (g)

    // AI 분석 이미지 URL (옵션)
    @Column(name = "image_url", length = 500)
    private String imageUrl;

    @Column(name = "ai_confidence_score", precision = 5, scale = 2)
    private BigDecimal aiConfidenceScore; // AI 신뢰도 점수 (0-100)

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
}