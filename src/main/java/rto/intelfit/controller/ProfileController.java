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
import rto.intelfit.dto.ProfileDto;
import rto.intelfit.security.CustomUserPrincipal;
import rto.intelfit.service.ProfileService;

@Slf4j
@RestController
@RequestMapping("/api/profile")
@RequiredArgsConstructor
@Tag(name = "Profile API", description = "프로필 관련 API")
@SecurityRequirement(name = "bearerAuth")
public class ProfileController {

    private final ProfileService profileService;

    @Operation(summary = "프로필 조회", description = "로그인한 사용자의 프로필 정보를 조회합니다")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "프로필 조회 성공"),
            @ApiResponse(responseCode = "401", description = "인증이 필요합니다"),
            @ApiResponse(responseCode = "404", description = "사용자를 찾을 수 없습니다")
    })
    @GetMapping
    public ResponseEntity<ProfileDto.ProfileResponse> getProfile(
            @AuthenticationPrincipal CustomUserPrincipal userPrincipal) {
        log.info("프로필 조회 요청 - 사용자 ID: {}", userPrincipal.getUserId());

        ProfileDto.ProfileResponse response = profileService.getProfile(userPrincipal);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "프로필 수정", description = "로그인한 사용자의 프로필 정보를 수정합니다")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "프로필 수정 성공"),
            @ApiResponse(responseCode = "400", description = "잘못된 요청 데이터"),
            @ApiResponse(responseCode = "401", description = "인증이 필요합니다"),
            @ApiResponse(responseCode = "409", description = "중복된 전화번호")
    })
    @PutMapping
    public ResponseEntity<ProfileDto.ProfileUpdateResponse> updateProfile(
            @AuthenticationPrincipal CustomUserPrincipal userPrincipal,
            @Valid @RequestBody ProfileDto.ProfileUpdateRequest request) {
        log.info("프로필 수정 요청 - 사용자 ID: {}", userPrincipal.getUserId());

        ProfileDto.ProfileUpdateResponse response = profileService.updateProfile(userPrincipal, request);
        return ResponseEntity.ok(response);
    }


}