package rto.intelfit.domain;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/**
 * 사용자 뱃지 획득 내역
 * 
 * 사용자가 획득한 뱃지 정보를 관리합니다.
 */
@Entity
@Table(name = "user_badges", 
       uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "badge_id"}))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserBadge {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "badge_id", nullable = false)
    private Badge badge;

    @CreationTimestamp
    @Column(name = "earned_at", nullable = false, updatable = false)
    private LocalDateTime earnedAt;

    @Column(name = "is_displayed")
    @Builder.Default
    private Boolean isDisplayed = true; // 마이페이지에 표시할지 여부

    @Column(name = "display_order")
    private Integer displayOrder; // 표시 순서 (사용자 지정)
}
