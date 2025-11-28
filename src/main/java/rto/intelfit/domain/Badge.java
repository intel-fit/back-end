package rto.intelfit.domain;

import jakarta.persistence.*;
import lombok.*;

/**
 * 뱃지 엔티티
 * 
 * 달성 가능한 뱃지 종류를 정의합니다.
 * 피그마 디자인 기준: 3가지 뱃지 (운동, 식단, 체중)
 */
@Entity
@Table(name = "badges")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Badge {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(name = "badge_type", nullable = false, unique = true)
    private BadgeType badgeType;

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "description", length = 500)
    private String description;

    @Column(name = "icon_url")
    private String iconUrl;

    @Column(name = "required_count")
    private Integer requiredCount; // 획득에 필요한 횟수

    @Column(name = "display_order")
    private Integer displayOrder; // 표시 순서

    /**
     * 뱃지 타입
     * 피그마 디자인 기준: 식단, 운동, 체중 3가지
     */
    @Getter
    @RequiredArgsConstructor
    public enum BadgeType {
        // 운동 관련 뱃지
        EXERCISE_100("운동 100회 달성", "🏋️", "workout_100"),
        EXERCISE_50("운동 50회 달성", "🏋️", "workout_50"),
        EXERCISE_30("운동 30회 달성", "🏋️", "workout_30"),
        EXERCISE_10("운동 10회 달성", "🏋️", "workout_10"),
        EXERCISE_STREAK_7("운동 7일 연속", "🔥", "streak_7"),
        EXERCISE_STREAK_30("운동 30일 연속", "🔥", "streak_30"),

        // 식단 관련 뱃지
        MEAL_100("식단 100회 기록", "🍽️", "meal_100"),
        MEAL_50("식단 50회 기록", "🍽️", "meal_50"),
        MEAL_30("식단 30회 기록", "🍽️", "meal_30"),
        MEAL_10("식단 10회 기록", "🍽️", "meal_10"),
        CALORIE_GOAL_30("칼로리 목표 30일 달성", "🎯", "calorie_30"),
        HEALTHY_EATING_7("건강한 식단 7일", "🥗", "healthy_7"),

        // 체중 관련 뱃지
        WEIGHT_GOAL_ACHIEVED("목표 체중 달성", "⚖️", "weight_goal"),
        WEIGHT_LOSS_5KG("5kg 감량 성공", "📉", "loss_5kg"),
        WEIGHT_LOSS_10KG("10kg 감량 성공", "📉", "loss_10kg"),
        MUSCLE_GAIN_5KG("5kg 증량 성공", "📈", "gain_5kg"),
        BODY_FAT_REDUCED("체지방률 5% 감소", "💪", "fat_reduced"),

        // 종합 관련 뱃지
        BEGINNER("입문자", "🌱", "beginner"),
        INTERMEDIATE("중급자", "🌿", "intermediate"),
        ADVANCED("고급자", "🌳", "advanced"),
        MASTER("마스터", "👑", "master");

        private final String description;
        private final String emoji;
        private final String iconKey;
    }
}
