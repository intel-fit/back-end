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
import rto.intelfit.service.UserService;

@Slf4j
@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
@Tag(name = "User API", description = "사용자 관련 API")
public class UserController {

    private final UserService userService;

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

    @Operation(summary = "로그인", description = "사용자 로그인을 처리합니다")
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
    @PostMapping("/change-password")
    public ResponseEntity<LoginDto.PasswordChangeResponse> changePassword(
            @Valid @RequestBody LoginDto.PasswordChangeRequest request) {
        log.info("비밀번호 변경 요청 - 임시비밀번호: {}", request.getTempPassword());

        LoginDto.PasswordChangeResponse response = userService.changePassword(request);
        return ResponseEntity.ok(response);
    }
}