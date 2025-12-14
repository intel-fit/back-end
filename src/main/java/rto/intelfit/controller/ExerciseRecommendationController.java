package rto.intelfit.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import rto.intelfit.dto.RecommendedExerciseDto;
import rto.intelfit.security.CustomUserPrincipal;
import rto.intelfit.service.ExerciseRecommendationService;
// import 추가
import rto.intelfit.dto.WeeklyRecommendedExerciseSaveDto;

import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import jakarta.validation.Valid;
import rto.intelfit.dto.RecommendedExerciseDto;
import rto.intelfit.domain.UserRecommendedExercise; // 필요하면
import java.util.List;
import rto.intelfit.service.TempExerciseSummaryService;
import rto.intelfit.dto.TempExerciseSummaryDto;
import java.util.Map;

import java.time.LocalDate;

/**
 * AI 기반 운동 추천 컨트롤러
 *
 * 주요 기능:
 * 1. 사용자 맞춤 운동 추천 생성
 * 2. 식단 연동 운동 추천
 * 3. 추천 운동 플랜 조회/저장
 */
@Slf4j
@RestController
@RequestMapping("/api/exercise-recommendations")
@RequiredArgsConstructor
@Tag(name = "Exercise Recommendation", description = "AI 기반 운동 추천 API")
public class ExerciseRecommendationController {

    private final ExerciseRecommendationService exerciseRecommendationService;
    private final TempExerciseSummaryService tempExerciseSummaryService;

