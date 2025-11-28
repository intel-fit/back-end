package rto.intelfit.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import rto.intelfit.dto.ExerciseGoalDto;
import rto.intelfit.security.CustomUserPrincipal;
import rto.intelfit.service.ExerciseGoalService;

@Slf4j
@RestController
@RequestMapping("/api/exercise-goal")
@RequiredArgsConstructor
@Tag(name = "Exercise Goal API", description = "운동 목표 설정/조회 API")
@SecurityRequirement(name = "bearerAuth")
public class ExerciseGoalController {

    private final ExerciseGoalService exerciseGoalService;

    @Operation(summary = "운동 목표 저장", description = "프론트에서 전달된 JSON을 받아 운동 목표를 저장합니다.")
    @PostMapping
    public ResponseEntity<ExerciseGoalDto.Response> saveGoal(
            @AuthenticationPrincipal CustomUserPrincipal userPrincipal,
            @RequestBody ExerciseGoalDto.Request request
    ) {
        var response = exerciseGoalService.saveGoal(userPrincipal, request);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "운동 목표 요약 조회", description = "현재 로그인한 사용자의 주 3회/운동시간/진행률 정보를 반환합니다.")
    @GetMapping("/summary")
    public ResponseEntity<ExerciseGoalDto.SummaryResponse> getGoalSummary(
            @AuthenticationPrincipal CustomUserPrincipal userPrincipal
    ) {
        var response = exerciseGoalService.getGoalSummary(userPrincipal);
        return ResponseEntity.ok(response);
    }
    @Operation(summary = "운동 목표 삭제", description = "현재 로그인한 사용자의 운동 목표를 삭제합니다.")
    @DeleteMapping
    public ResponseEntity<Void> deleteGoal(
            @AuthenticationPrincipal CustomUserPrincipal userPrincipal
    ) {
        exerciseGoalService.deleteGoal(userPrincipal);
        return ResponseEntity.noContent().build();  // 204 No Content
    }

}
