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
    private final KakaoAuthService kakaoAuthService;


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
    public ProfileDto.AccountDeleteResponse deleteAccount(CustomUserPrincipal userPrincipal,
                                                          ProfileDto.AccountDeleteRequest request) {
        User user = findUserByPrincipal(userPrincipal);

        // 비밀번호 확인
        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new BusinessException(ErrorCode.INVALID_LOGIN_CREDENTIALS, "비밀번호가 올바르지 않습니다");
        }

        // 사용자의 모든 토큰 무효화
        jwtUtil.deleteRefreshToken(user.getUserId());

        // User와 연관된 모든 데이터를 명시적으로 삭제 (FK 제약 조건 문제 해결)
        log.info("회원 탈퇴 시작 - 사용자 ID: {}, 연관 데이터 삭제 시작", user.getUserId());

        // 1. InBody 레코드 삭제
        inBodyRepository.deleteAllByUser(user);
        log.debug("InBody 레코드 삭제 완료");

        // 2. 음식 선호도 삭제
        userFoodPreferenceRepository.deleteAllByUser(user);
        log.debug("음식 선호도 삭제 완료");

        // 3. 영양 목표 삭제
        dailyNutritionGoalRepository.deleteAllByUser(user);
        log.debug("영양 목표 삭제 완료");

        // 4. 식사 기록 삭제
        mealRepository.deleteAllByUser(user);
        log.debug("식사 기록 삭제 완료");

        // 5. 추천 식단 삭제
        recommendedMealPlanRepository.deleteAllByUser(user);
        log.debug("추천 식단 삭제 완료");

        // 6. 운동 기록 삭제
        exerciseRepository.deleteAllByUser(user);
        log.debug("운동 기록 삭제 완료");

        // 7. 추천 운동 플랜 삭제
        recommendedExercisePlanRepository.deleteAllByUser(user);
        log.debug("추천 운동 플랜 삭제 완료");

        // 8. 사용자 뱃지 삭제
        userBadgeRepository.deleteAllByUser(user);
        log.debug("사용자 뱃지 삭제 완료");

        paymentHistoryRepository.deleteAllByUser_Id(user.getId());
        subscriptionRepository.deleteAllByUserId(user.getUserId());
        aiChatMessageRepository.deleteAllByUser_UserId(user.getUserId());


        // 9. 마지막으로 사용자 삭제
        userRepository.delete(user);

        log.info("회원 탈퇴 완료 - 사용자 ID: {}, 탈퇴 사유: {}",
                user.getUserId(), request.getReason());

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
    //이원웅 추가
    @Transactional
    public ProfileDto.AccountDeleteResponse deleteKakaoUser(
            CustomUserPrincipal userPrincipal,
            ProfileDto.AccountDeleteRequest request) {

        User user = findUserByPrincipal(userPrincipal);

        // socialId 검증
        if (!request.getKakaoId().equals(user.getSocialId())) {
            throw new BusinessException(ErrorCode.INVALID_LOGIN_CREDENTIALS, "카카오 ID가 일치하지 않습니다.");
        }

        // 1) 카카오 unlink 먼저 호출
        kakaoAuthService.unlinkKakaoUser(user.getSocialId());

        // 2) Refresh Token 삭제
        jwtUtil.deleteRefreshToken(user.getUserId());

        // 3) 연관 데이터 삭제 (지금 있는 코드 그대로 유지)
        inBodyRepository.deleteAllByUser(user);
        userFoodPreferenceRepository.deleteAllByUser(user);
        dailyNutritionGoalRepository.deleteAllByUser(user);
        mealRepository.deleteAllByUser(user);
        recommendedMealPlanRepository.deleteAllByUser(user);
        exerciseRepository.deleteAllByUser(user);
        recommendedExercisePlanRepository.deleteAllByUser(user);
        userBadgeRepository.deleteAllByUser(user);

        // 4) 최종 사용자 삭제
        userRepository.delete(user);

        log.info("카카오 회원 탈퇴 완료 - 사용자 ID: {}", user.getUserId());

        return ProfileDto.AccountDeleteResponse.builder()
                .success(true)
                .message("카카오 회원 탈퇴 완료")
                .build();
    }




}