package rto.intelfit.domain;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import java.time.LocalDateTime;

@Entity
@Table(name = "user_exercises")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Exercise {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** ✅ User와 N:1 관계 (외래키 user_id → users.id) */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    /** ✅ 운동명 (예: 벤치프레스, 스쿼트) */
    @Column(name = "exercise_name", nullable = false, length = 100)
    private String exerciseName;

    /** ✅ 운동 부위 (예: 가슴, 등, 하체 등) */
    @Column(name = "category", length = 50)
    private String category;

    /** ✅ 중량, 횟수, 세트 */
    @Column(name = "weight")
    private Double weight;

    @Column(name = "reps")
    private Integer reps;

    @Column(name = "sets")
    private Integer sets;

    /** ✅ 계산된 칼로리 (운동 수행 시 소모 칼로리 kcal 단위) */
    @Column(name = "calories")
    private Double calories;

    /** ✅ 생성, 수정시간 자동기록 */
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
}
