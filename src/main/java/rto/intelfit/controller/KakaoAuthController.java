package rto.intelfit.controller;

import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.util.UriComponentsBuilder;
import rto.intelfit.dto.KakaoAuthDto;
import rto.intelfit.service.KakaoAuthService;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

import static java.nio.charset.StandardCharsets.UTF_8;

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

        KakaoAuthDto.LoginResponse loginResponse =
                kakaoAuthService.login(new KakaoAuthDto.LoginRequest(code));

        log.info(
                "카카오 로그인 callback - userId={}, isNewUser={}, isOnboarded={}",
                loginResponse.getUserId(),
                loginResponse.isNewUser(),
                loginResponse.isOnboarded()
        );

        String deepLink = "intelfit://auth/kakao"
                + "?accessToken=" + URLEncoder.encode(loginResponse.getAccessToken(), UTF_8)
                + "&refreshToken=" + URLEncoder.encode(loginResponse.getRefreshToken(), UTF_8)
                + "&userId=" + loginResponse.getUserId()
                + "&nickname=" + URLEncoder.encode(
                loginResponse.getNickname() != null ? loginResponse.getNickname() : "", UTF_8)
                + "&profileImageUrl=" + URLEncoder.encode(
                loginResponse.getProfileImageUrl() != null ? loginResponse.getProfileImageUrl() : "", UTF_8)
                + "&isNewUser=" + loginResponse.isNewUser()
                + "&isOnboarded=" + loginResponse.isOnboarded()
                + "&membershipType=FREE";

        response.sendRedirect(deepLink);
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
    //@DeleteMapping("/unlink")
    //public ResponseEntity<KakaoAuthDto.MessageResponse> unlink(
     //       @AuthenticationPrincipal UserDetails userDetails) {

     //   log.info("카카오 회원탈퇴 - userId: {}", userDetails.getUsername());
     //   return ResponseEntity.ok(kakaoAuthService.unlink(userDetails.getUsername()));
    //}









}