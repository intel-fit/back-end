package rto.intelfit.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import rto.intelfit.domain.*;
import java.util.ArrayList;

import lombok.Getter;
import lombok.Setter;


import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

import java.util.Map;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.NotBlank;


public class RecommendedExerciseDto {

    @Getter
    @Setter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(name = "DailyExerciseRecommendationRequest", description = "AI 일일 운동 추천 요청 DTO")
    public static class DailyRecommendationRequest {

        @Schema(description = "운동 숙련도 (User.ExperienceLevel)", example = "INTERMEDIATE")
        @NotNull
        private User.ExperienceLevel experienceLevel;

        @Schema(description = "운동 환경 (home | gym)", example = "gym")
        @NotBlank
        private String environment;

        @Schema(description = "사용 가능한 장비 목록", example = "[\"덤벨\", \"머신\"]")
        @Builder.Default
        private List<String> availableEquipment = new ArrayList<>();

        @Schema(description = "주의해야 할 건강 상태/질환", example = "[\"허리통증\"]")
        @Builder.Default
        private List<String> healthConditions = new ArrayList<>();

        @Schema(description = "목표 운동 시간(분)", example = "60")
        private Integer targetTimeMin;
    }

    @Getter
    @Setter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(name = "DailyExerciseRecommendationResponse", description = "AI 일일 운동 추천 응답 DTO")
    public static class DailyRecommendationResponse {

        @Schema(description = "요청 성공 여부")
        private boolean success;

        @Schema(description = "메시지")
        private String message;

        @Schema(description = "오늘의 포커스(Upper/Lower/Core 등)")
        private String focus;

        @Schema(description = "세션 메트릭(총 시간, 추정 칼로리 등)")
        private Map<String, Object> metrics;

