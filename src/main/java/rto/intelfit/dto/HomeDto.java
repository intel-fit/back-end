package rto.intelfit.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public class HomeDto {

    // ========== 홈 화면 메인 응답 ==========
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "홈 화면 메인 정보")
    public static class HomeMainResponse {

        @Schema(description = "사용자 정보")
        private UserSummary userSummary;

        @Schema(description = "오늘의 운동 기록")
        private TodayExerciseSummary todayExercise;

        @Schema(description = "오늘의 식단 기록")
        private TodayMealSummary todayMeal;

        @Schema(description = "최근 인바디 정보")
        private LatestInBodySummary latestInBody;

        @Schema(description = "주간 요약")
        private WeeklySummary weeklySummary;

        @Schema(description = "AI 챗봇 접근 가능 여부", example = "true")
        @Builder.Default
        private Boolean aiChatbotAvailable = true;
    }

    // ========== 사용자 요약 ==========
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "사용자 요약 정보")
    public static class UserSummary {

        @Schema(description = "사용자 이름", example = "홍길동")
        private String name;

        @Schema(description = "숙련도", example = "BEGINNER")
        private String experienceLevel;

        @Schema(description = "숙련도 설명", example = "초보자")
        private String experienceLevelName;

        @Schema(description = "건강 목표", example = "DIET")
        private String healthGoal;

        @Schema(description = "건강 목표 설명", example = "다이어트")
        private String healthGoalName;

        @Schema(description = "현재 체중 (kg)", example = "70")
        private Integer currentWeight;

        @Schema(description = "목표 체중 (kg)", example = "65")
        private Integer targetWeight;

        @Schema(description = "주간 운동 일수", example = "3-4일")
        private String workoutDaysPerWeek;
    }

    // ========== 오늘의 운동 요약 ==========
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "오늘의 운동 요약")
    public static class TodayExerciseSummary {

        @Schema(description = "오늘 날짜", example = "2025-01-15")
        @JsonFormat(pattern = "yyyy-MM-dd")
        private LocalDate date;

        @Schema(description = "운동 완료 여부", example = "true")
        private Boolean completed;

        @Schema(description = "총 운동 시간 (분)", example = "60")
        private Integer totalDurationMinutes;

        @Schema(description = "총 칼로리 소모", example = "450")
        private BigDecimal totalCaloriesBurned;

        @Schema(description = "운동 횟수", example = "2")
        private Integer exerciseCount;

        @Schema(description = "주요 운동 카테고리")
        private List<String> mainCategories;

        @Schema(description = "오늘의 메시지", example = "훌륭합니다! 오늘 운동을 완료했어요! 💪")
        private String message;
    }

    // ========== 오늘의 식단 요약 ==========
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "오늘의 식단 요약")
    public static class TodayMealSummary {

        @Schema(description = "오늘 날짜", example = "2025-01-15")
        @JsonFormat(pattern = "yyyy-MM-dd")
        private LocalDate date;

        @Schema(description = "총 칼로리", example = "1800")
        private BigDecimal totalCalories;

        @Schema(description = "목표 칼로리", example = "2000")
        private BigDecimal targetCalories;

        @Schema(description = "칼로리 달성률 (%)", example = "90")
        private BigDecimal calorieAchievementRate;

        @Schema(description = "탄수화물 (g)", example = "200")
        private BigDecimal totalCarbs;

        @Schema(description = "단백질 (g)", example = "120")
        private BigDecimal totalProtein;

        @Schema(description = "지방 (g)", example = "60")
        private BigDecimal totalFat;

        @Schema(description = "식사 횟수", example = "3")
        private Integer mealCount;

        @Schema(description = "오늘의 메시지", example = "목표 칼로리의 90%를 달성했어요! 👍")
        private String message;
    }

    // ========== 최근 인바디 요약 ==========
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "최근 인바디 요약")
    public static class LatestInBodySummary {

        @Schema(description = "측정 날짜", example = "2025-01-10")
        @JsonFormat(pattern = "yyyy-MM-dd")
        private LocalDate measurementDate;

        @Schema(description = "체중 (kg)", example = "70")
        private BigDecimal weight;

        @Schema(description = "골격근량 (kg)", example = "32.5")
        private BigDecimal skeletalMuscleMass;

        @Schema(description = "체지방률 (%)", example = "18.5")
        private BigDecimal bodyFatPercentage;

        @Schema(description = "BMI", example = "23.5")
        private BigDecimal bmi;

        @Schema(description = "이전 측정 대비 체중 변화 (kg)", example = "-1.5")
        private BigDecimal weightChange;

        @Schema(description = "달성 뱃지", example = "GOLD")
        private String achievementBadge;

        @Schema(description = "메시지", example = "지난 측정 대비 1.5kg 감량에 성공했어요! 🎉")
        private String message;
    }

    // ========== 주간 요약 ==========
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "주간 요약")
    public static class WeeklySummary {

        @Schema(description = "주간 시작일", example = "2025-01-13")
        @JsonFormat(pattern = "yyyy-MM-dd")
        private LocalDate weekStartDate;

        @Schema(description = "주간 종료일", example = "2025-01-19")
        @JsonFormat(pattern = "yyyy-MM-dd")
        private LocalDate weekEndDate;

        @Schema(description = "주간 운동 일수", example = "4")
        private Integer weeklyExerciseDays;

        @Schema(description = "주간 총 운동 시간 (분)", example = "240")
        private Integer weeklyTotalDurationMinutes;

        @Schema(description = "주간 평균 칼로리 섭취", example = "1850")
        private BigDecimal weeklyAvgCalories;

        @Schema(description = "지난주 대비 운동 시간 변화 (%)", example = "15.5")
        private BigDecimal exerciseChangeRate;

        @Schema(description = "지난주 대비 칼로리 변화 (%)", example = "-5.2")
        private BigDecimal calorieChangeRate;

        @Schema(description = "이번주 목표 달성률 (%)", example = "85")
        private BigDecimal weeklyGoalAchievementRate;

        @Schema(description = "메시지", example = "이번주 목표의 85%를 달성했어요! 조금만 더 화이팅! 💪")
        private String message;
    }

    // ========== 빠른 접근 링크 ==========
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "빠른 접근 링크")
    public static class QuickAccessLinks {

        @Schema(description = "운동 기록 추가 가능 여부", example = "true")
        private Boolean canAddExercise;

        @Schema(description = "식단 기록 추가 가능 여부", example = "true")
        private Boolean canAddMeal;

        @Schema(description = "AI 챗봇 접근 가능 여부", example = "true")
        private Boolean canAccessAIChatbot;

        @Schema(description = "인바디 기록 추가 가능 여부", example = "true")
        private Boolean canAddInBody;

        @Schema(description = "AI 운동 추천 요청 가능 여부", example = "true")
        private Boolean canRequestExerciseRecommendation;

        @Schema(description = "AI 식단 추천 요청 가능 여부", example = "true")
        private Boolean canRequestMealRecommendation;
    }
}