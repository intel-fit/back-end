package rto.intelfit.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import rto.intelfit.dto.ExerciseDto;
import rto.intelfit.security.CustomUserPrincipal;
import rto.intelfit.service.ExerciseService;

import java.time.LocalDate;

/**
 * 운동 기록 관리 컨트롤러
 *
 * 주요 기능:
 * 1. 운동 기록 추가/수정/삭제
 * 2. 일별/주간 운동 기록 조회
 * 3. 운동 통계 조회
 */
@Slf4j
@RestController
@RequestMapping("/api/exercises")
@RequiredArgsConstructor
@Tag(name = "Exercise", description = "운동 기록 관리 API")
public class ExerciseController {

    private final ExerciseService exerciseService;

    /**
     * 운동 기록 추가
     */
    @PostMapping
    @Operation(summary = "운동 기록 추가", description = "새로운 운동 기록을 추가합니다")
    public ResponseEntity<ExerciseDto.ExerciseCreateResponse> createExercise(
            @AuthenticationPrincipal CustomUserPrincipal userPrincipal,
            @Valid @RequestBody ExerciseDto.ExerciseCreateRequest request) {

        log.info("운동 기록 추가 요청 - 사용자: {}, 날짜: {}, 카테고리: {}",
                userPrincipal.getUserId(), request.getExerciseDate(), request.getExerciseCategory());

        ExerciseDto.ExerciseCreateResponse response = exerciseService.createExercise(userPrincipal, request);

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * 운동 기록 수정
     */
    @PutMapping("/{exerciseId}")
    @Operation(summary = "운동 기록 수정", description = "기존 운동 기록을 수정합니다")
    public ResponseEntity<ExerciseDto.ExerciseCreateResponse> updateExercise(
            @AuthenticationPrincipal CustomUserPrincipal userPrincipal,
            @Parameter(description = "운동 기록 ID") @PathVariable Long exerciseId,
            @Valid @RequestBody ExerciseDto.ExerciseCreateRequest request) {

        log.info("운동 기록 수정 요청 - 사용자: {}, 운동 ID: {}", userPrincipal.getUserId(), exerciseId);

        ExerciseDto.ExerciseCreateResponse response = exerciseService.updateExercise(userPrincipal, exerciseId, request);

        return ResponseEntity.ok(response);
    }

    /**
     * 운동 기록 삭제
     */
    @DeleteMapping("/{exerciseId}")
    @Operation(summary = "운동 기록 삭제", description = "운동 기록을 삭제합니다")
    public ResponseEntity<Void> deleteExercise(
            @AuthenticationPrincipal CustomUserPrincipal userPrincipal,
            @Parameter(description = "운동 기록 ID") @PathVariable Long exerciseId) {

        log.info("운동 기록 삭제 요청 - 사용자: {}, 운동 ID: {}", userPrincipal.getUserId(), exerciseId);

        exerciseService.deleteExercise(userPrincipal, exerciseId);

        return ResponseEntity.noContent().build();
    }

    /**
     * 특정 날짜의 운동 기록 조회
     */
    @GetMapping("/daily")
    @Operation(summary = "일별 운동 기록 조회", description = "특정 날짜의 모든 운동 기록을 조회합니다")
    public ResponseEntity<ExerciseDto.DailyExercisesResponse> getDailyExercises(
            @AuthenticationPrincipal CustomUserPrincipal userPrincipal,
            @Parameter(description = "조회 날짜 (yyyy-MM-dd)")
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate date) {

        log.info("일별 운동 기록 조회 요청 - 사용자: {}, 날짜: {}", userPrincipal.getUserId(), date);

        ExerciseDto.DailyExercisesResponse response = exerciseService.getDailyExercises(userPrincipal, date);

        return ResponseEntity.ok(response);
    }

    /**
     * 특정 운동 기록 상세 조회
     */
    @GetMapping("/{exerciseId}")
    @Operation(summary = "운동 기록 상세 조회", description = "특정 운동 기록의 상세 정보를 조회합니다")
    public ResponseEntity<ExerciseDto.ExerciseDetailResponse> getExerciseDetail(
            @AuthenticationPrincipal CustomUserPrincipal userPrincipal,
            @Parameter(description = "운동 기록 ID") @PathVariable Long exerciseId) {

        log.info("운동 기록 상세 조회 요청 - 사용자: {}, 운동 ID: {}", userPrincipal.getUserId(), exerciseId);

        ExerciseDto.ExerciseDetailResponse response = exerciseService.getExerciseDetail(userPrincipal, exerciseId);

        return ResponseEntity.ok(response);
    }

    /**
     * 주간 운동 통계 조회
     */
    @GetMapping("/weekly-stats")
    @Operation(summary = "주간 운동 통계 조회", description = "특정 주간의 운동 통계를 조회합니다")
    public ResponseEntity<ExerciseDto.WeeklyExerciseStatsResponse> getWeeklyStats(
            @AuthenticationPrincipal CustomUserPrincipal userPrincipal,
            @Parameter(description = "시작 날짜 (yyyy-MM-dd)")
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate startDate,
            @Parameter(description = "종료 날짜 (yyyy-MM-dd)")
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate endDate) {

        log.info("주간 운동 통계 조회 요청 - 사용자: {}, 기간: {} ~ {}",
                userPrincipal.getUserId(), startDate, endDate);

        ExerciseDto.WeeklyExerciseStatsResponse response = exerciseService.getWeeklyStats(userPrincipal, startDate, endDate);

        return ResponseEntity.ok(response);
    }

    /**
     * 체지방 감량 분석 조회
     */
    @GetMapping("/fat-loss-analysis")
    @Operation(summary = "체지방 감량 분석", description = "사용자의 체지방 감량 진행 상황과 권장 운동량을 분석합니다")
    public ResponseEntity<ExerciseDto.FatLossAnalysisResponse> getFatLossAnalysis(
            @AuthenticationPrincipal CustomUserPrincipal userPrincipal) {

        log.info("체지방 감량 분석 요청 - 사용자: {}", userPrincipal.getUserId());

        ExerciseDto.FatLossAnalysisResponse response = exerciseService.getFatLossAnalysis(userPrincipal);

        return ResponseEntity.ok(response);
    }
}
