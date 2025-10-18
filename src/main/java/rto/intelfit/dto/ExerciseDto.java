package rto.intelfit.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import lombok.*;
import rto.intelfit.domain.Exercise;
import rto.intelfit.domain.User;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

public class ExerciseDto {

    // ==================== 운동 등록 요청 ====================
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "운동 등록 요청")
    public static class Request {

        @Schema(description = "사용자 ID", example = "3")
        @NotNull(message = "사용자 ID는 필수입니다.")
        private Long userId;

        @Schema(description = "운동 이름", example = "벤치프레스")
        @NotBlank(message = "운동 이름을 입력해주세요.")
        private String exerciseName;

        @Schema(description = "운동 부위", example = "가슴")
        private String category;

        @Schema(description = "중량 (kg)", example = "60.5")
        @DecimalMin(value = "0.0", message = "중량은 0 이상이어야 합니다.")
        private Double weight;

        @Schema(description = "반복 횟수", example = "10")
        @Min(value = 1, message = "반복 횟수는 1 이상이어야 합니다.")
        private Integer reps;

        @Schema(description = "세트 수", example = "4")
        @Min(value = 1, message = "세트 수는 1 이상이어야 합니다.")
        private Integer sets;
    }

    // ==================== 운동 단건 응답 ====================
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "운동 단건 응답")
    public static class Response {

        @Schema(description = "운동 ID", example = "12")
        private Long exerciseId;

        @Schema(description = "운동 이름", example = "벤치프레스")
        private String exerciseName;

        @Schema(description = "운동 부위", example = "가슴")
        private String category;

        @Schema(description = "중량 (kg)", example = "60.5")
        private Double weight;

        @Schema(description = "반복 횟수", example = "10")
        private Integer reps;

        @Schema(description = "세트 수", example = "4")
        private Integer sets;

        @Schema(description = "등록 일시")
        @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
        private LocalDateTime createdAt;

        // ✅ Entity → DTO 변환
        public static Response from(Exercise e) {
            return Response.builder()
                    .exerciseId(e.getId())
                    .exerciseName(e.getExerciseName())
                    .category(e.getCategory())
                    .weight(e.getWeight())
                    .reps(e.getReps())
                    .sets(e.getSets())
                    .createdAt(e.getCreatedAt())
                    .build();
        }
    }

    // ==================== 사용자 운동 목록 응답 ====================
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "사용자별 운동 목록 응답")
    public static class ListResponse {

        @Schema(description = "사용자 ID", example = "3")
        private Long userId;

        @Schema(description = "운동 목록")
        private List<Response> exercises;

        // ✅ Entity List → DTO List 변환
        public static ListResponse from(User user, List<Exercise> list) {
            return ListResponse.builder()
                    .userId(user.getId())
                    .exercises(list.stream().map(Response::from).collect(Collectors.toList()))
                    .build();
        }
    }

    // ==================== 운동 삭제 응답 ====================
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "운동 삭제 응답")
    public static class DeleteResponse {

        @Schema(description = "성공 여부", example = "true")
        private boolean success;

        @Schema(description = "메시지", example = "운동이 삭제되었습니다.")
        private String message;
    }
}
