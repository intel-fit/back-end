package rto.intelfit.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import rto.intelfit.dto.UserSetupDto;
import rto.intelfit.security.CustomUserPrincipal;
import rto.intelfit.service.UserSetupService;

@Slf4j
@RestController
@RequestMapping("/api/setup")
@RequiredArgsConstructor
@Tag(name = "User Setup API", description = "사용자 초기 설정 API")
@SecurityRequirement(name = "bearerAuth")
public class UserSetupController {

    private final UserSetupService userSetupService;

    @Operation(summary = "초기 피트니스 정보 설정", description = "회원가입 후 초기 피트니스 정보를 입력합니다")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "초기 설정 완료"),
            @ApiResponse(responseCode = "400", description = "잘못된 요청 데이터"),
            @ApiResponse(responseCode = "401", description = "인증이 필요합니다")
    })
    @PostMapping("/initial")
    public ResponseEntity<UserSetupDto.InitialSetupResponse> completeInitialSetup(
            @AuthenticationPrincipal CustomUserPrincipal userPrincipal,
            @Valid @RequestBody UserSetupDto.InitialSetupRequest request) {
        log.info("초기 설정 요청 - 사용자 ID: {}", userPrincipal.getUserId());

        UserSetupDto.InitialSetupResponse response =
                userSetupService.completeInitialSetup(userPrincipal, request);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "초기 설정 상태 확인", description = "사용자의 초기 설정 완료 여부를 확인합니다")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "설정 상태 조회 성공"),
            @ApiResponse(responseCode = "401", description = "인증이 필요합니다")
    })
    @GetMapping("/status")
    public ResponseEntity<UserSetupDto.SetupStatusResponse> getSetupStatus(
            @AuthenticationPrincipal CustomUserPrincipal userPrincipal) {
        log.info("초기 설정 상태 확인 - 사용자 ID: {}", userPrincipal.getUserId());

        UserSetupDto.SetupStatusResponse response =
                userSetupService.getSetupStatus(userPrincipal);
        return ResponseEntity.ok(response);
    }
}