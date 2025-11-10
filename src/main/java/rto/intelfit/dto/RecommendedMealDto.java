    package rto.intelfit.dto;

    import com.fasterxml.jackson.annotation.JsonFormat;
    import io.swagger.v3.oas.annotations.media.Schema;
    import lombok.AllArgsConstructor;
    import lombok.Builder;
    import lombok.Data;
    import lombok.NoArgsConstructor;
    import rto.intelfit.domain.Meal;
    import rto.intelfit.domain.RecommendedFood;
    import rto.intelfit.domain.RecommendedMeal;
    import rto.intelfit.domain.RecommendedMealPlan;
    import jakarta.validation.constraints.NotBlank;
    import jakarta.validation.constraints.NotNull;
    import java.time.LocalDate;


    import java.math.BigDecimal;
    import java.time.LocalDateTime;
    import java.util.List;
    import java.util.stream.Collectors;

    public class RecommendedMealDto {

        // ========== AI 추천 식단 저장 응답 ==========
        @Data
        @Builder
        @NoArgsConstructor
        @AllArgsConstructor
        @Schema(description = "AI 추천 식단 저장 응답")
        public static class SaveRecommendedPlanResponse {

            @Schema(description = "성공 여부", example = "true")
            private boolean success;

            @Schema(description = "메시지", example = "추천 식단이 저장되었습니다")
            private String message;

            @Schema(description = "저장된 추천 식단")
            private RecommendedPlanDetailResponse plan;
        }

        @Data
        @Builder
        @NoArgsConstructor
        @AllArgsConstructor
        @Schema(description = "저장된 번들 응답")
        public static class SavedRecommendedBundlesResponse {
            private int totalCount;
            private List<BundleSummary> bundles;
        }

        @Data
        @Builder
        @NoArgsConstructor
        @AllArgsConstructor
        @Schema(description = "번들 요약 정보")
        public static class BundleSummary {
            private String bundleId;
            private int planCount;
            private BigDecimal totalCalories;
            private LocalDateTime createdAt;
            private List<RecommendedPlanDetailResponse> plans;
        }


        // ========== 추천 식단 상세 응답 ==========
        @Data
        @Builder
        @NoArgsConstructor
        @AllArgsConstructor
        @Schema(description = "추천 식단 상세 정보")
        public static class RecommendedPlanDetailResponse {

            @Schema(description = "추천 식단 ID", example = "1")
            private Long id;

            @Schema(description = "식단 이름", example = "다이어트를 위한 균형 잡힌 식단")
            private String planName;

            @Schema(description = "식단 설명", example = "하루 1800kcal 목표의 저칼로리 고단백 식단입니다")
            private String description;

            @Schema(description = "총 칼로리", example = "1800")
            private BigDecimal totalCalories;

            @Schema(description = "총 탄수화물", example = "200")
            private BigDecimal totalCarbs;

            @Schema(description = "총 단백질", example = "120")
            private BigDecimal totalProtein;

            @Schema(description = "총 지방", example = "60")
            private BigDecimal totalFat;

            @Schema(description = "추천 이유", example = "사용자의 목표 체중 감량에 최적화된 영양 비율입니다")
            private String recommendationReason;

            @Schema(description = "저장 여부", example = "true")
            private Boolean isSaved;

            @Schema(description = "식사 목록")
            private List<RecommendedMealResponse> meals;

            @Schema(description = "생성 시간", example = "2025-01-15T10:00:00")
            @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
            private LocalDateTime createdAt;

            @Schema(description = "주간 번들 ID", example = "7eb063d2-7ab8-4e49-9c9c-7e7b0e1b4e92")
            private String bundleId;

            @Schema(description = "번들 내 일자 (1~7)", example = "3")
            private Integer bundleDay;

            @Schema(description = "플랜 날짜(옵션)", example = "2025-11-10")
            @JsonFormat(pattern = "yyyy-MM-dd")
            private LocalDate planDate;

            public static RecommendedPlanDetailResponse from(RecommendedMealPlan plan) {
                return RecommendedPlanDetailResponse.builder()
                        .id(plan.getId())
                        .planName(plan.getPlanName())
                        .description(plan.getDescription())
                        .totalCalories(plan.getTotalCalories())
                        .totalCarbs(plan.getTotalCarbs())
                        .totalProtein(plan.getTotalProtein())
                        .totalFat(plan.getTotalFat())
                        .recommendationReason(plan.getRecommendationReason())
                        .isSaved(plan.getIsSaved())
                        .meals(plan.getRecommendedMeals().stream()
                                .map(RecommendedMealResponse::from)
                                .collect(Collectors.toList()))
                        .createdAt(plan.getCreatedAt())
                        // ⬇ 추가된 3개
                        .bundleId(plan.getBundleId())
                        .bundleDay(plan.getBundleDay())
                        .planDate(plan.getPlanDate())
                        .build();
            }
        }

        // ========== 추천 식사 응답 ==========
        @Data
        @Builder
        @NoArgsConstructor
        @AllArgsConstructor
        @Schema(description = "추천 식사 정보")
        public static class RecommendedMealResponse {

            @Schema(description = "식사 ID", example = "1")
            private Long id;

            @Schema(description = "식사 타입", example = "BREAKFAST")
            private Meal.MealType mealType;

            @Schema(description = "식사 타입 설명", example = "아침")
            private String mealTypeName;

            @Schema(description = "총 칼로리", example = "450")
            private BigDecimal totalCalories;

            @Schema(description = "총 탄수화물", example = "50")
            private BigDecimal totalCarbs;

            @Schema(description = "총 단백질", example = "30")
            private BigDecimal totalProtein;

            @Schema(description = "총 지방", example = "15")
            private BigDecimal totalFat;

            @Schema(description = "음식 목록")
            private List<RecommendedFoodResponse> foods;

            public static RecommendedMealResponse from(RecommendedMeal meal) {
                return RecommendedMealResponse.builder()
                        .id(meal.getId())
                        .mealType(meal.getMealType())
                        .mealTypeName(meal.getMealType().getDescription())
                        .totalCalories(meal.getTotalCalories())
                        .totalCarbs(meal.getTotalCarbs())
                        .totalProtein(meal.getTotalProtein())
                        .totalFat(meal.getTotalFat())
                        .foods(meal.getRecommendedFoods().stream()
                                .map(RecommendedFoodResponse::from)
                                .collect(Collectors.toList()))
                        .build();
            }
        }

        // ========== 추천 음식 응답 ==========
        @Data
        @Builder
        @NoArgsConstructor
        @AllArgsConstructor
        @Schema(description = "추천 음식 정보")
        public static class RecommendedFoodResponse {

            @Schema(description = "음식 ID", example = "1")
            private Long id;

            @NotBlank
            @Schema(description = "음식 이름", example = "현미밥")
            private String foodName;

            @NotNull                    // ← 필수!
            @Schema(description = "1인분 기준량 (g)", example = "150")
            private BigDecimal servingSize;

            @Schema(description = "칼로리", example = "180")
            private BigDecimal calories;

            @Schema(description = "탄수화물", example = "38")
            private BigDecimal carbs;

            @Schema(description = "단백질", example = "4")
            private BigDecimal protein;

            @Schema(description = "지방", example = "1")
            private BigDecimal fat;

            @Schema(description = "나트륨", example = "0")
            private BigDecimal sodium;

            @Schema(description = "당", example = "0")
            private BigDecimal sugar;

            public static RecommendedFoodResponse from(RecommendedFood food) {
                return RecommendedFoodResponse.builder()
                        .id(food.getId())
                        .foodName(food.getFoodName())
                        .servingSize(food.getServingSize())
                        .calories(food.getCalories())
                        .carbs(food.getCarbs())
                        .protein(food.getProtein())
                        .fat(food.getFat())
                        .sodium(food.getSodium())
                        .sugar(food.getSugar())
                        .build();
            }
        }

        // ========== 저장된 추천 식단 목록 조회 응답 ==========
        @Data
        @Builder
        @NoArgsConstructor
        @AllArgsConstructor
        @Schema(description = "저장된 추천 식단 목록 응답")
        public static class SavedRecommendedPlansResponse {

            @Schema(description = "저장된 추천 식단 개수", example = "3")
            private int totalCount;

            @Schema(description = "저장된 추천 식단 목록")
            private List<RecommendedPlanSummaryResponse> plans;
        }
        // === 번들 요약/상세/삭제 DTO 추가 ===
        @Data
        @Builder
        @NoArgsConstructor
        @AllArgsConstructor
        @Schema(description = "번들별 영양 요약 정보")
        public static class BundleNutritionSummary {
            @Schema(description = "번들 ID")
            private String bundleId;

            @Schema(description = "총 칼로리")
            private BigDecimal totalCalories;

            @Schema(description = "총 단백질")
            private BigDecimal totalProtein;

            @Schema(description = "총 탄수화물")
            private BigDecimal totalCarbs;

            @Schema(description = "총 지방")
            private BigDecimal totalFat;
        }

        @Data
        @Builder
        @NoArgsConstructor
        @AllArgsConstructor
        @Schema(description = "저장된 번들 요약 리스트 응답")
        public static class SavedBundleSummaryListResponse {
            @Schema(description = "총 번들 개수")
            private int totalCount;

            @Schema(description = "번들 요약 리스트")
            private List<BundleNutritionSummary> bundles;
        }

        @Data
        @Builder
        @NoArgsConstructor
        @AllArgsConstructor
        @Schema(description = "번들 상세 응답")
        public static class BundleDetailResponse {
            @Schema(description = "번들 ID")
            private String bundleId;

            @Schema(description = "일수 (보통 7)")
            private int days;

            @Schema(description = "하루별 추천 식단 상세 목록")
            private List<RecommendedPlanDetailResponse> plans;
        }

        @Data
        @Builder
        @NoArgsConstructor
        @AllArgsConstructor
        @Schema(description = "번들 삭제 응답")
        public static class DeleteBundleResponse {
            @Schema(description = "성공 여부")
            private boolean success;

            @Schema(description = "결과 메시지")
            private String message;

            @Schema(description = "삭제된 번들 ID")
            private String bundleId;

            @Schema(description = "삭제된 식단 수")
            private int deletedCount;
        }


        // ========== 추천 식단 요약 응답 ==========
        @Data
        @Builder
        @NoArgsConstructor
        @AllArgsConstructor
        @Schema(description = "추천 식단 요약 정보")
        public static class RecommendedPlanSummaryResponse {

            @Schema(description = "추천 식단 ID", example = "1")
            private Long id;

            @Schema(description = "식단 이름", example = "다이어트를 위한 균형 잡힌 식단")
            private String planName;

            @Schema(description = "총 칼로리", example = "1800")
            private BigDecimal totalCalories;

            @Schema(description = "식사 개수", example = "4")
            private int mealCount;

            @Schema(description = "저장 여부", example = "true")
            private Boolean isSaved;

            @Schema(description = "생성 시간", example = "2025-01-15T10:00:00")
            @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
            private LocalDateTime createdAt;

            @Schema(description = "주간 번들 ID", example = "7eb063d2-7ab8-4e49-9c9c-7e7b0e1b4e92")
            private String bundleId;

            @Schema(description = "번들 내 일자 (1~7)", example = "3")
            private Integer bundleDay;

            @Schema(description = "플랜 날짜(옵션)", example = "2025-11-10")
            @JsonFormat(pattern = "yyyy-MM-dd")
            private LocalDate planDate;

            public static RecommendedPlanSummaryResponse from(RecommendedMealPlan plan) {
                return RecommendedPlanSummaryResponse.builder()
                        .id(plan.getId())
                        .planName(plan.getPlanName())
                        .totalCalories(plan.getTotalCalories())
                        .mealCount(plan.getRecommendedMeals().size())
                        .isSaved(plan.getIsSaved())
                        .createdAt(plan.getCreatedAt())
                        // ⬇ 추가된 3개
                        .bundleId(plan.getBundleId())
                        .bundleDay(plan.getBundleDay())
                        .planDate(plan.getPlanDate())
                        .build();
            }
        }
    }