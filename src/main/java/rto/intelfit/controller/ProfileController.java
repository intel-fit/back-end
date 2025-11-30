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
import rto.intelfit.domain.User;
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

    @Operation(summary = "비밀번호 변경", description = "로그인한 사용자의 비밀번호를 변경합니다")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "비밀번호 변경 성공"),
            @ApiResponse(responseCode = "400", description = "잘못된 요청 데이터"),
            @ApiResponse(responseCode = "401", description = "현재 비밀번호가 올바르지 않습니다")
    })
    @PutMapping("/password")
    public ResponseEntity<ProfileDto.PasswordUpdateResponse> updatePassword(
            @AuthenticationPrincipal CustomUserPrincipal userPrincipal,
            @Valid @RequestBody ProfileDto.PasswordUpdateRequest request) {
        log.info("비밀번호 변경 요청 - 사용자 ID: {}", userPrincipal.getUserId());

        ProfileDto.PasswordUpdateResponse response = profileService.updatePassword(userPrincipal, request);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "회원 탈퇴", description = "로그인한 사용자의 계정을 삭제합니다")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "회원 탈퇴 성공"),
            @ApiResponse(responseCode = "400", description = "잘못된 요청 데이터"),
            @ApiResponse(responseCode = "401", description = "비밀번호가 올바르지 않습니다")
    })
    @DeleteMapping
    public ResponseEntity<ProfileDto.AccountDeleteResponse> deleteAccount(
            @AuthenticationPrincipal CustomUserPrincipal userPrincipal,
            @Valid @RequestBody ProfileDto.AccountDeleteRequest request) {
        log.info("회원 탈퇴 요청 - 사용자 ID: {}", userPrincipal.getUserId());

        // 카카오 회원탈퇴: 카카오 ID 입력 확인 (이원웅 추가)
        if (userPrincipal.getSocialProvider() == User.SocialProvider.KAKAO) {
            ProfileDto.AccountDeleteResponse response =
                    profileService.deleteKakaoUser(userPrincipal, request);

            return ResponseEntity.ok(response);

        } //여기까지

        ProfileDto.AccountDeleteResponse response = profileService.deleteAccount(userPrincipal, request);
        return ResponseEntity.ok(response);
    }

}