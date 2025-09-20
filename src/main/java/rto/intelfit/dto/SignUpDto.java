package rto.intelfit.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.validation.constraints.*;
import java.time.LocalDate;

public class SignUpDto {

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "회원가입 요청")
    public static class Request {

        @Schema(description = "사용자 ID", example = "lkmm1108")
        @NotBlank(message = "아이디를 입력해주세요")
        @Pattern(regexp = "^[a-zA-Z0-9]{4,20}$", message = "아이디는 4~20자의 영문, 숫자만 사용 가능합니다")
        private String userId;

        @Schema(description = "이름", example = "홍길동")
        @NotBlank(message = "이름을 입력해주세요")
        @Size(max = 50, message = "이름은 50자 이내로 입력해주세요")
        private String name;

        @Schema(description = "이메일", example = "lkmm1108@gmail.com")
        @NotBlank(message = "이메일을 입력해주세요")
        @Email(message = "올바른 이메일 형식이 아닙니다")
        @Size(max = 50, message = "이메일은 50자 이내로 입력해주세요")
        private String email;

        @Schema(description = "비밀번호", example = "password123!")
        @NotBlank(message = "비밀번호를 입력해주세요")
        @Size(min = 8, max = 255, message = "비밀번호는 8자 이상이어야 합니다")
        @Pattern(regexp = "^(?=.*[a-zA-Z])(?=.*\\d)(?=.*[@$!%*?&])[A-Za-z\\d@$!%*?&].*$",
                message = "비밀번호는 영문, 숫자, 특수문자를 포함해야 합니다")
        private String password;

        @Schema(description = "비밀번호 확인", example = "password123!")
        @NotBlank(message = "비밀번호 확인을 입력해주세요")
        private String passwordConfirm;

        @Schema(description = "생년월일", example = "1995-01-01")
        @NotNull(message = "생년월일을 입력해주세요")
        @JsonFormat(pattern = "yyyy-MM-dd")
        private LocalDate birthDate;

        @Schema(description = "전화번호", example = "01028147460")
        @NotBlank(message = "전화번호를 입력해주세요")
        @Pattern(regexp = "^\\d{10,11}$", message = "올바른 전화번호 형식이 아닙니다")
        private String phoneNumber;

        @Schema(description = "이메일 인증코드", example = "123456")
        @NotBlank(message = "이메일 인증코드를 입력해주세요")
        @Pattern(regexp = "^\\d{6}$", message = "인증코드는 6자리 숫자여야 합니다")
        private String verificationCode;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "회원가입 응답")
    public static class Response {

        @Schema(description = "성공 여부", example = "true")
        private boolean success;

        @Schema(description = "메시지", example = "회원가입이 완료되었습니다")
        private String message;

        @Schema(description = "생성된 사용자 ID", example = "1")
        private Long userId;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "아이디 중복확인 응답")
    public static class UserIdCheckResponse {

        @Schema(description = "사용 가능 여부", example = "true")
        private boolean available;

        @Schema(description = "메시지", example = "사용 가능한 아이디입니다")
        private String message;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "이메일 인증코드 발송 요청")
    public static class EmailVerificationRequest {

        @Schema(description = "이메일", example = "lkmm1108@gmail.com")
        @NotBlank(message = "이메일을 입력해주세요")
        @Email(message = "올바른 이메일 형식이 아닙니다")
        private String email;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "이메일 인증코드 발송 응답")
    public static class EmailVerificationResponse {

        @Schema(description = "성공 여부", example = "true")
        private boolean success;

        @Schema(description = "메시지", example = "인증코드가 발송되었습니다")
        private String message;
    }
}