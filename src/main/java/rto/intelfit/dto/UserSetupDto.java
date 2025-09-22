package rto.intelfit.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import rto.intelfit.domain.User;

import jakarta.validation.constraints.*;

public class UserSetupDto {

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "초기 피트니스 정보 입력 요청")
    public static class InitialSetupRequest {

        @Schema(description = "성별", example = "F", allowableValues = {"M", "F"})
        @NotNull(message = "성별을 선택해주세요")
        private User.Gender gender;

        @Schema(description = "키 (cm)", example = "165")
        @NotNull(message = "키를 입력해주세요")
        @Min(value = 100, message = "키는 100cm 이상이어야 합니다")
        @Max(value = 250, message = "키는 250cm 이하여야 합니다")
        private Integer height;

        @Schema(description = "현재 체중 (kg)", example = "53")
        @NotNull(message = "체중을 입력해주세요")
        @Min(value = 30, message = "체중은 30kg 이상이어야 합니다")
        @Max(value = 200, message = "체중은 200kg 이하여야 합니다")
        private Integer weight;

        @Schema(description = "목표 체중 (kg)", example = "50")
        @NotNull(message = "목표 체중을 입력해주세요")
        @Min(value = 30, message = "목표 체중은 30kg 이상이어야 합니다")
        @Max(value = 200, message = "목표 체중은 200kg 이하여야 합니다")
        private Integer weightGoal;

        @Schema(description = "운동 목표", example = "DIET", allowableValues = {"DIET", "BULK", "LEAN_MASS"})
        @NotNull(message = "운동 목표를 선택해주세요")
        private User.HealthGoal healthGoal;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "초기 설정 완료 응답")
    public static class InitialSetupResponse {

        @Schema(description = "성공 여부", example = "true")
        private boolean success;

        @Schema(description = "메시지", example = "초기 설정이 완료되었습니다")
        private String message;

        @Schema(description = "사용자 ID", example = "1")
        private Long userId;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "초기 설정 상태 조회 응답")
    public static class SetupStatusResponse {

        @Schema(description = "초기 설정 완료 여부", example = "true")
        private boolean isSetupCompleted;

        @Schema(description = "메시지", example = "초기 설정이 완료되었습니다")
        private String message;
    }
}