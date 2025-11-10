package rto.intelfit.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import rto.intelfit.domain.RecommendedMealPlan;
import rto.intelfit.dto.RecommendedMealDto;
import rto.intelfit.security.CustomUserPrincipal;
import rto.intelfit.service.AIServerService;
import rto.intelfit.service.RecommendedMealService;

@Slf4j
@RestController
@RequestMapping("/api/recommended-meals")
@RequiredArgsConstructor
@Tag(name = "Recommended Meal API", description = "AI 추천 식단 관련 API")
@SecurityRequirement(name = "bearerAuth")
public class RecommendedMealController {

    private final RecommendedMealService recommendedMealService;
    private final AIServerService aiServerService;

    // 신규: 일간 추천 생성 (권장 엔드포인트)
    @Operation(
            summary = "AI 일간 식단 생성",
            description = "사용자 프로필(목표/활동량 등)을 기반으로 AI 서버에서 하루치 식단을 생성하고 저장합니다."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "추천 식단 생성 성공"),
            @ApiResponse(responseCode = "401", description = "인증 필요"),
            @ApiResponse(responseCode = "500", description = "AI 서버 오류")
    })
    @PostMapping(value = "/daily", produces = "application/json")
    public ResponseEntity<RecommendedMealDto.RecommendedPlanDetailResponse> createDailyPlan(
            @AuthenticationPrincipal CustomUserPrincipal userPrincipal,
            @Parameter(description = "하루 끼니 수(예: 3)", example = "3")
            @RequestParam(name = "mealsPerDay", required = false) Integer mealsPerDay,
            @Parameter(description = "목표 오버라이드(예: DIET/BULK/MAINTENANCE)", example = "DIET")
            @RequestParam(name = "goalOverride", required = false) String goalOverride
    ) {
        // 현재 AIServerService는 mealsPerDay/goalOverride 미사용 → 추후 서비스 시그니처 확장 시 여기만 1줄 교체
        log.info("AI 일간 식단 생성 요청 - userId={}, mealsPerDay={}, goalOverride={}",
                userPrincipal.getUserId(), mealsPerDay, goalOverride);

        RecommendedMealPlan plan = aiServerService.requestRecommendedMealPlan(userPrincipal);
        RecommendedMealDto.RecommendedPlanDetailResponse response =
                RecommendedMealDto.RecommendedPlanDetailResponse.from(plan);
        return ResponseEntity.ok(response);
    }

    // 기존 generate 엔드포인트: 유지하되 문서상 deprecated 처리
    @Operation(
            summary = "[Deprecated] AI 추천 식단 생성",
            description = "새 엔드포인트(/daily) 사용 권장. 기존 동작과 동일합니다."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "추천 식단 생성 성공"),
            @ApiResponse(responseCode = "401", description = "인증 필요"),
            @ApiResponse(responseCode = "500", description = "AI 서버 오류")
    })
    @PostMapping(value = "/generate", produces = "application/json")
    public ResponseEntity<RecommendedMealDto.RecommendedPlanDetailResponse> generateRecommendedMealPlan(
            @AuthenticationPrincipal CustomUserPrincipal userPrincipal
    ) {
        log.info("AI 추천 식단 생성 요청(legacy) - userId={}", userPrincipal.getUserId());
        RecommendedMealPlan plan = aiServerService.requestRecommendedMealPlan(userPrincipal);
        RecommendedMealDto.RecommendedPlanDetailResponse response =
                RecommendedMealDto.RecommendedPlanDetailResponse.from(plan);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "추천 식단 저장", description = "AI가 생성한 추천 식단을 저장 목록에 추가합니다")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "추천 식단 저장 성공"),
            @ApiResponse(responseCode = "401", description = "인증 필요"),
            @ApiResponse(responseCode = "403", description = "본인 식단만 저장 가능"),
            @ApiResponse(responseCode = "404", description = "식단을 찾을 수 없음")
    })
    @PostMapping("/{planId}/save")
    public ResponseEntity<RecommendedMealDto.SaveRecommendedPlanResponse> saveRecommendedPlan(
            @AuthenticationPrincipal CustomUserPrincipal userPrincipal,
            @Parameter(description = "추천 식단 ID", example = "1")
            @PathVariable Long planId
    ) {
        log.info("추천 식단 저장 요청 - userId={}, planId={}", userPrincipal.getUserId(), planId);
        var response = recommendedMealService.saveRecommendedPlan(userPrincipal, planId);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "추천 식단 저장 취소", description = "저장된 추천 식단을 저장 목록에서 제거합니다")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "저장 취소 성공"),
            @ApiResponse(responseCode = "401", description = "인증 필요"),
            @ApiResponse(responseCode = "403", description = "본인 식단만 수정 가능"),
            @ApiResponse(responseCode = "404", description = "식단을 찾을 수 없음")
    })
    @PostMapping("/{planId}/unsave")
    public ResponseEntity<RecommendedMealDto.SaveRecommendedPlanResponse> unsaveRecommendedPlan(
            @AuthenticationPrincipal CustomUserPrincipal userPrincipal,
            @Parameter(description = "추천 식단 ID", example = "1")
            @PathVariable Long planId
    ) {
        log.info("추천 식단 저장 취소 요청 - userId={}, planId={}", userPrincipal.getUserId(), planId);
        var response = recommendedMealService.unsaveRecommendedPlan(userPrincipal, planId);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "저장된 추천 식단 목록 조회", description = "사용자가 저장한 추천 식단 목록을 조회합니다")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "저장 목록 조회 성공"),
            @ApiResponse(responseCode = "401", description = "인증 필요")
    })
    @GetMapping("/saved")
    public ResponseEntity<RecommendedMealDto.SavedRecommendedPlansResponse> getSavedRecommendedPlans(
            @AuthenticationPrincipal CustomUserPrincipal userPrincipal
    ) {
        log.info("저장된 추천 식단 목록 조회 - userId={}", userPrincipal.getUserId());
        var response = recommendedMealService.getSavedRecommendedPlans(userPrincipal);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "추천 식단 상세 조회", description = "특정 추천 식단의 상세 정보를 조회합니다")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "식단 상세 조회 성공"),
            @ApiResponse(responseCode = "401", description = "인증 필요"),
            @ApiResponse(responseCode = "403", description = "본인 식단만 조회 가능"),
            @ApiResponse(responseCode = "404", description = "식단을 찾을 수 없음")
    })
    @GetMapping("/{planId}")
    public ResponseEntity<RecommendedMealDto.RecommendedPlanDetailResponse> getRecommendedPlanDetail(
            @AuthenticationPrincipal CustomUserPrincipal userPrincipal,
            @Parameter(description = "추천 식단 ID", example = "1")
            @PathVariable Long planId
    ) {
        log.info("추천 식단 상세 조회 요청 - userId={}, planId={}", userPrincipal.getUserId(), planId);
        var response = recommendedMealService.getRecommendedPlanDetail(userPrincipal, planId);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "추천 식단 삭제", description = "추천 식단을 완전히 삭제합니다")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "식단 삭제 성공"),
            @ApiResponse(responseCode = "401", description = "인증 필요"),
            @ApiResponse(responseCode = "403", description = "본인 식단만 삭제 가능"),
            @ApiResponse(responseCode = "404", description = "식단을 찾을 수 없음")
    })
    @DeleteMapping("/{planId}")
    public ResponseEntity<Void> deleteRecommendedPlan(
            @AuthenticationPrincipal CustomUserPrincipal userPrincipal,
            @Parameter(description = "추천 식단 ID", example = "1")
            @PathVariable Long planId
    ) {
        log.info("추천 식단 삭제 요청 - userId={}, planId={}", userPrincipal.getUserId(), planId);
        recommendedMealService.deleteRecommendedPlan(userPrincipal, planId);
        return ResponseEntity.noContent().build();
    }
}
