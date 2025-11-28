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
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

import rto.intelfit.domain.User;
import rto.intelfit.dto.RecommendedMealDto;
import rto.intelfit.security.CustomUserPrincipal;
import rto.intelfit.service.AIServerService;
import rto.intelfit.service.RecommendedMealService;
import rto.intelfit.repository.UserRepository;
import rto.intelfit.exception.BusinessException;
import rto.intelfit.exception.ErrorCode;

@Slf4j
@RestController
@RequestMapping("/api/recommended-meals")
@RequiredArgsConstructor
@Tag(name = "Recommended Meal API", description = "AI 추천 식단 관련 API")
@SecurityRequirement(name = "bearerAuth")
public class RecommendedMealController {

    private final RecommendedMealService recommendedMealService;
    private final AIServerService aiServerService;
    private final UserRepository userRepository;

    // === 1️⃣ 일간 생성 ===
    @Operation(summary = "AI 일간 식단 생성")
    @PostMapping(value = "/daily", produces = "application/json")
    public ResponseEntity<RecommendedMealDto.RecommendedPlanDetailResponse> createDailyPlan(
            @AuthenticationPrincipal CustomUserPrincipal userPrincipal,
            @RequestParam(name = "mealsPerDay", required = false) Integer mealsPerDay,
            @RequestParam(name = "goalOverride", required = false) String goalOverride
    ) {

        var plan = aiServerService.requestRecommendedMealPlan(userPrincipal);
        return ResponseEntity.ok(RecommendedMealDto.RecommendedPlanDetailResponse.from(plan));
    }

