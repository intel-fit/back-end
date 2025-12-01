package rto.intelfit.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import rto.intelfit.config.KakaoOAuthConfig;
import rto.intelfit.dto.KakaoOAuthDto;
import rto.intelfit.security.CustomUserPrincipal;
import rto.intelfit.service.KakaoOAuthService;
import rto.intelfit.util.JwtUtil;

import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
@Tag(name = "Kakao OAuth API", description = "카카오 소셜 로그인 API")
public class KakaoOAuthController {

    private final KakaoOAuthService kakaoOAuthService;
    private final KakaoOAuthConfig kakaoConfig;
    private final JwtUtil jwtUtil;

    /**
     * 카카오 로그인 URL 조회
     */
    @Operation(summary = "카카오 로그인 URL 조회", description = "카카오 로그인 페이지 URL을 반환합니다")
    @GetMapping("/kakao/login-url")
    public ResponseEntity<Map<String, String>> getKakaoLoginUrl(
            @RequestParam(required = false) String redirectUri) {

        String finalRedirectUri = redirectUri != null ? redirectUri : kakaoConfig.getRedirectUri();

        String loginUrl = KakaoOAuthConfig.KAKAO_AUTH_URL + "/oauth/authorize"
                + "?client_id=" + kakaoConfig.getClientId()
                + "&redirect_uri=" + finalRedirectUri
                + "&response_type=code"
                + "&scope=profile_nickname,profile_image,account_email";

        log.info("카카오 로그인 URL 생성");

        return ResponseEntity.ok(Map.of("loginUrl", loginUrl));
    }

    /**
     * 카카오 로그인 처리 (인가코드로 로그인)
     */
    @Operation(summary = "카카오 로그인", description = "카카오 인가코드로 로그인/회원가입을 처리합니다")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "로그인 성공"),
            @ApiResponse(responseCode = "400", description = "잘못된 인가코드")
    })
    @PostMapping("/login/kakao")
    public ResponseEntity<KakaoOAuthDto.KakaoLoginResponse> kakaoLogin(
            @RequestBody KakaoOAuthDto.KakaoLoginRequest request) {
        log.info("카카오 로그인 요청");

        KakaoOAuthDto.KakaoLoginResponse response = kakaoOAuthService.kakaoLogin(request);
        return ResponseEntity.ok(response);
    }

    /**
     * 카카오 인가코드 콜백 (웹 브라우저용)
     */
    @Operation(summary = "카카오 콜백", description = "카카오 인가코드 콜백을 처리합니다")
    @GetMapping("/login/kakao")
    public ResponseEntity<KakaoOAuthDto.KakaoLoginResponse> kakaoCallback(
            @RequestParam String code,
            @RequestParam(required = false) String state) {
        log.info("카카오 콜백 - code: {}...", code.substring(0, Math.min(10, code.length())));

        KakaoOAuthDto.KakaoLoginRequest request = KakaoOAuthDto.KakaoLoginRequest.builder()
                .code(code)
                .build();

        KakaoOAuthDto.KakaoLoginResponse response = kakaoOAuthService.kakaoLogin(request);
        return ResponseEntity.ok(response);
    }

    /**
     * 카카오 로그아웃 URL 조회
     */
    @Operation(summary = "카카오 로그아웃 URL 조회", description = "카카오 로그아웃 페이지 URL을 반환합니다")
    @GetMapping("/kakao/logout-url")
    public ResponseEntity<Map<String, String>> getKakaoLogoutUrl() {
        String logoutRedirectUri = kakaoConfig.getRedirectUri().replace("/login/", "/logout/");

        String logoutUrl = "https://kauth.kakao.com/oauth/logout"
                + "?client_id=" + kakaoConfig.getClientId()
                + "&logout_redirect_uri=" + logoutRedirectUri;

        return ResponseEntity.ok(Map.of("logoutUrl", logoutUrl));
    }

    /**
     * 로그아웃 처리
     */
    @Operation(summary = "로그아웃", description = "JWT 토큰을 무효화하고 로그아웃 처리합니다")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "로그아웃 성공"),
            @ApiResponse(responseCode = "401", description = "인증이 필요합니다")
    })
    @PostMapping("/logout")
    @SecurityRequirement(name = "bearerAuth")
    public ResponseEntity<Map<String, Object>> logout(
            @RequestHeader("Authorization") String authHeader,
            @AuthenticationPrincipal CustomUserPrincipal userPrincipal) {
        log.info("로그아웃 요청 - userId: {}", userPrincipal.getUserId());

        String token = authHeader.replace("Bearer ", "");
        kakaoOAuthService.logout(token, userPrincipal.getUserId());

        return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "로그아웃 되었습니다"
        ));
    }

    /**
     * 카카오 로그아웃 콜백
     */
    @Operation(summary = "카카오 로그아웃 콜백", description = "카카오 로그아웃 후 리다이렉트됩니다")
    @GetMapping("/logout/kakao")
    public ResponseEntity<Map<String, Object>> kakaoLogoutCallback() {
        log.info("카카오 로그아웃 콜백");

        return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "카카오 로그아웃이 완료되었습니다"
        ));
    }

    /**
     * 카카오 연결 끊기 (회원 탈퇴)
     */
    @Operation(summary = "카카오 연결 끊기", description = "카카오 계정 연결을 해제합니다")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "연결 끊기 성공"),
            @ApiResponse(responseCode = "401", description = "인증이 필요합니다")
    })
    @PostMapping("/kakao/unlink")
    @SecurityRequirement(name = "bearerAuth")
    public ResponseEntity<Map<String, Object>> unlinkKakao(
            @AuthenticationPrincipal CustomUserPrincipal userPrincipal) {
        log.info("카카오 연결 끊기 요청 - userId: {}", userPrincipal.getUserId());

        kakaoOAuthService.unlinkKakao(userPrincipal);

        return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "카카오 계정 연결이 해제되었습니다"
        ));
    }
}