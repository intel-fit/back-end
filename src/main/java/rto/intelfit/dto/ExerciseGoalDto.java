package rto.intelfit.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

public class ExerciseGoalDto {

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "운동 목표 저장 요청")
    public static class Request {
        @Schema(description = "주당 운동 횟수", example = "주 3회")
        private String weeklyFrequency;

        @Schema(description = "세션당 운동시간", example = "30분 이상")
        private String durationPerSession;

        @Schema(description = "운동 종류", example = "유산소")
        private String exerciseType;

        @Schema(description = "주간 칼로리 소모 목표(kcal 제외)", example = "1500")
        private Long weeklyCalorieGoal;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "운동 목표 저장 응답")
    public static class Response {
        private Long id;
        private String weeklyFrequency;
        private String durationPerSession;
        private String exerciseType;
        private Long weeklyCalorieGoal;
        private Long progress;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "운동 목표 요약 응답")
    public static class SummaryResponse {
        private String weeklyFrequency;
        private String durationPerSession;
        private Double progress;
    }
}
