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
    @GetMapping("/check-userid")
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
}