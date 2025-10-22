package rto.intelfit.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import rto.intelfit.domain.Meal;
import rto.intelfit.domain.MealFood;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

public class MealDto {

    // ========== 식사 추가 요청 ==========
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
        @NotNull(message = "식사 타입을 선택해주세요")
        private Meal.MealType mealType;

        @Schema(description = "음식 목록")
        @NotEmpty(message = "최소 1개 이상의 음식을 추가해주세요")
        @Valid
        private List<FoodItemRequest> foods;

        @Schema(description = "메모", example = "맛있게 먹었어요")
        @Size(max = 500, message = "메모는 500자 이내로 입력해주세요")
        private String memo;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "음식 항목 요청 (AI 분석 결과 또는 수동 입력)")
    public static class FoodItemRequest {

        @Schema(description = "음식 이름", example = "닭가슴살")
        @NotBlank(message = "음식 이름을 입력해주세요")
        @Size(max = 200, message = "음식 이름은 200자 이내로 입력해주세요")
        private String foodName;

        @Schema(description = "1인분 기준량 (g)", example = "100")
        @NotNull(message = "1인분 기준량을 입력해주세요")
        @DecimalMin(value = "0.1", message = "1인분 기준량은 0.1g 이상이어야 합니다")
        private BigDecimal servingSize;

        @Schema(description = "칼로리 (kcal)", example = "165")
        @NotNull(message = "칼로리를 입력해주세요")
        @DecimalMin(value = "0", message = "칼로리는 0 이상이어야 합니다")
        private BigDecimal calories;

        @Schema(description = "탄수화물 (g)", example = "0")
        @NotNull(message = "탄수화물을 입력해주세요")
        @DecimalMin(value = "0", message = "탄수화물은 0 이상이어야 합니다")
        private BigDecimal carbs;

        @Schema(description = "단백질 (g)", example = "31")
        @NotNull(message = "단백질을 입력해주세요")
        @DecimalMin(value = "0", message = "단백질은 0 이상이어야 합니다")
        private BigDecimal protein;

        @Schema(description = "지방 (g)", example = "3.6")
        @NotNull(message = "지방을 입력해주세요")
        @DecimalMin(value = "0", message = "지방은 0 이상이어야 합니다")
        private BigDecimal fat;

        @Schema(description = "나트륨 (mg)", example = "74")
        @DecimalMin(value = "0", message = "나트륨은 0 이상이어야 합니다")
        private BigDecimal sodium;

        @Schema(description = "콜레스테롤 (mg)", example = "85")
        @DecimalMin(value = "0", message = "콜레스테롤은 0 이상이어야 합니다")
        private BigDecimal cholesterol;

        @Schema(description = "당 (g)", example = "0")
        @DecimalMin(value = "0", message = "당은 0 이상이어야 합니다")
        private BigDecimal sugar;

        @Schema(description = "식이섬유 (g)", example = "0")
        @DecimalMin(value = "0", message = "식이섬유는 0 이상이어야 합니다")
        private BigDecimal fiber;

        @Schema(description = "이미지 URL", example = "https://example.com/food.jpg")
        @Size(max = 500, message = "이미지 URL은 500자 이내로 입력해주세요")
        private String imageUrl;

        @Schema(description = "AI 신뢰도 점수 (0-100)", example = "95.5")
        @DecimalMin(value = "0", message = "AI 신뢰도 점수는 0 이상이어야 합니다")
        @DecimalMax(value = "100", message = "AI 신뢰도 점수는 100 이하여야 합니다")
        private BigDecimal aiConfidenceScore;
    }

    // ========== 식사 추가 응답 ==========
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "식사 추가 응답")
    public static class MealCreateResponse {

        @Schema(description = "성공 여부", example = "true")
        private boolean success;

        @Schema(description = "메시지", example = "식사가 추가되었습니다")
        private String message;

        @Schema(description = "생성된 식사 정보")
        private MealDetailResponse meal;
    }

    // ========== 식사 상세 응답 ==========
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "식사 상세 정보")
    public static class MealDetailResponse {

        @Schema(description = "식사 ID", example = "1")
        private Long id;

        @Schema(description = "식사 날짜", example = "2025-01-15")
        @JsonFormat(pattern = "yyyy-MM-dd")
        private LocalDate mealDate;

        @Schema(description = "식사 타입", example = "BREAKFAST")
        private Meal.MealType mealType;

        @Schema(description = "식사 타입 설명", example = "아침")
        private String mealTypeName;

        @Schema(description = "총 칼로리 (kcal)", example = "520")
        private BigDecimal totalCalories;

        @Schema(description = "총 탄수화물 (g)", example = "50")
        private BigDecimal totalCarbs;

        @Schema(description = "총 단백질 (g)", example = "40")
        private BigDecimal totalProtein;

        @Schema(description = "총 지방 (g)", example = "15")
        private BigDecimal totalFat;

        @Schema(description = "음식 목록")
        private List<FoodItemResponse> foods;

        @Schema(description = "메모", example = "맛있게 먹었어요")
        private String memo;

        @Schema(description = "생성 시간", example = "2025-01-15T08:30:00")
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

    // ========== 음식 항목 응답 ==========
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "음식 항목 상세 정보")
    public static class FoodItemResponse {

        @Schema(description = "음식 ID", example = "1")
        private Long id;

        @Schema(description = "음식 이름", example = "닭가슴살")
        private String foodName;

        @Schema(description = "1인분 기준량 (g)", example = "100")
        private BigDecimal servingSize;

        @Schema(description = "칼로리 (kcal)", example = "165")
        private BigDecimal calories;

        @Schema(description = "탄수화물 (g)", example = "0")
        private BigDecimal carbs;

        @Schema(description = "단백질 (g)", example = "31")
        private BigDecimal protein;

        @Schema(description = "지방 (g)", example = "3.6")
        private BigDecimal fat;

        @Schema(description = "나트륨 (mg)", example = "74")
        private BigDecimal sodium;

        @Schema(description = "콜레스테롤 (mg)", example = "85")
        private BigDecimal cholesterol;

        @Schema(description = "당 (g)", example = "0")
        private BigDecimal sugar;

        @Schema(description = "식이섬유 (g)", example = "0")
        private BigDecimal fiber;

        @Schema(description = "이미지 URL", example = "https://example.com/food.jpg")
        private String imageUrl;

        @Schema(description = "AI 신뢰도 점수", example = "95.5")
        private BigDecimal aiConfidenceScore;

        public static FoodItemResponse from(MealFood mealFood) {
            return FoodItemResponse.builder()
                    .id(mealFood.getId())
                    .foodName(mealFood.getFoodName())
                    .servingSize(mealFood.getServingSize())
                    .calories(mealFood.getCalories())
                    .carbs(mealFood.getCarbs())
                    .protein(mealFood.getProtein())
                    .fat(mealFood.getFat())
                    .sodium(mealFood.getSodium())
                    .cholesterol(mealFood.getCholesterol())
                    .sugar(mealFood.getSugar())
                    .fiber(mealFood.getFiber())
                    .imageUrl(mealFood.getImageUrl())
                    .aiConfidenceScore(mealFood.getAiConfidenceScore())
                    .build();
        }
    }

    // ========== 일별 식단 조회 응답 ==========
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "일별 식단 조회 응답")
    public static class DailyMealsResponse {

        @Schema(description = "조회 날짜", example = "2025-01-15")
        @JsonFormat(pattern = "yyyy-MM-dd")
        private LocalDate date;

        @Schema(description = "해당 날짜의 모든 식사 기록")
        private List<MealDetailResponse> meals;

        @Schema(description = "일일 총 칼로리", example = "1800")
        private BigDecimal dailyTotalCalories;

        @Schema(description = "일일 총 탄수화물", example = "200")
        private BigDecimal dailyTotalCarbs;

        @Schema(description = "일일 총 단백질", example = "120")
        private BigDecimal dailyTotalProtein;

        @Schema(description = "일일 총 지방", example = "60")
        private BigDecimal dailyTotalFat;
    }

    // ========== 주간 식단 통계 응답 ==========
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "주간 식단 통계 응답")
    public static class WeeklyMealStatsResponse {

        @Schema(description = "조회 시작일", example = "2025-01-13")
        @JsonFormat(pattern = "yyyy-MM-dd")
        private LocalDate startDate;

        @Schema(description = "조회 종료일", example = "2025-01-19")
        @JsonFormat(pattern = "yyyy-MM-dd")
        private LocalDate endDate;

        @Schema(description = "주간 총 칼로리", example = "12600")
        private BigDecimal weeklyTotalCalories;

        @Schema(description = "주간 총 탄수화물", example = "1400")
        private BigDecimal weeklyTotalCarbs;

        @Schema(description = "주간 총 단백질", example = "840")
        private BigDecimal weeklyTotalProtein;

        @Schema(description = "주간 총 지방", example = "420")
        private BigDecimal weeklyTotalFat;

        @Schema(description = "일일 평균 칼로리", example = "1800")
        private BigDecimal dailyAvgCalories;

        @Schema(description = "목표 칼로리 대비 달성률 (%)", example = "90")
        private BigDecimal calorieAchievementRate;

        @Schema(description = "목표 탄수화물 대비 달성률 (%)", example = "93")
        private BigDecimal carbsAchievementRate;

        @Schema(description = "목표 단백질 대비 달성률 (%)", example = "87")
        private BigDecimal proteinAchievementRate;

        @Schema(description = "목표 지방 대비 달성률 (%)", example = "91")
        private BigDecimal fatAchievementRate;

        @Schema(description = "일별 상세 통계")
        private List<DailyStatItem> dailyStats;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "일별 통계 항목")
    public static class DailyStatItem {

        @Schema(description = "날짜", example = "2025-01-15")
        @JsonFormat(pattern = "yyyy-MM-dd")
        private LocalDate date;

        @Schema(description = "일일 총 칼로리", example = "1800")
        private BigDecimal dailyCalories;

        @Schema(description = "목표 칼로리 대비 달성률 (%)", example = "90")
        private BigDecimal achievementRate;
    }

    // ========== 지난주 대비 이번주 비교 응답 ==========
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "지난주 대비 이번주 비교 응답")
    public static class WeekComparisonResponse {

        @Schema(description = "이번주 시작일", example = "2025-01-13")
        @JsonFormat(pattern = "yyyy-MM-dd")
        private LocalDate thisWeekStart;

        @Schema(description = "이번주 종료일", example = "2025-01-19")
        @JsonFormat(pattern = "yyyy-MM-dd")
        private LocalDate thisWeekEnd;

        @Schema(description = "이번주 총 칼로리", example = "12600")
        private BigDecimal thisWeekCalories;

        @Schema(description = "지난주 총 칼로리", example = "13200")
        private BigDecimal lastWeekCalories;

        @Schema(description = "칼로리 변화량 (이번주 - 지난주)", example = "-600")
        private BigDecimal caloriesDifference;

        @Schema(description = "칼로리 변화율 (%)", example = "-4.5")
        private BigDecimal caloriesChangeRate;

        @Schema(description = "탄수화물 변화율 (%)", example = "-3.2")
        private BigDecimal carbsChangeRate;

        @Schema(description = "단백질 변화율 (%)", example = "5.1")
        private BigDecimal proteinChangeRate;

        @Schema(description = "지방 변화율 (%)", example = "-2.8")
        private BigDecimal fatChangeRate;

        @Schema(description = "변화 분석 메시지", example = "지난주 대비 칼로리를 4.5% 줄였습니다")
        private String analysisMessage;
    }
}