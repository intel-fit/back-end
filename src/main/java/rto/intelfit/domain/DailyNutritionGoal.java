package rto.intelfit.domain;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "daily_nutrition_goals")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DailyNutritionGoal {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, unique = true)

    private User user;

    // 일일 목표 영양소 (사용자 신체 정보 기반 자동 계산 또는 수동 설정)
    @Column(name = "target_calories", precision = 8, scale = 2, nullable = false)
    private BigDecimal targetCalories;

    @Column(name = "target_carbs", precision = 8, scale = 2, nullable = false)
    private BigDecimal targetCarbs;

    @Column(name = "target_protein", precision = 8, scale = 2, nullable = false)
    private BigDecimal targetProtein;

    @Column(name = "target_fat", precision = 8, scale = 2, nullable = false)
    private BigDecimal targetFat;

    // 목표 설정 방식
    @Enumerated(EnumType.STRING)
    @Column(name = "goal_type", nullable = false)
    @Builder.Default
    private GoalType goalType = GoalType.AUTO;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    public enum GoalType {
        AUTO,   // 자동 계산 (신체 정보 기반)
        MANUAL  // 사용자 수동 설정
    }
}