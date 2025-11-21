package rto.intelfit.domain;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDate;

@Entity
@Table(name = "daily_progress")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DailyProgress {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 사용자 (FK)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    // 날짜
    @Column(nullable = false)
    private LocalDate date;

    // 운동 달성률 (%)
    @Builder.Default
    @Column(name = "exercise_rate", nullable = false)
    private double exerciseRate = 0.0;

    // 섭취 칼로리 (kcal)
    @Builder.Default
    @Column(name = "total_calorie", nullable = false)
    private double totalCalorie = 0.0;

    //오늘의 총 운동 시간 추가

    // 오늘 총 운동 시간 (초 단위)
    @Builder.Default
    @Column(name = "total_exercise_seconds", nullable = false)
    private long totalExerciseSeconds = 0L;

}
