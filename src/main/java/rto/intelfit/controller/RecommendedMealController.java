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

    @Operation(summary = "AI 추천 식단 생성",
            description = "사용자 정보와 선호 음식을 기반으로 AI 추천 식단을 생성합니다")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "추천 식단 생성 성공"),
            @ApiResponse(responseCode = "401", description = "인증이 필요합니다"),
            @ApiResponse(responseCode = "500", description = "AI 서버 오류")
    })
    @PostMapping("/generate")
    public ResponseEntity<RecommendedMealDto.RecommendedPlanDetailResponse> generateRecommendedMealPlan(
            @AuthenticationPrincipal CustomUserPrincipal userPrincipal) {
        log.info("AI 추천 식단 생성 요청 - 사용자 ID: {}", userPrincipal.getUserId());

        RecommendedMealPlan plan = aiServerService.requestRecommendedMealPlan(userPrincipal);
        RecommendedMealDto.RecommendedPlanDetailResponse response =
                RecommendedMealDto.RecommendedPlanDetailResponse.from(plan);

        return ResponseEntity.ok(response);
    }

    @Operation(summary = "추천 식단 저장",
            description = "AI가 생성한 추천 식단을 사용자의 저장 목록에 추가합니다")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "추천 식단 저장 성공"),
            @ApiResponse(responseCode = "401", description = "인증이 필요합니다"),
            @ApiResponse(responseCode = "403", description = "본인의 추천 식단만 저장할 수 있습니다"),
            @ApiResponse(responseCode = "404", description = "추천 식단을 찾을 수 없습니다")
    })
    @PostMapping("/{planId}/save")
    public ResponseEntity<RecommendedMealDto.SaveRecommendedPlanResponse> saveRecommendedPlan(
            @AuthenticationPrincipal CustomUserPrincipal userPrincipal,
            @Parameter(description = "추천 식단 ID", example = "1")
            @PathVariable Long planId) {
        log.info("추천 식단 저장 요청 - 사용자 ID: {}, 식단 ID: {}",
                userPrincipal.getUserId(), planId);

        RecommendedMealDto.SaveRecommendedPlanResponse response =
                recommendedMealService.saveRecommendedPlan(userPrincipal, planId);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "추천 식단 저장 취소",
            description = "저장된 추천 식단을 저장 목록에서 제거합니다")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "저장 취소 성공"),
            @ApiResponse(responseCode = "401", description = "인증이 필요합니다"),
            @ApiResponse(responseCode = "403", description = "본인의 추천 식단만 수정할 수 있습니다"),
            @ApiResponse(responseCode = "404", description = "추천 식단을 찾을 수 없습니다")
    })
    @PostMapping("/{planId}/unsave")
    public ResponseEntity<RecommendedMealDto.SaveRecommendedPlanResponse> unsaveRecommendedPlan(
            @AuthenticationPrincipal CustomUserPrincipal userPrincipal,
            @Parameter(description = "추천 식단 ID", example = "1")
            @PathVariable Long planId) {
        log.info("추천 식단 저장 취소 요청 - 사용자 ID: {}, 식단 ID: {}",
                userPrincipal.getUserId(), planId);

        RecommendedMealDto.SaveRecommendedPlanResponse response =
                recommendedMealService.unsaveRecommendedPlan(userPrincipal, planId);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "저장된 추천 식단 목록 조회",
            description = "사용자가 저장한 추천 식단 목록을 조회합니다")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "저장 목록 조회 성공"),
            @ApiResponse(responseCode = "401", description = "인증이 필요합니다")
    })
    @GetMapping("/saved")
    public ResponseEntity<RecommendedMealDto.SavedRecommendedPlansResponse> getSavedRecommendedPlans(
            @AuthenticationPrincipal CustomUserPrincipal userPrincipal) {
        log.info("저장된 추천 식단 목록 조회 요청 - 사용자 ID: {}", userPrincipal.getUserId());

        RecommendedMealDto.SavedRecommendedPlansResponse response =
                recommendedMealService.getSavedRecommendedPlans(userPrincipal);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "추천 식단 상세 조회",
            description = "특정 추천 식단의 상세 정보를 조회합니다")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "식단 상세 조회 성공"),
            @ApiResponse(responseCode = "401", description = "인증이 필요합니다"),
            @ApiResponse(responseCode = "403", description = "본인의 추천 식단만 조회할 수 있습니다"),
            @ApiResponse(responseCode = "404", description = "추천 식단을 찾을 수 없습니다")
    })
    @GetMapping("/{planId}")
    public ResponseEntity<RecommendedMealDto.RecommendedPlanDetailResponse> getRecommendedPlanDetail(
            @AuthenticationPrincipal CustomUserPrincipal userPrincipal,
            @Parameter(description = "추천 식단 ID", example = "1")
            @PathVariable Long planId) {
        log.info("추천 식단 상세 조회 요청 - 사용자 ID: {}, 식단 ID: {}",
                userPrincipal.getUserId(), planId);

        RecommendedMealDto.RecommendedPlanDetailResponse response =
                recommendedMealService.getRecommendedPlanDetail(userPrincipal, planId);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "추천 식단 삭제",
            description = "추천 식단을 완전히 삭제합니다")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "식단 삭제 성공"),
            @ApiResponse(responseCode = "401", description = "인증이 필요합니다"),
            @ApiResponse(responseCode = "403", description = "본인의 추천 식단만 삭제할 수 있습니다"),
            @ApiResponse(responseCode = "404", description = "추천 식단을 찾을 수 없습니다")
    })
    @DeleteMapping("/{planId}")
    public ResponseEntity<Void> deleteRecommendedPlan(
            @AuthenticationPrincipal CustomUserPrincipal userPrincipal,
            @Parameter(description = "추천 식단 ID", example = "1")
            @PathVariable Long planId) {
        log.info("추천 식단 삭제 요청 - 사용자 ID: {}, 식단 ID: {}",
                userPrincipal.getUserId(), planId);

        recommendedMealService.deleteRecommendedPlan(userPrincipal, planId);
        return ResponseEntity.noContent().build();
    }
}