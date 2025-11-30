package rto.intelfit.domain;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDate;

@Entity
@Table(
        name = "daily_progress",
        uniqueConstraints = {
                @UniqueConstraint(columnNames = {"user_id", "date"})
        }
)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DailyProgress {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false)
    private LocalDate date;

    @Builder.Default
    @Column(name = "exercise_rate", nullable = false)
    private double exerciseRate = 0.0;

    @Builder.Default
    @Column(name = "total_calorie", nullable = false)
    private double totalCalorie = 0.0;

    @Builder.Default
    @Column(name = "total_exercise_seconds", nullable = false)
    private long totalExerciseSeconds = 0L;
}
