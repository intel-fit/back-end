package rto.intelfit.domain;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * ✅ DailyProgressExercisePlan
 * DailyProgress 전용 운동 계획 엔티티
 * (운동 달성률 계산을 위한 "계획된 운동 수" 집계용)
 */
@Entity
@Table(name = "daily_progress_exercise_plans")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DailyProgressExercisePlan {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 계획을 세운 사용자
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    // 계획 날짜
    @Column(name = "plan_date", nullable = false)
    private LocalDate planDate;

    // 계획 이름 (예: 하체 루틴, 전신 루틴 등)
    @Column(name = "plan_name", length = 100, nullable = false)
    private String planName;

    // 메모 (선택)
    @Column(name = "memo", length = 500)
    private String memo;

    // 하위 운동 항목들
    @OneToMany(mappedBy = "exercisePlan", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<DailyProgressExercisePlanDetail> details = new ArrayList<>();

    // 편의 메서드
    public void addDetail(DailyProgressExercisePlanDetail detail) {
        details.add(detail);
        detail.setExercisePlan(this);
    }
}
