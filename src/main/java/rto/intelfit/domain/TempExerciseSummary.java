package rto.intelfit.domain;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(
        name = "temp_exercise_summary",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_temp_exercise_user_date",
                        columnNames = {"user_id", "exercise_date"}
                )
        }
)
public class TempExerciseSummary {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;  // 여러 유저 가능

    @Column(name = "exercise_date", nullable = false)
    private LocalDate date;

    @Column(nullable = false)
    private String focus;  // Upper / Lower / Rest 등

    @Column(nullable = false)
    private double durationMin;

    @Column(nullable = false)
    private double kcal;

    @Column(nullable = false)
    private int exerciseCount;

    @Column(nullable = false)
    private String title;

    @CreationTimestamp
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;
}
