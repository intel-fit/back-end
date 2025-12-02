package rto.intelfit.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import rto.intelfit.domain.User;
import rto.intelfit.dto.LoginDto;
import rto.intelfit.repository.UserRepository;
import rto.intelfit.util.JwtUtil;

import java.time.LocalDate;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class KakaoAuthService {

    private final WebClient webClient;
    private final UserRepository userRepository;
    private final JwtUtil jwtUtil;

    @Value("${kakao.rest-api-key}")
    private String kakaoClientId;

    @Value("${kakao.client-secret:}")
    private String kakaoClientSecret;

    @Value("${kakao.redirect-uri}")
    private String kakaoRedirectUri;

    @Value("${kakao.token-url}")
    private String kakaoTokenUrl;

    @Value("${kakao.user-info-url}")
    private String kakaoUserInfoUrl;

    @Value("${jwt.access-token-expiration:3600000}")
    private long accessTokenExpiration;

    @Value("${kakao.admin-key}")
    private String kakaoAdminKey;

    @Transactional
    public LoginDto.Response loginWithKakao(String code) {

        // 1. Authorization Code → Access Token
        Map<String, Object> tokenResponse = webClient.post()
                .uri(kakaoTokenUrl)
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(
                        BodyInserters.fromFormData("grant_type", "authorization_code")
                                .with("client_id", kakaoClientId)
                                .with("redirect_uri", kakaoRedirectUri)
                                .with("code", code)
                                .with("client_secret", kakaoClientSecret)
                )
                .retrieve()
                .bodyToMono(new ParameterizedTypeReference<Map<String, Object>>() {})
                .block();

        if (tokenResponse == null || !tokenResponse.containsKey("access_token")) {
            log.error("카카오 토큰 발급 실패: {}", tokenResponse);
            throw new RuntimeException("카카오 토큰 발급 실패");
        }

        String accessToken = (String) tokenResponse.get("access_token");

        // 2. Access Token → 사용자 정보 조회
        Map<String, Object> userInfo = webClient.get()
                .uri(kakaoUserInfoUrl)
                .headers(h -> h.setBearerAuth(accessToken))
                .retrieve()
                .bodyToMono(new ParameterizedTypeReference<Map<String, Object>>() {})
                .block();

        if (userInfo == null || !userInfo.containsKey("id")) {
            throw new RuntimeException("카카오 사용자 정보 조회 실패");
        }

        Long kakaoId = ((Number) userInfo.get("id")).longValue();
        String socialId = String.valueOf(kakaoId);

        Map<String, Object> account = (Map<String, Object>) userInfo.get("kakao_account");
        Map<String, Object> profile = account != null ? (Map<String, Object>) account.get("profile") : null;

        String email = account != null ? (String) account.get("email") : null;
        String nickname = profile != null ? (String) profile.get("nickname") : null;
        String profileImage = profile != null ? (String) profile.get("profile_image_url") : null;

        // 3. DB 조회 or 회원가입
        Optional<User> existingUserOpt = userRepository.findBySocialId(socialId);
        User user;

        if (existingUserOpt.isPresent()) {
            user = existingUserOpt.get();
        } else {
            // 신규 유저 생성
            String generatedUserId = socialId;  // 카카오 ID 그대로 userId 사용
            String finalEmail = (email != null) ? email : generatedUserId + "@kakao-user.com";

            user = User.builder()
                    .userId(generatedUserId)
                    .name(nickname != null ? nickname : "카카오사용자")
                    .email(finalEmail)
                    .password(socialId)
                    .socialProvider(User.SocialProvider.KAKAO)
                    .socialId(socialId)
                    .profileImage(profileImage)
                    .birthDate(LocalDate.of(2000, 1, 1))
                    .build();

            user = userRepository.save(user);
        }

        user.updateLastLoginAt();
        userRepository.save(user);

        // 4. JWT 발급
        String jwtAccess = jwtUtil.generateAccessToken(user.getUserId(), user.getId());
        String jwtRefresh = jwtUtil.generateRefreshToken(user.getUserId(), user.getId());
        log.info("JWT ACCESS TOKEN = {}", jwtAccess);
        log.info("JWT REFRESH TOKEN = {}", jwtRefresh);

        return LoginDto.Response.builder()
                .success(true)
                .message("카카오 로그인 성공")
                .userId(user.getId())
                .name(user.getName())
                .accessToken(jwtAccess)
                .refreshToken(jwtRefresh)
                .tokenType("Bearer")
                .expiresIn(accessTokenExpiration / 1000)
                .build();
    }
    @Transactional
    public void unlinkKakaoUser(String socialId) {

        String unlinkUrl = "https://kapi.kakao.com/v1/user/unlink";

        try {
            Map<String, Object> response = webClient.post()
                    .uri(unlinkUrl)
                    .header("Authorization", "KakaoAK " + kakaoAdminKey) // ← Admin Key 방식
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .body(
                            BodyInserters.fromFormData("target_id_type", "user_id")
                                    .with("target_id", socialId)
                    )
                    .retrieve()
                    .bodyToMono(new ParameterizedTypeReference<Map<String, Object>>() {})
                    .block();

            log.info("카카오 unlink 성공 → {}", response);

        } catch (Exception e) {
            log.error("카카오 unlink 실패: {}", e.getMessage(), e);
            // 실패하더라도 사용자 탈퇴는 계속 진행되게 하고 싶으면 예외는 던지지 않기
        }
    }



}
