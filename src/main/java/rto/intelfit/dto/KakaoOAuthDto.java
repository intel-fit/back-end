package rto.intelfit.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

public class KakaoOAuthDto {

    // ==================== 인가코드 요청 ====================
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "카카오 로그인 요청")
    public static class KakaoLoginRequest {
        @Schema(description = "카카오 인가 코드", example = "authorization_code_here")
        private String code;

        @Schema(description = "리다이렉트 URI (선택)", example = "exp://localhost:19002/--/auth/kakao")
        private String redirectUri;
    }

    // ==================== 카카오 토큰 응답 ====================
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

        private String scope;
    }

    // ==================== 카카오 사용자 정보 ====================
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class KakaoUserInfo {
        private Long id;

        @JsonProperty("connected_at")
        private String connectedAt;

        @JsonProperty("kakao_account")
        private KakaoAccount kakaoAccount;

        private Properties properties;

        @Data
        @NoArgsConstructor
        @AllArgsConstructor
        public static class KakaoAccount {
            private String email;

            @JsonProperty("email_needs_agreement")
            private Boolean emailNeedsAgreement;

            @JsonProperty("is_email_valid")
            private Boolean isEmailValid;

            @JsonProperty("is_email_verified")
            private Boolean isEmailVerified;

            private Profile profile;

            @JsonProperty("profile_needs_agreement")
            private Boolean profileNeedsAgreement;

            private String gender;

            @JsonProperty("age_range")
            private String ageRange;

            private String birthday;

            @Data
            @NoArgsConstructor
            @AllArgsConstructor
            public static class Profile {
                private String nickname;

                @JsonProperty("thumbnail_image_url")
                private String thumbnailImageUrl;

                @JsonProperty("profile_image_url")
                private String profileImageUrl;

                @JsonProperty("is_default_image")
                private Boolean isDefaultImage;
            }
        }

        @Data
        @NoArgsConstructor
        @AllArgsConstructor
        public static class Properties {
            private String nickname;

            @JsonProperty("profile_image")
            private String profileImage;

            @JsonProperty("thumbnail_image")
            private String thumbnailImage;
        }
    }

    // ==================== 로그인 응답 ====================
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

        @Schema(description = "신규 회원 여부")
        private boolean isNewUser;

        @Schema(description = "Access Token")
        private String accessToken;

        @Schema(description = "Refresh Token")
        private String refreshToken;

        @Schema(description = "사용자 정보")
        private UserInfo user;

        @Data
        @Builder
        @NoArgsConstructor
        @AllArgsConstructor
        public static class UserInfo {
            private Long id;
            private String email;
            private String nickname;
            private String profileImage;
        }
    }
}