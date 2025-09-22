package rto.intelfit.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.validation.constraints.*;

public class LoginDto {

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "로그인 요청")
    public static class Request {

        @Schema(description = "사용자 ID", example = "lkmm1108")
        @NotBlank(message = "아이디를 입력해주세요")
        private String userId;

        @Schema(description = "비밀번호", example = "password123!")
        @NotBlank(message = "비밀번호를 입력해주세요")
        private String password;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "로그인 응답")
    public static class Response {

        @Schema(description = "성공 여부", example = "true")
        private boolean success;

        @Schema(description = "메시지", example = "로그인이 완료되었습니다")
        private String message;

        @Schema(description = "사용자 ID", example = "1")
        private Long userId;

        @Schema(description = "사용자명", example = "홍길동")
        private String name;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "아이디 찾기 요청")
    public static class FindUserIdRequest {

        @Schema(description = "이메일", example = "lkmm1108@gmail.com")
        @NotBlank(message = "이메일을 입력해주세요")
        @Email(message = "올바른 이메일 형식이 아닙니다")
        private String email;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "아이디 찾기 응답")
    public static class FindUserIdResponse {

        @Schema(description = "성공 여부", example = "true")
        private boolean success;

        @Schema(description = "메시지", example = "아이디가 이메일로 발송되었습니다")
        private String message;

        @Schema(description = "마스킹된 아이디", example = "lk***08")
        private String maskedUserId;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "비밀번호 재설정 요청 (임시비밀번호 발송)")
    public static class PasswordResetRequest {

        @Schema(description = "이메일", example = "lkmm1108@gmail.com")
        @NotBlank(message = "이메일을 입력해주세요")
        @Email(message = "올바른 이메일 형식이 아닙니다")
        private String email;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "비밀번호 재설정 응답")
    public static class PasswordResetResponse {

        @Schema(description = "성공 여부", example = "true")
        private boolean success;

        @Schema(description = "메시지", example = "임시 비밀번호가 이메일로 발송되었습니다")
        private String message;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "비밀번호 변경 요청")
    public static class PasswordChangeRequest {

        @Schema(description = "임시 비밀번호", example = "ABCDEF")
        @NotBlank(message = "임시 비밀번호를 입력해주세요")
        @Pattern(regexp = "^[A-Z]{6}$", message = "임시 비밀번호는 6자리 영어 대문자여야 합니다")
        private String tempPassword;

        @Schema(description = "새 비밀번호", example = "newPassword123!")
        @NotBlank(message = "새 비밀번호를 입력해주세요")
        @Size(min = 8, max = 255, message = "비밀번호는 8자 이상이어야 합니다")
        @Pattern(regexp = "^(?=.*[a-zA-Z])(?=.*\\d)(?=.*[@$!%*?&])[A-Za-z\\d@$!%*?&].*$",
                message = "비밀번호는 영문, 숫자, 특수문자를 포함해야 합니다")
        private String newPassword;

        @Schema(description = "새 비밀번호 확인", example = "newPassword123!")
        @NotBlank(message = "새 비밀번호 확인을 입력해주세요")
        private String newPasswordConfirm;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "비밀번호 변경 응답")
    public static class PasswordChangeResponse {

        @Schema(description = "성공 여부", example = "true")
        private boolean success;

        @Schema(description = "메시지", example = "비밀번호가 변경되었습니다")
        private String message;
    }
}