    // ===========================
    // 🔹 일일 운동 추천 API 추가
    // ===========================
    @PostMapping("/generate/daily")
    @Operation(summary = "AI 일일 운동 추천 생성",
            description = "사용자 정보 + 인바디 + 프론트 입력을 기반으로 AI 서버의 /ai/exercise_plan/daily 를 호출합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "일일 운동 추천 생성 성공"),
            @ApiResponse(responseCode = "400", description = "요청 값 오류"),
            @ApiResponse(responseCode = "401", description = "인증 필요"),
            @ApiResponse(responseCode = "404", description = "인바디 정보 없음")
    })
    public ResponseEntity<RecommendedExerciseDto.DailyRecommendationResponse> generateDailyRecommendation(
            @AuthenticationPrincipal CustomUserPrincipal userPrincipal,
            @Valid @RequestBody RecommendedExerciseDto.DailyRecommendationRequest request
    ) {
        log.info("AI 일일 운동 추천 생성 요청 - 사용자: {}, 요청: {}",
                userPrincipal.getUserId(), request);
//
        RecommendedExerciseDto.DailyRecommendationResponse response =
                exerciseRecommendationService.generateDailyRecommendation(userPrincipal, request);

        return ResponseEntity.ok(response);
    }

    @GetMapping("/temp")
    @Operation(summary = "TEMP 홈요약 조회",
            description = "특정 날짜 기준으로 TEMP 운동 요약 정보를 조회합니다.")
    public ResponseEntity<?> getTempSummary(
            @AuthenticationPrincipal CustomUserPrincipal principal,
            @RequestParam(required = false)
            @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate date
    ) {

        LocalDate targetDate = (date != null) ? date : LocalDate.now();

        log.info("📤 TEMP Summary 조회 요청 user={}, date={}",
                principal.getUserId(), targetDate);

        TempExerciseSummaryDto summary =
                tempExerciseSummaryService.getTempSummary(principal.getUserId(), targetDate);

        return ResponseEntity.ok(summary != null ? summary : Map.of());
    }


    @PostMapping("/weekly/save")
    @Operation(summary = "7일 AI 추천 운동 저장",
            description = "프론트가 AI 7일 추천값을 전달하면 날짜별 추천 운동을 DB에 저장합니다.")
    public ResponseEntity<?> saveWeeklyRecommendations(
            @AuthenticationPrincipal CustomUserPrincipal userPrincipal,
            @RequestBody WeeklyRecommendedExerciseSaveDto dto
    ) {

        log.info("📥 7일 추천 운동 저장 요청 - user={}, days={}",
                userPrincipal.getUserId(),
                dto.getDays().size());

        exerciseRecommendationService.saveWeeklyRecommendations(userPrincipal.getUserId(), dto);

        return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "7일 추천 운동 저장 완료"
        ));
    }

    /**
     * AI 기반 맞춤 운동 추천 생성
     */



    @PostMapping("/generate")
    @Operation(summary = "AI 운동 추천 생성", 
               description = "사용자의 건강 정보, 목표, 식단 데이터를 기반으로 AI가 맞춤 운동을 추천합니다")
    public ResponseEntity<RecommendedExerciseDto.GenerateRecommendationResponse> generateRecommendation(
            @AuthenticationPrincipal CustomUserPrincipal userPrincipal,
            @Parameter(description = "추천 기준 날짜 (yyyy-MM-dd), 미지정 시 오늘 날짜")
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate baseDate) {

        log.info("AI 운동 추천 생성 요청 - 사용자: {}, 기준 날짜: {}",
                userPrincipal.getUserId(), baseDate != null ? baseDate : "오늘");

        LocalDate targetDate = baseDate != null ? baseDate : LocalDate.now();

        RecommendedExerciseDto.GenerateRecommendationResponse response =
                exerciseRecommendationService.generateRecommendation(userPrincipal, targetDate);

        return ResponseEntity.ok(response);
    }

    //추천된 운동 조회
        @GetMapping("/recommended-exercises")
    @Operation(summary = "날짜별 AI 추천 운동 조회",
            description = "AI 추천 운동을 exercise_date 기준으로 묶어서 반환합니다.")
    public ResponseEntity<?> getAiRecommendedExercises(
            @AuthenticationPrincipal CustomUserPrincipal principal) {

        Map<LocalDate, List<RecommendedExerciseDto.ExerciseSimpleResponse>> response =
                exerciseRecommendationService.getGroupedRecommendedExercises(principal);

        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/temp")
    @Operation(
            summary = "TEMP 홈요약 범위 삭제",
            description = "startDate ~ endDate 범위에 해당하는 TEMP 운동 요약을 모두 삭제합니다."
    )
    public ResponseEntity<?> deleteTempSummariesInRange(
            @AuthenticationPrincipal CustomUserPrincipal userPrincipal,
            @RequestParam("startDate")
            @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate startDate,
            @RequestParam("endDate")
            @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate endDate
    ) {

        if (endDate.isBefore(startDate)) {
            throw new rto.intelfit.exception.BusinessException(
                    rto.intelfit.exception.ErrorCode.INVALID_INPUT_VALUE,
                    "endDate는 startDate보다 앞설 수 없습니다."
            );
        }

        int deletedCount = tempExerciseSummaryService.deleteTempSummariesInRange(
                userPrincipal.getUserId(),
                startDate,
                endDate
        );

        log.info("🗑 TEMP Summary 삭제 완료 user={}, start={}, end={}, deleted={}",
                userPrincipal.getUserId(), startDate, endDate, deletedCount);

        return ResponseEntity.ok(Map.of(
                "success", true,
                "deletedCount", deletedCount,
                "startDate", startDate,
                "endDate", endDate
        ));
    }



    /**
     * 식단 연동 운동 추천 생성
     */
    @PostMapping("/generate-with-meal")
    @Operation(summary = "식단 연동 운동 추천", 
               description = "특정 날짜의 식단 섭취량을 고려하여 필요한 운동을 추천합니다")
    public ResponseEntity<RecommendedExerciseDto.MealBasedRecommendationResponse> generateMealBasedRecommendation(
            @AuthenticationPrincipal CustomUserPrincipal userPrincipal,
            @Parameter(description = "식단 기준 날짜 (yyyy-MM-dd)")
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate mealDate) {

        log.info("식단 연동 운동 추천 생성 요청 - 사용자: {}, 식단 날짜: {}",
                userPrincipal.getUserId(), mealDate);

        RecommendedExerciseDto.MealBasedRecommendationResponse response =
                exerciseRecommendationService.generateMealBasedRecommendation(userPrincipal, mealDate);

        return ResponseEntity.ok(response);
    }

    /**
     * 저장된 추천 운동 플랜 목록 조회
     */
    @GetMapping({"", "/"})
    @Operation(summary = "저장된 추천 플랜 목록", 
               description = "사용자가 저장한 추천 운동 플랜 목록을 조회합니다")
    public ResponseEntity<RecommendedExerciseDto.SavedPlansResponse> getSavedPlans(
            @AuthenticationPrincipal CustomUserPrincipal userPrincipal) {

        log.info("저장된 추천 플랜 목록 조회 요청 - 사용자: {}", userPrincipal.getUserId());

        RecommendedExerciseDto.SavedPlansResponse response =
                exerciseRecommendationService.getSavedPlans(userPrincipal);

        return ResponseEntity.ok(response);
    }

    /**
     * 특정 추천 플랜 상세 조회
     */
    @GetMapping("/{planId:\\d+}")
    @Operation(summary = "추천 플랜 상세 조회", 
               description = "특정 추천 운동 플랜의 상세 정보를 조회합니다")
    public ResponseEntity<RecommendedExerciseDto.ExercisePlanDetailResponse> getPlanDetail(
            @AuthenticationPrincipal CustomUserPrincipal userPrincipal,
            @Parameter(description = "추천 플랜 ID") @PathVariable Long planId) {

        log.info("추천 플랜 상세 조회 요청 - 사용자: {}, 플랜 ID: {}",
                userPrincipal.getUserId(), planId);

        RecommendedExerciseDto.ExercisePlanDetailResponse response =
                exerciseRecommendationService.getPlanDetail(userPrincipal, planId);

        return ResponseEntity.ok(response);
    }

    /**
     * 추천 플랜 저장
     */
    @PostMapping("/{planId}/save")
    @Operation(summary = "추천 플랜 저장", 
               description = "AI가 생성한 추천 플랜을 사용자 계정에 저장합니다")
    public ResponseEntity<RecommendedExerciseDto.SavePlanResponse> savePlan(
            @AuthenticationPrincipal CustomUserPrincipal userPrincipal,
            @Parameter(description = "추천 플랜 ID") @PathVariable Long planId) {

        log.info("추천 플랜 저장 요청 - 사용자: {}, 플랜 ID: {}",
                userPrincipal.getUserId(), planId);

        RecommendedExerciseDto.SavePlanResponse response =
                exerciseRecommendationService.savePlan(userPrincipal, planId);

        return ResponseEntity.ok(response);
    }

    /**  2
     * 추천 플랜 삭제
     */
    @DeleteMapping("/{planId}")
    @Operation(summary = "추천 플랜 삭제", 
               description = "저장된 추천 플랜을 삭제합니다")
    public ResponseEntity<Void> deletePlan(
            @AuthenticationPrincipal CustomUserPrincipal userPrincipal,
            @Parameter(description = "추천 플랜 ID") @PathVariable Long planId) {

        log.info("추천 플랜 삭제 요청 - 사용자: {}, 플랜 ID: {}",
                userPrincipal.getUserId(), planId);

        exerciseRecommendationService.deletePlan(userPrincipal, planId);

        return ResponseEntity.noContent().build();
    }

    /**
     * 추천 플랜을 실제 운동 기록으로 적용
     */
    @PostMapping("/{planId}/apply")
    @Operation(summary = "추천 플랜 적용", 
               description = "추천 플랜을 실제 운동 기록으로 저장합니다")
    public ResponseEntity<RecommendedExerciseDto.ApplyPlanResponse> applyPlan(
            @AuthenticationPrincipal CustomUserPrincipal userPrincipal,
            @Parameter(description = "추천 플랜 ID") @PathVariable Long planId,
            @Parameter(description = "적용할 운동 날짜 (yyyy-MM-dd)")
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate exerciseDate) {

        log.info("추천 플랜 적용 요청 - 사용자: {}, 플랜 ID: {}, 날짜: {}",
                userPrincipal.getUserId(), planId, exerciseDate);

        RecommendedExerciseDto.ApplyPlanResponse response =
                exerciseRecommendationService.applyPlan(userPrincipal, planId, exerciseDate);

        return ResponseEntity.ok(response);
    }
    @PostMapping("/temp")
    @Operation(summary = "홈화면 TEMP 운동요약 저장",
            description = "프론트가 AI 응답을 파싱하여 요약값만 TEMP 테이블에 저장합니다.")
    public ResponseEntity<?> saveTempSummary(
            @AuthenticationPrincipal CustomUserPrincipal userPrincipal,
            @RequestBody TempExerciseSummaryDto dto
    ) {

        log.info("📥 TEMP Summary 저장 요청 user={}, date={}",
                userPrincipal.getUserId(), dto.getDate());

        tempExerciseSummaryService.saveOrUpdateTempSummary(userPrincipal.getUserId(), dto);

        return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "TEMP summary 저장 완료"
        ));
    }

}
