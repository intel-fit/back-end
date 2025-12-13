package rto.intelfit.domain;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "user_recommended_exercises")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserRecommendedExercise {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * AI 서버에서 보내주는 exerciseId
     * ex) "xUwnBMT", "seed_calf_raise"
     */
    @Column(name = "exercise_id", nullable = false, length = 50)
    private String exerciseId;

    /**
     * 운동 이름
     */
    @Column(name = "name", nullable = false, length = 100)
    private String name;

    /**
     * 타겟 부위 (ex: "가슴근육", "등 위쪽", "비복근|가자미근")
     */
    @Column(name = "target", length = 100)
    private String target;

    /**
     * AI 추천 운동은 사용자에 종속 → User FK 필요
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_fk", nullable = false)
    private User user;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
}
