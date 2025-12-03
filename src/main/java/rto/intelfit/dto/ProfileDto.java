package rto.intelfit.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import rto.intelfit.domain.User;

import jakarta.validation.constraints.*;
import java.time.LocalDate;
import java.time.LocalDateTime;

public class ProfileDto {

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "프로필 조회 응답")
    public static class ProfileResponse {

        @Schema(description = "사용자 ID", example = "1")
        private Long id;

        @Schema(description = "사용자 계정 ID", example = "lkmm1108")
        private String userId;

        @Schema(description = "이름", example = "홍길동")
        private String name;

        @Schema(description = "이메일", example = "lkmm1108@gmail.com")
        private String email;

        @Schema(description = "생년월일", example = "1995-01-01")
        @JsonFormat(pattern = "yyyy-MM-dd")
        private LocalDate birthDate;

        @Schema(description = "전화번호", example = "01028147460")
        private String phoneNumber;

        @Schema(description = "키 (cm)", example = "175")
        private Integer height;

        @Schema(description = "현재 체중 (kg)", example = "70")
        private Integer weight;

        @Schema(description = "성별", example = "M")
        private User.Gender gender;

        @Schema(description = "멤버십 유형", example = "FREE")
        private User.MembershipType membershipType;

        @Schema(description = "운동 목표", example = "LEAN_MASS")
        private User.HealthGoal healthGoal;

        @Schema(description = "주간 운동 일수", example = "3-4일")
        private String workoutDaysPerWeek;

        @Schema(description = "목표 체중 (kg)", example = "70")
        private Integer weightGoal;

        @Schema(description = "마지막 로그인 시간", example = "2024-01-15T14:30:00")
        @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
        private LocalDateTime lastLoginAt;

        @Schema(description = "계정 생성 시간", example = "2024-01-01T10:00:00")
        @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
        private LocalDateTime createdAt;

        public static ProfileResponse from(User user) {
            return ProfileResponse.builder()
                    .id(user.getId())
                    .userId(user.getUserId())
                    .name(user.getName())
                    .email(user.getEmail())
                    .birthDate(user.getBirthDate())
                    .height(user.getHeight())
                    .weight(user.getWeight())
                    .gender(user.getGender())
                    .membershipType(user.getMembershipType())
                    .healthGoal(user.getHealthGoal())
                    .workoutDaysPerWeek(user.getWorkoutDaysPerWeek())
                    .weightGoal(user.getWeightGoal())
                    .lastLoginAt(user.getLastLoginAt())
                    .createdAt(user.getCreatedAt())
                    .build();
        }
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "프로필 수정 요청")
    public static class ProfileUpdateRequest {

        @Schema(description = "이름", example = "홍길동")
        @Size(max = 50, message = "이름은 50자 이내로 입력해주세요")
        private String name;

        @Schema(description = "전화번호", example = "01028147460")
        @Pattern(regexp = "^\\d{10,11}$", message = "올바른 전화번호 형식이 아닙니다")
        private String phoneNumber;

        @Schema(description = "키 (cm)", example = "175")
        @Min(value = 100, message = "키는 100cm 이상이어야 합니다")
        @Max(value = 250, message = "키는 250cm 이하여야 합니다")
        private Integer height;

        @Schema(description = "현재 체중 (kg)", example = "70")
        @Min(value = 30, message = "체중은 30kg 이상이어야 합니다")
        @Max(value = 200, message = "체중은 200kg 이하여야 합니다")
        private Integer weight;

        @Schema(description = "성별", example = "M")
        private User.Gender gender;

        @Schema(description = "운동 목표", example = "LEAN_MASS")
        private User.HealthGoal healthGoal;

        @Schema(description = "주간 운동 일수", example = "3-4일")
        @Size(max = 20, message = "주간 운동 일수는 20자 이내로 입력해주세요")
        private String workoutDaysPerWeek;

        @Schema(description = "목표 체중 (kg)", example = "70")
        @Min(value = 30, message = "목표 체중은 30kg 이상이어야 합니다")
        @Max(value = 200, message = "목표 체중은 200kg 이하여야 합니다")
        private Integer weightGoal;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "프로필 수정 응답")
    public static class ProfileUpdateResponse {

        @Schema(description = "성공 여부", example = "true")
        private boolean success;

        @Schema(description = "메시지", example = "프로필이 수정되었습니다")
        private String message;

        @Schema(description = "수정된 프로필 정보")
        private ProfileResponse profile;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "비밀번호 변경 요청 (로그인된 사용자)")
    public static class PasswordUpdateRequest {

        @Schema(description = "현재 비밀번호", example = "currentPassword123!")
        @NotBlank(message = "현재 비밀번호를 입력해주세요")
        private String currentPassword;

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
    public static class PasswordUpdateResponse {

        @Schema(description = "성공 여부", example = "true")
        private boolean success;

        @Schema(description = "메시지", example = "비밀번호가 변경되었습니다")
        private String message;
    }


}