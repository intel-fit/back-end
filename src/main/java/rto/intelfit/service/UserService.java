package rto.intelfit.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import rto.intelfit.domain.User;
import rto.intelfit.dto.SignUpDto;
import rto.intelfit.exception.BusinessException;
import rto.intelfit.exception.ErrorCode;
import rto.intelfit.repository.UserRepository;

import java.security.SecureRandom;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final RedisTemplate<String, String> redisTemplate;

    private static final String EMAIL_VERIFICATION_PREFIX = "email_verification:";
    private static final int VERIFICATION_CODE_LENGTH = 6;
    private static final int VERIFICATION_CODE_EXPIRE_MINUTES = 5;

    @Transactional
    public SignUpDto.Response signUp(SignUpDto.Request request) {
        // 비밀번호 확인
        if (!request.getPassword().equals(request.getPasswordConfirm())) {
            throw new BusinessException(ErrorCode.PASSWORD_MISMATCH);
        }

        // 아이디 중복 확인
        if (userRepository.existsByUserId(request.getUserId())) {
            throw new BusinessException(ErrorCode.DUPLICATE_USER_ID);
        }

        // 이메일 인증코드 확인
        if (!verifyEmailCode(request.getEmail(), request.getVerificationCode())) {
            throw new BusinessException(ErrorCode.INVALID_VERIFICATION_CODE);
        }

        // 비밀번호 암호화
        String encodedPassword = passwordEncoder.encode(request.getPassword());

        // 사용자 생성
        User user = User.builder()
                .userId(request.getUserId())
                .name(request.getName())
                .email(request.getEmail())
                .password(encodedPassword)
                .birthDate(request.getBirthDate())
                .emailVerified(true) // 인증코드 확인 완료
                .build();

        User savedUser = userRepository.save(user);

        // 인증코드 삭제
        deleteEmailVerificationCode(request.getEmail());

        return SignUpDto.Response.builder()
                .success(true)
                .message("회원가입이 완료되었습니다")
                .userId(savedUser.getId())
                .build();
    }

    public SignUpDto.UserIdCheckResponse checkUserIdAvailability(String userId) {
        // 기본 유효성 검증
        if (userId == null || userId.trim().isEmpty()) {
            throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE, "아이디를 입력해주세요");
        }

        if (!userId.matches("^[a-zA-Z0-9]{4,20}$")) {
            throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE, "아이디는 4~20자의 영문, 숫자만 사용 가능합니다");
        }

        boolean isAvailable = !userRepository.existsByUserId(userId);

        return SignUpDto.UserIdCheckResponse.builder()
                .available(isAvailable)
                .message(isAvailable ? "사용 가능한 아이디입니다" : "이미 사용중인 아이디입니다")
                .build();
    }

    public SignUpDto.EmailVerificationResponse sendEmailVerificationCode(String email) {
        // 인증코드 생성
        String verificationCode = generateVerificationCode();

        // Redis에 인증코드 저장 (5분 만료)
        String key = EMAIL_VERIFICATION_PREFIX + email;
        redisTemplate.opsForValue().set(key, verificationCode, VERIFICATION_CODE_EXPIRE_MINUTES, TimeUnit.MINUTES);

        // TODO: 실제 이메일 발송 로직 구현 예정
        log.info("이메일 인증코드 발송 - 이메일: {}, 인증코드: {}", email, verificationCode);

        return SignUpDto.EmailVerificationResponse.builder()
                .success(true)
                .message("인증코드가 발송되었습니다")
                .build();
    }

    private boolean verifyEmailCode(String email, String code) {
        String key = EMAIL_VERIFICATION_PREFIX + email;
        String storedCode = redisTemplate.opsForValue().get(key);
        return code.equals(storedCode);
    }

    private void deleteEmailVerificationCode(String email) {
        String key = EMAIL_VERIFICATION_PREFIX + email;
        redisTemplate.delete(key);
    }

    private String generateVerificationCode() {
        SecureRandom random = new SecureRandom();
        StringBuilder sb = new StringBuilder();

        for (int i = 0; i < VERIFICATION_CODE_LENGTH; i++) {
            sb.append(random.nextInt(10));
        }

        return sb.toString();
    }
}