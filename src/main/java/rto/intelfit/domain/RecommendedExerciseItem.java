package rto.intelfit.domain;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

@Entity
@Table(name = "recommended_exercise_items")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RecommendedExerciseItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "recommended_exercise_routine_id", nullable = false)
    private RecommendedExerciseRoutine recommendedExerciseRoutine;

    @Column(name = "exercise_order")
    private Integer exerciseOrder;

    // 유산소 운동
    @Enumerated(EnumType.STRING)
    @Column(name = "cardio_type")
    private Exercise.CardioType cardioType;

    @Column(name = "target_distance", precision = 10, scale = 2)
    private BigDecimal targetDistance; // 유산소 - 목표 거리 (km)

    @Column(name = "target_duration_minutes")
    private Integer targetDurationMinutes; // 유산소 - 목표 시간 (분)

    @Column(name = "target_calories_burn", precision = 10, scale = 2)
    private BigDecimal targetCaloriesBurn; // 유산소 - 목표 칼로리 소모

    // 무산소 운동
    @Enumerated(EnumType.STRING)
    @Column(name = "resistance_exercise_type")
    private Exercise.ResistanceExerciseType resistanceExerciseType;

    @Enumerated(EnumType.STRING)
    @Column(name = "muscle_group")
    private Exercise.MuscleGroup muscleGroup;

    @Column(name = "recommended_sets")
    private Integer recommendedSets;

    @Column(name = "recommended_reps", length = 50)
    private String recommendedReps; // 예: "10-12", "8-10"

    @Column(name = "recommended_weight", length = 50)
    private String recommendedWeight; // 예: "80-100kg", "체중의 70%"

    @Column(name = "recommended_rest_seconds")
    private Integer recommendedRestSeconds;

    @Column(name = "description", length = 500)
    private String description; // 운동 설명 및 팁

    // 헬퍼 메서드
    public boolean isCardio() {
        return cardioType != null;
    }

    public boolean isResistance() {
        return resistanceExerciseType != null;
    }
}