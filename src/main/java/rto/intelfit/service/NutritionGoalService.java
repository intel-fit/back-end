package rto.intelfit.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import rto.intelfit.domain.DailyNutritionGoal;
import rto.intelfit.domain.InBody;
import rto.intelfit.domain.User;
import rto.intelfit.dto.NutritionGoalDto;
import rto.intelfit.exception.BusinessException;
import rto.intelfit.exception.ErrorCode;
import rto.intelfit.repository.DailyNutritionGoalRepository;
import rto.intelfit.repository.InBodyRepository;
import rto.intelfit.repository.UserRepository;
import rto.intelfit.security.CustomUserPrincipal;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class NutritionGoalService {

    private final DailyNutritionGoalRepository nutritionGoalRepository;
    private final UserRepository userRepository;
    private final InBodyRepository inBodyRepository;

    /**
     * 영양 목표 설정
     */
    @Transactional
    public NutritionGoalDto.NutritionGoalSetResponse setNutritionGoal(
            CustomUserPrincipal userPrincipal,
            NutritionGoalDto.NutritionGoalSetRequest request) {

        User user = findUserByPrincipal(userPrincipal);

        DailyNutritionGoal goal = nutritionGoalRepository.findByUser(user)
                .orElse(DailyNutritionGoal.builder()
                        .user(user)
                        .build());

        // 목표 설정
        goal.setTargetCalories(request.getTargetCalories());
        goal.setTargetCarbs(request.getTargetCarbs());
        goal.setTargetProtein(request.getTargetProtein());
        goal.setTargetFat(request.getTargetFat());
        goal.setGoalType(request.getGoalType());

        DailyNutritionGoal savedGoal = nutritionGoalRepository.save(goal);

        log.info("영양 목표 설정 완료 - 사용자 ID: {}, 목표 칼로리: {}",
                user.getUserId(), request.getTargetCalories());

        return NutritionGoalDto.NutritionGoalSetResponse.builder()
                .success(true)
                .message("영양 목표가 설정되었습니다")
                .goal(NutritionGoalDto.NutritionGoalResponse.from(savedGoal))
                .build();
    }

    /**
     * 영양 목표 조회
     */
    public NutritionGoalDto.NutritionGoalResponse getNutritionGoal(
            CustomUserPrincipal userPrincipal) {

        User user = findUserByPrincipal(userPrincipal);

        DailyNutritionGoal goal = nutritionGoalRepository.findByUser(user)
                .orElseGet(() -> createAutoGoalForUser(user));

        log.info("영양 목표 조회 - 사용자 ID: {}", user.getUserId());

        return NutritionGoalDto.NutritionGoalResponse.from(goal);
    }

    /**
     * 자동으로 영양 목표 생성 (신체 정보 기반)
     */
    @Transactional
    public DailyNutritionGoal createAutoGoalForUser(User user) {
        // 사용자의 신체 정보 기반으로 목표 칼로리 계산
        BigDecimal targetCalories = calculateTargetCalories(user);
        BigDecimal targetCarbs = calculateTargetCarbs(targetCalories);
        BigDecimal targetProtein = calculateTargetProtein(user);
        BigDecimal targetFat = calculateTargetFat(targetCalories);

        DailyNutritionGoal goal = DailyNutritionGoal.builder()
                .user(user)
                .targetCalories(targetCalories)
                .targetCarbs(targetCarbs)
                .targetProtein(targetProtein)
                .targetFat(targetFat)
                .goalType(DailyNutritionGoal.GoalType.AUTO)
                .build();

        DailyNutritionGoal savedGoal = nutritionGoalRepository.save(goal);

        log.info("자동 영양 목표 생성 - 사용자 ID: {}, 목표 칼로리: {}",
                user.getUserId(), targetCalories);

        return savedGoal;
    }

    // Private helper methods

    private User findUserByPrincipal(CustomUserPrincipal userPrincipal) {
        return userRepository.findByUserId(userPrincipal.getUserId())
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
    }

    /**
     * 목표 칼로리 계산 (Harris-Benedict 공식 + 활동량)
     */
    private BigDecimal calculateTargetCalories(User user) {
        // 기초대사량 계산
        BigDecimal bmr;

        if (user.getGender() == User.Gender.M) {
            // 남성: 10 × 체중(kg) + 6.25 × 키(cm) - 5 × 나이(세) + 5
            bmr = BigDecimal.valueOf(10)
                    .multiply(BigDecimal.valueOf(user.getWeight() != null ? user.getWeight() : 70))
                    .add(BigDecimal.valueOf(6.25).multiply(BigDecimal.valueOf(user.getHeight() != null ? user.getHeight() : 170)))
                    .subtract(BigDecimal.valueOf(5).multiply(BigDecimal.valueOf(calculateAge(user))))
                    .add(BigDecimal.valueOf(5));
        } else {
            // 여성: 10 × 체중(kg) + 6.25 × 키(cm) - 5 × 나이(세) - 161
            bmr = BigDecimal.valueOf(10)
                    .multiply(BigDecimal.valueOf(user.getWeight() != null ? user.getWeight() : 60))
                    .add(BigDecimal.valueOf(6.25).multiply(BigDecimal.valueOf(user.getHeight() != null ? user.getHeight() : 160)))
                    .subtract(BigDecimal.valueOf(5).multiply(BigDecimal.valueOf(calculateAge(user))))
                    .subtract(BigDecimal.valueOf(161));
        }

        // 활동량 계수 적용 (주간 운동 일수 기반)
        BigDecimal activityMultiplier = getActivityMultiplier(user.getWorkoutDaysPerWeek());
        BigDecimal tdee = bmr.multiply(activityMultiplier);

        // 목표에 따른 칼로리 조정
        BigDecimal adjustedCalories = adjustCaloriesForGoal(tdee, user.getHealthGoal());

        return adjustedCalories.setScale(0, RoundingMode.HALF_UP);
    }

    /**
     * 나이 계산
     */
    private int calculateAge(User user) {
        if (user.getBirthDate() == null) {
            return 30; // 기본값
        }
        return java.time.Period.between(user.getBirthDate(), java.time.LocalDate.now()).getYears();
    }

    /**
     * 활동량 계수 가져오기
     */
    private BigDecimal getActivityMultiplier(String workoutDaysPerWeek) {
        if (workoutDaysPerWeek == null) {
            return BigDecimal.valueOf(1.375); // 기본: 가볍게 활동적
        }

        if (workoutDaysPerWeek.contains("5") || workoutDaysPerWeek.contains("6") || workoutDaysPerWeek.contains("7")) {
            return BigDecimal.valueOf(1.55); // 매우 활동적
        } else if (workoutDaysPerWeek.contains("3") || workoutDaysPerWeek.contains("4")) {
            return BigDecimal.valueOf(1.465); // 활동적
        } else if (workoutDaysPerWeek.contains("1") || workoutDaysPerWeek.contains("2")) {
            return BigDecimal.valueOf(1.375); // 가볍게 활동적
        } else {
            return BigDecimal.valueOf(1.2); // 거의 활동 없음
        }
    }

    /**
     * 건강 목표에 따른 칼로리 조정
     */
    private BigDecimal adjustCaloriesForGoal(BigDecimal tdee, User.HealthGoal healthGoal) {
        if (healthGoal == null) {
            return tdee;
        }

        switch (healthGoal) {
            case DIET:
                // 체중 감량: TDEE - 500kcal
                return tdee.subtract(BigDecimal.valueOf(500));
            case BULK:
                // 벌크업: TDEE + 500kcal
                return tdee.add(BigDecimal.valueOf(500));
            case LEAN_MASS:
                // 린매스업: TDEE + 300kcal
                return tdee.add(BigDecimal.valueOf(300));
            default:
                return tdee;
        }
    }

    /**
     * 목표 탄수화물 계산 (칼로리의 50%)
     */
    private BigDecimal calculateTargetCarbs(BigDecimal targetCalories) {
        // 탄수화물 1g = 4kcal
        return targetCalories
                .multiply(BigDecimal.valueOf(0.5))
                .divide(BigDecimal.valueOf(4), 2, RoundingMode.HALF_UP);
    }

    /**
     * 목표 단백질 계산 (체중 × 2g)
     */
    private BigDecimal calculateTargetProtein(User user) {
        int weight = user.getWeight() != null ? user.getWeight() : 70;
        return BigDecimal.valueOf(weight)
                .multiply(BigDecimal.valueOf(2));
    }

    /**
     * 목표 지방 계산 (칼로리의 30%)
     */
    private BigDecimal calculateTargetFat(BigDecimal targetCalories) {
        // 지방 1g = 9kcal
        return targetCalories
                .multiply(BigDecimal.valueOf(0.3))
                .divide(BigDecimal.valueOf(9), 2, RoundingMode.HALF_UP);
    }
}