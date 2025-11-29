package rto.intelfit.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import rto.intelfit.domain.User;
import rto.intelfit.validation.ValidEmailDomain;

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

        @Schema(description = "이메일 (Gmail 또는 Naver만 가능)", example = "lkmm1108@gmail.com") // ✅ 수정
        @NotBlank(message = "이메일을 입력해주세요")
        @Email(message = "올바른 이메일 형식이 아닙니다")
        @Size(max = 50, message = "이메일은 50자 이내로 입력해주세요")
        @ValidEmailDomain(message = "Gmail 또는 Naver 이메일만 사용 가능합니다") // ✅ 추가
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


        @Schema(description = "개인정보 처리방침 동의", example = "true")
        @NotNull(message = "개인정보 처리방침에 동의해주세요")
        @AssertTrue(message = "개인정보 처리방침에 동의해야 합니다")
        private Boolean agreePrivacy;

        @Schema(description = "서비스 이용약관 동의", example = "true")
        @NotNull(message = "서비스 이용약관에 동의해주세요")
        @AssertTrue(message = "서비스 이용약관에 동의해야 합니다")
        private Boolean agreeTerms;

        @Schema(description = "이메일 인증코드", example = "123456")
        @NotBlank(message = "이메일 인증코드를 입력해주세요")
        @Pattern(regexp = "^\\d{6}$", message = "인증코드는 6자리 숫자여야 합니다")
        private String verificationCode;

        // 초기 피트니스 설정 필드 추가
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

        @Schema(description = "운동 목표", example = "MAINTENANCE", allowableValues = {"DIET", "BULK", "LEAN_MASS", "MAINTENANCE"})
        @NotNull(message = "운동 목표를 선택해주세요")
        private User.HealthGoal healthGoal = User.HealthGoal.MAINTENANCE;  // 기본값 지정


        @Schema(description = "주간 운동 일수", example = "3-4일")
        @Size(max = 20, message = "주간 운동 일수는 20자 이내로 입력해주세요")
        private String workoutDaysPerWeek;

        @Schema(description = "헬스 고민 (쉼표로 구분된 문자열)", 
                example = "의지 부족,루틴 짜기 어려움",
                allowableValues = {"의지 부족", "근육의 자극", "루틴 짜기 어려움", "올바른 운동 자세", "식단 관리", "기타"})
        @Size(max = 500, message = "헬스 고민은 500자 이내로 입력해주세요")
        private String fitnessConcerns;

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

        @Schema(description = "이메일 (Gmail 또는 Naver만 가능)", example = "lkmm1108@gmail.com") // ✅ 수정
        @NotBlank(message = "이메일을 입력해주세요")
        @Email(message = "올바른 이메일 형식이 아닙니다")
        @ValidEmailDomain(message = "Gmail 또는 Naver 이메일만 사용 가능합니다") // ✅ 추가
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