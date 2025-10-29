package rto.intelfit.domain;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "workout_plan_details")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WorkoutPlanDetail {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 요일 (예: MONDAY, TUESDAY ...)
    @Column(name = "day_of_week", nullable = false)
    private String dayOfWeek;

    // 계획된 세트 수
    @Column(name = "sets", nullable = false)
    private Integer sets;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;
}
