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
import rto.intelfit.domain.SocialAccount;
import rto.intelfit.domain.User;
import rto.intelfit.dto.KakaoDto;
import rto.intelfit.exception.BusinessException;
import rto.intelfit.exception.ErrorCode;
import rto.intelfit.repository.SocialAccountRepository;
import rto.intelfit.repository.UserRepository;
import rto.intelfit.util.JwtUtil;

import java.time.LocalDate;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class KakaoService {

    private final UserRepository userRepository;
    private final SocialAccountRepository socialAccountRepository;
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
     * 2단계: 카카오 액세스 토큰으로 사용자 정보 조회
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
     * 3단계: 카카오 로그인 처리 (전체 흐름)
     */
    @Transactional
    public KakaoDto.KakaoLoginResponse kakaoLogin(String code) {
        // 1. 인가 코드로 카카오 액세스 토큰 발급
        KakaoDto.KakaoTokenResponse tokenResponse = getKakaoToken(code);
        String kakaoAccessToken = tokenResponse.getAccessToken();

        // 2. 카카오 액세스 토큰으로 사용자 정보 조회
        KakaoDto.KakaoUserInfo kakaoUserInfo = getKakaoUserInfo(kakaoAccessToken);
        String providerId = String.valueOf(kakaoUserInfo.getId());
        KakaoDto.KakaoUserInfo.KakaoAccount kakaoAccount = kakaoUserInfo.getKakaoAccount();

        if (kakaoAccount == null || kakaoAccount.getEmail() == null) {
            throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE,
                    "카카오 계정에서 이메일 정보를 가져올 수 없습니다. 카카오 계정에 이메일을 등록해주세요.");
        }

        // 3. 기존 소셜 계정 확인
        SocialAccount socialAccount = socialAccountRepository
                .findByProviderAndProviderId(SocialAccount.Provider.KAKAO, providerId)
                .orElse(null);

        User user;
        boolean isNewUser = false;

        if (socialAccount == null) {
            // 신규 사용자
            user = userRepository.findByEmail(kakaoAccount.getEmail()).orElse(null);

            if (user != null) {
                // 기존 일반 회원 - 소셜 계정만 연결
                socialAccount = createSocialAccount(user, kakaoUserInfo);
                log.info("기존 회원에 카카오 계정 연결 - 사용자 ID: {}, 이메일: {}",
                        user.getUserId(), kakaoAccount.getEmail());
            } else {
                // 완전 신규 회원
                user = createUserForKakao(kakaoUserInfo);
                socialAccount = createSocialAccount(user, kakaoUserInfo);
                isNewUser = true;
                log.info("카카오 신규 회원가입 완료 - 카카오 ID: {}, 이메일: {}",
                        providerId, kakaoAccount.getEmail());
            }
        } else {
            // 기존 사용자
            user = socialAccount.getUser();
            updateSocialAccount(socialAccount, kakaoUserInfo);
            log.info("카카오 기존 회원 로그인 - 사용자 ID: {}, 카카오 ID: {}",
                    user.getUserId(), providerId);
        }

        // 4. 마지막 로그인 시간 업데이트
        user.updateLastLoginAt();
        userRepository.save(user);

        // 5. IntelFit JWT 토큰 생성
        String accessToken = jwtUtil.generateAccessToken(user.getUserId(), user.getId());
        String refreshToken = jwtUtil.generateRefreshToken(user.getUserId(), user.getId());

        return KakaoDto.KakaoLoginResponse.builder()
                .success(true)
                .message(isNewUser ? "카카오 회원가입이 완료되었습니다" : "카카오 로그인이 완료되었습니다")
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
     * 카카오 신규 사용자 생성
     */
    private User createUserForKakao(KakaoDto.KakaoUserInfo kakaoUserInfo) {
        KakaoDto.KakaoUserInfo.KakaoAccount kakaoAccount = kakaoUserInfo.getKakaoAccount();
        KakaoDto.KakaoUserInfo.KakaoAccount.Profile profile = kakaoAccount.getProfile();

        String userId = "kakao_" + System.currentTimeMillis() + "_" +
                UUID.randomUUID().toString().substring(0, 8);

        return userRepository.save(User.builder()
                .userId(userId)
                .name(profile != null ? profile.getNickname() : "카카오사용자")
                .email(kakaoAccount.getEmail())
                .password(UUID.randomUUID().toString())
                .birthDate(LocalDate.of(2000, 1, 1))
                .emailVerified(true)
                .agreePrivacy(true)
                .agreeTerms(true)
                .agreedAt(java.time.LocalDateTime.now())
                .build());
    }

    /**
     * 소셜 계정 생성
     */
    private SocialAccount createSocialAccount(User user, KakaoDto.KakaoUserInfo kakaoUserInfo) {
        KakaoDto.KakaoUserInfo.KakaoAccount kakaoAccount = kakaoUserInfo.getKakaoAccount();
        KakaoDto.KakaoUserInfo.KakaoAccount.Profile profile = kakaoAccount.getProfile();

        return socialAccountRepository.save(SocialAccount.builder()
                .user(user)
                .provider(SocialAccount.Provider.KAKAO)
                .providerId(String.valueOf(kakaoUserInfo.getId()))
                .email(kakaoAccount.getEmail())
                .nickname(profile != null ? profile.getNickname() : null)
                .profileImageUrl(profile != null ? profile.getProfileImageUrl() : null)
                .build());
    }

    /**
     * 소셜 계정 정보 업데이트
     */
    private void updateSocialAccount(SocialAccount socialAccount, KakaoDto.KakaoUserInfo kakaoUserInfo) {
        KakaoDto.KakaoUserInfo.KakaoAccount kakaoAccount = kakaoUserInfo.getKakaoAccount();
        KakaoDto.KakaoUserInfo.KakaoAccount.Profile profile = kakaoAccount.getProfile();

        socialAccount.updateProfile(
                kakaoAccount.getEmail(),
                profile != null ? profile.getProfileImageUrl() : null,
                profile != null ? profile.getNickname() : null
        );
        socialAccountRepository.save(socialAccount);
    }
}