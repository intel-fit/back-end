package rto.intelfit.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import rto.intelfit.dto.KakaoAuthDto;
import rto.intelfit.service.KakaoAuthService;

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

    /**
     * 카카오 콜백 (브라우저 리다이렉트)
     */
    @GetMapping("/callback")
    public ResponseEntity<KakaoAuthDto.LoginResponse> callback(@RequestParam String code) {
        log.info("카카오 콜백 요청");

        KakaoAuthDto.LoginRequest request = new KakaoAuthDto.LoginRequest();
        // code를 설정하기 위해 setter 또는 생성자 필요
        return ResponseEntity.ok(kakaoAuthService.login(createLoginRequest(code)));
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

    // 콜백용 Request 생성 헬퍼
    private KakaoAuthDto.LoginRequest createLoginRequest(String code) {
        return new KakaoAuthDto.LoginRequest() {
            @Override
            public String getCode() {
                return code;
            }
        };
    }
}