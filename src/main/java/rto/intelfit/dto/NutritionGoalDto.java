package rto.intelfit.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import rto.intelfit.domain.DailyNutritionGoal;

import java.math.BigDecimal;

public class NutritionGoalDto {

    // ========== 영양 목표 설정 요청 ==========
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "영양 목표 설정 요청")
    public static class NutritionGoalSetRequest {

        @Schema(description = "목표 칼로리 (kcal)", example = "2000")
        @NotNull(message = "목표 칼로리를 입력해주세요")
        @DecimalMin(value = "500", message = "목표 칼로리는 500kcal 이상이어야 합니다")
        private BigDecimal targetCalories;

        @Schema(description = "목표 탄수화물 (g)", example = "250")
        @NotNull(message = "목표 탄수화물을 입력해주세요")
        @DecimalMin(value = "0", message = "목표 탄수화물은 0g 이상이어야 합니다")
        private BigDecimal targetCarbs;

        @Schema(description = "목표 단백질 (g)", example = "150")
        @NotNull(message = "목표 단백질을 입력해주세요")
        @DecimalMin(value = "0", message = "목표 단백질은 0g 이상이어야 합니다")
        private BigDecimal targetProtein;

        @Schema(description = "목표 지방 (g)", example = "67")
        @NotNull(message = "목표 지방을 입력해주세요")
        @DecimalMin(value = "0", message = "목표 지방은 0g 이상이어야 합니다")
        private BigDecimal targetFat;

        @Schema(description = "목표 타입 (AUTO: 자동, MANUAL: 수동)", example = "MANUAL")
        @NotNull(message = "목표 타입을 선택해주세요")
        private DailyNutritionGoal.GoalType goalType;
    }

    // ========== 영양 목표 설정 응답 ==========
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "영양 목표 설정 응답")
    public static class NutritionGoalSetResponse {

        @Schema(description = "성공 여부", example = "true")
        private boolean success;

        @Schema(description = "메시지", example = "영양 목표가 설정되었습니다")
        private String message;

        @Schema(description = "설정된 영양 목표")
        private NutritionGoalResponse goal;
    }

    // ========== 영양 목표 조회 응답 ==========
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "영양 목표 정보")
    public static class NutritionGoalResponse {

        @Schema(description = "목표 ID", example = "1")
        private Long id;

        @Schema(description = "목표 칼로리 (kcal)", example = "2000")
        private BigDecimal targetCalories;

        @Schema(description = "목표 탄수화물 (g)", example = "250")
        private BigDecimal targetCarbs;

        @Schema(description = "목표 단백질 (g)", example = "150")
        private BigDecimal targetProtein;

        @Schema(description = "목표 지방 (g)", example = "67")
        private BigDecimal targetFat;

        @Schema(description = "목표 타입", example = "AUTO")
        private DailyNutritionGoal.GoalType goalType;

        @Schema(description = "목표 타입 설명", example = "자동 계산")
        private String goalTypeDescription;

        public static NutritionGoalResponse from(DailyNutritionGoal goal) {
            return NutritionGoalResponse.builder()
                    .id(goal.getId())
                    .targetCalories(goal.getTargetCalories())
                    .targetCarbs(goal.getTargetCarbs())
                    .targetProtein(goal.getTargetProtein())
                    .targetFat(goal.getTargetFat())
                    .goalType(goal.getGoalType())
                    .goalTypeDescription(
                            goal.getGoalType() == DailyNutritionGoal.GoalType.AUTO
                                    ? "자동 계산" : "수동 설정"
                    )
                    .build();
        }
    }
}