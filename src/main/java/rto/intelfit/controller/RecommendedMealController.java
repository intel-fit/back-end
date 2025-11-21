// rto.intelfit.controller.RecommendedMealController
package rto.intelfit.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import java.time.LocalDate;
import java.util.List;

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

    // === 1️⃣ 일간 생성 ===
    @Operation(summary = "AI 일간 식단 생성", description = "하루치 식단을 생성하고 저장합니다")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "성공"),
            @ApiResponse(responseCode = "401", description = "인증 필요")
    })
    @PostMapping(value = "/daily", produces = "application/json")
    public ResponseEntity<RecommendedMealDto.RecommendedPlanDetailResponse> createDailyPlan(
            @AuthenticationPrincipal CustomUserPrincipal userPrincipal,
            @Parameter(description = "하루 끼니 수(예: 3)", example = "3")
            @RequestParam(name = "mealsPerDay", required = false) Integer mealsPerDay,
            @Parameter(description = "목표 오버라이드(예: DIET/BULK/MAINTENANCE)", example = "DIET")
            @RequestParam(name = "goalOverride", required = false) String goalOverride
    ) {
        log.info("AI 일간 식단 생성 요청 - userId={}, mealsPerDay={}, goalOverride={}",
                userPrincipal.getUserId(), mealsPerDay, goalOverride);

        RecommendedMealPlan plan = aiServerService.requestRecommendedMealPlan(userPrincipal);
        return ResponseEntity.ok(RecommendedMealDto.RecommendedPlanDetailResponse.from(plan));
    }

    // === 2️⃣ 주간(7일) 생성 ===
    @PostMapping("/week")
    @Operation(summary = "주간(7일) AI 식단 생성 및 저장")
    @Transactional
    public ResponseEntity<List<RecommendedMealDto.RecommendedPlanDetailResponse>> createWeeklyPlan(
            @AuthenticationPrincipal CustomUserPrincipal principal,
            @RequestParam(value = "start", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate start
    ) {
        LocalDate weekStart = (start != null) ? start : LocalDate.now();
        List<RecommendedMealDto.RecommendedPlanDetailResponse> response =
                aiServerService.requestWeeklyRecommendedMealPlans(principal, weekStart);
        return ResponseEntity.ok(response);
    }

    // === 3️⃣ 7일치 추천받은 식단 저장 / 취소 ===
    @PostMapping("/save-bundle")
    @Operation(summary = "추천 식단 번들 저장", description = "프론트에서 받은 7일치 DTO를 그대로 DB에 저장합니다")
    public ResponseEntity<RecommendedMealDto.SaveRecommendedPlanResponse> saveBundleFromClient(
            @AuthenticationPrincipal CustomUserPrincipal principal,
            @RequestBody RecommendedMealDto.SaveBundleRequest request
    ) {
        var resp = recommendedMealService.saveBundleFromClient(principal, request);
        return ResponseEntity.ok(resp);
    }

    // === 3️⃣ 추천 식단 저장 / 취소 ===
    @Operation(summary = "추천 식단 저장")
    @PostMapping("/{planId}/save")
    public ResponseEntity<RecommendedMealDto.SaveRecommendedPlanResponse> saveRecommendedPlan(
            @AuthenticationPrincipal CustomUserPrincipal userPrincipal,
            @Parameter(description = "추천 식단 ID", example = "1") @PathVariable Long planId
    ) {
        var response = recommendedMealService.saveRecommendedPlan(userPrincipal, planId);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "추천 식단 저장 취소")
    @PostMapping("/{planId}/unsave")
    public ResponseEntity<RecommendedMealDto.SaveRecommendedPlanResponse> unsaveRecommendedPlan(
            @AuthenticationPrincipal CustomUserPrincipal userPrincipal,
            @Parameter(description = "추천 식단 ID", example = "1") @PathVariable Long planId
    ) {
        var response = recommendedMealService.unsaveRecommendedPlan(userPrincipal, planId);
        return ResponseEntity.ok(response);
    }

    // === 4️⃣ 저장된 식단 조회 ===
    @Operation(summary = "저장된 추천 식단 목록 조회")
    @GetMapping("/saved")
    public ResponseEntity<RecommendedMealDto.SavedRecommendedPlansResponse> getSavedRecommendedPlans(
            @AuthenticationPrincipal CustomUserPrincipal userPrincipal
    ) {
        var response = recommendedMealService.getSavedRecommendedPlans(userPrincipal);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "저장된 식단 번들 목록 조회", description = "저장된 주간 번들별 식단을 일자순으로 반환")
    @GetMapping("/saved/bundles")
    public ResponseEntity<RecommendedMealDto.SavedRecommendedBundlesResponse> getSavedRecommendedBundles(
            @AuthenticationPrincipal CustomUserPrincipal userPrincipal
    ) {
        var resp = recommendedMealService.getSavedRecommendedBundles(userPrincipal);
        return ResponseEntity.ok(resp);
    }

    // === 5️⃣ 식단 상세 / 삭제 ===
    @Operation(summary = "추천 식단 상세 조회")
    @GetMapping("/{planId}")
    public ResponseEntity<RecommendedMealDto.RecommendedPlanDetailResponse> getRecommendedPlanDetail(
            @AuthenticationPrincipal CustomUserPrincipal userPrincipal,
            @Parameter(description = "추천 식단 ID", example = "1") @PathVariable Long planId
    ) {
        var response = recommendedMealService.getRecommendedPlanDetail(userPrincipal, planId);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "추천 식단 삭제")
    @DeleteMapping("/{planId}")
    public ResponseEntity<Void> deleteRecommendedPlan(
            @AuthenticationPrincipal CustomUserPrincipal userPrincipal,
            @Parameter(description = "추천 식단 ID", example = "1") @PathVariable Long planId
    ) {
        recommendedMealService.deleteRecommendedPlan(userPrincipal, planId);
        return ResponseEntity.noContent().build();
    }

    // === 6️⃣ 번들(주간) API ===

    @Operation(summary = "번들 저장", description = "bundleId에 속한 1~7일 식단을 모두 저장")
    @PostMapping("/bundles/{bundleId}/save")
    public ResponseEntity<RecommendedMealDto.SaveRecommendedPlanResponse> saveBundle(
            @AuthenticationPrincipal CustomUserPrincipal userPrincipal,
            @PathVariable String bundleId
    ) {
        var resp = recommendedMealService.saveBundle(userPrincipal, bundleId);
        return ResponseEntity.ok(resp);
    }

    @Operation(summary = "번들 저장 취소", description = "bundleId에 속한 1~7일 식단의 저장을 모두 해제")
    @PostMapping("/bundles/{bundleId}/unsave")
    public ResponseEntity<RecommendedMealDto.SaveRecommendedPlanResponse> unsaveBundle(
            @AuthenticationPrincipal CustomUserPrincipal userPrincipal,
            @PathVariable String bundleId
    ) {
        var resp = recommendedMealService.unsaveBundle(userPrincipal, bundleId);
        return ResponseEntity.ok(resp);
    }

    @Operation(summary = "내 번들 ID 목록 조회", description = "내가 가진 주간 번들의 bundleId 목록(최신순)")
    @GetMapping("/bundles")
    public ResponseEntity<List<String>> getMyBundles(
            @AuthenticationPrincipal CustomUserPrincipal userPrincipal
    ) {
        return ResponseEntity.ok(recommendedMealService.getMyBundleIds(userPrincipal));
    }

    @Operation(summary = "번들 상세 조회", description = "bundleId에 속한 1~7일 식단을 일자순으로 반환")
    @GetMapping("/bundles/{bundleId}")
    public ResponseEntity<List<RecommendedMealDto.RecommendedPlanDetailResponse>> getBundlePlans(
            @AuthenticationPrincipal CustomUserPrincipal userPrincipal,
            @PathVariable String bundleId
    ) {
        return ResponseEntity.ok(recommendedMealService.getBundlePlans(userPrincipal, bundleId));
    }

    @Operation(summary = "번들 삭제", description = "bundleId에 속한 1~7일 식단 전체 삭제")
    @DeleteMapping("/bundles/{bundleId}")
    public ResponseEntity<Void> deleteBundle(
            @AuthenticationPrincipal CustomUserPrincipal userPrincipal,
            @PathVariable String bundleId
    ) {
        recommendedMealService.deleteBundle(userPrincipal, bundleId);
        return ResponseEntity.noContent().build();
    }

    // === 7️⃣ 🔹 새로 추가된 번들 요약 조회 (총 칼로리/탄단지 리스트) ===
    @Operation(summary = "저장된 번들 요약 리스트 조회", description = "저장된 모든 번들의 총 칼로리/탄단지 정보를 요약 형태로 반환")
    @GetMapping("/bundles/summary")
    public ResponseEntity<RecommendedMealDto.SavedBundleSummaryListResponse> getSavedBundleSummaries(
            @AuthenticationPrincipal CustomUserPrincipal userPrincipal
    ) {
        var response = recommendedMealService.getSavedBundleSummaries(userPrincipal);
        return ResponseEntity.ok(response);
    }

    // === 8️⃣ 🔹 새로 추가된 번들 상세조회 (DTO형) ===
    @Operation(summary = "번들 상세 조회 (DTO 응답)", description = "bundleId에 해당하는 번들의 7일 식단을 DTO 형태로 반환")
    @GetMapping("/bundles/{bundleId}/detail")
    public ResponseEntity<RecommendedMealDto.BundleDetailResponse> getBundleDetail(
            @AuthenticationPrincipal CustomUserPrincipal userPrincipal,
            @PathVariable String bundleId
    ) {
        var response = recommendedMealService.getBundleDetail(userPrincipal, bundleId);
        return ResponseEntity.ok(response);
    }

    // === 9️⃣ 🔹 새로 추가된 번들 삭제 (DTO 응답) ===
    @Operation(summary = "번들 삭제 (DTO 응답)", description = "bundleId에 해당하는 번들을 삭제하고 삭제 결과를 반환")
    @DeleteMapping("/bundles/{bundleId}/delete")
    public ResponseEntity<RecommendedMealDto.DeleteBundleResponse> deleteBundleById(
            @AuthenticationPrincipal CustomUserPrincipal userPrincipal,
            @PathVariable String bundleId
    ) {
        var response = recommendedMealService.deleteBundleById(userPrincipal, bundleId);
        return ResponseEntity.ok(response);
    }
}
