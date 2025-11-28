package rto.intelfit.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.*;
import rto.intelfit.domain.Meal;
import rto.intelfit.domain.MealFood;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

public class MealDto {

    // =============================================================
    // ✔ 1) 식사 생성 요청 DTO
    // =============================================================
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "식사 추가 요청")
    public static class MealCreateRequest {

        @Schema(description = "식사 날짜", example = "2025-01-15")
        @NotNull(message = "식사 날짜를 입력해주세요")
        @JsonFormat(pattern = "yyyy-MM-dd")
        private LocalDate mealDate;

        @Schema(description = "식사 타입", example = "BREAKFAST")
        @NotNull(message = "식사 타입을 입력해주세요")
        private Meal.MealType mealType;

        @Schema(description = "음식 리스트")
        @NotEmpty(message = "최소 1개 이상의 음식을 추가해주세요")
        @Valid
        private List<FoodItemRequest> foods;

        @Schema(description = "메모", example = "맛있게 먹었어요")
        @Size(max = 500)
        private String memo;
    }


    // =============================================================
    // ✔ 2) 음식 생성 요청 DTO
    // =============================================================
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "음식 항목 요청")
    public static class FoodItemRequest {

        @Schema(description = "음식명", example = "닭가슴살")
        @NotBlank
        private String foodName;

        @Schema(description = "기준 제공량(g)", example = "100")
        @NotNull
        @DecimalMin("0.1")
        private BigDecimal servingSize;

        @Schema(description = "칼로리(kcal)", example = "165")
        @NotNull
        @DecimalMin("0")
        private BigDecimal calories;

        @Schema(description = "탄수화물(g)", example = "0")
        @NotNull
        private BigDecimal carbs;

        @Schema(description = "단백질(g)", example = "31")
        @NotNull
        private BigDecimal protein;

        @Schema(description = "지방(g)", example = "3.6")
        @NotNull
        private BigDecimal fat;

        @Schema(description = "나트륨(mg)")
        private BigDecimal sodium;

        @Schema(description = "콜레스테롤(mg)")
        private BigDecimal cholesterol;

        @Schema(description = "당(g)")
        private BigDecimal sugar;

        @Schema(description = "식이섬유(g)")
        private BigDecimal fiber;

        @Schema(description = "이미지 URL")
        @Size(max = 500)
        private String imageUrl;

        @Schema(description = "AI 신뢰도 점수 (0~100)")
        @DecimalMin("0")
        @DecimalMax("100")
        private BigDecimal aiConfidenceScore;
    }


    // =============================================================
    // ✔ 3) 식사 생성 응답 DTO (최종 단일 버전)
    // =============================================================
    @Getter
    @Setter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "식사 생성 응답")
    public static class MealCreateResponse {

        private Long mealId;
        private Meal.MealType mealType;
        private LocalDate mealDate;
        private BigDecimal totalCalories;
        private BigDecimal totalCarbs;
        private BigDecimal totalProtein;
        private BigDecimal totalFat;

        public static MealCreateResponse from(Meal meal) {
            return MealCreateResponse.builder()
                    .mealId(meal.getId())
                    .mealType(meal.getMealType())
                    .mealDate(meal.getMealDate())
                    .totalCalories(meal.getTotalCalories())
                    .totalCarbs(meal.getTotalCarbs())
                    .totalProtein(meal.getTotalProtein())
                    .totalFat(meal.getTotalFat())
                    .build();
        }
    }


    // =============================================================
    // ✔ 4) 식사 상세 조회 응답
    // =============================================================
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "식사 상세 정보")
    public static class MealDetailResponse {

        private Long id;

        @JsonFormat(pattern = "yyyy-MM-dd")
        private LocalDate mealDate;

        private Meal.MealType mealType;

        private String mealTypeName;

        private BigDecimal totalCalories;
        private BigDecimal totalCarbs;
        private BigDecimal totalProtein;
        private BigDecimal totalFat;

        private List<FoodItemResponse> foods;

        private String memo;

        @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
        private LocalDateTime createdAt;

        public static MealDetailResponse from(Meal meal) {
            return MealDetailResponse.builder()
                    .id(meal.getId())
                    .mealDate(meal.getMealDate())
                    .mealType(meal.getMealType())
                    .mealTypeName(meal.getMealType().getDescription())
                    .totalCalories(meal.getTotalCalories())
                    .totalCarbs(meal.getTotalCarbs())
                    .totalProtein(meal.getTotalProtein())
                    .totalFat(meal.getTotalFat())
                    .foods(meal.getMealFoods().stream()
                            .map(FoodItemResponse::from)
                            .collect(Collectors.toList()))
                    .memo(meal.getMemo())
                    .createdAt(meal.getCreatedAt())
                    .build();
        }
    }


    // =============================================================
    // ✔ 5) 음식 상세 응답
    // =============================================================
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "음식 항목 상세 정보")
    public static class FoodItemResponse {

        private Long id;
        private String foodName;
        private BigDecimal servingSize;
        private BigDecimal calories;
        private BigDecimal carbs;
        private BigDecimal protein;
        private BigDecimal fat;
        private BigDecimal sodium;
        private BigDecimal cholesterol;
        private BigDecimal sugar;
        private BigDecimal fiber;
        private String imageUrl;
        private BigDecimal aiConfidenceScore;

        public static FoodItemResponse from(MealFood mf) {
            return FoodItemResponse.builder()
                    .id(mf.getId())
                    .foodName(mf.getFoodName())
                    .servingSize(mf.getServingSize())
                    .calories(mf.getCalories())
                    .carbs(mf.getCarbs())
                    .protein(mf.getProtein())
                    .fat(mf.getFat())
                    .sodium(mf.getSodium())
                    .cholesterol(mf.getCholesterol())
                    .sugar(mf.getSugar())
                    .fiber(mf.getFiber())
                    .imageUrl(mf.getImageUrl())
                    .aiConfidenceScore(mf.getAiConfidenceScore())
                    .build();
        }
    }


    // =============================================================
    // ✔ 6) 일별 식단 조회 응답
    // =============================================================
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DailyMealsResponse {
        private LocalDate date;
        private List<MealDetailResponse> meals;
        private BigDecimal dailyTotalCalories;
        private BigDecimal dailyTotalCarbs;
        private BigDecimal dailyTotalProtein;
        private BigDecimal dailyTotalFat;
    }


    // =============================================================
    // ✔ 7) 주간 통계 응답
    // =============================================================
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class WeeklyMealStatsResponse {

        private LocalDate startDate;
        private LocalDate endDate;

        private BigDecimal weeklyTotalCalories;
        private BigDecimal weeklyTotalCarbs;
        private BigDecimal weeklyTotalProtein;
        private BigDecimal weeklyTotalFat;

        private BigDecimal dailyAvgCalories;

        private BigDecimal calorieAchievementRate;
        private BigDecimal carbsAchievementRate;
        private BigDecimal proteinAchievementRate;
        private BigDecimal fatAchievementRate;

        private List<DailyStatItem> dailyStats;
    }


    // =============================================================
    // ✔ 8) 일별 통계 아이템
    // =============================================================
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DailyStatItem {

        private LocalDate date;
        private BigDecimal dailyCalories;
        private BigDecimal achievementRate;
    }


    // =============================================================
    // ✔ 9) 지난주 대비 이번주 비교 응답
    // =============================================================
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class WeekComparisonResponse {
        private LocalDate thisWeekStart;
        private LocalDate thisWeekEnd;
        private BigDecimal thisWeekCalories;
        private BigDecimal lastWeekCalories;
        private BigDecimal caloriesDifference;
        private BigDecimal caloriesChangeRate;
        private BigDecimal carbsChangeRate;
        private BigDecimal proteinChangeRate;
        private BigDecimal fatChangeRate;
        private String analysisMessage;
    }
}
