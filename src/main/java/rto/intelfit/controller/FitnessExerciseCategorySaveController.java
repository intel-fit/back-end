package rto.intelfit.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import rto.intelfit.dto.FitnessExerciseCategorySaveDto;
import rto.intelfit.service.FitnessExerciseCategorySaveService;
import java.util.Map;
import java.util.List;
//*
// 운동 기록 페이지
// 1. user id 별 운동 기록 리스트 반환
// 2. 해당 유저의 운동 기록 삭제
// 3. 운동 기록 추가
// *//
@Slf4j
@RestController
@RequestMapping("/api/workouts")
@RequiredArgsConstructor
@Tag(name = "Workout Save API", description = "운동 기록 관리 API")
public class FitnessExerciseCategorySaveController {

    private final FitnessExerciseCategorySaveService saveService;

    @Operation(summary = "운동 기록 조회 (세션 단위)",
            description = "특정 유저의 운동 기록을 세션 단위로 그룹핑하여 반환합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "조회 성공"),
            @ApiResponse(responseCode = "404", description = "유저 또는 세션 없음")
    })
    @GetMapping("/{userId}")
    public ResponseEntity<List<FitnessExerciseCategorySaveDto.SessionResponse>> getUserWorkoutSessions(
            @PathVariable Long userId) {

        log.info("📋 운동 세션 조회 요청: userId={}", userId);
        List<FitnessExerciseCategorySaveDto.SessionResponse> response = saveService.getUserGroupedSessions(userId);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "운동 기록 세션 삭제",
            description = "세션 ID를 기준으로 해당 운동 기록(모든 세트)을 삭제합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "삭제 성공"),
            @ApiResponse(responseCode = "404", description = "해당 세션 없음")
    })
    @DeleteMapping("/{sessionId}")
    public ResponseEntity<FitnessExerciseCategorySaveDto.DeleteResponse> deleteWorkoutSession(
            @PathVariable @NotBlank String sessionId) {

        log.info("🗑 운동 세션 삭제 요청: sessionId={}", sessionId);
        FitnessExerciseCategorySaveDto.DeleteResponse response = saveService.deleteBySessionId(sessionId);
        return ResponseEntity.ok(response);
    }
    @Operation(summary = "운동 기록 추가", description = "운동 세션(여러 세트 포함)을 새로 저장합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "저장 성공"),
            @ApiResponse(responseCode = "404", description = "유저 없음")
    })
    @PostMapping
    public ResponseEntity<Map<String, String>> addWorkoutSession(
            @RequestBody FitnessExerciseCategorySaveDto.CreateRequest request) {

        log.info("📥 운동 세션 저장 요청: userId={}, exercise={}",
                request.getUserId(), request.getExerciseName());

        String sessionId = saveService.addWorkoutSession(request);

        return ResponseEntity.ok(Map.of("sessionId", sessionId, "status", "success"));
    }

}
