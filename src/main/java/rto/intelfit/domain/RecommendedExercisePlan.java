package rto.intelfit.domain;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "recommended_exercise_plans")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RecommendedExercisePlan {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "plan_name", nullable = false, length = 200)
    private String planName;

    @Column(name = "description", length = 1000)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "target_level", nullable = false)
    private User.ExperienceLevel targetLevel;

    @Enumerated(EnumType.STRING)
    @Column(name = "fitness_goal", nullable = false)
    private User.HealthGoal fitnessGoal;

    @Column(name = "weekly_frequency")
    private Integer weeklyFrequency; // 주당 운동 횟수

    @Column(name = "target_weekly_minutes")
    private Integer targetWeeklyMinutes; // 주당 목표 운동 시간 (분)

    @Column(name = "estimated_duration_minutes")
    private Integer estimatedDurationMinutes; // 예상 운동 시간

    @Column(name = "recommendation_reason", length = 1000)
    private String recommendationReason;

    @Column(name = "is_saved")
    @Builder.Default
    public Boolean isSaved = false;

    @OneToMany(mappedBy = "recommendedExercisePlan", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<RecommendedExerciseRoutine> routines = new ArrayList<>();

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    // 편의 메서드
    public void addRoutine(RecommendedExerciseRoutine routine) {
        routines.add(routine);
        routine.setRecommendedExercisePlan(this);
    }

    public void removeRoutine(RecommendedExerciseRoutine routine) {
        routines.remove(routine);
        routine.setRecommendedExercisePlan(null);
    }
}