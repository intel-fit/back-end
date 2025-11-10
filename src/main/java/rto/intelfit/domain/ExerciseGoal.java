package rto.intelfit.domain;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import java.time.LocalDateTime;

@Entity
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "exercise_goal")
public class ExerciseGoal {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false)
    private String weeklyFrequency;  // 예: "주 3회"

    @Column(nullable = false)
    private String durationPerSession;  // 예: "30분 이상"

    @Column(nullable = false)
    private String exerciseType;  // 예: "유산소", "무산소", "전체"

    @Column(nullable = false)
    private Long weeklyCalorieGoal;  // kcal 제외, 숫자만

    @Column(nullable = false)
    private Long progress;  // 진행률 (0~100)

    @CreationTimestamp
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;
}
