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
@Table(name = "fitness_exercise_category_db")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FitnessExerciseCategoryDB {

    /** 내부 PK */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** ExerciseDB 원본 JSON의 고유 ID (예: 9Z23cLE) */
    @Column(name = "external_id", unique = true)
    private String externalId;

    /** 운동명 (예: 바벨 인클라인 벤치프레스(앉은 자세)) */
    @Column(nullable = false, length = 200)
    private String name;

    /** 운동 부위 (예: 가슴, 어깨, 하체 등) */
    @Column(length = 50)
    private String bodyPart;

    /** 주요 타겟 근육 (예: 대흉근, 이두근 등) */
    @Column(length = 100)
    private String targetMuscle;

    /** 보조 근육 (예: 삼두근, 어깨 등 — 콤마로 구분) */
    @Column(columnDefinition = "TEXT")
    private String secondaryMuscles;

    /** 사용 장비 (예: 바벨, 덤벨 등) */
    @Column(length = 100)
    private String equipment;

    /** 운동 이미지 또는 GIF URL */
    @Column(columnDefinition = "TEXT")
    private String imageUrl;

    /** 운동 단계별 설명 (1단계~n단계) */
    @Column(columnDefinition = "TEXT")
    private String instructions;
}