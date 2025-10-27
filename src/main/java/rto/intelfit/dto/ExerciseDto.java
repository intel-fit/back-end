package rto.intelfit.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import rto.intelfit.domain.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

public class ExerciseDto {

    // ========== 운동 기록 추가 요청 ==========
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "운동 기록 추가 요청")
    public static class ExerciseCreateRequest {

        @Schema(description = "운동 날짜", example = "2025-01-15")
        @NotNull(message = "운동 날짜를 입력해주세요")
        @JsonFormat(pattern = "yyyy-MM-dd")
        private LocalDate exerciseDate;

        @Schema(description = "운동 카테고리", example = "RESISTANCE")
        @NotNull(message = "운동 카테고리를 선택해주세요")
        private Exercise.ExerciseCategory exerciseCategory;

        @Schema(description = "총 운동 시간 (분)", example = "60")
        private Integer totalDurationMinutes;

        @Schema(description = "메모", example = "오늘은 하체 집중!")
        @Size(max = 500, message = "메모는 500자 이내로 입력해주세요")
        private String memo;

        @Schema(description = "운동 세트 목록")
        @NotEmpty(message = "최소 1개 이상의 운동 세트를 추가해주세요")
        @Valid
        private List<ExerciseSetRequest> exerciseSets;
    }

    // ========== 운동 세트 요청 ==========
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "운동 세트 정보")
    public static class ExerciseSetRequest {

        // 유산소 운동
        @Schema(description = "유산소 운동 타입", example = "TREADMILL")
        private Exercise.CardioType cardioType;

        @Schema(description = "거리 (km)", example = "5.0")
        @DecimalMin(value = "0", message = "거리는 0 이상이어야 합니다")
        private BigDecimal distance;

        @Schema(description = "운동 시간 (분)", example = "30")
        @Min(value = 0, message = "운동 시간은 0 이상이어야 합니다")
        private Integer durationMinutes;

        @Schema(description = "칼로리 소모량", example = "250")
        @DecimalMin(value = "0", message = "칼로리는 0 이상이어야 합니다")
        private BigDecimal caloriesBurned;

        @Schema(description = "평균 심박수", example = "140")
        @Min(value = 0, message = "심박수는 0 이상이어야 합니다")
        private Integer averageHeartRate;

        // 무산소 운동
        @Schema(description = "무산소 운동 타입", example = "BARBELL_SQUAT")
        private Exercise.ResistanceExerciseType resistanceExerciseType;

        @Schema(description = "근육 부위", example = "LEGS")
        private Exercise.MuscleGroup muscleGroup;

        @Schema(description = "세트 번호", example = "1")
        @Min(value = 1, message = "세트 번호는 1 이상이어야 합니다")
        private Integer setNumber;

        @Schema(description = "무게 (kg)", example = "100")
        @DecimalMin(value = "0", message = "무게는 0 이상이어야 합니다")
        private BigDecimal weight;

        @Schema(description = "반복 횟수", example = "10")
        @Min(value = 0, message = "반복 횟수는 0 이상이어야 합니다")
        private Integer reps;

        @Schema(description = "휴식 시간 (초)", example = "90")
        @Min(value = 0, message = "휴식 시간은 0 이상이어야 합니다")
        private Integer restSeconds;

        @Schema(description = "메모", example = "폼 체크 필요")
        @Size(max = 500, message = "메모는 500자 이내로 입력해주세요")
        private String memo;

        @Schema(description = "타겟 부위 목록")
        @Valid
        private List<ExerciseTargetRequest> targets;
    }

    // ========== 타겟 부위 요청 ==========
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "운동 타겟 부위")
    public static class ExerciseTargetRequest {

        @Schema(description = "타겟 근육", example = "QUADRICEPS")
        @NotNull(message = "타겟 근육을 선택해주세요")
        private ExerciseTarget.TargetMuscle targetMuscle;

        @Schema(description = "강도 비율 (%)", example = "80")
        @Min(value = 0, message = "강도는 0 이상이어야 합니다")
        @Max(value = 100, message = "강도는 100 이하여야 합니다")
        private Integer intensityPercentage;
    }

    // ========== 운동 기록 추가 응답 ==========
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "운동 기록 추가 응답")
    public static class ExerciseCreateResponse {

        @Schema(description = "성공 여부", example = "true")
        private boolean success;

        @Schema(description = "메시지", example = "운동 기록이 추가되었습니다")
        private String message;

        @Schema(description = "생성된 운동 정보")
        private ExerciseDetailResponse exercise;
    }

    // ========== 운동 상세 응답 ==========
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "운동 상세 정보")
    public static class ExerciseDetailResponse {

        @Schema(description = "운동 ID", example = "1")
        private Long id;

        @Schema(description = "운동 날짜", example = "2025-01-15")
        @JsonFormat(pattern = "yyyy-MM-dd")
        private LocalDate exerciseDate;

        @Schema(description = "운동 카테고리", example = "RESISTANCE")
        private Exercise.ExerciseCategory exerciseCategory;

        @Schema(description = "운동 카테고리 설명", example = "무산소")
        private String exerciseCategoryName;

        @Schema(description = "총 운동 시간 (분)", example = "60")
        private Integer totalDurationMinutes;

        @Schema(description = "메모", example = "오늘은 하체 집중!")
        private String memo;

        @Schema(description = "운동 세트 목록")
        private List<ExerciseSetResponse> exerciseSets;

        @Schema(description = "생성 시간", example = "2025-01-15T08:30:00")
        @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
        private LocalDateTime createdAt;

        public static ExerciseDetailResponse from(Exercise exercise) {
            return ExerciseDetailResponse.builder()
                    .id(exercise.getId())
                    .exerciseDate(exercise.getExerciseDate())
                    .exerciseCategory(exercise.getExerciseCategory())
                    .exerciseCategoryName(exercise.getExerciseCategory().getDescription())
                    .totalDurationMinutes(exercise.getTotalDurationMinutes())
                    .memo(exercise.getMemo())
                    .exerciseSets(exercise.getExerciseSets().stream()
                            .map(ExerciseSetResponse::from)
                            .collect(Collectors.toList()))
                    .createdAt(exercise.getCreatedAt())
                    .build();
        }
    }

    // ========== 운동 세트 응답 ==========
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "운동 세트 상세 정보")
    public static class ExerciseSetResponse {

        @Schema(description = "세트 ID", example = "1")
        private Long id;

        // 유산소
        @Schema(description = "유산소 운동 타입", example = "TREADMILL")
        private Exercise.CardioType cardioType;

        @Schema(description = "유산소 운동 타입 설명", example = "런닝머신")
        private String cardioTypeName;

        @Schema(description = "거리 (km)", example = "5.0")
        private BigDecimal distance;

        @Schema(description = "운동 시간 (분)", example = "30")
        private Integer durationMinutes;

        @Schema(description = "칼로리 소모량", example = "250")
        private BigDecimal caloriesBurned;

        @Schema(description = "평균 속도 (km/h)", example = "10.0")
        private BigDecimal averageSpeed;

        @Schema(description = "평균 심박수", example = "140")
        private Integer averageHeartRate;

        // 무산소
        @Schema(description = "무산소 운동 타입", example = "BARBELL_SQUAT")
        private Exercise.ResistanceExerciseType resistanceExerciseType;

        @Schema(description = "무산소 운동 타입 설명", example = "바벨 스쿼트")
        private String resistanceExerciseTypeName;

        @Schema(description = "근육 부위", example = "LEGS")
        private Exercise.MuscleGroup muscleGroup;

        @Schema(description = "근육 부위 설명", example = "하체")
        private String muscleGroupName;

        @Schema(description = "세트 번호", example = "1")
        private Integer setNumber;

        @Schema(description = "무게 (kg)", example = "100")
        private BigDecimal weight;

        @Schema(description = "반복 횟수", example = "10")
        private Integer reps;

        @Schema(description = "휴식 시간 (초)", example = "90")
        private Integer restSeconds;

        @Schema(description = "1RM (추정)", example = "133.3")
        private BigDecimal oneRepMax;

        @Schema(description = "볼륨 (무게 × 횟수)", example = "1000")
        private BigDecimal volume;

        @Schema(description = "메모", example = "폼 체크 필요")
        private String memo;

        @Schema(description = "타겟 부위 목록")
        private List<ExerciseTargetResponse> targets;

        public static ExerciseSetResponse from(ExerciseSet set) {
            ExerciseSetResponseBuilder builder = ExerciseSetResponse.builder()
                    .id(set.getId())
                    .durationMinutes(set.getDurationMinutes())
                    .restSeconds(set.getRestSeconds())
                    .memo(set.getMemo());

            if (set.isCardio()) {
                builder.cardioType(set.getCardioType())
                        .cardioTypeName(set.getCardioType().getDescription())
                        .distance(set.getDistance())
                        .caloriesBurned(set.getCaloriesBurned())
                        .averageSpeed(set.getAverageSpeed())
                        .averageHeartRate(set.getAverageHeartRate());
            }

            if (set.isResistance()) {
                builder.resistanceExerciseType(set.getResistanceExerciseType())
                        .resistanceExerciseTypeName(set.getResistanceExerciseType().getDescription())
                        .muscleGroup(set.getMuscleGroup())
                        .muscleGroupName(set.getMuscleGroup().getDescription())
                        .setNumber(set.getSetNumber())
                        .weight(set.getWeight())
                        .reps(set.getReps())
                        .oneRepMax(set.calculateOneRepMax())
                        .volume(set.calculateVolume());
            }

            return builder.build();
        }
    }

    // ========== 타겟 부위 응답 ==========
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "운동 타겟 부위 정보")
    public static class ExerciseTargetResponse {

        @Schema(description = "타겟 ID", example = "1")
        private Long id;

        @Schema(description = "타겟 근육", example = "QUADRICEPS")
        private ExerciseTarget.TargetMuscle targetMuscle;

        @Schema(description = "타겟 근육 설명", example = "대퇴사두근")
        private String targetMuscleName;

        @Schema(description = "카테고리", example = "하체")
        private String category;

        @Schema(description = "강도 비율 (%)", example = "80")
        private Integer intensityPercentage;

        public static ExerciseTargetResponse from(ExerciseTarget target) {
            return ExerciseTargetResponse.builder()
                    .id(target.getId())
                    .targetMuscle(target.getTargetMuscle())
                    .targetMuscleName(target.getTargetMuscle().getDescription())
                    .category(target.getTargetMuscle().getCategory())
                    .intensityPercentage(target.getIntensityPercentage())
                    .build();
        }
    }

    // ========== 일별 운동 조회 응답 ==========
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "일별 운동 조회 응답")
    public static class DailyExercisesResponse {

        @Schema(description = "조회 날짜", example = "2025-01-15")
        @JsonFormat(pattern = "yyyy-MM-dd")
        private LocalDate date;

        @Schema(description = "해당 날짜의 모든 운동 기록")
        private List<ExerciseDetailResponse> exercises;

        @Schema(description = "일일 총 운동 시간 (분)", example = "120")
        private Integer dailyTotalDurationMinutes;

        @Schema(description = "일일 총 칼로리 소모", example = "500")
        private BigDecimal dailyTotalCalories;
    }

    // ========== 주간 운동 통계 응답 ==========
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "주간 운동 통계 응답")
    public static class WeeklyExerciseStatsResponse {

        @Schema(description = "조회 시작일", example = "2025-01-13")
        @JsonFormat(pattern = "yyyy-MM-dd")
        private LocalDate startDate;

        @Schema(description = "조회 종료일", example = "2025-01-19")
        @JsonFormat(pattern = "yyyy-MM-dd")
        private LocalDate endDate;

        @Schema(description = "주간 총 운동 시간 (분)", example = "360")
        private Integer weeklyTotalDurationMinutes;

        @Schema(description = "주간 총 운동 일수", example = "4")
        private Integer weeklyExerciseDays;

        @Schema(description = "주간 총 칼로리 소모", example = "2000")
        private BigDecimal weeklyTotalCalories;

        @Schema(description = "일일 평균 운동 시간 (분)", example = "90")
        private Integer dailyAvgDurationMinutes;

        @Schema(description = "지난주 대비 변화율 (%)", example = "15.5")
        private BigDecimal weeklyChangeRate;

        @Schema(description = "일별 상세 통계")
        private List<DailyExerciseStatItem> dailyStats;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "일별 운동 통계 항목")
    public static class DailyExerciseStatItem {

        @Schema(description = "날짜", example = "2025-01-15")
        @JsonFormat(pattern = "yyyy-MM-dd")
        private LocalDate date;

        @Schema(description = "일일 운동 시간 (분)", example = "90")
        private Integer dailyDurationMinutes;

        @Schema(description = "일일 칼로리 소모", example = "500")
        private BigDecimal dailyCalories;

        @Schema(description = "운동 횟수", example = "2")
        private Integer exerciseCount;
    }

    // ========== 체지방 감량 분석 응답 ==========
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "체지방 감량 분석")
    public static class FatLossAnalysisResponse {

        @Schema(description = "현재 체지방률 (%)", example = "18.5")
        private BigDecimal currentBodyFatPercentage;

        @Schema(description = "목표 체지방률 (%)", example = "15.0")
        private BigDecimal targetBodyFatPercentage;

        @Schema(description = "감량 필요 체지방 (kg)", example = "3.5")
        private BigDecimal requiredFatLoss;

        @Schema(description = "예상 소요 기간 (주)", example = "8")
        private Integer estimatedWeeks;

        @Schema(description = "주간 권장 운동 시간 (분)", example = "300")
        private Integer recommendedWeeklyExerciseMinutes;

        @Schema(description = "권장 운동 타입")
        private List<String> recommendedExerciseTypes;

        @Schema(description = "분석 메시지", example = "목표 달성을 위해 주 300분 이상의 운동이 필요합니다")
        private String analysisMessage;
    }
}