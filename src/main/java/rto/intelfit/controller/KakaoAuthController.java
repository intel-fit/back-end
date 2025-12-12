package rto.intelfit.controller;

import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import rto.intelfit.dto.KakaoAuthDto;
import rto.intelfit.service.KakaoAuthService;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

@Slf4j
@RestController
@RequestMapping("/api/auth/kakao")
@RequiredArgsConstructor
public class KakaoAuthController {

    private final KakaoAuthService kakaoAuthService;

    /**
     * 카카오 로그인 URL 반환
     */
    @GetMapping("/login-url")
    public ResponseEntity<KakaoAuthDto.LoginUrlResponse> getLoginUrl() {
        return ResponseEntity.ok(kakaoAuthService.getLoginUrl());
    }

    /**
     * 카카오 로그인 (인가 코드)
     */
    @PostMapping("/login")
    public ResponseEntity<KakaoAuthDto.LoginResponse> login(
            @RequestBody KakaoAuthDto.LoginRequest request) {

        log.info("카카오 로그인 요청");
        return ResponseEntity.ok(kakaoAuthService.login(request));
    }




    @GetMapping("/callback")
    public void callback(
            @RequestParam String code,
            HttpServletResponse response
    ) throws IOException {

        log.info("카카오 콜백 요청");

        KakaoAuthDto.LoginResponse loginResponse =
                kakaoAuthService.login(KakaoAuthDto.LoginRequest.of(code));

        // 1️⃣ Access Token 쿠키
        ResponseCookie accessCookie = ResponseCookie.from(
                        "accessToken", loginResponse.getAccessToken())
                .httpOnly(true)
                .secure(true)
                .path("/")
                .maxAge(60 * 60) // 1시간
                .sameSite("None")
                .build();

        // 2️⃣ Refresh Token 쿠키
        ResponseCookie refreshCookie = ResponseCookie.from(
                        "refreshToken", loginResponse.getRefreshToken())
                .httpOnly(true)
                .secure(true)
                .path("/")
                .maxAge(60 * 60 * 24 * 14) // 14일
                .sameSite("None")
                .build();

        response.addHeader("Set-Cookie", accessCookie.toString());
        response.addHeader("Set-Cookie", refreshCookie.toString());

        // 3️⃣ 상태값만 redirect
        String redirectUrl =
                "https://www.intelfits.com/login/callback"
                        + "?isNewUser=" + loginResponse.isNewUser()
                        + "&isOnboarded=" + loginResponse.isOnboarded();

        response.sendRedirect(redirectUrl);
    }







    @PostMapping("/logout")
    public ResponseEntity<KakaoAuthDto.MessageResponse> logout(
            @AuthenticationPrincipal UserDetails userDetails) {

        log.info("카카오 로그아웃 - userId: {}", userDetails.getUsername());
        return ResponseEntity.ok(kakaoAuthService.logout(userDetails.getUsername()));
    }

    /**
     * 카카오 연결 끊기 (회원 탈퇴)
     */
    @DeleteMapping("/unlink")
    public ResponseEntity<KakaoAuthDto.MessageResponse> unlink(
            @AuthenticationPrincipal UserDetails userDetails) {

        log.info("카카오 회원탈퇴 - userId: {}", userDetails.getUsername());
        return ResponseEntity.ok(kakaoAuthService.unlink(userDetails.getUsername()));
    }









}