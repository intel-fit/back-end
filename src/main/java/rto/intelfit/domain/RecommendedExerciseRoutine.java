package rto.intelfit.domain;

import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "recommended_exercise_routines")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RecommendedExerciseRoutine {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "recommended_exercise_plan_id", nullable = false)
    private RecommendedExercisePlan recommendedExercisePlan;

    @Column(name = "day_of_week")
    private String dayOfWeek; // "월요일", "화요일" 등

    @Column(name = "routine_name", nullable = false, length = 200)
    private String routineName;

    @Enumerated(EnumType.STRING)
    @Column(name = "focus_area")
    private Exercise.MuscleGroup focusArea; // 집중 부위 (무산소)

    @Enumerated(EnumType.STRING)
    @Column(name = "exercise_category", nullable = false)
    private Exercise.ExerciseCategory exerciseCategory;

    @Column(name = "estimated_duration_minutes")
    private Integer estimatedDurationMinutes; // 예상 소요 시간 (분)

    @OneToMany(mappedBy = "recommendedExerciseRoutine", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<RecommendedExerciseItem> items = new ArrayList<>();

    @Column(name = "notes", length = 500)
    private String notes;

    // 편의 메서드
    public void addItem(RecommendedExerciseItem item) {
        items.add(item);
        item.setRecommendedExerciseRoutine(this);
    }

    public void removeItem(RecommendedExerciseItem item) {
        items.remove(item);
        item.setRecommendedExerciseRoutine(null);
    }
}