        @Schema(description = "운동 리스트 (AI 서버 raw 구조 그대로 전달)")
        private List<Map<String, Object>> exercises;
    }
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "AI 운동 추천 생성 응답")
    public static class GenerateRecommendationResponse {

        @Schema(description = "성공 여부", example = "true")
        private boolean success;

        @Schema(description = "메시지", example = "맞춤 운동 플랜이 생성되었습니다")
        private String message;

        @Schema(description = "생성된 추천 플랜")
        private ExercisePlanDetailResponse plan;
    }

    // ========== 식단 연동 운동 추천 응답 ==========
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "식단 연동 운동 추천 응답")
    public static class MealBasedRecommendationResponse {

        @Schema(description = "성공 여부", example = "true")
        private boolean success;

        @Schema(description = "메시지", example = "식단 기반 운동 플랜이 생성되었습니다")
        private String message;

        @Schema(description = "식단 분석 정보")
        private MealAnalysisInfo mealAnalysis;

        @Schema(description = "생성된 추천 플랜")
        private ExercisePlanDetailResponse plan;
    }

    // ========== 식단 분석 정보 ==========
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "식단 분석 정보")
    public static class MealAnalysisInfo {

        @Schema(description = "분석 날짜", example = "2025-01-15")
        @JsonFormat(pattern = "yyyy-MM-dd")
        private LocalDate analysisDate;

        @Schema(description = "총 섭취 칼로리", example = "2000")
        private BigDecimal totalCalories;

        @Schema(description = "목표 칼로리", example = "1800")
        private BigDecimal targetCalories;

        @Schema(description = "칼로리 초과량", example = "200")
        private BigDecimal excessCalories;

        @Schema(description = "권장 칼로리 소모량 (운동)", example = "250")
        private BigDecimal recommendedCaloriesBurn;

        @Schema(description = "분석 메시지", example = "목표 대비 200kcal 초과 섭취하셨습니다")
        private String analysisMessage;
    }

    // ========== 추천 플랜 상세 응답 ==========
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "추천 운동 플랜 상세")
    public static class ExercisePlanDetailResponse {

        @Schema(description = "플랜 ID", example = "1")
        private Long id;

        @Schema(description = "플랜 이름", example = "체지방 감량을 위한 유산소 중심 플랜")
        private String planName;

        @Schema(description = "플랜 설명", example = "주 4회, 유산소 및 근력 운동 병행")
        private String description;

        @Schema(description = "목표 주간 운동 시간 (분)", example = "300")
        private Integer targetWeeklyMinutes;

        @Schema(description = "추천 운동 루틴 목록")
        private List<ExerciseRoutineResponse> routines;

        @Schema(description = "추천 이유", example = "사용자의 건강 목표와 현재 체지방률에 최적화된 플랜입니다")
        private String recommendationReason;

        @Schema(description = "저장 여부", example = "false")
        private boolean isSaved;

        @Schema(description = "생성 일시", example = "2025-01-15T08:30:00")
        @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
        private LocalDateTime createdAt;

        public static ExercisePlanDetailResponse from(RecommendedExercisePlan plan) {
            return ExercisePlanDetailResponse.builder()
                    .id(plan.getId())
                    .planName(plan.getPlanName())
                    .description(plan.getDescription())
                    .targetWeeklyMinutes(plan.getTargetWeeklyMinutes())
                    .routines(plan.getRoutines().stream()
                            .map(ExerciseRoutineResponse::from)
                            .collect(Collectors.toList()))
                    .recommendationReason(plan.getRecommendationReason())

                    .createdAt(plan.getCreatedAt())
                    .build();
        }
    }

    // ========== 추천 운동 루틴 응답 ==========
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "추천 운동 루틴")
    public static class ExerciseRoutineResponse {

        @Schema(description = "루틴 ID", example = "1")
        private Long id;

        @Schema(description = "루틴 이름", example = "하체 집중 데이")
        private String routineName;

        @Schema(description = "요일", example = "월요일")
        private String dayOfWeek;

        @Schema(description = "운동 카테고리", example = "RESISTANCE")
        private Exercise.ExerciseCategory exerciseCategory;

        @Schema(description = "운동 카테고리 설명", example = "무산소")
        private String exerciseCategoryName;

        @Schema(description = "예상 소요 시간 (분)", example = "60")
        private Integer estimatedDurationMinutes;

        @Schema(description = "운동 항목 목록")
        private List<ExerciseItemResponse> items;

        public static ExerciseRoutineResponse from(RecommendedExerciseRoutine routine) {
            return ExerciseRoutineResponse.builder()
                    .id(routine.getId())
                    .routineName(routine.getRoutineName())
                    .dayOfWeek(routine.getDayOfWeek())
                    .exerciseCategory(routine.getExerciseCategory())
                    .exerciseCategoryName(routine.getExerciseCategory().getDescription())
                    .estimatedDurationMinutes(routine.getEstimatedDurationMinutes())
                    .items(routine.getItems().stream()
                            .map(ExerciseItemResponse::from)
                            .collect(Collectors.toList()))
                    .build();
        }
    }

    // ========== 추천 운동 항목 응답 ==========
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "추천 운동 항목")
    public static class ExerciseItemResponse {

        @Schema(description = "항목 ID", example = "1")
        private Long id;

        // 유산소
        @Schema(description = "유산소 운동 타입", example = "TREADMILL")
        private Exercise.CardioType cardioType;

        @Schema(description = "유산소 운동 타입 설명", example = "런닝머신")
        private String cardioTypeName;

        @Schema(description = "목표 거리 (km)", example = "5.0")
        private BigDecimal targetDistance;

        @Schema(description = "목표 운동 시간 (분)", example = "30")
        private Integer targetDurationMinutes;

        @Schema(description = "목표 칼로리 소모", example = "250")
        private BigDecimal targetCaloriesBurn;

        // 무산소
        @Schema(description = "무산소 운동 타입", example = "BARBELL_SQUAT")
        private Exercise.ResistanceExerciseType resistanceExerciseType;

        @Schema(description = "무산소 운동 타입 설명", example = "바벨 스쿼트")
        private String resistanceExerciseTypeName;

        @Schema(description = "근육 부위", example = "LEGS")
        private Exercise.MuscleGroup muscleGroup;

        @Schema(description = "근육 부위 설명", example = "하체")
        private String muscleGroupName;

        @Schema(description = "추천 세트 수", example = "4")
        private Integer recommendedSets;

        @Schema(description = "추천 반복 횟수", example = "10-12")
        private String recommendedReps;

        @Schema(description = "추천 무게 (kg)", example = "80-100")
        private String recommendedWeight;

        @Schema(description = "권장 휴식 시간 (초)", example = "90")
        private Integer recommendedRestSeconds;

        @Schema(description = "운동 설명 및 팁")
        private String description;

        @Schema(description = "순서", example = "1")
        private Integer exerciseOrder;

        public static ExerciseItemResponse from(RecommendedExerciseItem item) {
            ExerciseItemResponseBuilder builder = ExerciseItemResponse.builder()
                    .id(item.getId())
                    .description(item.getDescription())
                    .exerciseOrder(item.getExerciseOrder());

            if (item.isCardio()) {
                builder.cardioType(item.getCardioType())
                        .cardioTypeName(item.getCardioType().getDescription())
                        .targetDistance(item.getTargetDistance())
                        .targetDurationMinutes(item.getTargetDurationMinutes())
                        .targetCaloriesBurn(item.getTargetCaloriesBurn());
            }

            if (item.isResistance()) {
                builder.resistanceExerciseType(item.getResistanceExerciseType())
                        .resistanceExerciseTypeName(item.getResistanceExerciseType().getDescription())
                        .muscleGroup(item.getMuscleGroup())
                        .muscleGroupName(item.getMuscleGroup().getDescription())
                        .recommendedSets(item.getRecommendedSets())
                        .recommendedReps(item.getRecommendedReps())
                        .recommendedWeight(item.getRecommendedWeight())
                        .recommendedRestSeconds(item.getRecommendedRestSeconds());
            }

            return builder.build();
        }
    }

    // ========== 저장된 플랜 목록 응답 ==========
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "저장된 추천 플랜 목록")
    public static class SavedPlansResponse {

        @Schema(description = "저장된 플랜 목록")
        private List<ExercisePlanSummary> plans;

        @Schema(description = "총 플랜 수", example = "5")
        private Integer totalCount;
    }

    // ========== 플랜 요약 정보 ==========
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "추천 플랜 요약 정보")
    public static class ExercisePlanSummary {

        @Schema(description = "플랜 ID", example = "1")
        private Long id;

        @Schema(description = "플랜 이름", example = "체지방 감량을 위한 유산소 중심 플랜")
        private String planName;

        @Schema(description = "플랜 설명", example = "주 4회, 유산소 및 근력 운동 병행")
        private String description;

        @Schema(description = "목표 주간 운동 시간 (분)", example = "300")
        private Integer targetWeeklyMinutes;

        @Schema(description = "루틴 수", example = "4")
        private Integer routineCount;

        @Schema(description = "생성 일시", example = "2025-01-15T08:30:00")
        @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
        private LocalDateTime createdAt;

        public static ExercisePlanSummary from(RecommendedExercisePlan plan) {
            return ExercisePlanSummary.builder()
                    .id(plan.getId())
                    .planName(plan.getPlanName())
                    .description(plan.getDescription())
                    .targetWeeklyMinutes(plan.getTargetWeeklyMinutes())
                    .routineCount(plan.getRoutines().size())
                    .createdAt(plan.getCreatedAt())
                    .build();
        }
    }

    // ========== 플랜 저장 응답 ==========
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "플랜 저장 응답")
    public static class SavePlanResponse {

        @Schema(description = "성공 여부", example = "true")
        private boolean success;

        @Schema(description = "메시지", example = "추천 플랜이 저장되었습니다")
        private String message;

        @Schema(description = "플랜 ID", example = "1")
        private Long planId;
    }

    // ========== 플랜 적용 응답 ==========
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "플랜 적용 응답")
    public static class ApplyPlanResponse {

        @Schema(description = "성공 여부", example = "true")
        private boolean success;

        @Schema(description = "메시지", example = "추천 플랜이 운동 기록으로 저장되었습니다")
        private String message;

        @Schema(description = "생성된 운동 기록 ID 목록")
        private List<Long> createdExerciseIds;
    }
}
