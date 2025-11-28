package rto.intelfit.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;
import rto.intelfit.domain.FitnessExerciseCategorySave;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;


/**
 * 운동 기록 조회/추가/삭제 DTO
 */
public class FitnessExerciseCategorySaveDto {

    // ✅ 세트 단위 DTO
    @Getter
    @Setter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "운동 세트 단위 정보")
    public static class SetDetail {
        @Schema(description = "세트 번호")
        private Integer setNumber;

        @Schema(description = "중량 (kg)")
        private Double weight;

        @Schema(description = "횟수 (reps)")
        private Integer reps;
    }


    // ✅ 운동 기록 추가 요청 DTO
    @Getter
    @Setter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "운동 세션 추가 요청 DTO")
    public static class CreateRequest {
        @Schema(description = "유저 ID", example = "1")
        private Long userId;

        @Schema(description = "운동 ID (Exercise DB의 externalId)", example = "27NNGFr")
        private String externalId; // ✅ 추가됨

        @Schema(description = "운동명", example = "벤치프레스")
        private String exerciseName;

        @Schema(description = "카테고리", example = "가슴")
        private String category;

        @Schema(description = "운동 날짜 (YYYY-MM-DDTHH:MM:SS)", example = "2025-10-27T09:00:00")
        private LocalDateTime workoutDate;

        @Schema(description = "세트 목록")
        private List<SetDetail> sets;
    }

    // ✅ 세션 단위 조회 응답 DTO
    @Getter
    @Setter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "운동 세션 단위 응답 DTO")
    public static class SessionResponse {
        @Schema(description = "세션 ID")
        private String sessionId;

        @Schema(description = "운동 ID (Exercise DB의 externalId)")
        private String externalId; // ✅ 추가됨

        @Schema(description = "운동명")
        private String exerciseName;

        @Schema(description = "카테고리")
        private String category;

        @Schema(description = "운동 날짜")
        private LocalDateTime workoutDate;

        @Schema(description = "세트 목록")
        private List<SetDetail> sets;

        public static SessionResponse from(String sessionId, List<FitnessExerciseCategorySave> records) {
            FitnessExerciseCategorySave first = records.get(0);
            return SessionResponse.builder()
                    .sessionId(sessionId)
                    .externalId(first.getExternalId()) // ✅ 추가됨
                    .exerciseName(first.getExerciseName())
                    .category(first.getCategory())
                    .workoutDate(first.getWorkoutDate())
                    .sets(records.stream()
                            .map(r -> SetDetail.builder()
                                    .setNumber(r.getSetNumber())
                                    .weight(r.getWeight())
                                    .reps(r.getReps())
                                    .build())
                            .collect(Collectors.toList()))
                    .build();
        }
    }

    // ✅ 세션 삭제 응답 DTO
    @Getter
    @Setter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "세션 삭제 응답 DTO")
    public static class DeleteResponse {
        @Schema(description = "삭제된 세션 ID")
        private String sessionId;

        @Schema(description = "운동 ID (Exercise DB의 externalId)")
        private String externalId; // ✅ 추가됨

        @Schema(description = "삭제된 세트 수")
        private int deletedCount;
    }

    // ✅ 세션 완료 상태 토글 응답 DTO
    @Getter
    @Setter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "세션 완료 상태 토글 응답 DTO")
    public static class ToggleResponse {
        @Schema(description = "세션 ID")
        private String sessionId;

        @Schema(description = "변경된 완료 상태 (true=완료, false=미완료)")
        private boolean completed;

        @Schema(description = "영향받은 세트 수")
        private int affectedSets;
    }

    @Getter @Setter
    public static class SaveRequest {
        private Long userId;
        private String saveTitle;
        private String date;          // ← 추가
        private List<Double> intensity;
        private List<String> feedback;
    }


    @Getter
    @Setter
    @Builder
    @AllArgsConstructor
    public static class SaveResponse {
        private List<String> sessionIds;   // 여러 세션
        private String saveTitle;
        private int updatedCount;
    }


    @Getter
    @Setter
    @Builder
    public static class SavedGroupResponse {

        private String title;
        private List<SessionGroup> sessions;

        @Getter
        @Setter
        @Builder
        public static class SessionGroup {
            private String sessionId;
            private List<SavedSetDetail> records;
        }
    }


    // 개별 세트 DTO (이미 있을 수 있음)
    @Getter
    @Setter
    @Builder
    public static class SavedSetDetail {

        private Long id;
        private Integer setNumber;
        private Double weight;
        private Integer reps;
        private String category;
        private String exerciseName;
        private LocalDateTime workoutDate;

        public static SavedSetDetail from(FitnessExerciseCategorySave entity) {
            return SavedSetDetail.builder()
                    .id(entity.getId())
                    .setNumber(entity.getSetNumber())
                    .weight(entity.getWeight())
                    .reps(entity.getReps())
                    .category(entity.getCategory())
                    .exerciseName(entity.getExerciseName())
                    .workoutDate(entity.getWorkoutDate())
                    .build();
        }
    }





}
