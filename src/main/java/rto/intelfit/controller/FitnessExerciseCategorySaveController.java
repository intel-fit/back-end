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
import lombok.Getter;
import lombok.Setter;
import lombok.AllArgsConstructor;
import java.time.LocalDate;
import rto.intelfit.exception.BusinessException;
import rto.intelfit.exception.ErrorCode;
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

    @Operation(summary = "하루 운동 소모 칼로리 조회",
            description = "특정 날짜의 운동 세션별 소모 칼로리 합산 및 총합을 반환합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "조회 성공"),
            @ApiResponse(responseCode = "404", description = "유저 없음")
    })
    @GetMapping("/{userId}/calories/{date}")
    public ResponseEntity<FitnessExerciseCategorySaveDto.DailyCaloriesResponse>
    getDailyCalories(
            @PathVariable Long userId,
            @PathVariable String date
    ) {

        LocalDate parsed = LocalDate.parse(date);

        log.info("🔥 하루 운동 칼로리 조회: userId={}, date={}", userId, parsed);

        FitnessExerciseCategorySaveDto.DailyCaloriesResponse response =
                saveService.getDailyCalories(userId, parsed);

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
//
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

    @Operation(
            summary = "현재 운동 기록 저장 + AI 피드백 전송",
            description = "isSaved=false 상태의 운동들을 저장 후 AI 서버로 운동 피드백을 전송합니다."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "운동 저장 및 AI 피드백 전송 성공"),
            @ApiResponse(responseCode = "404", description = "저장할 운동 없음")
    })
    @PostMapping("/save")
    public ResponseEntity<FitnessExerciseCategorySaveDto.SaveResponse> saveWorkout(
            @RequestBody FitnessExerciseCategorySaveDto.SaveRequest request
    ) {
        log.info("💾 운동 저장 API 호출 userId={}, title={}, date={}, seconds={}",
                request.getUserId(), request.getSaveTitle(), request.getDate());

        LocalDate date = LocalDate.parse(request.getDate());

        // 🔥 날짜 제약 제거됨 — 어떤 날짜든 저장 가능

        FitnessExerciseCategorySaveDto.SaveResponse response =
                saveService.saveUnsavedWorkoutsAndSendFeedback(
                        request.getUserId(),
                        request.getSaveTitle(),
                        request.getIntensity(),
                        request.getFeedback(),
                        date
                );

        return ResponseEntity.ok(response);
    }






    @Operation(summary = "저장된 운동 기록 조회 (제목 → 세션 단위)",
            description = "저장된 운동 기록들을 saveTitle 기준으로 묶고, 내부에서는 sessionId 기준으로 그룹핑하여 반환합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "조회 성공"),
    })
    @GetMapping("/saved/{userId}")
    public ResponseEntity<List<FitnessExerciseCategorySaveDto.SavedGroupResponse>> getSavedWorkouts(
            @PathVariable Long userId) {

        log.info("📂 저장된 운동 그룹 조회 요청 userId={}", userId);

        List<FitnessExerciseCategorySaveDto.SavedGroupResponse> response =
                saveService.getSavedWorkoutGroups(userId);

        return ResponseEntity.ok(response);
    }

    @Operation(summary = "날짜별 저장된 운동 기록 조회",
            description = "특정 날짜에 저장된(isSaved=true) 운동 기록을 saveTitle → sessionId 순으로 그룹핑하여 반환합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "조회 성공")
    })
    @GetMapping("/saved/{userId}/{date}")
    public ResponseEntity<List<FitnessExerciseCategorySaveDto.SavedGroupResponse>>
    getSavedWorkoutsByDate(
            @PathVariable Long userId,
            @PathVariable String date
    ) {
        LocalDate parsed = LocalDate.parse(date);

        log.info("📂 저장된 운동 날짜 조회 요청 userId={}, date={}", userId, parsed);

        List<FitnessExerciseCategorySaveDto.SavedGroupResponse> response =
                saveService.getSavedWorkoutGroupsByDate(userId, parsed);

        return ResponseEntity.ok(response);
    }






    @Operation(summary = "운동 세션 완료 상태 토글",
            description = "세션의 완료/미완료 상태를 토글합니다. 세션의 모든 세트가 일괄 변경됩니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "토글 성공"),
            @ApiResponse(responseCode = "404", description = "해당 세션 없음")
    })
    @PatchMapping("/{sessionId}/toggle")
    public ResponseEntity<FitnessExerciseCategorySaveDto.ToggleResponse> toggleSessionCompletion(
            @PathVariable @NotBlank String sessionId) {

        log.info("🔄 운동 세션 완료 토글 요청: sessionId={}", sessionId);
        FitnessExerciseCategorySaveDto.ToggleResponse response = saveService.toggleSessionCompletion(sessionId);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "오늘 운동 시간 추가", description = "프론트에서 보낸 운동 시간을 오늘의 DailyProgress에 누적 저장합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "운동 시간 누적 성공"),
            @ApiResponse(responseCode = "404", description = "유저 없음")
    })
    @PostMapping("/time")
    public ResponseEntity<Map<String, Object>> addWorkoutTime(@RequestBody WorkoutTimeRequest request) {

        log.info("⏱ 오늘 운동 시간 추가 요청: userId={}, seconds={}", request.getUserId(), request.getSeconds());

        saveService.addDailyExerciseSeconds(request.getUserId(), request.getSeconds());

        return ResponseEntity.ok(Map.of(
                "status", "success",
                "addedSeconds", request.getSeconds()
        ));
    }

    @Getter
    @Setter
    public static class WorkoutTimeRequest {
        private Long userId;
        private long seconds;
    }
    @Operation(summary = "오늘 운동 시간 조회", description = "특정 유저의 오늘 총 운동 시간을 반환합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "조회 성공"),
            @ApiResponse(responseCode = "404", description = "유저 없음")
    })
    @GetMapping("/time/{userId}")
    public ResponseEntity<TodayWorkoutTimeResponse> getTodayWorkoutTime(@PathVariable Long userId) {

        log.info("⏱ 오늘 운동시간 조회 요청: userId={}", userId);

        long seconds = saveService.getTodayWorkoutSeconds(userId);

        return ResponseEntity.ok(
                new TodayWorkoutTimeResponse(userId, seconds)
        );
    }

    @Getter
    @Setter
    @AllArgsConstructor
    public static class TodayWorkoutTimeResponse {
        private Long userId;
        private long totalSeconds;
    }


}
