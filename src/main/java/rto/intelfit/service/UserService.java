package rto.intelfit.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import rto.intelfit.domain.User;
import rto.intelfit.dto.LoginDto;
import rto.intelfit.dto.SignUpDto;
import rto.intelfit.exception.BusinessException;
import rto.intelfit.exception.ErrorCode;
import rto.intelfit.repository.UserRepository;
import rto.intelfit.security.CustomUserPrincipal;
import rto.intelfit.util.JwtUtil;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.security.SecureRandom;
import java.util.Set;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserService {

    private final UserService userService;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final RedisTemplate<String, String> redisTemplate;
    private final JwtUtil jwtUtil;
    private final EmailService emailService;
    private final AIServerService aiServerService;

    @Value("${jwt.access-token-expiration:3600000}")
    private long accessTokenExpiration;

    private static final String EMAIL_VERIFICATION_PREFIX = "email_verification:";
    private static final String TEMP_PASSWORD_PREFIX = "temp_password:";
    private static final int VERIFICATION_CODE_LENGTH = 6;
    private static final int VERIFICATION_CODE_EXPIRE_MINUTES = 5;
    private static final int TEMP_PASSWORD_LENGTH = 6;
    private static final int TEMP_PASSWORD_EXPIRE_MINUTES = 30;

    @Transactional
    public void createTestUserIfNotExists() {

        String testUserId = "testuser";
        String testEmail = "test@example.com";

        // 이미 있으면 AI 서버 동기화만 보장
        if (userRepository.existsByUserId(testUserId)) {
            User existing = userRepository.findByUserId(testUserId).get();
            log.info("✔ 테스트 유저 이미 존재: {} → AI 서버와 동기화만 수행", testUserId);

            aiServerService.createUserOnAI(existing);
            return;
        }

        // 비밀번호 암호화
        String encodedPassword = passwordEncoder.encode("Test1234!");

        User testUser = User.builder()
                .userId(testUserId)
                .name("테스트 유저")
                .email(testEmail)
                .emailVerified(true)
                .password(encodedPassword)

                // 필수 정보 기본값 설정
                .birthDate(LocalDate.of(1990, 1, 1))
                .gender(User.Gender.M)
                .height(170)
                .weight(60)
                .weightGoal(60)
                .healthGoal(User.HealthGoal.MAINTENANCE)
                .experienceLevel(User.ExperienceLevel.BEGINNER)
                .workoutDaysPerWeek("3-4일")

                // 약관 동의 ..
                .agreePrivacy(true)
                .agreeTerms(true)
                .agreedAt(LocalDateTime.now())

                // 기타 정보
                .fitnessConcerns("테스트 계정")
                .membershipType(User.MembershipType.PREMIUM)
                .socialProvider(User.SocialProvider.LOCAL)
                .build();

        // DB 저장
        User saved = userRepository.save(testUser);

        log.info("🎉 테스트 유저 자동 생성 완료: {}", testUserId);

        // AI 서버 동기화
        try {
            aiServerService.createUserOnAI(saved);
            log.info("🤖 AI 서버 테스트 유저 동기화 완료");
        } catch (Exception e) {
            log.warn("⚠ AI 테스트 유저 동기화 실패: {}", e.getMessage());
        }
    }



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

        // 이메일 중복 확인
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new BusinessException(ErrorCode.DUPLICATE_EMAIL);
        }


        // 이메일 인증코드 확인
        if (!verifyEmailCode(request.getEmail(), request.getVerificationCode())) {
            throw new BusinessException(ErrorCode.INVALID_VERIFICATION_CODE);
        }

        // 비밀번호 암호화
        String encodedPassword = passwordEncoder.encode(request.getPassword());

        // 사용자 생성 (초기 피트니스 정보 포함)
        User user = User.builder()
                .userId(request.getUserId())
                .name(request.getName())
                .email(request.getEmail())
                .password(encodedPassword)
                .birthDate(request.getBirthDate())
                .emailVerified(true) // 인증코드 확인 완료
                // 초기 피트니스 정보 추가
                .gender(request.getGender())
                .height(request.getHeight())
                .weight(request.getWeight())
                .weightGoal(request.getWeightGoal())
                .healthGoal(request.getHealthGoal())
                .workoutDaysPerWeek(request.getWorkoutDaysPerWeek())
                // 약관 동의 정보 추가
                .agreePrivacy(request.getAgreePrivacy())
                .agreeTerms(request.getAgreeTerms())
                .agreedAt(java.time.LocalDateTime.now())
                // 헬스 고민 추가
                .fitnessConcerns(request.getFitnessConcerns())
                .build();

        User savedUser = userRepository.save(user);

        // 인증코드 삭제
        deleteEmailVerificationCode(request.getEmail());

        // 커밋 이후 AI 서버에 사용자 동기화 (가입 성공 흐름은 절대 방해하지 않음)
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                try {
                    // 네가 만든 메서드명에 맞춰서 호출해. createUserOnAI 또는 createUserInAIServer 중 하나.
                    aiServerService.createUserOnAI(savedUser);
                    // aiServerService.createUserInAIServer(savedUser);
                } catch (Exception ex) {
                    // 가입은 이미 커밋되었으니, 동기화 실패는 경고 로그만 남김
                    log.warn("AI 사용자 동기화 실패 - userId={}, reason={}", savedUser.getUserId(), ex.getMessage(), ex);
                }
            }
        });

        return SignUpDto.Response.builder()
                .success(true)
                .message("회원가입이 완료되었습니다")
                .userId(savedUser.getId())
                .build();
    }

    @Transactional
    public User updateTokensToDefault(CustomUserPrincipal principal) {
        User user = findByPrincipal(principal);

        user.setMealRecommendTokens(1);
        user.setWorkoutRecommendTokens(1);
        user.setChatbotTokens(3);

        user.setMealTokenLastReset(LocalDate.now());
        user.setWorkoutRecommendLastReset(LocalDateTime.now());
        user.setChatbotLastReset(LocalDateTime.now());

        return userRepository.save(user);
    }

    @Transactional
    public User upgradeToPremium(CustomUserPrincipal principal) {
        User user = findByPrincipal(principal);

        user.setMembershipType(User.MembershipType.PREMIUM);

        return userRepository.save(user);
    }

    @Transactional
    public User downgradeToFree(CustomUserPrincipal principal) {
        User user = findByPrincipal(principal);

        user.setMembershipType(User.MembershipType.FREE);

        return userRepository.save(user);
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

    @Transactional
    public SignUpDto.EmailVerificationResponse sendEmailVerificationCode(String email) {
        // 인증코드 생성
        String verificationCode = generateVerificationCode();

        // Redis에 인증코드 저장 (5분 만료)
        String key = EMAIL_VERIFICATION_PREFIX + email;
        redisTemplate.opsForValue().set(key, verificationCode, VERIFICATION_CODE_EXPIRE_MINUTES, TimeUnit.MINUTES);

        // 실제 이메일 발송 (비동기)
        try {
            emailService.sendVerificationCode(email, verificationCode);
            log.info("이메일 인증코드 발송 성공 - 이메일: {}, 인증코드: {}", email, verificationCode);
        } catch (Exception e) {
            log.error("이메일 인증코드 발송 실패 - 이메일: {}, 에러: {}", email, e.getMessage(), e);
            // Redis에 저장은 되었으므로 에러를 던지지 않고 계속 진행
            // 사용자에게는 발송되었다고 응답하지만, 로그로 문제를 추적
        }

        return SignUpDto.EmailVerificationResponse.builder()
                .success(true)
                .message("인증코드가 발송되었습니다")
                .build();
    }

    @Transactional
    public LoginDto.Response login(LoginDto.Request request) {
        // 사용자 조회
        User user = userRepository.findByUserId(request.getUserId())
                .orElseThrow(() -> new BusinessException(ErrorCode.INVALID_LOGIN_CREDENTIALS));

        // 비밀번호 확인
        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new BusinessException(ErrorCode.INVALID_LOGIN_CREDENTIALS);
        }

        // 강제 로그아웃 플래그 해제
        jwtUtil.clearForceLogout(user.getUserId());

        // 마지막 로그인 시간 업데이트
        user.updateLastLoginAt();
        userRepository.save(user);


        return issueTokens(user, "로그인이 완료되었습니다.");



        @Transactional
        public LoginDto.LogoutResponse logout(LoginDto.LogoutRequest request) {
            String accessToken = request.getAccessToken();

            // 1. 토큰 유효성 검증
            if (!jwtUtil.validateToken(accessToken)) {
                throw new BusinessException(ErrorCode.INVALID_TOKEN);
            }

            // 2. 사용자 ID 추출
            String userId = jwtUtil.getUserIdFromToken(accessToken);

            // 3. Access Token 블랙리스트 등록
            jwtUtil.blacklistToken(accessToken);

            // 4. Refresh Token 삭제
            jwtUtil.deleteRefreshToken(userId);

            // 🔥 5. 강제 로그아웃 플래그 (이게 핵심)
            jwtUtil.forceLogoutUser(userId);

            log.info("로그아웃 완료 - 사용자 ID: {}", userId);

            return LoginDto.LogoutResponse.builder()
                    .success(true)
                    .message("로그아웃이 완료되었습니다")
                    .build();
        }


        @Transactional
    public LoginDto.TokenRefreshResponse refreshToken(LoginDto.TokenRefreshRequest request) {
        String refreshToken = request.getRefreshToken();

        // Refresh Token 유효성 검증
        if (!jwtUtil.validateToken(refreshToken)) {
            throw new BusinessException(ErrorCode.INVALID_REFRESH_TOKEN);
        }

        // 토큰 타입 확인
        if (!"refresh".equals(jwtUtil.getTokenType(refreshToken))) {
            throw new BusinessException(ErrorCode.INVALID_REFRESH_TOKEN);
        }

        // 사용자 정보 추출
        String userId = jwtUtil.getUserIdFromToken(refreshToken);
        Long userPk = jwtUtil.getUserPkFromToken(refreshToken);

        // Redis에 저장된 Refresh Token과 비교
        if (!jwtUtil.validateRefreshToken(refreshToken, userId)) {
            throw new BusinessException(ErrorCode.INVALID_REFRESH_TOKEN);
        }

        // 새로운 Access Token 생성
        String newAccessToken = jwtUtil.generateAccessToken(userId, userPk);

        log.info("토큰 재발급 완료 - 사용자 ID: {}", userId);

        return LoginDto.TokenRefreshResponse.builder()
                .success(true)
                .message("토큰이 재발급되었습니다")
                .accessToken(newAccessToken)
                .tokenType("Bearer")
                .expiresIn(accessTokenExpiration / 1000) // 초 단위로 변환
                .build();
    }

    @Transactional
    public LoginDto.FindUserIdResponse findUserId(String email) {
        // 이메일로 사용자 조회
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND_BY_EMAIL));

        // 아이디 마스킹 처리
        String maskedUserId = maskUserId(user.getUserId());

        //실제 이메일 발송 (비동기)
        try {
            emailService.sendUserId(email, user.getUserId());
            log.info("아이디 찾기 이메일 발송 성공 - 이메일: {}, 아이디: {}", email, user.getUserId());
        } catch (Exception e) {
            log.error("아이디 찾기 이메일 발송 실패 - 이메일: {}, 에러: {}", email, e.getMessage(), e);
            // 마스킹된 아이디는 응답으로 보내므로 에러를 던지지 않음
        }

        return LoginDto.FindUserIdResponse.builder()
                .success(true)
                .message("아이디가 이메일로 발송되었습니다")
                .maskedUserId(maskedUserId)
                .build();
    }

    @Transactional
    public LoginDto.PasswordResetResponse resetPassword(String email) {
        // 이메일로 사용자 조회
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND_BY_EMAIL));

        // 임시 비밀번호 생성
        String tempPassword = generateTempPassword();

        // Redis에 임시 비밀번호 저장 (30분 만료) - 평문으로 저장
        String key = TEMP_PASSWORD_PREFIX + email;
        redisTemplate.opsForValue().set(key, tempPassword, TEMP_PASSWORD_EXPIRE_MINUTES, TimeUnit.MINUTES);

        //실제 이메일 발송 (비동기)
        try {
            emailService.sendTempPassword(email, tempPassword);
            log.info("임시 비밀번호 이메일 발송 성공 - 이메일: {}, 임시 비밀번호: {}", email, tempPassword);
        } catch (Exception e) {
            log.error("임시 비밀번호 이메일 발송 실패 - 이메일: {}, 에러: {}", email, e.getMessage(), e);
            // Redis에 저장은 되었으므로 에러를 던지지 않음
        }

        return LoginDto.PasswordResetResponse.builder()
                .success(true)
                .message("임시 비밀번호가 이메일로 발송되었습니다")
                .build();
    }

    @Transactional
    public LoginDto.PasswordChangeResponse changePassword(LoginDto.PasswordChangeRequest request) {
        // 새 비밀번호 확인
        if (!request.getNewPassword().equals(request.getNewPasswordConfirm())) {
            throw new BusinessException(ErrorCode.PASSWORD_MISMATCH, "새 비밀번호가 일치하지 않습니다");
        }

        // Redis에서 모든 임시 비밀번호 키를 확인하여 해당하는 이메일 찾기
        String userEmail = null;
        String tempPasswordPrefix = TEMP_PASSWORD_PREFIX + "*";

        // Redis에서 패턴 매칭으로 모든 임시 비밀번호 키 검색
        Set<String> keys = redisTemplate.keys(tempPasswordPrefix);

        if (keys != null) {
            for (String key : keys) {
                String storedTempPassword = redisTemplate.opsForValue().get(key);
                if (request.getTempPassword().equals(storedTempPassword)) {
                    // 키에서 이메일 추출 (temp_password: 제거)
                    userEmail = key.substring(TEMP_PASSWORD_PREFIX.length());
                    break;
                }
            }
        }

        if (userEmail == null) {
            throw new BusinessException(ErrorCode.INVALID_TEMP_PASSWORD);
        }

        // 이메일로 사용자 조회
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND_BY_EMAIL));

        // 새 비밀번호로 변경
        String encodedNewPassword = passwordEncoder.encode(request.getNewPassword());
        user.setPassword(encodedNewPassword);
        userRepository.save(user);

        // 임시 비밀번호 삭제
        String tempPasswordKey = TEMP_PASSWORD_PREFIX + userEmail;
        redisTemplate.delete(tempPasswordKey);

        log.info("비밀번호 변경 완료 - 이메일: {}", userEmail);

        return LoginDto.PasswordChangeResponse.builder()
                .success(true)
                .message("비밀번호가 변경되었습니다")
                .build();
    }
    //이원웅 추가
    @Transactional
    public LoginDto.Response issueTokens(User user, String message) {

        // 강제 로그아웃 상태 해제 (있다면)
        jwtUtil.clearForceLogout(user.getUserId());

        // 마지막 로그인 시간 업데이트
        user.updateLastLoginAt();
        userRepository.save(user);

        // 🔐 토큰 생성 (여기가 유일한 토큰 생성 지점)
        String accessToken = jwtUtil.generateAccessToken(
                user.getUserId(),
                user.getId()
        );

        String refreshToken = jwtUtil.generateRefreshToken(
                user.getUserId(),
                user.getId()
        );

        return LoginDto.Response.builder()
                .success(true)
                .message(message)
                .userId(user.getId())
                .name(user.getName())
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .tokenType("Bearer")
                .expiresIn(jwtUtil.getAccessTokenExpirationSeconds())
                .membershipType(user.getMembershipType())
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

    private String generateTempPassword() {
        SecureRandom random = new SecureRandom();
        StringBuilder sb = new StringBuilder();
        String characters = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";

        for (int i = 0; i < TEMP_PASSWORD_LENGTH; i++) {
            sb.append(characters.charAt(random.nextInt(characters.length())));
        }

        return sb.toString();
    }

    private String maskUserId(String userId) {
        if (userId.length() <= 4) {
            return userId.substring(0, 2) + "**";
        }

        int maskLength = userId.length() - 4;
        String maskedPart = "*".repeat(maskLength);
        return userId.substring(0, 2) + maskedPart + userId.substring(userId.length() - 2);
    }

    public User findByPrincipal(CustomUserPrincipal principal) {
        return userRepository.findByUserId(principal.getUserId())
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
    }

    //이후 추가

}
