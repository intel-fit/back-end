package rto.intelfit.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import rto.intelfit.dto.LoginDto;
import rto.intelfit.dto.SignUpDto;
import rto.intelfit.dto.UserOnboardingDto;
import rto.intelfit.service.UserService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import rto.intelfit.security.CustomUserPrincipal;
import rto.intelfit.domain.User;
import rto.intelfit.security.CustomUserPrincipal;
import org.springframework.security.core.annotation.AuthenticationPrincipal;

@Slf4j
@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
@Tag(name = "User API", description = "사용자 관련 API")
public class UserController {

    private final UserService userService;

    @Operation(summary = "테스트 유저 자동 생성", description = "JWT 없이 호출 가능, 테스트 계정이 없으면 자동 생성합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "생성 완료 또는 이미 존재"),
    })
    @PostMapping("/create-test-user")
    public ResponseEntity<?> createTestUser() {
        userService.createTestUserIfNotExists();
        return ResponseEntity.ok("테스트 유저 준비 완료");
    }


    @Operation(summary = "회원가입", description = "새로운 사용자를 등록합니다")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "회원가입 성공"),
            @ApiResponse(responseCode = "400", description = "잘못된 요청 데이터"),
            @ApiResponse(responseCode = "409", description = "중복된 사용자 ID 또는 이메일")
    })
    @PostMapping("/signup")
    public ResponseEntity<SignUpDto.Response> signUp(@Valid @RequestBody SignUpDto.Request request) {
        log.info("회원가입 요청 - 사용자 ID: {}, 이메일: {}", request.getUserId(), request.getEmail());

        SignUpDto.Response response = userService.signUp(request);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "온보딩 완료", description = "카카오/소셜 로그인 후 초기 피트니스 정보 입력")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "온보딩 완료"),
            @ApiResponse(responseCode = "400", description = "이미 온보딩 완료됨"),
            @ApiResponse(responseCode = "401", description = "인증 필요")
    })
    @PostMapping("/onboarding")
    public ResponseEntity<?> completeOnboarding(
            @AuthenticationPrincipal CustomUserPrincipal principal,
            @Valid @RequestBody UserOnboardingDto request
    ) {
        userService.completeOnboarding(principal, request);
        return ResponseEntity.ok("온보딩이 완료되었습니다.");
    }



    @Operation(summary = "아이디 중복 확인", description = "사용자 ID의 중복 여부를 확인합니다")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "중복 확인 완료"),
            @ApiResponse(responseCode = "400", description = "잘못된 요청 데이터")
    })
    @GetMapping("/check-userId")
    public ResponseEntity<SignUpDto.UserIdCheckResponse> checkUserId(
            @RequestParam String userId) {
        log.info("아이디 중복 확인 요청 - 사용자 ID: {}", userId);

        SignUpDto.UserIdCheckResponse response = userService.checkUserIdAvailability(userId);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "이메일 인증코드 발송", description = "이메일로 6자리 인증코드를 발송합니다")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "인증코드 발송 완료"),
            @ApiResponse(responseCode = "400", description = "잘못된 이메일 형식")
    })
    @PostMapping("/send-verification-code")
    public ResponseEntity<SignUpDto.EmailVerificationResponse> sendVerificationCode(
            @Valid @RequestBody SignUpDto.EmailVerificationRequest request) {
        log.info("이메일 인증코드 발송 요청 - 이메일: {}", request.getEmail());

        SignUpDto.EmailVerificationResponse response = userService.sendEmailVerificationCode(request.getEmail());
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "로그인", description = "사용자 로그인을 처리하고 JWT 토큰을 발급합니다")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "로그인 성공"),
            @ApiResponse(responseCode = "401", description = "아이디 또는 비밀번호가 올바르지 않습니다")
    })
    @PostMapping("/login")
    public ResponseEntity<LoginDto.Response> login(@Valid @RequestBody LoginDto.Request request) {
        log.info("로그인 요청 - 사용자 ID: {}", request.getUserId());

        LoginDto.Response response = userService.login(request);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "로그아웃", description = "사용자 로그아웃을 처리하고 토큰을 무효화합니다")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "로그아웃 성공"),
            @ApiResponse(responseCode = "401", description = "유효하지 않은 토큰")
    })
    @PostMapping("/logout")
    public ResponseEntity<LoginDto.LogoutResponse> logout(@Valid @RequestBody LoginDto.LogoutRequest request) {
        log.info("로그아웃 요청");

        LoginDto.LogoutResponse response = userService.logout(request);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "토큰 재발급", description = "리프레시 토큰을 사용하여 새로운 액세스 토큰을 발급합니다")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "토큰 재발급 성공"),
            @ApiResponse(responseCode = "401", description = "유효하지 않은 리프레시 토큰")
    })
    @PostMapping("/refresh-token")
    public ResponseEntity<LoginDto.TokenRefreshResponse> refreshToken(
            @Valid @RequestBody LoginDto.TokenRefreshRequest request) {
        log.info("토큰 재발급 요청");

        LoginDto.TokenRefreshResponse response = userService.refreshToken(request);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "아이디 찾기", description = "이메일로 아이디를 찾습니다")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "아이디 찾기 완료"),
            @ApiResponse(responseCode = "404", description = "해당 이메일로 등록된 사용자를 찾을 수 없습니다")
    })
    @PostMapping("/find-userId")
    public ResponseEntity<LoginDto.FindUserIdResponse> findUserId(
            @Valid @RequestBody LoginDto.FindUserIdRequest request) {
        log.info("아이디 찾기 요청 - 이메일: {}", request.getEmail());

        LoginDto.FindUserIdResponse response = userService.findUserId(request.getEmail());
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "비밀번호 재설정 요청", description = "이메일로 임시 비밀번호를 발송합니다")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "임시 비밀번호 발송 완료"),
            @ApiResponse(responseCode = "404", description = "해당 이메일로 등록된 사용자를 찾을 수 없습니다")
    })
    @PostMapping("/reset-password")
    public ResponseEntity<LoginDto.PasswordResetResponse> resetPassword(
            @Valid @RequestBody LoginDto.PasswordResetRequest request) {
        log.info("비밀번호 재설정 요청 - 이메일: {}", request.getEmail());

        LoginDto.PasswordResetResponse response = userService.resetPassword(request.getEmail());
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "비밀번호 변경", description = "임시 비밀번호로 새 비밀번호로 변경합니다")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "비밀번호 변경 완료"),
            @ApiResponse(responseCode = "400", description = "임시 비밀번호가 올바르지 않거나 만료되었습니다"),
            @ApiResponse(responseCode = "404", description = "임시 비밀번호에 해당하는 사용자를 찾을 수 없습니다")
    })
    @PutMapping("/change-password")
    public ResponseEntity<LoginDto.PasswordChangeResponse> changePassword(
            @Valid @RequestBody LoginDto.PasswordChangeRequest request) {
        log.info("비밀번호 변경 요청 - 임시비밀번호: {}", request.getTempPassword());

        LoginDto.PasswordChangeResponse response = userService.changePassword(request);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "모든 토큰 초기화", description = "현재 로그인한 사용자의 식단/운동/챗봇 토큰을 기본값으로 초기화합니다")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "토큰 초기화 완료"),
            @ApiResponse(responseCode = "401", description = "인증 실패")
    })
    @PostMapping("/tokens/reset")
    public ResponseEntity<?> resetTokens(@AuthenticationPrincipal CustomUserPrincipal principal) {
        User updated = userService.updateTokensToDefault(principal);
        return ResponseEntity.ok("토큰이 기본값으로 초기화되었습니다.");
    }


    @Operation(summary = "프리미엄으로 변경", description = "현재 로그인한 사용자의 멤버십을 Premium으로 변경합니다")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "프리미엄 전환 완료")
    })
    @PostMapping("/membership/premium")
    public ResponseEntity<?> makePremium(@AuthenticationPrincipal CustomUserPrincipal principal) {
        userService.upgradeToPremium(principal);
        return ResponseEntity.ok("멤버십이 프리미엄으로 변경되었습니다.");
    }


    @Operation(summary = "무료로 변경", description = "현재 로그인한 사용자의 멤버십을 Free로 변경합니다")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "무료 전환 완료")
    })
    @PostMapping("/membership/free")
    public ResponseEntity<?> makeFree(@AuthenticationPrincipal CustomUserPrincipal principal) {
        userService.downgradeToFree(principal);
        return ResponseEntity.ok("멤버십이 무료 플랜으로 변경되었습니다.");
    }

}