    // === 2️⃣ 주간(7일) 생성 ===
    @Operation(summary = "주간(7일) AI 식단 생성 및 저장")
    @PostMapping("/week")
    public ResponseEntity<List<RecommendedMealDto.RecommendedPlanDetailResponse>> createWeeklyPlan(
            @AuthenticationPrincipal CustomUserPrincipal principal,
            @RequestParam(value = "start", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate start
    ) {

        User user = userRepository.findByUserId(principal.getUserId())
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        // 🔥 1) 무료 플랜이면 하루치 + 토큰 감소
        if (user.getMembershipType() == User.MembershipType.FREE) {
            log.info("무료 사용자 식단 요청 감지 - 하루치만 제공 debbbbbig userId={}", user.getId());
            return ResponseEntity.ok(
                    aiServerService.requestWeeklyRecommendedMealForFreeUser(user)
            );
        }

        // 🔥 2) 유료 사용자 → 기존 7일 생성
        LocalDate weekStart = (start != null) ? start : LocalDate.now();
        var response = aiServerService.requestWeeklyRecommendedMealPlans(principal, weekStart);
        log.info("aaaaaaaa2222");
        return ResponseEntity.ok(response);
    }


    // === 3️⃣ 7일치 추천받은 식단 저장 ===
    @PostMapping("/save-bundle")
    public ResponseEntity<RecommendedMealDto.SaveRecommendedPlanResponse> saveBundleFromClient(
            @AuthenticationPrincipal CustomUserPrincipal principal,
            @RequestBody RecommendedMealDto.SaveBundleRequest request
    ) {
        var resp = recommendedMealService.saveBundleFromClient(principal, request);
        return ResponseEntity.ok(resp);
    }

    // === 4️⃣ 추천 식단 저장 / 취소 ===
    @PostMapping("/{planId}/save")
    public ResponseEntity<RecommendedMealDto.SaveRecommendedPlanResponse> saveRecommendedPlan(
            @AuthenticationPrincipal CustomUserPrincipal userPrincipal,
            @PathVariable Long planId
    ) {
        var response = recommendedMealService.saveRecommendedPlan(userPrincipal, planId);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{planId}/unsave")
    public ResponseEntity<RecommendedMealDto.SaveRecommendedPlanResponse> unsaveRecommendedPlan(
            @AuthenticationPrincipal CustomUserPrincipal userPrincipal,
            @PathVariable Long planId
    ) {
        var response = recommendedMealService.unsaveRecommendedPlan(userPrincipal, planId);
        return ResponseEntity.ok(response);
    }
    @Operation(summary = "저장된 번들의 영양 요약 조회", description = "사용자가 저장한 모든 번들의 총 칼로리/영양 요약을 반환합니다.")
    @GetMapping("/saved/bundles/summary")
    public ResponseEntity<RecommendedMealDto.SavedBundleSummaryListResponse> getSavedBundleSummaries(
            @AuthenticationPrincipal CustomUserPrincipal principal
    ) {
        return ResponseEntity.ok(
                recommendedMealService.getSavedBundleSummaries(principal)
        );
    }


    // === 5️⃣ 저장된 식단 조회 ===
    @GetMapping("/saved")
    public ResponseEntity<RecommendedMealDto.SavedRecommendedPlansResponse> getSavedRecommendedPlans(
            @AuthenticationPrincipal CustomUserPrincipal userPrincipal
    ) {
        var response = recommendedMealService.getSavedRecommendedPlans(userPrincipal);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/saved/bundles")
    public ResponseEntity<RecommendedMealDto.SavedRecommendedBundlesResponse> getSavedRecommendedBundles(
            @AuthenticationPrincipal CustomUserPrincipal userPrincipal
    ) {
        var response = recommendedMealService.getSavedRecommendedBundles(userPrincipal);
        return ResponseEntity.ok(response);
    }

    // === 6️⃣ 식단 상세 ===
    @GetMapping("/{planId}")
    public ResponseEntity<RecommendedMealDto.RecommendedPlanDetailResponse> getRecommendedPlanDetail(
            @AuthenticationPrincipal CustomUserPrincipal userPrincipal,
            @PathVariable Long planId
    ) {
        var response = recommendedMealService.getRecommendedPlanDetail(userPrincipal, planId);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{planId}")
    public ResponseEntity<Void> deleteRecommendedPlan(
            @AuthenticationPrincipal CustomUserPrincipal userPrincipal,
            @PathVariable Long planId
    ) {
        recommendedMealService.deleteRecommendedPlan(userPrincipal, planId);
        return ResponseEntity.noContent().build();
    }

    // === 7️⃣ 번들 저장 / 삭제 ===
    @PostMapping("/bundles/{bundleId}/save")
    public ResponseEntity<RecommendedMealDto.SaveRecommendedPlanResponse> saveBundle(
            @AuthenticationPrincipal CustomUserPrincipal userPrincipal,
            @PathVariable String bundleId
    ) {
        var resp = recommendedMealService.saveBundle(userPrincipal, bundleId);
        return ResponseEntity.ok(resp);
    }

    @PostMapping("/bundles/{bundleId}/unsave")
    public ResponseEntity<RecommendedMealDto.SaveRecommendedPlanResponse> unsaveBundle(
            @AuthenticationPrincipal CustomUserPrincipal userPrincipal,
            @PathVariable String bundleId
    ) {
        var resp = recommendedMealService.unsaveBundle(userPrincipal, bundleId);
        return ResponseEntity.ok(resp);
    }

    @GetMapping("/bundles")
    public ResponseEntity<List<String>> getMyBundles(
            @AuthenticationPrincipal CustomUserPrincipal userPrincipal
    ) {
        return ResponseEntity.ok(recommendedMealService.getMyBundleIds(userPrincipal));
    }

    @GetMapping("/bundles/{bundleId}")
    public ResponseEntity<List<RecommendedMealDto.RecommendedPlanDetailResponse>> getBundlePlans(
            @AuthenticationPrincipal CustomUserPrincipal userPrincipal,
            @PathVariable String bundleId
    ) {
        return ResponseEntity.ok(recommendedMealService.getBundlePlans(userPrincipal, bundleId));
    }

    @DeleteMapping("/bundles/{bundleId}")
    public ResponseEntity<Void> deleteBundle(
            @AuthenticationPrincipal CustomUserPrincipal userPrincipal,
            @PathVariable String bundleId
    ) {
        recommendedMealService.deleteBundle(userPrincipal, bundleId);
        return ResponseEntity.noContent().build();
    }
}
