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
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import rto.intelfit.dto.HomeDto;
import rto.intelfit.exception.ErrorResponse;
import rto.intelfit.security.CustomUserPrincipal;
import rto.intelfit.service.HomeService;

import java.time.LocalDate;

/**
 * 홈 화면 컨트롤러
 * 
 * 주요 기능:
 * 1. 홈 화면 메인 정보 조회
 * 2. 오늘의 운동/식단 요약
 * 3. 주간 통계 정보
 */
@Tag(name = "Home", description = "홈 화면 API")
@Slf4j
@RestController
@RequestMapping("/api/home")
@RequiredArgsConstructor
public class HomeController {

    private final HomeService homeService;

    /**
     * 홈 화면 메인 정보 조회
     * 
     * @param userPrincipal 인증된 사용자
     * @param date 조회할 날짜 (기본값: 오늘)
     * @return 홈 화면 메인 정보
     */
    @Operation(summary = "홈 화면 메인 정보 조회", 
               description = "사용자의 홈 화면에 표시될 모든 정보를 조회합니다. " +
                           "오늘의 운동/식단 요약, 최근 인바디, 주간 통계 등을 포함합니다.")
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "홈 화면 정보 조회 성공",
                    content = @Content(schema = @Schema(implementation = HomeDto.HomeMainResponse.class))
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "인증 실패",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "사용자를 찾을 수 없음",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            )
    })
    @GetMapping
    public ResponseEntity<HomeDto.HomeMainResponse> getHomeMain(
            @Parameter(hidden = true) @AuthenticationPrincipal CustomUserPrincipal userPrincipal,
            @Parameter(description = "조회할 날짜 (yyyy-MM-dd)", example = "2025-01-15")
            @RequestParam(required = false) 
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {

        LocalDate targetDate = date != null ? date : LocalDate.now();
        
        log.info("홈 화면 조회 요청 - 사용자: {}, 날짜: {}", 
                userPrincipal.getUserId(), targetDate);

        HomeDto.HomeMainResponse response = homeService.getHomeMain(userPrincipal, targetDate);

        return ResponseEntity.ok(response);
    }

    /**
     * 오늘의 운동 요약 조회
     * 
     * @param userPrincipal 인증된 사용자
     * @param date 조회할 날짜 (기본값: 오늘)
     * @return 오늘의 운동 요약
     */
    @Operation(summary = "오늘의 운동 요약 조회", 
               description = "특정 날짜의 운동 요약 정보를 조회합니다.")
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "운동 요약 조회 성공",
                    content = @Content(schema = @Schema(implementation = HomeDto.TodayExerciseSummary.class))
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "인증 실패",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            )
    })
    @GetMapping("/exercise-summary")
    public ResponseEntity<HomeDto.TodayExerciseSummary> getTodayExerciseSummary(
            @Parameter(hidden = true) @AuthenticationPrincipal CustomUserPrincipal userPrincipal,
            @Parameter(description = "조회할 날짜 (yyyy-MM-dd)", example = "2025-01-15")
            @RequestParam(required = false) 
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {

        LocalDate targetDate = date != null ? date : LocalDate.now();
        
        log.info("운동 요약 조회 요청 - 사용자: {}, 날짜: {}", 
                userPrincipal.getUserId(), targetDate);

        HomeDto.TodayExerciseSummary summary = homeService.getTodayExerciseSummary(
                userPrincipal, targetDate);

        return ResponseEntity.ok(summary);
    }

    /**
     * 오늘의 식단 요약 조회
     * 
     * @param userPrincipal 인증된 사용자
     * @param date 조회할 날짜 (기본값: 오늘)
     * @return 오늘의 식단 요약
     */
    @Operation(summary = "오늘의 식단 요약 조회", 
               description = "특정 날짜의 식단 요약 정보를 조회합니다.")
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "식단 요약 조회 성공",
                    content = @Content(schema = @Schema(implementation = HomeDto.TodayMealSummary.class))
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "인증 실패",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            )
    })
    @GetMapping("/meal-summary")
    public ResponseEntity<HomeDto.TodayMealSummary> getTodayMealSummary(
            @Parameter(hidden = true) @AuthenticationPrincipal CustomUserPrincipal userPrincipal,
            @Parameter(description = "조회할 날짜 (yyyy-MM-dd)", example = "2025-01-15")
            @RequestParam(required = false) 
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {

        LocalDate targetDate = date != null ? date : LocalDate.now();
        
        log.info("식단 요약 조회 요청 - 사용자: {}, 날짜: {}", 
                userPrincipal.getUserId(), targetDate);

        HomeDto.TodayMealSummary summary = homeService.getTodayMealSummary(
                userPrincipal, targetDate);

        return ResponseEntity.ok(summary);
    }

    /**
     * 주간 요약 조회
     * 
     * @param userPrincipal 인증된 사용자
     * @param date 조회할 날짜가 속한 주 (기본값: 이번주)
     * @return 주간 요약
     */
    @Operation(summary = "주간 요약 조회", 
               description = "특정 날짜가 속한 주의 통계 정보를 조회합니다.")
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "주간 요약 조회 성공",
                    content = @Content(schema = @Schema(implementation = HomeDto.WeeklySummary.class))
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "인증 실패",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            )
    })
    @GetMapping("/weekly-summary")
    public ResponseEntity<HomeDto.WeeklySummary> getWeeklySummary(
            @Parameter(hidden = true) @AuthenticationPrincipal CustomUserPrincipal userPrincipal,
            @Parameter(description = "조회할 주에 속한 날짜 (yyyy-MM-dd)", example = "2025-01-15")
            @RequestParam(required = false) 
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {

        LocalDate targetDate = date != null ? date : LocalDate.now();
        
        log.info("주간 요약 조회 요청 - 사용자: {}, 날짜: {}", 
                userPrincipal.getUserId(), targetDate);

        HomeDto.WeeklySummary summary = homeService.getWeeklySummary(
                userPrincipal, targetDate);

        return ResponseEntity.ok(summary);
    }
}
