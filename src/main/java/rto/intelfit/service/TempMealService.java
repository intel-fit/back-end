package rto.intelfit.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import rto.intelfit.domain.User;
import rto.intelfit.domain.RecommendedMealPlan;
import rto.intelfit.domain.RecommendedMeal;
import rto.intelfit.domain.RecommendedFood;

import rto.intelfit.domain.TempMealDomain.TempMealBundle;
import rto.intelfit.domain.TempMealDomain.TempMealPlan;
import rto.intelfit.domain.TempMealDomain.TempMeal;
import rto.intelfit.domain.TempMealDomain.TempMealFood;

import rto.intelfit.repository.TempBundleRepo;
import rto.intelfit.repository.TempPlanRepo;
import rto.intelfit.repository.TempMealRepo;
import rto.intelfit.repository.TempFoodRepo;

import rto.intelfit.repository.UserRepository;
import rto.intelfit.repository.RecommendedMealPlanRepository;
import rto.intelfit.security.CustomUserPrincipal;
import rto.intelfit.exception.BusinessException;
import rto.intelfit.exception.ErrorCode;
import rto.intelfit.dto.TempMealDto;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class TempMealService {

    private final TempBundleRepo bundleRepo;
    private final TempPlanRepo planRepo;
    private final TempMealRepo mealRepo;
    private final TempFoodRepo foodRepo;

    private final UserRepository userRepository;
    private final RecommendedMealPlanRepository recommendedMealPlanRepository;
    private final AIServerService aiServerService;

    // -----------------------------------------------------------
    // 1) TEMP 생성 (AI 기반 주간 식단)
    // -----------------------------------------------------------
    @Transactional
    public Long generateWeeklyTempPlans(
            CustomUserPrincipal principal,
            TempMealDto.WeeklyRequest request
    ) {

        User user = userRepository.findByUserId(principal.getUserId())
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        int mealsPerDay = request.getMealsPerDay();

        // 기존 FREE 제한 로직 유지
        if (user.getMembershipType() == User.MembershipType.FREE) {
            resetWeeklyTokensIfNeeded(user);
            if (user.getMealRecommendTokens() <= 0)
                throw new BusinessException(ErrorCode.NO_MEAL_TOKENS);
        }

        // 기존 temp 삭제 후 생성
        bundleRepo.findByUser(user).ifPresent(bundleRepo::delete);
        TempMealBundle bundle = bundleRepo.save(TempMealBundle.builder().user(user).build());

        // AI 7일치 생성 (끼니수 반영)
        List<RecommendedMealPlan> weekly =
                aiServerService.requestWeeklyMealWithMealsPerDay(principal, mealsPerDay);

        int index = 1;

        for (RecommendedMealPlan plan : weekly) {

            TempMealPlan tempPlan = planRepo.save(
                    TempMealPlan.builder()
                            .tempBundle(bundle)
                            .dayIndex(index++)
                            .totalCalories(plan.getTotalCalories())
                            .totalCarbs(plan.getTotalCarbs())
                            .totalProtein(plan.getTotalProtein())
                            .totalFat(plan.getTotalFat())
                            .build()
            );

            for (RecommendedMeal rm : plan.getRecommendedMeals()) {

                TempMeal tempMeal = mealRepo.save(
                        TempMeal.builder()
                                .tempMealPlan(tempPlan)
                                .mealType(rm.getMealType())
                                .totalCalories(rm.getTotalCalories())
                                .totalCarbs(rm.getTotalCarbs())
                                .totalProtein(rm.getTotalProtein())
                                .totalFat(rm.getTotalFat())
                                .build()
                );

                for (RecommendedFood rf : rm.getRecommendedFoods()) {
                    foodRepo.save(
                            TempMealFood.builder()
                                    .tempMeal(tempMeal)
                                    .foodName(rf.getFoodName())
                                    .servingSize(rf.getServingSize())
                                    .calories(rf.getCalories())
                                    .carbs(rf.getCarbs())
                                    .protein(rf.getProtein())
                                    .fat(rf.getFat())
                                    .build()
                    );
                }
            }
        }

        if (user.getMembershipType() == User.MembershipType.FREE) {
            user.setMealRecommendTokens(user.getMealRecommendTokens() - 1);
        }

        return bundle.getId();
    }




    @Transactional
    public Long generateDailyPaid(
            CustomUserPrincipal principal,
            TempMealDto.DailyRequest request
    ) {

        User user = userRepository.findByUserId(principal.getUserId())
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        // 무료는 하루치만 받으려면 토큰 차감
        if (user.getMembershipType() == User.MembershipType.FREE) {
            resetWeeklyTokensIfNeeded(user);
            if (user.getMealRecommendTokens() <= 0) {
                throw new BusinessException(ErrorCode.NO_MEAL_TOKENS);
            }
            user.setMealRecommendTokens(user.getMealRecommendTokens() - 1);
        }

        // 기존 temp 번들 있으면 삭제 (weekly와 동일 정책)
        bundleRepo.findByUser(user).ifPresent(bundleRepo::delete);

        // 새 번들 생성
        TempMealBundle bundle = bundleRepo.save(
                TempMealBundle.builder()
                        .user(user)
                        .build()
        );

        // 🔹 AI 하루치 식단 생성 (끼니 수 반영)
        RecommendedMealPlan plan =
                aiServerService.requestDailyMealWithMealsPerDay(principal, request.getMealsPerDay());

        // 🔹 dayIndex = 1인 하루짜리 TempMealPlan 저장
        TempMealPlan tempPlan = planRepo.save(
                TempMealPlan.builder()
                        .tempBundle(bundle)
                        .dayIndex(1)  // 하루짜리 번들이라 1 고정
                        .totalCalories(plan.getTotalCalories())
                        .totalCarbs(plan.getTotalCarbs())
                        .totalProtein(plan.getTotalProtein())
                        .totalFat(plan.getTotalFat())
                        .build()
        );

        // 🔹 TempMeal & TempMealFood 저장 (weekly와 동일 패턴)
        for (RecommendedMeal rm : plan.getRecommendedMeals()) {

            TempMeal tempMeal = mealRepo.save(
                    TempMeal.builder()
                            .tempMealPlan(tempPlan)
                            .mealType(rm.getMealType())
                            .totalCalories(rm.getTotalCalories())
                            .totalCarbs(rm.getTotalCarbs())
                            .totalProtein(rm.getTotalProtein())
                            .totalFat(rm.getTotalFat())
                            .build()
            );

            for (RecommendedFood rf : rm.getRecommendedFoods()) {
                foodRepo.save(
                        TempMealFood.builder()
                                .tempMeal(tempMeal)
                                .foodName(rf.getFoodName())
                                .servingSize(rf.getServingSize())
                                .calories(rf.getCalories())
                                .carbs(rf.getCarbs())
                                .protein(rf.getProtein())
                                .fat(rf.getFat())
                                .build()
                );
            }
        }

        log.info("TEMP Daily 생성 완료 - user={}, bundleId={}", user.getUserId(), bundle.getId());

        return bundle.getId();
    }



    // ==== 무료 토큰 자동 리셋 (7일마다) ====
    private void resetWeeklyTokensIfNeeded(User user) {

        // 첫 요청이면 초기화
        if (user.getMealTokenLastReset() == null) {
            user.setMealTokenLastReset(LocalDate.now());
            user.setMealRecommendTokens(1); // 기본 1개
            return;
        }

        // 마지막 리셋 이후 7일이 지나면 리셋
        if (user.getMealTokenLastReset().plusDays(7).isBefore(LocalDate.now())) {
            user.setMealRecommendTokens(1); // 1개로 재충전
            user.setMealTokenLastReset(LocalDate.now());
        }
    }


    //1.5 선택한 식사 삭제 , 식사 id 로 삭제함
    @Transactional
    public void deleteTempMeal(CustomUserPrincipal principal, Long mealId) {

        User user = userRepository.findByUserId(principal.getUserId())
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        TempMeal meal = mealRepo.findById(mealId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND_TEMP_MEAL));

        // 다른 유저의 데이터 삭제 방지
        if (!meal.getTempMealPlan().getTempBundle().getUser().equals(user)) {
            throw new BusinessException(ErrorCode.ACCESS_DENIED);
        }

        // 식사에 포함된 food 먼저 삭제
        foodRepo.deleteByTempMeal_Id(mealId);        // ✔ JPA가 인식

        // 식사 삭제
        mealRepo.delete(meal);

        log.info("TempMeal 삭제 완료 - mealId={}, user={}", mealId, user.getUserId());
    }


    // -----------------------------------------------------------
    // 2) TEMP 조회
    // -----------------------------------------------------------
    @Transactional(readOnly = true)
    public List<TempMealDto.PlanResponse> getWeekly(CustomUserPrincipal principal) {

        User user = userRepository.findByUserId(principal.getUserId())
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        TempMealBundle bundle = bundleRepo.findByUser(user)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND_TEMP_MEAL));

        List<TempMealPlan> plans =
                planRepo.findByTempBundleId(bundle.getId());

        return plans.stream()
                .map(p ->
                        TempMealDto.PlanResponse.builder()
                                .dayIndex(p.getDayIndex())
                                .meals(
                                        p.getMeals().stream()
                                                .map(m ->
                                                        TempMealDto.MealResponse.builder()
                                                                .id(m.getId())
                                                                .mealType(m.getMealType().name())
                                                                .totalCalories(m.getTotalCalories())
                                                                .totalCarbs(m.getTotalCarbs())
                                                                .totalProtein(m.getTotalProtein())
                                                                .totalFat(m.getTotalFat())
                                                                .foods(
                                                                        m.getFoods().stream()
                                                                                .map(f ->
                                                                                        TempMealDto.FoodResponse.builder()
                                                                                                .id(f.getId())
                                                                                                .foodName(f.getFoodName())
                                                                                                .servingSize(f.getServingSize())
                                                                                                .calories(f.getCalories())
                                                                                                .carbs(f.getCarbs())
                                                                                                .protein(f.getProtein())
                                                                                                .fat(f.getFat())
                                                                                                .build()
                                                                                )
                                                                                .collect(Collectors.toList())
                                                                )
                                                                .build()
                                                ).collect(Collectors.toList())
                                )
                                .build()
                ).collect(Collectors.toList());
    }
    //2.5 하루치 식단을 다시 받는 api
    @Transactional
    public void regenerateDay(CustomUserPrincipal principal, int dayIndex) {

        User user = userRepository.findByUserId(principal.getUserId())
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        TempMealBundle bundle = bundleRepo.findByUser(user)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND_TEMP_MEAL));

        // 기존 dayIndex 데이터 삭제
        TempMealPlan oldPlan = planRepo.findByTempBundleIdAndDayIndex(bundle.getId(), dayIndex)
                .orElse(null);

        if (oldPlan != null) {

            // 하위 food 먼저 삭제
            for (TempMeal meal : oldPlan.getMeals()) {
                foodRepo.deleteByTempMeal_Id(meal.getId());
            }

            // 식사 삭제
            mealRepo.deleteByTempMealPlan_Id(oldPlan.getId());

            // 하루 plan 삭제
            planRepo.delete(oldPlan);
        }

        // AI 새 1일 식단 생성 (DB 저장 X)
        RecommendedMealPlan newDaily = aiServerService.requestDailyRecommendedMealPlanWithoutSave(principal);

        // 저장
        TempMealPlan savedPlan = planRepo.save(
                TempMealPlan.builder()
                        .tempBundle(bundle)
                        .dayIndex(dayIndex)
                        .totalCalories(newDaily.getTotalCalories())
                        .totalCarbs(newDaily.getTotalCarbs())
                        .totalProtein(newDaily.getTotalProtein())
                        .totalFat(newDaily.getTotalFat())
                        .build()
        );

        for (RecommendedMeal rm : newDaily.getRecommendedMeals()) {

            TempMeal tm = mealRepo.save(
                    TempMeal.builder()
                            .tempMealPlan(savedPlan)
                            .mealType(rm.getMealType())
                            .totalCalories(rm.getTotalCalories())
                            .totalCarbs(rm.getTotalCarbs())
                            .totalProtein(rm.getTotalProtein())
                            .totalFat(rm.getTotalFat())
                            .build()
            );

            rm.getRecommendedFoods().forEach(rf ->
                    foodRepo.save(
                            TempMealFood.builder()
                                    .tempMeal(tm)
                                    .foodName(rf.getFoodName())
                                    .servingSize(rf.getServingSize())
                                    .calories(rf.getCalories())
                                    .carbs(rf.getCarbs())
                                    .protein(rf.getProtein())
                                    .fat(rf.getFat())
                                    .build()
                    )
            );
        }

        log.info("TEMP 일일 식단 재생성 완료 - dayIndex={}, user={}", dayIndex, user.getUserId());
    }

    // -----------------------------------------------------------
    // 3) TEMP → RecommendedMealPlan (Commit)
    // -----------------------------------------------------------
    public void commitWeekly(CustomUserPrincipal principal) {

        User user = userRepository.findByUserId(principal.getUserId())
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        TempMealBundle bundle = bundleRepo.findByUser(user)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND_TEMP_MEAL));

        List<TempMealPlan> tempPlans =
                planRepo.findByTempBundleId(bundle.getId());

        // 최신 추천 번들만 유지: 기존 저장본 제거 후 새 번들로 교체
        recommendedMealPlanRepository.deleteAllByUser(user);

        String bundleId = UUID.randomUUID().toString();

        for (TempMealPlan tp : tempPlans) {

            RecommendedMealPlan finalPlan = RecommendedMealPlan.builder()
                    .user(user)
                    .bundleId(bundleId)
                    .bundleDay(tp.getDayIndex())
                    .planDate(LocalDate.now().plusDays(tp.getDayIndex() - 1))
                    .planName("AI Weekly Plan")
                    .description("AI 추천 기반 식단")
                    .totalCalories(tp.getTotalCalories())
                    .totalCarbs(tp.getTotalCarbs())
                    .totalProtein(tp.getTotalProtein())
                    .totalFat(tp.getTotalFat())
                    .isSaved(true)
                    .build();

            for (TempMeal tm : tp.getMeals()) {

                RecommendedMeal rm = RecommendedMeal.builder()
                        .mealType(tm.getMealType())
                        .totalCalories(tm.getTotalCalories())
                        .totalCarbs(tm.getTotalCarbs())
                        .totalProtein(tm.getTotalProtein())
                        .totalFat(tm.getTotalFat())
                        .build();

                for (TempMealFood tf : tm.getFoods()) {
                    rm.addRecommendedFood(
                            RecommendedFood.builder()
                                    .foodName(tf.getFoodName())
                                    .servingSize(tf.getServingSize())
                                    .calories(tf.getCalories())
                                    .carbs(tf.getCarbs())
                                    .protein(tf.getProtein())
                                    .fat(tf.getFat())
                                    .build()
                    );
                }

                finalPlan.addRecommendedMeal(rm);
            }

            aiServerService.saveRecommendedMealPlan(finalPlan);
        }

        bundleRepo.delete(bundle);
    }
}
