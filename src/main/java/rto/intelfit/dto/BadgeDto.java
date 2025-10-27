package rto.intelfit.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import rto.intelfit.domain.Badge;
import rto.intelfit.domain.UserBadge;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

public class BadgeDto {

    // ========== 뱃지 정보 응답 ==========
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "뱃지 정보")
    public static class BadgeInfo {

        @Schema(description = "뱃지 ID", example = "1")
        private Long badgeId;

        @Schema(description = "뱃지 타입", example = "EXERCISE_100")
        private String badgeType;

        @Schema(description = "뱃지 이름", example = "운동 100회 달성")
        private String name;

        @Schema(description = "뱃지 설명", example = "100회 운동을 완료한 대단한 사람!")
        private String description;

        @Schema(description = "뱃지 이모지", example = "🏋️")
        private String emoji;

        @Schema(description = "뱃지 아이콘 URL", example = "/badges/workout_100.png")
        private String iconUrl;

        @Schema(description = "달성 필요 횟수", example = "100")
        private Integer requiredCount;

        @Schema(description = "획득 여부", example = "true")
        private Boolean earned;

        @Schema(description = "획득 날짜", example = "2025-01-15T14:30:00")
        @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
        private LocalDateTime earnedAt;

        public static BadgeInfo from(Badge badge, UserBadge userBadge) {
            return BadgeInfo.builder()
                    .badgeId(badge.getId())
                    .badgeType(badge.getBadgeType().name())
                    .name(badge.getName())
                    .description(badge.getDescription())
                    .emoji(badge.getBadgeType().getEmoji())
                    .iconUrl(badge.getIconUrl())
                    .requiredCount(badge.getRequiredCount())
                    .earned(userBadge != null)
                    .earnedAt(userBadge != null ? userBadge.getEarnedAt() : null)
                    .build();
        }

        public static BadgeInfo from(Badge badge, boolean earned) {
            return BadgeInfo.builder()
                    .badgeId(badge.getId())
                    .badgeType(badge.getBadgeType().name())
                    .name(badge.getName())
                    .description(badge.getDescription())
                    .emoji(badge.getBadgeType().getEmoji())
                    .iconUrl(badge.getIconUrl())
                    .requiredCount(badge.getRequiredCount())
                    .earned(earned)
                    .build();
        }
    }

    // ========== 사용자 뱃지 목록 응답 ==========
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "사용자 뱃지 목록 응답")
    public static class UserBadgesResponse {

        @Schema(description = "획득한 뱃지 목록")
        private List<BadgeInfo> earnedBadges;

        @Schema(description = "미획득 뱃지 목록")
        private List<BadgeInfo> notEarnedBadges;

        @Schema(description = "총 뱃지 수", example = "20")
        private Integer totalBadges;

        @Schema(description = "획득한 뱃지 수", example = "5")
        private Integer earnedCount;

        @Schema(description = "달성률 (%)", example = "25.0")
        private Double achievementRate;

        @Schema(description = "운동 뱃지 수", example = "2")
        private Integer exerciseBadgeCount;

        @Schema(description = "식단 뱃지 수", example = "1")
        private Integer mealBadgeCount;

        @Schema(description = "체중 뱃지 수", example = "2")
        private Integer weightBadgeCount;
    }

    // ========== 새로 획득한 뱃지 알림 ==========
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "새로 획득한 뱃지 알림")
    public static class NewBadgeNotification {

        @Schema(description = "성공 여부", example = "true")
        private Boolean success;

        @Schema(description = "메시지", example = "축하합니다! 새로운 뱃지를 획득했습니다! 🎉")
        private String message;

        @Schema(description = "획득한 뱃지 정보")
        private BadgeInfo badge;

        @Schema(description = "획득 날짜", example = "2025-01-15T14:30:00")
        @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
        private LocalDateTime earnedAt;
    }

    // ========== 뱃지 진행 상황 ==========
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "뱃지 진행 상황")
    public static class BadgeProgressInfo {

        @Schema(description = "뱃지 정보")
        private BadgeInfo badge;

        @Schema(description = "현재 진행 횟수", example = "75")
        private Integer currentCount;

        @Schema(description = "목표 횟수", example = "100")
        private Integer requiredCount;

        @Schema(description = "진행률 (%)", example = "75.0")
        private Double progressRate;

        @Schema(description = "남은 횟수", example = "25")
        private Integer remainingCount;

        @Schema(description = "달성 여부", example = "false")
        private Boolean achieved;
    }

    // ========== 뱃지 통계 ==========
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "뱃지 통계")
    public static class BadgeStatistics {

        @Schema(description = "총 뱃지 수", example = "20")
        private Integer totalBadges;

        @Schema(description = "획득한 뱃지 수", example = "8")
        private Integer earnedBadges;

        @Schema(description = "미획득 뱃지 수", example = "12")
        private Integer notEarnedBadges;

        @Schema(description = "달성률 (%)", example = "40.0")
        private Double achievementRate;

        @Schema(description = "카테고리별 통계")
        private CategoryStatistics categoryStats;

        @Schema(description = "최근 획득한 뱃지 (최대 3개)")
        private List<BadgeInfo> recentBadges;
    }

    // ========== 카테고리별 통계 ==========
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "카테고리별 뱃지 통계")
    public static class CategoryStatistics {

        @Schema(description = "운동 뱃지", example = "3")
        private Integer exercise;

        @Schema(description = "식단 뱃지", example = "2")
        private Integer meal;

        @Schema(description = "체중 뱃지", example = "2")
        private Integer weight;

        @Schema(description = "종합 뱃지", example = "1")
        private Integer overall;
    }

    // ========== 마이페이지 뱃지 표시용 ==========
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "마이페이지 표시 뱃지 (상위 3개)")
    public static class DisplayBadgesResponse {

        @Schema(description = "표시할 뱃지 목록 (최대 3개)")
        private List<BadgeInfo> badges;

        @Schema(description = "총 획득 뱃지 수", example = "8")
        private Integer totalEarnedCount;

        @Schema(description = "메시지", example = "후워! 100개 성공! 상태 정말이 반짝✨")
        private String message;
    }
}
