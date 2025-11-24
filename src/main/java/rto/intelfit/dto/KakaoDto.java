package rto.intelfit.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

public class KakaoDto {

    // 카카오 사용자 정보
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class KakaoUserInfo {
        @JsonProperty("id")
        private Long id;

        @JsonProperty("kakao_account")
        private KakaoAccount kakaoAccount;

        @Data
        @NoArgsConstructor
        @AllArgsConstructor
        public static class KakaoAccount {
            @JsonProperty("email")
            private String email;

            @JsonProperty("profile")
            private Profile profile;

            @Data
            @NoArgsConstructor
            @AllArgsConstructor
            public static class Profile {
                @JsonProperty("nickname")
                private String nickname;

                @JsonProperty("profile_image_url")
                private String profileImageUrl;
            }
        }
    }

    // 카카오 토큰 응답
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class KakaoTokenResponse {
        @JsonProperty("access_token")
        private String accessToken;

        @JsonProperty("token_type")
        private String tokenType;

        @JsonProperty("refresh_token")
        private String refreshToken;

        @JsonProperty("expires_in")
        private Integer expiresIn;

        @JsonProperty("refresh_token_expires_in")
        private Integer refreshTokenExpiresIn;
    }

    // 프론트엔드 → 백엔드: 인가 코드 전달
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "카카오 로그인 요청 (인가 코드)")
    public static class KakaoLoginRequest {
        @Schema(description = "카카오 인가 코드", example = "abc123def456")
        @NotBlank(message = "인가 코드를 입력해주세요")
        private String code;
    }

    // 백엔드 → 프론트엔드: 로그인 응답
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "카카오 로그인 응답")
    public static class KakaoLoginResponse {
        @Schema(description = "성공 여부")
        private boolean success;

        @Schema(description = "메시지")
        private String message;

        @Schema(description = "사용자 ID")
        private Long userId;

        @Schema(description = "사용자명")
        private String name;

        @Schema(description = "IntelFit 액세스 토큰")
        private String accessToken;

        @Schema(description = "IntelFit 리프레시 토큰")
        private String refreshToken;

        @Schema(description = "토큰 타입")
        private String tokenType;

        @Schema(description = "만료 시간(초)")
        private long expiresIn;

        @Schema(description = "신규 가입 여부")
        private boolean isNewUser;
    }
}