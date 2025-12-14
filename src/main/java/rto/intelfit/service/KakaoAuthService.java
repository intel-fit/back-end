package rto.intelfit.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;
import rto.intelfit.domain.User;
import rto.intelfit.dto.KakaoAuthDto;
import rto.intelfit.exception.BusinessException;
import rto.intelfit.exception.ErrorCode;
import rto.intelfit.repository.*;
import rto.intelfit.util.JwtUtil;

import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class KakaoAuthService {

    private final UserRepository userRepository;
    private final JwtUtil jwtUtil;
    private final RestTemplate restTemplate;


    @Value("${kakao.client-id}")
    private String clientId;

    @Value("${kakao.client-secret:}")
    private String clientSecret;

    @Value("${kakao.redirect-uri}")
    private String redirectUri;

    private static final String KAKAO_TOKEN_URL = "https://kauth.kakao.com/oauth/token";
    private static final String KAKAO_USER_INFO_URL = "https://kapi.kakao.com/v2/user/me";
    private static final String KAKAO_LOGOUT_URL = "https://kapi.kakao.com/v1/user/logout";
    private static final String KAKAO_UNLINK_URL = "https://kapi.kakao.com/v1/user/unlink";


    private final InBodyRepository inBodyRepository;
    private final UserFoodPreferenceRepository userFoodPreferenceRepository;
    private final DailyNutritionGoalRepository dailyNutritionGoalRepository;
    private final MealRepository mealRepository;
    private final RecommendedMealPlanRepository recommendedMealPlanRepository;
    private final ExerciseRepository exerciseRepository;
    private final RecommendedExercisePlanRepository recommendedExercisePlanRepository;
    private final UserBadgeRepository userBadgeRepository;
    private final PaymentHistoryRepository paymentHistoryRepository;
    private final SubscriptionRepository subscriptionRepository;
    private final AIChatMessageRepository aiChatMessageRepository;


    /**
     * 카카오 로그인 URL 반환
     */
    public KakaoAuthDto.LoginUrlResponse getLoginUrl() {
        String url = "https://kauth.kakao.com/oauth/authorize"
                + "?client_id=" + clientId
                + "&redirect_uri=" + redirectUri
                + "&response_type=code"
                + "&scope=profile_nickname,profile_image";

        return KakaoAuthDto.LoginUrlResponse.builder()
                .url(url)
                .build();
    }

    /**
     * 카카오 로그인 처리
     */
    public KakaoAuthDto.LoginResponse login(KakaoAuthDto.LoginRequest request) {
        // 1. 인가 코드로 카카오 토큰 받기
        KakaoAuthDto.KakaoTokenResponse kakaoToken = getKakaoToken(request.getCode());
        log.info("카카오 토큰 발급 성공");

        // 2. 카카오 토큰으로 사용자 정보 조회
        KakaoAuthDto.KakaoUserInfo userInfo = getKakaoUserInfo(kakaoToken.getAccessToken());
        log.info("카카오 사용자 정보 조회 - ID: {}, 닉네임: {}", userInfo.getId(), userInfo.getNickname());

        // 3. 기존 회원 확인 또는 신규 생성
        boolean isNewUser = false;
        User user = userRepository.findByLoginTypeAndSocialId(User.SocialProvider.KAKAO, String.valueOf(userInfo.getId()))
                .orElse(null);

        if (user == null) {
            // 이메일 중복 확인
            if (userInfo.getEmail() != null && userRepository.existsByEmail(userInfo.getEmail())) {
                throw new IllegalStateException("이미 일반 회원으로 가입된 이메일입니다.");
            }

            user = createKakaoUser(userInfo);
            isNewUser = true;
            log.info("신규 카카오 사용자 생성 - userId: {}", user.getUserId());
        }

        // 4. 카카오 토큰 저장 및 로그인 시간 갱신
        user.updateKakaoAccessToken(kakaoToken.getAccessToken());
        user.updateLastLoginAt();
        userRepository.save(user);

        // 5. JWT 발급
        String accessToken = jwtUtil.generateAccessToken(user.getUserId(), user.getId());
        String refreshToken = jwtUtil.generateRefreshToken(user.getUserId(), user.getId());

        return KakaoAuthDto.LoginResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .userId(user.getId())
                .membershipType(user.getMembershipType())
                .nickname(user.getName())
                .profileImageUrl(user.getProfileImageUrl())
                .isNewUser(isNewUser)
                .isOnboarded(user.getIsOnboarded())
                .build();
    }

    public KakaoAuthDto.MessageResponse logout(String userId) {
        User user = userRepository.findByUserId(userId)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));

        // 1. 카카오 로그아웃 API 호출
        if (user.getKakaoAccessToken() != null) {
            try {
                HttpHeaders headers = new HttpHeaders();
                headers.setBearerAuth(user.getKakaoAccessToken());
                HttpEntity<Void> request = new HttpEntity<>(headers);
                restTemplate.postForEntity(KAKAO_LOGOUT_URL, request, Map.class);
            } catch (Exception e) {
                log.warn("카카오 로그아웃 API 실패 (무시): {}", e.getMessage());
            }
        }

        // 2. 서비스 토큰 정리
        user.clearKakaoToken();
        jwtUtil.deleteRefreshToken(userId);
        userRepository.save(user);

        return KakaoAuthDto.MessageResponse.of("로그아웃 되었습니다.");
    }



    @Transactional
    public KakaoAuthDto.MessageResponse withdrawKakaoUser(String userId) {

        User user = userRepository.findByUserId(userId)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));

        if (user.getLoginType() != User.SocialProvider.KAKAO) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST);
        }

        // 1. 카카오 unlink
        if (user.getKakaoAccessToken() != null) {
            try {
                HttpHeaders headers = new HttpHeaders();
                headers.setBearerAuth(user.getKakaoAccessToken());
                restTemplate.postForEntity(
                        KAKAO_UNLINK_URL,
                        new HttpEntity<>(headers),
                        Map.class
                );
            } catch (Exception e) {
                log.warn("카카오 unlink 실패 (무시): {}", e.getMessage());
            }
        }

        // 2. 토큰 정리
        jwtUtil.deleteRefreshToken(user.getUserId());

        // ✅ 3. 유저만 삭제 (DB가 전부 CASCADE)
        userRepository.delete(user);

        return KakaoAuthDto.MessageResponse.of("카카오 회원 탈퇴가 완료되었습니다.");
    }







    // ==================== Private Methods ====================

    private KakaoAuthDto.KakaoTokenResponse getKakaoToken(String code) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add("grant_type", "authorization_code");
        params.add("client_id", clientId);
        params.add("redirect_uri", redirectUri);
        params.add("code", code);

        if (clientSecret != null && !clientSecret.isEmpty()) {
            params.add("client_secret", clientSecret);
        }

        HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(params, headers);

        try {
            ResponseEntity<KakaoAuthDto.KakaoTokenResponse> response = restTemplate.postForEntity(
                    KAKAO_TOKEN_URL, request, KakaoAuthDto.KakaoTokenResponse.class);
            return response.getBody();
        } catch (Exception e) {
            log.error("카카오 토큰 발급 실패: {}", e.getMessage());
            throw new RuntimeException("카카오 토큰 발급에 실패했습니다.", e);
        }
    }

    private KakaoAuthDto.KakaoUserInfo getKakaoUserInfo(String accessToken) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(accessToken);
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        HttpEntity<Void> request = new HttpEntity<>(headers);

        try {
            ResponseEntity<KakaoAuthDto.KakaoUserInfo> response = restTemplate.exchange(
                    KAKAO_USER_INFO_URL, HttpMethod.GET, request, KakaoAuthDto.KakaoUserInfo.class);
            return response.getBody();
        } catch (Exception e) {
            log.error("카카오 사용자 정보 조회 실패: {}", e.getMessage());
            throw new RuntimeException("카카오 사용자 정보 조회에 실패했습니다.", e);
        }
    }

    private User createKakaoUser(KakaoAuthDto.KakaoUserInfo userInfo) {
        String uniqueUserId = "kakao_" + userInfo.getId();

        return userRepository.save(User.builder()
                .userId(uniqueUserId)
                .name(userInfo.getNickname() != null ? userInfo.getNickname() : "카카오 사용자")
                .email(userInfo.getEmail() != null ? userInfo.getEmail() : uniqueUserId + "@kakao.user")
                .emailVerified(true)
                .loginType(User.SocialProvider.KAKAO)
                .socialId(String.valueOf(userInfo.getId()))
                .profileImageUrl(userInfo.getProfileImageUrl())
                .birthDate(null)
                .isOnboarded(false)
                .password("{noop}SOCIAL_LOGIN")
                .agreePrivacy(true)
                .agreeTerms(true)
                .build());
    }
}
