package rto.intelfit.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import rto.intelfit.dto.MealDto;
import rto.intelfit.security.CustomUserPrincipal;
import rto.intelfit.service.MealService;

import java.time.LocalDate;
import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/meals")
@RequiredArgsConstructor
@Tag(name = "Meal API", description = "식단 관련 API")
@SecurityRequirement(name = "bearerAuth")
public class MealController {

    private final MealService mealService;

    @Operation(summary = "식사 추가", description = "사용자의 식사 기록을 추가합니다 (AI 분석 결과 또는 수동 입력)")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "식사 추가 성공"),
            @ApiResponse(responseCode = "400", description = "잘못된 요청 데이터"),
            @ApiResponse(responseCode = "401", description = "인증이 필요합니다"),
            @ApiResponse(responseCode = "409", description = "해당 날짜/타입의 식사가 이미 존재합니다")
    })
    @PostMapping
    public ResponseEntity<MealDto.MealCreateResponse> createMeal(
            @AuthenticationPrincipal CustomUserPrincipal userPrincipal,
            @Valid @RequestBody MealDto.MealCreateRequest request) {
        log.info("식사 추가 요청 - 사용자 ID: {}, 날짜: {}, 타입: {}",
                userPrincipal.getUserId(), request.getMealDate(), request.getMealType());

        MealDto.MealCreateResponse response = mealService.createMeal(userPrincipal, request);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "일별 식단 조회", description = "특정 날짜의 모든 식사 기록을 조회합니다")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "일별 식단 조회 성공"),
            @ApiResponse(responseCode = "401", description = "인증이 필요합니다")
    })
    @GetMapping("/daily")
    public ResponseEntity<MealDto.DailyMealsResponse> getDailyMeals(
            @AuthenticationPrincipal CustomUserPrincipal userPrincipal,
            @Parameter(description = "조회 날짜 (yyyy-MM-dd)", example = "2025-01-15")
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate date) {
        log.info("일별 식단 조회 요청 - 사용자 ID: {}, 날짜: {}",
                userPrincipal.getUserId(), date);

        MealDto.DailyMealsResponse response = mealService.getDailyMeals(userPrincipal, date);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "주간 식단 통계",
            description = "일주일간의 식단 통계 및 목표 대비 달성률을 조회합니다")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "주간 통계 조회 성공"),
            @ApiResponse(responseCode = "401", description = "인증이 필요합니다")
    })
    @GetMapping("/weekly-stats")
    public ResponseEntity<MealDto.WeeklyMealStatsResponse> getWeeklyStats(
            @AuthenticationPrincipal CustomUserPrincipal userPrincipal,
            @Parameter(description = "조회 시작일 (yyyy-MM-dd, 월요일 권장)", example = "2025-01-13")
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate startDate) {
        log.info("주간 식단 통계 요청 - 사용자 ID: {}, 시작일: {}",
                userPrincipal.getUserId(), startDate);

        MealDto.WeeklyMealStatsResponse response = mealService.getWeeklyStats(userPrincipal, startDate);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "이번주 월요일 기준 주간 통계",
            description = "이번주 월요일부터 시작하는 주간 식단 통계를 조회합니다")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "주간 통계 조회 성공"),
            @ApiResponse(responseCode = "401", description = "인증이 필요합니다")
    })
    @GetMapping("/this-week-stats")
    public ResponseEntity<MealDto.WeeklyMealStatsResponse> getThisWeekStats(
            @AuthenticationPrincipal CustomUserPrincipal userPrincipal) {
        log.info("이번주 식단 통계 요청 - 사용자 ID: {}", userPrincipal.getUserId());

        LocalDate thisWeekMonday = mealService.getThisWeekMonday();
        MealDto.WeeklyMealStatsResponse response = mealService.getWeeklyStats(userPrincipal, thisWeekMonday);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "지난주 대비 이번주 비교",
            description = "지난주와 이번주의 영양소 섭취량을 비교합니다")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "주간 비교 조회 성공"),
            @ApiResponse(responseCode = "401", description = "인증이 필요합니다")
    })
    @GetMapping("/week-comparison")
    public ResponseEntity<MealDto.WeekComparisonResponse> compareWeeks(
            @AuthenticationPrincipal CustomUserPrincipal userPrincipal,
            @Parameter(description = "이번주 시작일 (yyyy-MM-dd, 월요일 권장)", example = "2025-01-13")
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate thisWeekStart) {
        log.info("주간 비교 요청 - 사용자 ID: {}, 이번주 시작일: {}",
                userPrincipal.getUserId(), thisWeekStart);

        MealDto.WeekComparisonResponse response = mealService.compareWeeks(userPrincipal, thisWeekStart);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "이번주/지난주 비교 (자동)",
            description = "이번주 월요일 기준으로 지난주와 비교합니다")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "주간 비교 조회 성공"),
            @ApiResponse(responseCode = "401", description = "인증이 필요합니다")
    })
    @GetMapping("/this-vs-last-week")
    public ResponseEntity<MealDto.WeekComparisonResponse> compareThisWeekVsLastWeek(
            @AuthenticationPrincipal CustomUserPrincipal userPrincipal) {
        log.info("이번주/지난주 비교 요청 - 사용자 ID: {}", userPrincipal.getUserId());

        LocalDate thisWeekMonday = mealService.getThisWeekMonday();
        MealDto.WeekComparisonResponse response = mealService.compareWeeks(userPrincipal, thisWeekMonday);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "최근 식단 기록 조회",
            description = "최근 N일간의 식사 기록을 조회합니다")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "최근 식단 조회 성공"),
            @ApiResponse(responseCode = "401", description = "인증이 필요합니다")
    })
    @GetMapping("/recent")
    public ResponseEntity<List<MealDto.MealDetailResponse>> getRecentMeals(
            @AuthenticationPrincipal CustomUserPrincipal userPrincipal,
            @Parameter(description = "조회할 일수", example = "7")
            @RequestParam(defaultValue = "7") int days) {
        log.info("최근 식단 기록 조회 요청 - 사용자 ID: {}, 일수: {}",
                userPrincipal.getUserId(), days);

        List<MealDto.MealDetailResponse> response = mealService.getRecentMeals(userPrincipal, days);
        return ResponseEntity.ok(response);
    }
}