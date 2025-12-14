package rto.intelfit.domain;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDate;
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
     * AI 서버에서 보내주는 운동 ID
     */
    @Column(name = "exercise_id", nullable = false, length = 50)
    private String exerciseId;

    /**
     * 운동 이름
     */
    @Column(name = "name", nullable = false, length = 100)
    private String name;

    /**
     * 타겟 부위
     */
    @Column(name = "target", length = 100)
    private String target;

    /**
     * 날짜별 추천 운동 묶음
     * ex) 2025-12-04 → Upper Day 운동들
     */
    @Column(name = "exercise_date", nullable = false)
    private LocalDate exerciseDate;

    /**
     * 해당 운동이 속한 사용자
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_fk", nullable = false)
    private User user;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
}
