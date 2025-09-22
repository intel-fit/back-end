package rto.intelfit.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import rto.intelfit.domain.User;
import rto.intelfit.dto.UserSetupDto;
import rto.intelfit.exception.BusinessException;
import rto.intelfit.exception.ErrorCode;
import rto.intelfit.repository.UserRepository;
import rto.intelfit.security.CustomUserPrincipal;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserSetupService {

    private final UserRepository userRepository;

    @Transactional
    public UserSetupDto.InitialSetupResponse completeInitialSetup(CustomUserPrincipal userPrincipal,
                                                                  UserSetupDto.InitialSetupRequest request) {
        User user = findUserByPrincipal(userPrincipal);

        // 이미 설정이 완료된 경우 체크 (선택사항)
        if (isSetupCompleted(user)) {
            log.info("이미 초기 설정이 완료된 사용자 - 사용자 ID: {}", user.getUserId());
        }

        // 초기 피트니스 정보 설정
        user.setGender(request.getGender());
        user.setHeight(request.getHeight());
        user.setWeight(request.getWeight());
        user.setWeightGoal(request.getWeightGoal());
        user.setHealthGoal(request.getHealthGoal());

        userRepository.save(user);

        log.info("초기 피트니스 정보 설정 완료 - 사용자 ID: {}", user.getUserId());

        return UserSetupDto.InitialSetupResponse.builder()
                .success(true)
                .message("초기 설정이 완료되었습니다")
                .userId(user.getId())
                .build();
    }

    public UserSetupDto.SetupStatusResponse getSetupStatus(CustomUserPrincipal userPrincipal) {
        User user = findUserByPrincipal(userPrincipal);
        boolean isCompleted = isSetupCompleted(user);

        return UserSetupDto.SetupStatusResponse.builder()
                .isSetupCompleted(isCompleted)
                .message(isCompleted ? "초기 설정이 완료되었습니다" : "초기 설정을 완료해주세요")
                .build();
    }

    private User findUserByPrincipal(CustomUserPrincipal userPrincipal) {
        return userRepository.findByUserId(userPrincipal.getUserId())
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
    }

    private boolean isSetupCompleted(User user) {
        return user.getGender() != null &&
                user.getHeight() != null &&
                user.getWeight() != null &&
                user.getWeightGoal() != null &&
                user.getHealthGoal() != null;
    }
}