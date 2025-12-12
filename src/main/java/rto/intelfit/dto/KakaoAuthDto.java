package rto.intelfit.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

public class KakaoAuthDto {

    // ==================== Request ====================

    @Getter
    @NoArgsConstructor
    @AllArgsConstructor  // 추가!
    public static class LoginRequest {
        private String code;

        public static LoginRequest of(String code) {
            return new LoginRequest(code);
        }
    }

    // ==================== Response ====================

    @Getter
    @Builder
    public static class LoginResponse {
        private String accessToken;
        private String refreshToken;
        private Long userId;
        private String nickname;
        private String profileImageUrl;
        private boolean isNewUser;
        private boolean isOnboarded;
    }

    @Getter
    @Builder
    public static class LoginUrlResponse {
        private String url;
    }

    @Getter
    @Builder
    public static class MessageResponse {
        private String message;

        public static MessageResponse of(String message) {
            return MessageResponse.builder().message(message).build();
        }
    }

    // ==================== 카카오 API 응답 ====================

    @Getter
    @NoArgsConstructor
    public static class KakaoTokenResponse {

        @JsonProperty("access_token")
        private String accessToken;

        @JsonProperty("refresh_token")
        private String refreshToken;

        @JsonProperty("token_type")
        private String tokenType;

        @JsonProperty("expires_in")
        private Integer expiresIn;

        @JsonProperty("refresh_token_expires_in")
        private Integer refreshTokenExpiresIn;
    }

    @Getter
    @NoArgsConstructor
    public static class KakaoUserInfo {

        private Long id;

        @JsonProperty("kakao_account")
        private KakaoAccount kakaoAccount;

        @Getter
        @NoArgsConstructor
        public static class KakaoAccount {
            private String email;

            @JsonProperty("is_email_valid")
            private Boolean isEmailValid;

            @JsonProperty("is_email_verified")
            private Boolean isEmailVerified;

            private Profile profile;

            @Getter
            @NoArgsConstructor
            public static class Profile {
                private String nickname;

                @JsonProperty("profile_image_url")
                private String profileImageUrl;

                @JsonProperty("thumbnail_image_url")
                private String thumbnailImageUrl;
            }
        }

        public String getEmail() {
            return kakaoAccount != null ? kakaoAccount.getEmail() : null;
        }

        public String getNickname() {
            if (kakaoAccount != null && kakaoAccount.getProfile() != null) {
                return kakaoAccount.getProfile().getNickname();
            }
            return null;
        }

        public String getProfileImageUrl() {
            if (kakaoAccount != null && kakaoAccount.getProfile() != null) {
                return kakaoAccount.getProfile().getProfileImageUrl();
            }
            return null;
        }
    }
}