package rto.intelfit.domain;
import jakarta.persistence.*;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDate;
import java.time.LocalDateTime;


@Entity
@Table(name = "fitness_exercise_category_save")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FitnessExerciseCategorySave {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "session_id", nullable = false, length = 50)
    private String sessionId;

    @Column(name = "external_id")
    private String externalId;

    @Column(name = "exercise_name", nullable = false, length = 100)
    private String exerciseName;

    @Column(name = "category", length = 50)
    private String category;

    @Column(name = "set_number")
    private Integer setNumber;

    @Column(name = "weight")
    private Double weight;

    @Column(name = "reps")
    private Integer reps;

    @Column(name = "workout_date")
    private LocalDateTime workoutDate;

    @Builder.Default
    @Column(name = "completed", nullable = false)
    private boolean completed = false;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Builder.Default
    @Column(name = "is_saved", nullable = false)
    private boolean isSaved = false;

    @Column(name = "save_title", length = 100)
    private String saveTitle;

    @Builder.Default
    @Column(name = "date", nullable = false)
    private LocalDate date = LocalDate.now();

    /* ================== 추가 컬럼 ================== */

    // 세트(행) 단위 운동 시간 (초)
    @Builder.Default
    @Column(name = "exercise_seconds", nullable = false)
    private long exerciseSeconds = 0L;

    // MET 값
    @Column(name = "met")
    private Double met;

    // 계산된 소모 칼로리 (세션 귀속)
    @Builder.Default
    @Column(name = "calories_burned", nullable = false)
    private double caloriesBurned = 0.0;
}
