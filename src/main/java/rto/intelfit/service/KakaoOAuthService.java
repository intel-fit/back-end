package rto.intelfit.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import rto.intelfit.config.KakaoOAuthConfig;
import rto.intelfit.domain.User;
import rto.intelfit.dto.KakaoOAuthDto;
import rto.intelfit.exception.BusinessException;
import rto.intelfit.exception.ErrorCode;
import rto.intelfit.repository.UserRepository;
import rto.intelfit.security.CustomUserPrincipal;
import rto.intelfit.util.JwtUtil;

import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class KakaoOAuthService {

    private final KakaoOAuthConfig kakaoConfig;
    private final UserRepository userRepository;
    private final JwtUtil jwtUtil;
    private final WebClient webClient = WebClient.builder().build();

    /**
     * 카카오 로그인 처리
     */
    @Transactional
    public KakaoOAuthDto.KakaoLoginResponse kakaoLogin(KakaoOAuthDto.KakaoLoginRequest request) {
        log.info("카카오 로그인 시작 - code: {}", request.getCode().substring(0, 10) + "...");

        // 1. 인가코드로 카카오 토큰 요청
        String redirectUri = request.getRedirectUri() != null
                ? request.getRedirectUri()
                : kakaoConfig.getRedirectUri();

        KakaoOAuthDto.KakaoTokenResponse tokenResponse = getKakaoToken(request.getCode(), redirectUri);
        log.info("카카오 토큰 발급 성공");

        // 2. 토큰으로 사용자 정보 조회
        KakaoOAuthDto.KakaoUserInfo userInfo = getKakaoUserInfo(tokenResponse.getAccessToken());
        log.info("카카오 사용자 정보 조회 성공 - kakaoId: {}", userInfo.getId());

        // 3. 사용자 조회 또는 생성
        String kakaoId = String.valueOf(userInfo.getId());
        Optional<User> existingUser = userRepository.findByKakaoId(kakaoId);

        boolean isNewUser = existingUser.isEmpty();
        User user;

        if (isNewUser) {
            user = createKakaoUser(userInfo);
            log.info("신규 카카오 사용자 생성 - userId: {}", user.getUserId());
        } else {
            user = existingUser.get();
            updateKakaoUserInfo(user, userInfo);
            log.info("기존 카카오 사용자 로그인 - userId: {}", user.getUserId());
        }

        // 4. 강제 로그아웃 플래그 해제
        jwtUtil.clearForceLogout(user.getUserId());

        // 5. JWT 토큰 발급
        String accessToken = jwtUtil.generateAccessToken(user.getUserId(), user.getId());
        String refreshToken = jwtUtil.generateRefreshToken(user.getUserId(), user.getId());

        // 6. 마지막 로그인 시간 업데이트
        user.updateLastLoginAt();

        return KakaoOAuthDto.KakaoLoginResponse.builder()
                .success(true)
                .message(isNewUser ? "카카오 회원가입이 완료되었습니다" : "카카오 로그인 성공")
                .isNewUser(isNewUser)
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .user(KakaoOAuthDto.KakaoLoginResponse.UserInfo.builder()
                        .id(user.getId())
                        .email(user.getEmail())
                        .nickname(user.getName())  // name 필드 사용
                        .profileImage(user.getProfileImageUrl())
                        .build())
                .build();
    }

    /**
     * 로그아웃 처리
     */
    public void logout(String token, String userId) {
        // Access Token 블랙리스트 등록
        jwtUtil.blacklistToken(token);

        // Refresh Token 삭제
        jwtUtil.deleteRefreshToken(userId);

        log.info("로그아웃 처리 완료 - userId: {}", userId);
    }

    /**
     * 카카오 연결 끊기 (회원 탈퇴)
     */
    @Transactional
    public void unlinkKakao(CustomUserPrincipal userPrincipal) {
        User user = userRepository.findByUserId(userPrincipal.getUserId())
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        if (user.getKakaoId() == null) {
            throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE, "카카오 연동 계정이 아닙니다");
        }

        // 카카오 연결 끊기 API 호출 (Admin Key 필요)
        if (kakaoConfig.getAdminKey() != null && !kakaoConfig.getAdminKey().isEmpty()) {
            try {
                webClient.post()
                        .uri(KakaoOAuthConfig.KAKAO_API_URL + "/v1/user/unlink")
                        .header(HttpHeaders.AUTHORIZATION, "KakaoAK " + kakaoConfig.getAdminKey())
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                        .body(BodyInserters.fromFormData("target_id_type", "user_id")
                                .with("target_id", user.getKakaoId()))
                        .retrieve()
                        .bodyToMono(String.class)
                        .block();

                log.info("카카오 연결 끊기 완료 - kakaoId: {}", user.getKakaoId());
            } catch (Exception e) {
                log.warn("카카오 연결 끊기 API 실패 (계속 진행): {}", e.getMessage());
            }
        }

        // 강제 로그아웃
        jwtUtil.forceLogoutUser(user.getUserId());
        jwtUtil.deleteRefreshToken(user.getUserId());

        // DB에서 카카오 정보 초기화
        user.setKakaoId(null);
        user.setProvider(User.Provider.LOCAL);
        userRepository.save(user);

        log.info("사용자 카카오 연동 해제 완료 - userId: {}", user.getUserId());
    }

    /**
     * 카카오 토큰 요청
     */
    private KakaoOAuthDto.KakaoTokenResponse getKakaoToken(String code, String redirectUri) {
        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add("grant_type", "authorization_code");
        params.add("client_id", kakaoConfig.getClientId());
        params.add("redirect_uri", redirectUri);
        params.add("code", code);

        if (kakaoConfig.getClientSecret() != null && !kakaoConfig.getClientSecret().isEmpty()) {
            params.add("client_secret", kakaoConfig.getClientSecret());
        }

        try {
            return webClient.post()
                    .uri(KakaoOAuthConfig.KAKAO_AUTH_URL + "/oauth/token")
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .body(BodyInserters.fromFormData(params))
                    .retrieve()
                    .bodyToMono(KakaoOAuthDto.KakaoTokenResponse.class)
                    .block();
        } catch (Exception e) {
            log.error("카카오 토큰 요청 실패: {}", e.getMessage());
            throw new BusinessException(ErrorCode.KAKAO_TOKEN_ERROR, "카카오 토큰 요청에 실패했습니다");
        }
    }

    /**
     * 카카오 사용자 정보 조회
     */
    private KakaoOAuthDto.KakaoUserInfo getKakaoUserInfo(String accessToken) {
        try {
            return webClient.get()
                    .uri(KakaoOAuthConfig.KAKAO_API_URL + "/v2/user/me")
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                    .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_FORM_URLENCODED_VALUE)
                    .retrieve()
                    .bodyToMono(KakaoOAuthDto.KakaoUserInfo.class)
                    .block();
        } catch (Exception e) {
            log.error("카카오 사용자 정보 조회 실패: {}", e.getMessage());
            throw new BusinessException(ErrorCode.KAKAO_USER_INFO_ERROR, "카카오 사용자 정보 조회에 실패했습니다");
        }
    }

    /**
     * 신규 카카오 사용자 생성
     */
    private User createKakaoUser(KakaoOAuthDto.KakaoUserInfo userInfo) {
        String email = null;
        String name = "카카오유저";
        String profileImage = null;

        if (userInfo.getKakaoAccount() != null) {
            email = userInfo.getKakaoAccount().getEmail();

            if (userInfo.getKakaoAccount().getProfile() != null) {
                name = userInfo.getKakaoAccount().getProfile().getNickname();
                profileImage = userInfo.getKakaoAccount().getProfile().getProfileImageUrl();
            }
        }

        if (userInfo.getProperties() != null) {
            if (name.equals("카카오유저") && userInfo.getProperties().getNickname() != null) {
                name = userInfo.getProperties().getNickname();
            }
            if (profileImage == null) {
                profileImage = userInfo.getProperties().getProfileImage();
            }
        }

        // 이메일이 없으면 기본값 생성
        if (email == null || email.isEmpty()) {
            email = "kakao_" + userInfo.getId() + "@kakao.local";
        }

        // userId 생성 (카카오 ID 기반)
        String generatedUserId = "kakao_" + userInfo.getId();

        User newUser = User.builder()
                .userId(generatedUserId)
                .email(email)
                .password(UUID.randomUUID().toString())
                .name(name)
                .kakaoId(String.valueOf(userInfo.getId()))
                .profileImageUrl(profileImage)
                .provider(User.Provider.KAKAO)
                .birthDate(LocalDate.of(2000, 1, 1))  // 기본값
                .gender(User.Gender.M)  // 기본값
                .agreePrivacy(true)
                .agreeTerms(true)
                .build();

        return userRepository.save(newUser);
    }

    /**
     * 기존 카카오 사용자 정보 업데이트
     */
    private void updateKakaoUserInfo(User user, KakaoOAuthDto.KakaoUserInfo userInfo) {
        if (userInfo.getKakaoAccount() != null &&
                userInfo.getKakaoAccount().getProfile() != null) {

            String newProfileImage = userInfo.getKakaoAccount().getProfile().getProfileImageUrl();
            if (newProfileImage != null) {
                user.setProfileImageUrl(newProfileImage);
            }
        }
        userRepository.save(user);
    }
}