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

    @Enumerated(EnumType.STRING)
    @Column(name = "membership_type", nullable = false)
    @Builder.Default
    private MembershipType membershipType = MembershipType.FREE;

    @Enumerated(EnumType.STRING)
    @Column(name = "health_goal")
    private HealthGoal healthGoal;

    @Enumerated(EnumType.STRING)
    @Column(name = "experience_level")
    private ExperienceLevel experienceLevel;

    @Column(name = "workout_days_per_week", length = 20)
    private String workoutDaysPerWeek;

    @Column(name = "weight_goal")
    private Integer weightGoal;

    @Column(name = "last_login_at")
    private LocalDateTime lastLoginAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "gender")
    private Gender gender;

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

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;



    // InBody와의 1:N 관계
    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<InBody> inBodyRecords = new ArrayList<>();


    // 소셜 로그인 구분
    @Enumerated(EnumType.STRING)
    @Column(name = "social_provider", nullable = false)
    @Builder.Default
    private SocialProvider socialProvider = SocialProvider.LOCAL;

    // 소셜 고유 ID(카카오 userId)
    @Column(name = "social_id", unique = true)
    private String socialId;

    // 프로필 이미지(카카오)
    @Column(name = "profile_image")
    private String profileImage;

    // ===== 토큰 관련 =====

    // 식단 추천 토큰 (기본 1, 7일 후 초기화)
    @Builder.Default
    @Column(name = "meal_recommend_tokens", nullable = false)
    private Integer mealRecommendTokens = 1;

    @Column(name = "meal_token_last_reset")
    private LocalDate mealTokenLastReset;


    // 운동 추천 토큰 (기본 1, 7일 후 초기화)
    @Builder.Default
    @Column(name = "workout_recommend_tokens", nullable = false)
    private Integer workoutRecommendTokens = 1;

    @Column(name = "workout_recommend_last_reset")
    private LocalDateTime workoutRecommendLastReset;


    // 챗봇 토큰 (기본 3, 1일 후 초기화)
    @Builder.Default
    @Column(name = "chatbot_tokens", nullable = false)
    private Integer chatbotTokens = 3;

    @Column(name = "chatbot_last_reset")
    private LocalDateTime chatbotLastReset;




    public enum SocialProvider {
        LOCAL, KAKAO
    }

    // 열거형 정의
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
        BEGINNER,      // 초보자
        INTERMEDIATE,  // 중급자
        ADVANCED       // 숙련자
    }

    public enum Gender {
        M, F
    }

    // 마지막 로그인 시간 업데이트 메서드
    public void updateLastLoginAt() {
        this.lastLoginAt = LocalDateTime.now();
    }

    // InBody 관계 편의 메서드
    public void addInBodyRecord(InBody inBody) {
        inBodyRecords.add(inBody);
        inBody.setUser(this);
    }

    public void removeInBodyRecord(InBody inBody) {
        inBodyRecords.remove(inBody);
        inBody.setUser(null);
    }
}
