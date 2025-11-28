package rto.intelfit.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import rto.intelfit.domain.User;
import rto.intelfit.dto.DailyProgressDto;
import rto.intelfit.security.CustomUserPrincipal;
import rto.intelfit.service.DailyProgressService;
import rto.intelfit.service.UserService;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;

/**
 * DailyProgressController (운영 DB 기반)
 * 홈 화면/캘린더용 운동 달성률 & 칼로리 API
 */
@Tag(name = "DailyProgress API", description = "운동 달성률 및 칼로리 조회")
@RestController
@RequestMapping("/api/daily-progress")
@RequiredArgsConstructor
public class DailyProgressController {

    private final DailyProgressService dailyProgressService;
    private final UserService userService;

    /** 인증된 사용자 정보 가져오기 */
    private User resolveUser(CustomUserPrincipal principal) {
        return userService.findByPrincipal(principal);
    }

    /** 오늘의 운동 달성률 & 칼로리 */
    @Operation(summary = "오늘의 운동 달성률과 칼로리 조회")
    @GetMapping("/today")
    public ResponseEntity<DailyProgressDto> getTodayProgress(
            @AuthenticationPrincipal CustomUserPrincipal userPrincipal) {
        User user = resolveUser(userPrincipal);
        DailyProgressDto progress = dailyProgressService.calculateTodayProgress(user);
        return ResponseEntity.ok(progress);
    }

    /** 특정 날짜의 운동 달성률 & 칼로리 */
    @Operation(summary = "특정 날짜의 운동 달성률과 칼로리 조회")
    @GetMapping("/date")
    public ResponseEntity<DailyProgressDto> getProgressByDate(
            @AuthenticationPrincipal CustomUserPrincipal userPrincipal,
            @RequestParam("date") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        User user = resolveUser(userPrincipal);
        DailyProgressDto progress = dailyProgressService.getProgressByDate(user, date);
        return ResponseEntity.ok(progress);
    }

    /** ✅ 이번 주(일~토) 운동 달성률 & 칼로리 리스트 */
    @Operation(summary = "이번 주(일~토) 운동 달성률 및 칼로리 목록 조회")
    @GetMapping("/week")
    public ResponseEntity<List<DailyProgressDto>> getWeeklyProgress(
            @AuthenticationPrincipal CustomUserPrincipal userPrincipal) {
        User user = resolveUser(userPrincipal);
        List<DailyProgressDto> weeklyProgress = dailyProgressService.getWeeklyProgress(user);
        return ResponseEntity.ok(weeklyProgress);
    }

    /** 월별 운동 달성률 & 칼로리 리스트 */
    @Operation(summary = "월별 운동 달성률 및 칼로리 목록 조회")
    @GetMapping("/month")
    public ResponseEntity<List<DailyProgressDto>> getMonthlyProgress(
            @AuthenticationPrincipal CustomUserPrincipal userPrincipal,
            @RequestParam("yearMonth") String yearMonth) {
        User user = resolveUser(userPrincipal);
        YearMonth month = YearMonth.parse(yearMonth);
        List<DailyProgressDto> monthlyProgress = dailyProgressService.getMonthlyProgress(user, month);
        return ResponseEntity.ok(monthlyProgress);
    }

    /** 최근 N일간 운동 달성률 & 칼로리 리스트 */
    @Operation(summary = "최근 N일간 운동 달성률 및 칼로리 목록 조회")
    @GetMapping("/recent")
    public ResponseEntity<List<DailyProgressDto>> getRecentProgress(
            @AuthenticationPrincipal CustomUserPrincipal userPrincipal,
            @RequestParam(defaultValue = "7") int days) {
        User user = resolveUser(userPrincipal);
        List<DailyProgressDto> recentProgress = dailyProgressService.getRecentProgress(user, days);
        return ResponseEntity.ok(recentProgress);
    }
}
