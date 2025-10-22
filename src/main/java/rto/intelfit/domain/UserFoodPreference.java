package rto.intelfit.domain;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "user_food_preferences",
        uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "food_name"}))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserFoodPreference {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)

    private User user;

    @Column(name = "food_name", nullable = false, length = 200)
    private String foodName;

    // 선호도 타입
    @Enumerated(EnumType.STRING)
    @Column(name = "preference_type", nullable = false)
    private PreferenceType preferenceType;

    // 선호도 점수 (1-5)
    @Column(name = "preference_score")
    @Builder.Default
    private Integer preferenceScore = 3;

    // 카테고리 (선택)
    @Column(name = "category", length = 100)
    private String category;

    // 태그 (예: "고단백", "저칼로리", "채식")
    @Column(name = "tags", length = 500)
    private String tags;

    // 메모
    @Column(name = "memo", length = 500)
    private String memo;

    // 해당 음식을 먹은 횟수
    @Column(name = "consumed_count")
    @Builder.Default
    private Integer consumedCount = 0;

    // 마지막 섭취 날짜
    @Column(name = "last_consumed_at")
    private LocalDateTime lastConsumedAt;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    // 선호도 타입 열거형
    public enum PreferenceType {
        LIKE("좋아요"),
        DISLIKE("싫어요"),
        FAVORITE("즐겨찾기"),
        NEUTRAL("보통");

        private final String description;

        PreferenceType(String description) {
            this.description = description;
        }

        public String getDescription() {
            return description;
        }
    }

    // 섭취 횟수 증가
    public void incrementConsumedCount() {
        this.consumedCount++;
        this.lastConsumedAt = LocalDateTime.now();
    }
}