package rto.intelfit.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import rto.intelfit.domain.RecommendedMealPlan;
import rto.intelfit.domain.User;
import rto.intelfit.dto.RecommendedMealDto;
import rto.intelfit.exception.BusinessException;
import rto.intelfit.exception.ErrorCode;
import rto.intelfit.repository.RecommendedMealPlanRepository;
import rto.intelfit.repository.UserRepository;
import rto.intelfit.security.CustomUserPrincipal;
import java.math.BigDecimal;
import java.util.Objects;
import java.util.List;
import java.util.stream.Collectors;
import java.util.UUID;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class RecommendedMealService {

    private final RecommendedMealPlanRepository recommendedMealPlanRepository;
    private final UserRepository userRepository;

    //----------------------------------------------------------------------------------------//
    @Transactional(readOnly = true)
    public RecommendedMealDto.SavedBundleSummaryListResponse getSavedBundleSummaries(CustomUserPrincipal userPrincipal) {

        User user = findUserByPrincipal(userPrincipal);

        // 저장된 번들 ID 목록
        List<String> bundleIds = recommendedMealPlanRepository.findMyBundleIdsOrderByLatest(user);

        // 각 번들의 총합 요약 계산
        List<RecommendedMealDto.BundleNutritionSummary> summaries = bundleIds.stream()
                .map(bundleId -> {
                    List<RecommendedMealPlan> plans =
                            recommendedMealPlanRepository.findByUserAndBundleIdOrderByBundleDayAsc(user, bundleId);

                    List<RecommendedMealPlan> savedPlans = plans.stream()
                            .filter(RecommendedMealPlan::getIsSaved)
                            .toList();

                    if (savedPlans.isEmpty()) return null;

                    BigDecimal totalCalories = savedPlans.stream()
                            .map(p -> p.getTotalCalories() == null ? BigDecimal.ZERO : p.getTotalCalories())
                            .reduce(BigDecimal.ZERO, BigDecimal::add);

                    BigDecimal totalProtein = savedPlans.stream()
                            .map(p -> p.getTotalProtein() == null ? BigDecimal.ZERO : p.getTotalProtein())
                            .reduce(BigDecimal.ZERO, BigDecimal::add);

                    BigDecimal totalCarbs = savedPlans.stream()
                            .map(p -> p.getTotalCarbs() == null ? BigDecimal.ZERO : p.getTotalCarbs())
                            .reduce(BigDecimal.ZERO, BigDecimal::add);

                    BigDecimal totalFat = savedPlans.stream()
                            .map(p -> p.getTotalFat() == null ? BigDecimal.ZERO : p.getTotalFat())
                            .reduce(BigDecimal.ZERO, BigDecimal::add);

                    return RecommendedMealDto.BundleNutritionSummary.builder()
                            .bundleId(bundleId)
                            .totalCalories(totalCalories)
                            .totalProtein(totalProtein)
                            .totalCarbs(totalCarbs)
                            .totalFat(totalFat)
                            .build();
                })
                .filter(Objects::nonNull)
                .toList();

        log.info("번들 요약 조회 완료 - userId={}, count={}", user.getUserId(), summaries.size());

        return RecommendedMealDto.SavedBundleSummaryListResponse.builder()
                .totalCount(summaries.size())
                .bundles(summaries)
                .build();
    }
    @Transactional(readOnly = true)
    public RecommendedMealDto.BundleDetailResponse getBundleDetail(
            CustomUserPrincipal userPrincipal, String bundleId) {

        User user = findUserByPrincipal(userPrincipal);

        List<RecommendedMealPlan> plans =
                recommendedMealPlanRepository.findByUserAndBundleIdOrderByBundleDayAsc(user, bundleId);

        if (plans.isEmpty()) {
            throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE, "해당 번들을 찾을 수 없습니다");
        }

        log.info("번들 상세 조회 - userId={}, bundleId={}, days={}",
                user.getUserId(), bundleId, plans.size());

        return RecommendedMealDto.BundleDetailResponse.builder()
                .bundleId(bundleId)
                .days(plans.size())
                .plans(plans.stream()
                        .map(RecommendedMealDto.RecommendedPlanDetailResponse::from)
                        .toList())
                .build();
    }
    @Transactional
    public RecommendedMealDto.DeleteBundleResponse deleteBundleById(
            CustomUserPrincipal userPrincipal, String bundleId) {

        User user = findUserByPrincipal(userPrincipal);

        List<RecommendedMealPlan> plans =
                recommendedMealPlanRepository.findByUserAndBundleIdOrderByBundleDayAsc(user, bundleId);

        if (plans.isEmpty()) {
            throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE, "삭제할 번들을 찾을 수 없습니다");
        }

        recommendedMealPlanRepository.deleteByUserAndBundleId(user, bundleId);

        log.info("번들 삭제 완료 - userId={}, bundleId={}", user.getUserId(), bundleId);

        return RecommendedMealDto.DeleteBundleResponse.builder()
                .success(true)
                .message("번들이 삭제되었습니다.")
                .bundleId(bundleId)
                .deletedCount(plans.size())
                .build();
    }



    //----------------------------------------------------------------------------------------//

    /**
     * AI 추천 식단 저장
     * (AI 서버로부터 받은 추천 식단을 저장)
     */
    @Transactional
    public RecommendedMealDto.SaveRecommendedPlanResponse saveRecommendedPlan(
            CustomUserPrincipal userPrincipal,
            Long planId) {

        User user = findUserByPrincipal(userPrincipal);

        RecommendedMealPlan plan = recommendedMealPlanRepository.findById(planId)
                .orElseThrow(() -> new BusinessException(ErrorCode.INVALID_INPUT_VALUE,
                        "추천 식단을 찾을 수 없습니다"));

        // 본인의 추천 식단인지 확인
        if (!plan.getUser().getId().equals(user.getId())) {
            throw new BusinessException(ErrorCode.ACCESS_DENIED,
                    "본인의 추천 식단만 저장할 수 있습니다");
        }

        // 저장 상태 업데이트
        plan.setIsSaved(true);
        RecommendedMealPlan savedPlan = recommendedMealPlanRepository.save(plan);

        log.info("추천 식단 저장 완료 - 사용자 ID: {}, 식단 ID: {}",
                user.getUserId(), planId);

        return RecommendedMealDto.SaveRecommendedPlanResponse.builder()
                .success(true)
                .message("추천 식단이 저장되었습니다")
                .plan(RecommendedMealDto.RecommendedPlanDetailResponse.from(savedPlan))
                .build();
    }

    /**
     * 저장된 추천 식단 목록 조회
     */
    public RecommendedMealDto.SavedRecommendedPlansResponse getSavedRecommendedPlans(
            CustomUserPrincipal userPrincipal) {

        User user = findUserByPrincipal(userPrincipal);

        List<RecommendedMealPlan> plans = recommendedMealPlanRepository
                .findByUserAndIsSavedTrueOrderByCreatedAtDesc(user);

        List<RecommendedMealDto.RecommendedPlanSummaryResponse> planSummaries = plans.stream()
                .map(RecommendedMealDto.RecommendedPlanSummaryResponse::from)
                .collect(Collectors.toList());

        log.info("저장된 추천 식단 목록 조회 - 사용자 ID: {}, 개수: {}",
                user.getUserId(), plans.size());

        return RecommendedMealDto.SavedRecommendedPlansResponse.builder()
                .totalCount(plans.size())
                .plans(planSummaries)
                .build();
    }
    //번들단위조회
    @Transactional(readOnly = true)
    public RecommendedMealDto.SavedRecommendedBundlesResponse getSavedRecommendedBundles(
            CustomUserPrincipal userPrincipal) {

        User user = findUserByPrincipal(userPrincipal);

        // 1️⃣ 저장된 번들 ID 목록을 최신순으로 가져오기
        List<String> bundleIds = recommendedMealPlanRepository.findMyBundleIdsOrderByLatest(user);

        // 2️⃣ 각 번들별로 1~7일 상세 식단 조회
        List<RecommendedMealDto.BundleSummary> bundles = bundleIds.stream()
                .map(bundleId -> {
                    List<RecommendedMealPlan> plans =
                            recommendedMealPlanRepository.findByUserAndBundleIdOrderByBundleDayAsc(user, bundleId);

                    // 번들 내 저장된 식단만 필터링
                    List<RecommendedMealPlan> savedPlans = plans.stream()
                            .filter(RecommendedMealPlan::getIsSaved)
                            .toList();

                    if (savedPlans.isEmpty()) return null;

                    // 대표 설명용: 1일차 또는 총합
                    RecommendedMealPlan first = savedPlans.get(0);

                    return RecommendedMealDto.BundleSummary.builder()
                            .bundleId(bundleId)
                            //번들 목록만 나오기 or day 정보도 나오기
                            .planCount(savedPlans.size())
                            .totalCalories(savedPlans.stream()
                                    .map(p -> p.getTotalCalories() != null ? p.getTotalCalories() : BigDecimal.ZERO)
                                    .reduce(BigDecimal.ZERO, BigDecimal::add))
                            .plans(savedPlans.stream()
                                    .map(RecommendedMealDto.RecommendedPlanDetailResponse::from)
                                    .toList())
                            .createdAt(first.getCreatedAt())
                            .build();
                })
                .filter(Objects::nonNull)
                .toList();

        log.info("저장된 번들 목록 조회 - userId={}, count={}", user.getUserId(), bundles.size());

        return RecommendedMealDto.SavedRecommendedBundlesResponse.builder()
                .totalCount(bundles.size())
                .bundles(bundles)
                .build();
    }


    /**
     * 추천 식단 상세 조회
     */
    public RecommendedMealDto.RecommendedPlanDetailResponse getRecommendedPlanDetail(
            CustomUserPrincipal userPrincipal,
            Long planId) {

        User user = findUserByPrincipal(userPrincipal);

        RecommendedMealPlan plan = recommendedMealPlanRepository.findById(planId)
                .orElseThrow(() -> new BusinessException(ErrorCode.INVALID_INPUT_VALUE,
                        "추천 식단을 찾을 수 없습니다"));

        // 본인의 추천 식단인지 확인
        if (!plan.getUser().getId().equals(user.getId())) {
            throw new BusinessException(ErrorCode.ACCESS_DENIED,
                    "본인의 추천 식단만 조회할 수 있습니다");
        }

        log.info("추천 식단 상세 조회 - 사용자 ID: {}, 식단 ID: {}",
                user.getUserId(), planId);

        return RecommendedMealDto.RecommendedPlanDetailResponse.from(plan);
    }

    /**
     * 추천 식단 저장 취소
     */
    @Transactional
    public RecommendedMealDto.SaveRecommendedPlanResponse unsaveRecommendedPlan(
            CustomUserPrincipal userPrincipal,
            Long planId) {

        User user = findUserByPrincipal(userPrincipal);

        RecommendedMealPlan plan = recommendedMealPlanRepository.findById(planId)
                .orElseThrow(() -> new BusinessException(ErrorCode.INVALID_INPUT_VALUE,
                        "추천 식단을 찾을 수 없습니다"));

        // 본인의 추천 식단인지 확인
        if (!plan.getUser().getId().equals(user.getId())) {
            throw new BusinessException(ErrorCode.ACCESS_DENIED,
                    "본인의 추천 식단만 수정할 수 있습니다");
        }

        // 저장 상태 해제
        plan.setIsSaved(false);
        RecommendedMealPlan savedPlan = recommendedMealPlanRepository.save(plan);

        log.info("추천 식단 저장 취소 - 사용자 ID: {}, 식단 ID: {}",
                user.getUserId(), planId);

        return RecommendedMealDto.SaveRecommendedPlanResponse.builder()
                .success(true)
                .message("추천 식단 저장이 취소되었습니다")
                .plan(RecommendedMealDto.RecommendedPlanDetailResponse.from(savedPlan))
                .build();
    }

    /**
     * 추천 식단 삭제
     */
    @Transactional
    public void deleteRecommendedPlan(CustomUserPrincipal userPrincipal, Long planId) {
        User user = findUserByPrincipal(userPrincipal);

        RecommendedMealPlan plan = recommendedMealPlanRepository.findById(planId)
                .orElseThrow(() -> new BusinessException(ErrorCode.INVALID_INPUT_VALUE,
                        "추천 식단을 찾을 수 없습니다"));

        // 본인의 추천 식단인지 확인
        if (!plan.getUser().getId().equals(user.getId())) {
            throw new BusinessException(ErrorCode.ACCESS_DENIED,
                    "본인의 추천 식단만 삭제할 수 있습니다");
        }

        recommendedMealPlanRepository.delete(plan);

        log.info("추천 식단 삭제 완료 - 사용자 ID: {}, 식단 ID: {}",
                user.getUserId(), planId);
    }
    // rto.intelfit.service.RecommendedMealService (추가 메서드들)
    @Transactional(readOnly = true)
    public List<RecommendedMealDto.RecommendedPlanDetailResponse> getBundlePlans(
            CustomUserPrincipal userPrincipal, String bundleId) {

        User user = findUserByPrincipal(userPrincipal);
        List<RecommendedMealPlan> plans = recommendedMealPlanRepository
                .findByUserAndBundleIdOrderByBundleDayAsc(user, bundleId);

        if (plans.isEmpty()) {
            throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE, "번들을 찾을 수 없습니다");
        }

        log.info("번들 조회 - userId={}, bundleId={}, count={}",
                user.getUserId(), bundleId, plans.size());

        return plans.stream()
                .map(RecommendedMealDto.RecommendedPlanDetailResponse::from)
                .collect(Collectors.toList());
    }

    /** 내 번들 ID 목록 (최신순) */
    @Transactional(readOnly = true)
    public List<String> getMyBundleIds(CustomUserPrincipal userPrincipal) {
        User user = findUserByPrincipal(userPrincipal);
        List<String> bundleIds = recommendedMealPlanRepository.findMyBundleIdsOrderByLatest(user);
        log.info("내 번들 목록 조회 - userId={}, count={}", user.getUserId(), bundleIds.size());
        return bundleIds;
    }
    // RecommendedMealService

    @Transactional
    public RecommendedMealDto.SaveRecommendedPlanResponse saveBundle(
            CustomUserPrincipal userPrincipal, String bundleId) {

        User user = findUserByPrincipal(userPrincipal);
        List<RecommendedMealPlan> plans =
                recommendedMealPlanRepository.findByUserAndBundleIdOrderByBundleDayAsc(user, bundleId);

        if (plans.isEmpty()) {
            throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE, "번들을 찾을 수 없습니다");
        }

        plans.forEach(p -> p.setIsSaved(true));
        recommendedMealPlanRepository.saveAll(plans);

        log.info("번들 저장 - userId={}, bundleId={}, count={}", user.getUserId(), bundleId, plans.size());

        // 대표 1건(예: day=1)로 상세 응답을 돌려주거나, 개수/메시지 중심으로 반환
        return RecommendedMealDto.SaveRecommendedPlanResponse.builder()
                .success(true)
                .message("번들(7일) 전체가 저장되었습니다")
                .plan(RecommendedMealDto.RecommendedPlanDetailResponse.from(plans.get(0)))
                .build();
    }

    @Transactional
    public RecommendedMealDto.SaveRecommendedPlanResponse unsaveBundle(
            CustomUserPrincipal userPrincipal, String bundleId) {

        User user = findUserByPrincipal(userPrincipal);
        List<RecommendedMealPlan> plans =
                recommendedMealPlanRepository.findByUserAndBundleIdOrderByBundleDayAsc(user, bundleId);

        if (plans.isEmpty()) {
            throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE, "번들을 찾을 수 없습니다");
        }

        plans.forEach(p -> p.setIsSaved(false));
        recommendedMealPlanRepository.saveAll(plans);

        log.info("번들 저장 취소 - userId={}, bundleId={}, count={}", user.getUserId(), bundleId, plans.size());

        return RecommendedMealDto.SaveRecommendedPlanResponse.builder()
                .success(true)
                .message("번들(7일) 저장이 취소되었습니다")
                .plan(RecommendedMealDto.RecommendedPlanDetailResponse.from(plans.get(0)))
                .build();
    }

    /** 번들 전체 삭제 (1~7일 모두) */
    @Transactional
    public void deleteBundle(CustomUserPrincipal userPrincipal, String bundleId) {
        User user = findUserByPrincipal(userPrincipal);
        // 권한 체크는 repository 쿼리 레벨에서 user 조건으로 제한됨
        recommendedMealPlanRepository.deleteByUserAndBundleId(user, bundleId);
        log.info("번들 삭제 완료 - userId={}, bundleId={}", user.getUserId(), bundleId);
    }



    private User findUserByPrincipal(CustomUserPrincipal userPrincipal) {
        return userRepository.findByUserId(userPrincipal.getUserId())
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
    }
    @Transactional
    public RecommendedMealDto.SaveRecommendedPlanResponse saveBundleFromClient(
            CustomUserPrincipal userPrincipal,
            RecommendedMealDto.SaveBundleRequest request
    ) {
        User user = findUserByPrincipal(userPrincipal);

        if (request.getPlans() == null || request.getPlans().isEmpty()) {
            throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE, "저장할 식단 정보가 없습니다");
        }

        String bundleId = UUID.randomUUID().toString();

        List<RecommendedMealPlan> entities = new ArrayList<>();

        int day = 1;
        for (RecommendedMealDto.RecommendedPlanDetailResponse dto : request.getPlans()) {

            RecommendedMealPlan plan = RecommendedMealPlan.builder()
                    .user(user)
                    .planName(dto.getPlanName())
                    .description(dto.getDescription())
                    .totalCalories(dto.getTotalCalories())
                    .totalCarbs(dto.getTotalCarbs())
                    .totalProtein(dto.getTotalProtein())
                    .totalFat(dto.getTotalFat())
                    .recommendationReason(dto.getRecommendationReason())
                    .isSaved(true)
                    .bundleId(bundleId)
                    .bundleDay(day++)
                    .planDate(dto.getPlanDate())
                    .build();

            // meals


            entities.add(plan);
        }

        // 7일치 저장
        recommendedMealPlanRepository.saveAll(entities);

        log.info("프론트 기반 번들 저장 완료 - userId={}, bundleId={}, days={}",
                user.getUserId(), bundleId, entities.size());

        return RecommendedMealDto.SaveRecommendedPlanResponse.builder()
                .success(true)
                .message("7일 식단이 저장되었습니다")
                .plan(RecommendedMealDto.RecommendedPlanDetailResponse.from(entities.get(0)))
                .build();
    }

}