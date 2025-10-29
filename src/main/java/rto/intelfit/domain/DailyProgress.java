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

    // 사용자
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    // 날짜
    @Column(nullable = false)
    private LocalDate date;

    // 운동 달성률 (%)
    @Column(nullable = false)
    private double exerciseRate;

    // 섭취 칼로리 (kcal)
    @Column(nullable = false)
    private double totalCalorie;
}
