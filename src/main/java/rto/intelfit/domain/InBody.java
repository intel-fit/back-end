package rto.intelfit.domain;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "inbody_records")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InBody {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "measurement_date", nullable = false)
    private LocalDate measurementDate;

    // 기본 체성분
    @Column(name = "weight", precision = 5, scale = 2, nullable = false)
    private BigDecimal weight;

    @Column(name = "muscle_mass", precision = 5, scale = 2)
    private BigDecimal muscleMass;

    @Column(name = "body_fat_mass", precision = 5, scale = 2)
    private BigDecimal bodyFatMass;

    // 골격근/지방 분석
    @Column(name = "skeletal_muscle_mass", precision = 5, scale = 2)
    private BigDecimal skeletalMuscleMass;

    @Column(name = "body_fat_percentage", precision = 5, scale = 2)
    private BigDecimal bodyFatPercentage;

    // 부위별 근육량
    @Column(name = "left_arm_muscle", precision = 5, scale = 2)
    private BigDecimal leftArmMuscle;

    @Column(name = "right_arm_muscle", precision = 5, scale = 2)
    private BigDecimal rightArmMuscle;

    @Column(name = "trunk_muscle", precision = 5, scale = 2)
    private BigDecimal trunkMuscle;

    @Column(name = "left_leg_muscle", precision = 5, scale = 2)
    private BigDecimal leftLegMuscle;

    @Column(name = "right_leg_muscle", precision = 5, scale = 2)
    private BigDecimal rightLegMuscle;

    // 부위별 지방량
    @Column(name = "left_arm_fat", precision = 5, scale = 2)
    private BigDecimal leftArmFat;

    @Column(name = "right_arm_fat", precision = 5, scale = 2)
    private BigDecimal rightArmFat;

    @Column(name = "trunk_fat", precision = 5, scale = 2)
    private BigDecimal trunkFat;

    @Column(name = "left_leg_fat", precision = 5, scale = 2)
    private BigDecimal leftLegFat;

    @Column(name = "right_leg_fat", precision = 5, scale = 2)
    private BigDecimal rightLegFat;

    // 체수분/단백질/무기질
    @Column(name = "total_body_water", precision = 5, scale = 2)
    private BigDecimal totalBodyWater;

    @Column(name = "protein", precision = 5, scale = 2)
    private BigDecimal protein;

    @Column(name = "mineral", precision = 5, scale = 2)
    private BigDecimal mineral;

    // BMI 및 기타
    @Column(name = "bmi", precision = 5, scale = 2)
    private BigDecimal bmi;

    @Column(name = "body_fat_percentage_standard", precision = 5, scale = 2)
    private BigDecimal bodyFatPercentageStandard;

    @Column(name = "obesity_degree", precision = 5, scale = 2)
    private BigDecimal obesityDegree;

    // 내장지방 레벨
    @Column(name = "visceral_fat_level", precision = 5, scale = 2)
    private BigDecimal visceralFatLevel;

    // 기초대사량
    @Column(name = "basal_metabolic_rate")
    private Integer basalMetabolicRate;

    // 달성 뱃지
    @Enumerated(EnumType.STRING)
    @Column(name = "achievement_badge")
    @Builder.Default
    private AchievementBadge achievementBadge = AchievementBadge.NONE;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    // 달성 뱃지 열거형
    public enum AchievementBadge {
        GOLD,   // 금
        SILVER, // 은
        BRONZE, // 동
        NONE    // 없음
    }
}