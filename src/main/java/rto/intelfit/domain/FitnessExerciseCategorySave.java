package rto.intelfit.domain;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

/**
 * 운동 기록 엔티티
 * - 한 운동 세션(sessionId)에 여러 세트(row)가 포함될 수 있다.
 * - sessionId를 기준으로 그룹핑하여 사용자에게 보여준다.
 */
@Entity
@Table(name = "fitness_exercise_category_save")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FitnessExerciseCategorySave {
//운동 id 행 만들어야한다
    /** 각 행(세트)의 고유 PK */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** N:1 → User */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    /** 운동 세션 식별자 (한 운동 묶음) */
    @Column(name = "session_id", nullable = false, length = 50)
    private String sessionId;

    @Column(name = "external_id")
    private String externalId;

    /** 운동명 (예: 벤치프레스, 스쿼트 등) */
    @Column(name = "exercise_name", nullable = false, length = 100)
    private String exerciseName;

    /** 운동 부위 (예: 가슴, 등, 하체 등) */
    @Column(name = "category", length = 50)
    private String category;

    /** 세트 번호 (1세트, 2세트 등) */
    @Column(name = "set_number")
    private Integer setNumber;

    /** 중량 (kg) */
    @Column(name = "weight")
    private Double weight;

    /** 횟수 (reps) */
    @Column(name = "reps")
    private Integer reps;

    /** 운동 수행 날짜 */
    @Column(name = "workout_date")
    private LocalDateTime workoutDate;

    /** 완료 여부 (true=완료, false=미완료) */
    @Builder.Default
    @Column(name = "completed", nullable = false)
    private boolean completed = false;

    /** 생성 시각 */
    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    /** 수정 시각 */
    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    /** 저장 여부 (처음에는 false) */
    @Builder.Default
    @Column(name = "is_saved", nullable = false)
    private boolean isSaved = false;

    /** 저장 제목 */
    @Column(name = "save_title", length = 100)
    private String saveTitle;

}
