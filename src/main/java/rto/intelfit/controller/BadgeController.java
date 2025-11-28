package rto.intelfit.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import rto.intelfit.domain.Badge;
import rto.intelfit.dto.BadgeDto;
import rto.intelfit.exception.ErrorResponse;
import rto.intelfit.security.CustomUserPrincipal;
import rto.intelfit.service.BadgeService;

/**
 * 뱃지 컨트롤러
 * <p>
 * 주요 기능:
 * 1. 사용자 뱃지 목록 조회
 * 2. 마이페이지 표시 뱃지 조회
 * 3. 뱃지 통계 조회
 * 4. 뱃지 수동 부여 (관리자)
 */
@Tag(name = "Badge", description = "뱃지 API - 운동/식단/체중 목표 달성 뱃지 시스템")
@Slf4j
@RestController
@RequestMapping("/api/badges")
@RequiredArgsConstructor
public class BadgeController {

    private final BadgeService badgeService;

    /**
     * 사용자의 모든 뱃지 조회 (획득/미획득 포함)
     *
     * @param userPrincipal 인증된 사용자
     * @return 뱃지 목록
     */
    @Operation(summary = "전체 뱃지 목록 조회",
            description = "사용자가 획득한 뱃지와 미획득 뱃지를 모두 조회합니다.")
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "뱃지 목록 조회 성공",
                    content = @Content(schema = @Schema(implementation = BadgeDto.UserBadgesResponse.class))
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "인증 실패",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            )
    })
    @GetMapping
    public ResponseEntity<BadgeDto.UserBadgesResponse> getUserBadges(
            @Parameter(hidden = true) @AuthenticationPrincipal CustomUserPrincipal userPrincipal) {

        log.info("전체 뱃지 목록 조회 요청 - 사용자: {}", userPrincipal.getUserId());

        BadgeDto.UserBadgesResponse response = badgeService.getUserBadges(userPrincipal);

        return ResponseEntity.ok(response);
    }

    /**
     * 마이페이지 표시 뱃지 조회 (최대 3개)
     *
     * @param userPrincipal 인증된 사용자
     * @return 표시 뱃지 목록
     */
    @Operation(summary = "마이페이지 표시 뱃지 조회",
            description = "마이페이지에 표시할 뱃지를 조회합니다. (최대 3개, 피그마 디자인 기준)")
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "표시 뱃지 조회 성공",
                    content = @Content(schema = @Schema(implementation = BadgeDto.DisplayBadgesResponse.class))
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "인증 실패",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            )
    })
    @GetMapping("/display")
    public ResponseEntity<BadgeDto.DisplayBadgesResponse> getDisplayBadges(
            @Parameter(hidden = true) @AuthenticationPrincipal CustomUserPrincipal userPrincipal) {

        log.info("마이페이지 표시 뱃지 조회 요청 - 사용자: {}", userPrincipal.getUserId());

        BadgeDto.DisplayBadgesResponse response = badgeService.getDisplayBadges(userPrincipal);

        return ResponseEntity.ok(response);
    }

    /**
     * 뱃지 통계 조회
     *
     * @param userPrincipal 인증된 사용자
     * @return 뱃지 통계
     */
    @Operation(summary = "뱃지 통계 조회",
            description = "사용자의 뱃지 획득 통계를 조회합니다.")
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "뱃지 통계 조회 성공",
                    content = @Content(schema = @Schema(implementation = BadgeDto.BadgeStatistics.class))
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "인증 실패",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            )
    })
    @GetMapping("/statistics")
    public ResponseEntity<BadgeDto.BadgeStatistics> getBadgeStatistics(
            @Parameter(hidden = true) @AuthenticationPrincipal CustomUserPrincipal userPrincipal) {

        log.info("뱃지 통계 조회 요청 - 사용자: {}", userPrincipal.getUserId());

        BadgeDto.BadgeStatistics response = badgeService.getBadgeStatistics(userPrincipal);

        return ResponseEntity.ok(response);
    }

    /**
     * 특정 뱃지 수동 부여 (테스트/관리자용)
     *
     * @param userPrincipal 인증된 사용자
     * @param badgeType     부여할 뱃지 타입
     * @return 뱃지 부여 결과
     */
    @Operation(summary = "뱃지 수동 부여 (테스트용)",
            description = "특정 뱃지를 수동으로 부여합니다. (테스트 및 관리자 전용)")
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "뱃지 부여 성공",
                    content = @Content(schema = @Schema(implementation = BadgeDto.NewBadgeNotification.class))
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "이미 획득한 뱃지",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "인증 실패",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "뱃지를 찾을 수 없음",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            )
    })
    @PostMapping("/award/{badgeType}")
    public ResponseEntity<BadgeDto.NewBadgeNotification> awardBadge(
            @Parameter(hidden = true) @AuthenticationPrincipal CustomUserPrincipal userPrincipal,
            @Parameter(description = "부여할 뱃지 타입", example = "EXERCISE_100")
            @PathVariable Badge.BadgeType badgeType) {

        log.info("뱃지 수동 부여 요청 - 사용자: {}, 뱃지: {}",
                userPrincipal.getUserId(), badgeType);

        BadgeDto.NewBadgeNotification response = badgeService.awardBadge(
                userPrincipal, badgeType);

        return ResponseEntity.ok(response);
    }
}
