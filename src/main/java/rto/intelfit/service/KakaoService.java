package rto.intelfit.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import rto.intelfit.domain.User;
import rto.intelfit.dto.KakaoDto;
import rto.intelfit.exception.BusinessException;
import rto.intelfit.exception.ErrorCode;
import rto.intelfit.repository.UserRepository;
import rto.intelfit.util.JwtUtil;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class KakaoService {

    private final UserRepository userRepository;
    private final JwtUtil jwtUtil;
    private final WebClient webClient;

    @Value("${kakao.rest-api-key}")
    private String kakaoRestApiKey;

    @Value("${kakao.redirect-uri}")
    private String kakaoRedirectUri;

    @Value("${kakao.token-url}")
    private String kakaoTokenUrl;

    @Value("${kakao.user-info-url}")
    private String kakaoUserInfoUrl;

    @Value("${jwt.access-token-expiration}")
    private long accessTokenExpiration;

    /**
     * 1단계: 인가 코드로 카카오 액세스 토큰 발급
     */
    public KakaoDto.KakaoTokenResponse getKakaoToken(String code) {
        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add("grant_type", "authorization_code");
        params.add("client_id", kakaoRestApiKey);
        params.add("redirect_uri", kakaoRedirectUri);
        params.add("code", code);

        try {
            return webClient.post()
                    .uri(kakaoTokenUrl)
                    .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_FORM_URLENCODED_VALUE)
                    .bodyValue(params)
                    .retrieve()
                    .bodyToMono(KakaoDto.KakaoTokenResponse.class)
                    .block();
        } catch (WebClientResponseException e) {
            log.error("카카오 토큰 발급 실패: {}", e.getMessage());
            throw new BusinessException(ErrorCode.EXTERNAL_API_ERROR, "카카오 토큰 발급에 실패했습니다");
        }
    }

    /**
     * 2단계: 액세스 토큰으로 카카오 사용자 정보 조회
     */
    public KakaoDto.KakaoUserInfo getKakaoUserInfo(String accessToken) {
        try {
            return webClient.get()
                    .uri(kakaoUserInfoUrl)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                    .retrieve()
                    .bodyToMono(KakaoDto.KakaoUserInfo.class)
                    .block();
        } catch (WebClientResponseException e) {
            log.error("카카오 사용자 정보 조회 실패: {}", e.getMessage());
            throw new BusinessException(ErrorCode.EXTERNAL_API_ERROR, "카카오 사용자 정보를 가져올 수 없습니다");
        }
    }

    /**
     * 3단계: 카카오 로그인 처리 (User 테이블에 직접 저장)
     */
    @Transactional
    public KakaoDto.KakaoLoginResponse kakaoLogin(String code) {
        // 1. 카카오 토큰 발급
        KakaoDto.KakaoTokenResponse tokenResponse = getKakaoToken(code);
        String kakaoAccessToken = tokenResponse.getAccessToken();

        // 2. 사용자 정보 조회
        KakaoDto.KakaoUserInfo kakaoUserInfo = getKakaoUserInfo(kakaoAccessToken);

        long kakaoId = kakaoUserInfo.getId();
        KakaoDto.KakaoUserInfo.KakaoAccount account = kakaoUserInfo.getKakaoAccount();
        String email = account.getEmail();

        // 기존 유저 조회 (socialId 기준)
        User existingUser = userRepository.findBySocialId(String.valueOf(kakaoId)).orElse(null);

        boolean isNewUser = false;
        User user;

        if (existingUser == null) {
            // 이메일 기반으로 기존 LOCAL 사용자 있는지 체크
            user = userRepository.findByEmail(email).orElse(null);

            if (user == null) {
                // 완전 신규
                user = createKakaoUser(kakaoUserInfo);
                isNewUser = true;
                log.info("카카오 신규 회원가입: email={}, kakaoId={}", email, kakaoId);
            } else {
                // 기존 로컬 사용자 → 카카오 연결
                connectKakaoAccount(user, kakaoUserInfo);
                log.info("기존 로컬 회원 → 카카오 연결 완료: email={}", email);
            }
        } else {
            // 기존 카카오 회원
            user = existingUser;
            updateKakaoProfile(user, kakaoUserInfo);
            log.info("기존 카카오 회원 로그인: kakaoId={}", kakaoId);
        }

        user.updateLastLoginAt();
        userRepository.save(user);

        // JWT 발급
        String accessToken = jwtUtil.generateAccessToken(user.getUserId(), user.getId());
        String refreshToken = jwtUtil.generateRefreshToken(user.getUserId(), user.getId());

        return KakaoDto.KakaoLoginResponse.builder()
                .success(true)
                .message(isNewUser ? "카카오 회원가입 완료" : "카카오 로그인 성공")
                .userId(user.getId())
                .name(user.getName())
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .tokenType("Bearer")
                .expiresIn(accessTokenExpiration / 1000)
                .isNewUser(isNewUser)
                .build();
    }

    /**
     * 카카오 신규 사용자 생성 (User 테이블)
     */
    private User createKakaoUser(KakaoDto.KakaoUserInfo info) {
        KakaoDto.KakaoUserInfo.KakaoAccount acc = info.getKakaoAccount();
        KakaoDto.KakaoUserInfo.KakaoAccount.Profile profile = acc.getProfile();

        String loginId = "kakao_" + System.currentTimeMillis();

        return userRepository.save(User.builder()
                .userId(loginId)
                .name(profile != null ? profile.getNickname() : "카카오사용자")
                .email(acc.getEmail())
                .socialProvider(User.SocialProvider.KAKAO)
                .socialId(String.valueOf(info.getId()))
                .profileImage(profile != null ? profile.getProfileImageUrl() : null)
                .birthDate(null)
                .emailVerified(true)
                .agreePrivacy(true)
                .agreeTerms(true)
                .password(null)
                .build());
    }

    /**
     * 기존 로컬 → 카카오 계정 연결
     */
    private void connectKakaoAccount(User user, KakaoDto.KakaoUserInfo info) {
        KakaoDto.KakaoUserInfo.KakaoAccount acc = info.getKakaoAccount();
        KakaoDto.KakaoUserInfo.KakaoAccount.Profile profile = acc.getProfile();

        user.setSocialProvider(User.SocialProvider.KAKAO);
        user.setSocialId(String.valueOf(info.getId()));
        user.setProfileImage(profile != null ? profile.getProfileImageUrl() : user.getProfileImage());
    }

    /**
     * 기존 카카오 사용자 프로필 업데이트
     */
    private void updateKakaoProfile(User user, KakaoDto.KakaoUserInfo info) {
        KakaoDto.KakaoUserInfo.KakaoAccount acc = info.getKakaoAccount();
        KakaoDto.KakaoUserInfo.KakaoAccount.Profile profile = acc.getProfile();

        user.setEmail(acc.getEmail());
        user.setProfileImage(profile != null ? profile.getProfileImageUrl() : null);
        user.setName(profile != null ? profile.getNickname() : user.getName());
    }
}
