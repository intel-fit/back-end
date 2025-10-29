package rto.intelfit.domain;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;

@Entity
@Table(name = "workout_records")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WorkoutRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 수행된 세트 수
    @Column(name = "sets_completed", nullable = false)
    private Integer setsCompleted;

    // 운동한 날짜
    @Column(name = "workout_date", nullable = false)
    private LocalDate workoutDate;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;
}
