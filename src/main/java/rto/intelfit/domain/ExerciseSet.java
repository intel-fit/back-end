package rto.intelfit.domain;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import java.math.BigDecimal;

@Entity
@Table(name = "exercise_sets")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ExerciseSet {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "exercise_id", nullable = false)
    private Exercise exercise;

    // 유산소 운동 타입 (유산소인 경우만)
    @Enumerated(EnumType.STRING)
    @Column(name = "cardio_type", length = 30)
    private Exercise.CardioType cardioType;

    // 무산소 운동 타입 (무산소인 경우만)
    @Enumerated(EnumType.STRING)
    @Column(name = "resistance_exercise_type", length = 50)
    private Exercise.ResistanceExerciseType resistanceExerciseType;

    // 무산소 운동 부위 (무산소인 경우만)
    @Enumerated(EnumType.STRING)
    @Column(name = "muscle_group", length = 20)
    private Exercise.MuscleGroup muscleGroup;

    // ========== 유산소 운동 필드 ==========

    // 거리 (km)
    @Column(name = "distance", precision = 10, scale = 2)
    private BigDecimal distance;

    // 운동 시간 (분)
    @Column(name = "duration_minutes")
    private Integer durationMinutes;

    // 칼로리 소모량 (kcal)
    @Column(name = "calories_burned", precision = 10, scale = 2)
    private BigDecimal caloriesBurned;

    // 평균 속도 (km/h)
    @Column(name = "average_speed", precision = 10, scale = 2)
    private BigDecimal averageSpeed;

    // 평균 심박수 (bpm)
    @Column(name = "average_heart_rate")
    private Integer averageHeartRate;

    // ========== 무산소 운동 필드 ==========

    // 세트 번호
    @Column(name = "set_number")
    private Integer setNumber;

    // 무게 (kg)
    @Column(name = "weight", precision = 10, scale = 2)
    private BigDecimal weight;

    // 반복 횟수 (reps)
    @Column(name = "reps")
    private Integer reps;

    // 휴식 시간 (초)
    @Column(name = "rest_seconds")
    private Integer restSeconds;

    // 메모
    @Column(name = "memo", length = 500)
    private String memo;

    // ========== 헬퍼 메서드 ==========

    /**
     * 유산소 운동인지 확인
     */
    public boolean isCardio() {
        return cardioType != null;
    }

    /**
     * 무산소 운동인지 확인
     */
    public boolean isResistance() {
        return resistanceExerciseType != null;
    }

    /**
     * 1RM 계산 (Epley 공식)
     * 1RM = weight × (1 + reps / 30)
     */
    public BigDecimal calculateOneRepMax() {
        if (weight == null || reps == null || reps == 0) {
            return BigDecimal.ZERO;
        }

        BigDecimal repsDecimal = BigDecimal.valueOf(reps);
        BigDecimal coefficient = repsDecimal.divide(BigDecimal.valueOf(30), 4, BigDecimal.ROUND_HALF_UP);
        BigDecimal multiplier = BigDecimal.ONE.add(coefficient);

        return weight.multiply(multiplier).setScale(2, BigDecimal.ROUND_HALF_UP);
    }

    /**
     * 볼륨 계산 (무산소)
     * Volume = weight × reps
     */
    public BigDecimal calculateVolume() {
        if (weight == null || reps == null) {
            return BigDecimal.ZERO;
        }

        return weight.multiply(BigDecimal.valueOf(reps)).setScale(2, BigDecimal.ROUND_HALF_UP);
    }

    /**
     * 평균 페이스 계산 (유산소)
     * Pace = duration / distance (분/km)
     */
    public BigDecimal calculateAveragePace() {
        if (durationMinutes == null || distance == null || distance.compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.ZERO;
        }

        return BigDecimal.valueOf(durationMinutes)
                .divide(distance, 2, BigDecimal.ROUND_HALF_UP);
    }
}