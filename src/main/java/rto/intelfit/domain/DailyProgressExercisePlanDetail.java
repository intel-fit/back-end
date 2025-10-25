package rto.intelfit.domain;

import jakarta.persistence.*;
import lombok.*;

/**
 * ✅ DailyProgressExercisePlanDetail
 * DailyProgress 전용 운동 계획 세부 항목 엔티티
 */
@Entity
@Table(name = "daily_progress_exercise_plan_details")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DailyProgressExercisePlanDetail {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 상위 계획 (DailyProgressExercisePlan)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "exercise_plan_id", nullable = false)
    private DailyProgressExercisePlan exercisePlan;

    // 운동 종류 (기존 Exercise의 Enum 재사용)
    @Enumerated(EnumType.STRING)
    @Column(name = "exercise_type", nullable = false, length = 50)
    private Exercise.ResistanceExerciseType exerciseType;

    // 세트 수
    @Column(name = "sets")
    private Integer sets;

    // 반복 횟수
    @Column(name = "reps")
    private Integer reps;

    // 목표 중량 (kg)
    @Column(name = "weight")
    private Double weight;

    // 휴식 시간 (초)
    @Column(name = "rest_seconds")
    private Integer restSeconds;
}
