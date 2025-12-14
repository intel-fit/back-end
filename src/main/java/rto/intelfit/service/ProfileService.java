package rto.intelfit.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import rto.intelfit.domain.User;
import rto.intelfit.dto.ProfileDto;
import rto.intelfit.exception.BusinessException;
import rto.intelfit.exception.ErrorCode;
import rto.intelfit.repository.*;
import rto.intelfit.security.CustomUserPrincipal;
import rto.intelfit.util.JwtUtil;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ProfileService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final InBodyRepository inBodyRepository;
    private final UserFoodPreferenceRepository userFoodPreferenceRepository;
    private final DailyNutritionGoalRepository dailyNutritionGoalRepository;
    private final MealRepository mealRepository;
    private final RecommendedMealPlanRepository recommendedMealPlanRepository;
    private final ExerciseRepository exerciseRepository;
    private final RecommendedExercisePlanRepository recommendedExercisePlanRepository;
    private final UserBadgeRepository userBadgeRepository;
    private final PaymentHistoryRepository paymentHistoryRepository;
    private final SubscriptionRepository subscriptionRepository;
    private final AIChatMessageRepository aiChatMessageRepository;


    public ProfileDto.ProfileResponse getProfile(CustomUserPrincipal userPrincipal) {
        User user = findUserByPrincipal(userPrincipal);
        return ProfileDto.ProfileResponse.from(user);
    }

    @Transactional
    public ProfileDto.ProfileUpdateResponse updateProfile(CustomUserPrincipal userPrincipal,
                                                          ProfileDto.ProfileUpdateRequest request) {
        User user = findUserByPrincipal(userPrincipal);


        // 수정 가능한 필드들 업데이트
        updateUserFields(user, request);
        User savedUser = userRepository.save(user);

        log.info("프로필 수정 완료 - 사용자 ID: {}", user.getUserId());

        return ProfileDto.ProfileUpdateResponse.builder()
                .success(true)
                .message("프로필이 수정되었습니다")
                .profile(ProfileDto.ProfileResponse.from(savedUser))
                .build();
    }

    @Transactional
    public ProfileDto.PasswordUpdateResponse updatePassword(CustomUserPrincipal userPrincipal,
                                                            ProfileDto.PasswordUpdateRequest request) {
        User user = findUserByPrincipal(userPrincipal);

        // 새 비밀번호 확인
        if (!request.getNewPassword().equals(request.getNewPasswordConfirm())) {
            throw new BusinessException(ErrorCode.PASSWORD_MISMATCH, "새 비밀번호가 일치하지 않습니다");
        }

        // 현재 비밀번호 확인
        if (!passwordEncoder.matches(request.getCurrentPassword(), user.getPassword())) {
            throw new BusinessException(ErrorCode.INVALID_LOGIN_CREDENTIALS, "현재 비밀번호가 올바르지 않습니다");
        }

        // 새 비밀번호로 변경
        String encodedNewPassword = passwordEncoder.encode(request.getNewPassword());
        user.setPassword(encodedNewPassword);
        userRepository.save(user);

        log.info("비밀번호 변경 완료 - 사용자 ID: {}", user.getUserId());

        return ProfileDto.PasswordUpdateResponse.builder()
                .success(true)
                .message("비밀번호가 변경되었습니다")
                .build();
    }

    @Transactional
    public ProfileDto.AccountDeleteResponse deleteAccount(
            CustomUserPrincipal userPrincipal,
            ProfileDto.AccountDeleteRequest request) {

        User user = findUserByPrincipal(userPrincipal);

        if (user.isSocialUser()) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST);
        }

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new BusinessException(ErrorCode.INVALID_LOGIN_CREDENTIALS);
        }

        jwtUtil.deleteRefreshToken(user.getUserId());

        // 연결된 데이터 선삭제 (FK 제약 해소)
        cleanupUserData(user);

        // ✅ 유저만 삭제
        userRepository.delete(user);

        return ProfileDto.AccountDeleteResponse.builder()
                .success(true)
                .message("회원 탈퇴가 완료되었습니다")
                .build();
    }




    private User findUserByPrincipal(CustomUserPrincipal userPrincipal) {
        return userRepository.findByUserId(userPrincipal.getUserId())
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
    }

    private void updateUserFields(User user, ProfileDto.ProfileUpdateRequest request) {
        if (StringUtils.hasText(request.getName())) {
            user.setName(request.getName());
        }
        if (request.getHeight() != null) {
            user.setHeight(request.getHeight());
        }
        if (request.getWeight() != null) {
            user.setWeight(request.getWeight());
        }
        if (request.getGender() != null) {
            user.setGender(request.getGender());
        }
        if (request.getHealthGoal() != null) {
            user.setHealthGoal(request.getHealthGoal());
        }
        if (StringUtils.hasText(request.getWorkoutDaysPerWeek())) {
            user.setWorkoutDaysPerWeek(request.getWorkoutDaysPerWeek());
        }
        if (request.getWeightGoal() != null) {
            user.setWeightGoal(request.getWeightGoal());
        }
    }

    private void cleanupUserData(User user) {
        inBodyRepository.deleteAllByUser(user);
        userFoodPreferenceRepository.deleteAllByUser(user);
        dailyNutritionGoalRepository.deleteAllByUser(user);
        mealRepository.deleteAllByUser(user);
        recommendedMealPlanRepository.deleteAllByUser(user);
        exerciseRepository.deleteAllByUser(user);
        recommendedExercisePlanRepository.deleteAllByUser(user);
        userBadgeRepository.deleteAllByUser(user);
        paymentHistoryRepository.deleteAllByUser_Id(user.getId());
        subscriptionRepository.deleteAllByUserId(user.getUserId());
        aiChatMessageRepository.deleteAllByUser_UserId(user.getUserId());
    }
}
