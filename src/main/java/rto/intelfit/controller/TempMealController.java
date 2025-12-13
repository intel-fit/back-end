package rto.intelfit.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.core.annotation.AuthenticationPrincipal;

import rto.intelfit.dto.TempMealDto;
import rto.intelfit.security.CustomUserPrincipal;
import rto.intelfit.service.TempMealService;

import java.util.List;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/temp-meals")
public class TempMealController {

    private final TempMealService tempMealService;

    @PostMapping("/daily")
    public ResponseEntity<?> generateDailyPaid(
            @AuthenticationPrincipal CustomUserPrincipal principal,
            @RequestBody TempMealDto.DailyRequest request
    ) {
        return ResponseEntity.ok(
                tempMealService.generateDailyPaid(principal, request)
        );
    }
    @PostMapping("/weekly")
    public ResponseEntity<?> generateWeekly(
            @AuthenticationPrincipal CustomUserPrincipal principal,
            @RequestBody TempMealDto.WeeklyRequest request
    ) {
        Long id = tempMealService.generateWeeklyTempPlans(principal, request);
        return ResponseEntity.ok("TEMP Weekly 생성 완료. tempBundleId=" + id);
    }



    // 2) TEMP 조회
    @GetMapping("/weekly")
    public ResponseEntity<List<TempMealDto.PlanResponse>> getWeekly(
            @AuthenticationPrincipal CustomUserPrincipal principal
    ) {
        return ResponseEntity.ok(tempMealService.getWeekly(principal));
    }

    // 3) TEMP → 본 저장 Commit
    @PostMapping("/weekly/commit")
    public ResponseEntity<?> commitWeekly(
            @AuthenticationPrincipal CustomUserPrincipal principal
    ) {
        tempMealService.commitWeekly(principal);
        return ResponseEntity.ok("Commit 완료. 본 DB 저장됨.");
    }
    // 4) TEMP 개별 식사 삭제
    @DeleteMapping("/meal/{mealId}")
    public ResponseEntity<?> deleteTempMeal(
            @AuthenticationPrincipal CustomUserPrincipal principal,
            @PathVariable Long mealId
    ) {
        tempMealService.deleteTempMeal(principal, mealId);
        return ResponseEntity.ok("선택한 식사가 삭제되었습니다.");
    }

    // 5) 하루치 TEMP 식단 다시 재생성
    @PostMapping("/daily/regenerate/{dayIndex}")
    public ResponseEntity<?> regenerateDaily(
            @AuthenticationPrincipal CustomUserPrincipal principal,
            @PathVariable int dayIndex
    ) {
        tempMealService.regenerateDay(principal, dayIndex);
        return ResponseEntity.ok("해당 날짜의 TEMP 식단이 재생성되었습니다.");
    }

}
