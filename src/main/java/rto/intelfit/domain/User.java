package rto.intelfit.domain;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "users")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", unique = true, nullable = false)
    private String userId;

    @Column(name = "name", nullable = false, length = 50)
    private String name;

    @Column(name = "email", nullable = false, length = 50)
    private String email;

    @Column(name = "email_verified", nullable = false)
    @Builder.Default
    private Boolean emailVerified = false;

    @Column(name = "birth_date", nullable = false)
    private LocalDate birthDate;

    @Column(name = "password", nullable = false, length = 255)
    private String password;

    @Column(name = "height")
    private Integer height;

    @Column(name = "weight")
    private Integer weight;

    /* ===================== ENUM 컬럼 ===================== */

    @Enumerated(EnumType.STRING)
    @Column(name = "membership_type", nullable = false)
    @Builder.Default
    private MembershipType membershipType = MembershipType.FREE;

    @Enumerated(EnumType.STRING)
    @Column(name = "health_goal", nullable = false)
    @Builder.Default
    private HealthGoal healthGoal = HealthGoal.MAINTENANCE;

    @Enumerated(EnumType.STRING)
    @Column(name = "experience_level")
    private ExperienceLevel experienceLevel;

    @Enumerated(EnumType.STRING)
    @Column(name = "gender")
    private Gender gender;

    @Enumerated(EnumType.STRING)
    @Column(name = "login_type", nullable = false)
    @Builder.Default
    private SocialProvider loginType = SocialProvider.LOCAL;

    /* ===================== 기본 정보 ===================== */

    @Column(name = "workout_days_per_week", length = 20)
    private String workoutDaysPerWeek;

    @Column(name = "weight_goal")
    private Integer weightGoal;

    @Column(name = "last_login_at")
    private LocalDateTime lastLoginAt;

    @Column(name = "agree_privacy", nullable = false)
    @Builder.Default
    private Boolean agreePrivacy = false;

    @Column(name = "agree_terms", nullable = false)
    @Builder.Default
    private Boolean agreeTerms = false;

    @Column(name = "agreed_at")
    private LocalDateTime agreedAt;

    @Column(name = "fitness_concerns", length = 500)
    private String fitnessConcerns;

    /* ===================== 소셜 로그인 ===================== */

    @Column(name = "social_id")
    private String socialId;

    @Column(name = "kakao_access_token", length = 500)
    private String kakaoAccessToken;

    @Column(name = "profile_image_url", length = 500)
    private String profileImageUrl;

    /* ===================== 토큰 관리 ===================== */

    @Builder.Default
    @Column(name = "meal_recommend_tokens", nullable = false)
    private Integer mealRecommendTokens = 1;

    @Column(name = "meal_token_last_reset")
    private LocalDate mealTokenLastReset;

    @Builder.Default
    @Column(name = "workout_recommend_tokens", nullable = false)
    private Integer workoutRecommendTokens = 1;

    @Column(name = "workout_recommend_last_reset")
    private LocalDateTime workoutRecommendLastReset;

    @Builder.Default
    @Column(name = "chatbot_tokens", nullable = false)
    private Integer chatbotTokens = 3;

    @Column(name = "chatbot_last_reset")
    private LocalDateTime chatbotLastReset;

    /* ===================== 타임스탬프 ===================== */

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    /* ===================== InBody 관계 ===================== */

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<InBody> inBodyRecords = new ArrayList<>();

    public void addInBodyRecord(InBody inBody) {
        inBodyRecords.add(inBody);
        inBody.setUser(this);
    }

    public void removeInBodyRecord(InBody inBody) {
        inBodyRecords.remove(inBody);
        inBody.setUser(null);
    }

    /* ===================== 편의 메서드 ===================== */

    public boolean isSocialUser() {
        return this.loginType != SocialProvider.LOCAL;
    }

    public void updateKakaoAccessToken(String kakaoAccessToken) {
        this.kakaoAccessToken = kakaoAccessToken;
    }

    public void clearKakaoToken() {
        this.kakaoAccessToken = null;
    }

    public void updateLastLoginAt() {
        this.lastLoginAt = LocalDateTime.now();
    }

    /* ===================== ENUM 정의 ===================== */

    public enum SocialProvider {
        LOCAL, KAKAO, GOOGLE, APPLE
    }

    public enum MembershipType {
        FREE, PREMIUM
    }

    public enum HealthGoal {
        DIET,           // 체중 감량
        BULK,           // 벌크업
        LEAN_MASS,      // 린매스
        MUSCLE_GAIN,    // 근육 증가
        MAINTENANCE     // 유지
    }

    public enum ExperienceLevel {
        BEGINNER,
        INTERMEDIATE,
        ADVANCED
    }

    public enum Gender {
        M, F
    }
}
