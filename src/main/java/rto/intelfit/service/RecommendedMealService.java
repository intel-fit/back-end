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

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class RecommendedMealService {

    private final RecommendedMealPlanRepository recommendedMealPlanRepository;
    private final UserRepository userRepository;

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
     * 추천 식단 저장 취소()
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



    private User findUserByPrincipal(CustomUserPrincipal userPrincipal) {
        return userRepository.findByUserId(userPrincipal.getUserId())
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
    }
}