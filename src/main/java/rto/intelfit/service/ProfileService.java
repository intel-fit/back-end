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



    public ProfileDto.ProfileResponse getProfile(CustomUserPrincipal userPrincipal) {
        User user = findUserByPrincipal(userPrincipal);
        return ProfileDto.ProfileResponse.from(user);
    }

    private User findUserByPrincipal(CustomUserPrincipal userPrincipal) {
        return userRepository.findByUserId(userPrincipal.getUserId())
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND, "사용자를 찾을 수 없습니다"));
    }

    @Transactional
    public ProfileDto.ProfileUpdateResponse updateProfile(CustomUserPrincipal userPrincipal,
                                                          ProfileDto.ProfileUpdateRequest request) {
        User user = findUserByPrincipal(userPrincipal);


        User savedUser = userRepository.save(user);

        log.info("프로필 수정 완료 - 사용자 ID: {}", user.getUserId());

        return ProfileDto.ProfileUpdateResponse.builder()
                .success(true)
                .message("프로필이 수정되었습니다")
                .profile(ProfileDto.ProfileResponse.from(savedUser))
                .build();
    }

}