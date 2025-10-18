package rto.intelfit.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum ErrorCode {

    // 사용자 관련 에러
    DUPLICATE_USER_ID(HttpStatus.CONFLICT, "USER_001", "이미 사용중인 아이디입니다"),
    DUPLICATE_EMAIL(HttpStatus.CONFLICT, "USER_002", "이미 사용중인 이메일입니다"),
    DUPLICATE_PHONE_NUMBER(HttpStatus.CONFLICT, "USER_003", "이미 사용중인 전화번호입니다"),
    USER_NOT_FOUND_BY_EMAIL(HttpStatus.NOT_FOUND, "USER_006", "해당 이메일로 등록된 사용자를 찾을 수 없습니다"),
    USER_NOT_FOUND(HttpStatus.NOT_FOUND, "USER_007", "사용자를 찾을 수 없습니다"),
    PASSWORD_MISMATCH(HttpStatus.BAD_REQUEST, "USER_004", "비밀번호가 일치하지 않습니다"),

    // 인증 관련 에러
    INVALID_VERIFICATION_CODE(HttpStatus.BAD_REQUEST, "AUTH_001", "인증코드가 올바르지 않습니다"),
    VERIFICATION_CODE_EXPIRED(HttpStatus.BAD_REQUEST, "AUTH_002", "인증코드가 만료되었습니다"),
    INVALID_LOGIN_CREDENTIALS(HttpStatus.UNAUTHORIZED, "AUTH_003", "아이디 또는 비밀번호가 올바르지 않습니다"),
    INVALID_TEMP_PASSWORD(HttpStatus.BAD_REQUEST, "AUTH_004", "임시 비밀번호가 올바르지 않습니다"),
    TEMP_PASSWORD_EXPIRED(HttpStatus.BAD_REQUEST, "AUTH_005", "임시 비밀번호가 만료되었습니다"),

    // JWT 토큰 관련 에러
    INVALID_TOKEN(HttpStatus.UNAUTHORIZED, "JWT_001", "유효하지 않은 토큰입니다"),
    EXPIRED_TOKEN(HttpStatus.UNAUTHORIZED, "JWT_002", "만료된 토큰입니다"),
    INVALID_REFRESH_TOKEN(HttpStatus.UNAUTHORIZED, "JWT_003", "유효하지 않은 리프레시 토큰입니다"),
    EXPIRED_REFRESH_TOKEN(HttpStatus.UNAUTHORIZED, "JWT_004", "만료된 리프레시 토큰입니다"),
    TOKEN_NOT_FOUND(HttpStatus.UNAUTHORIZED, "JWT_005", "토큰이 제공되지 않았습니다"),
    BLACKLISTED_TOKEN(HttpStatus.UNAUTHORIZED, "JWT_006", "블랙리스트에 등록된 토큰입니다"),

    // 인바디 관련 에러
    INBODY_NOT_FOUND(HttpStatus.NOT_FOUND, "INBODY_001", "인바디 기록을 찾을 수 없습니다"),
    INBODY_NO_RECORDS(HttpStatus.NOT_FOUND, "INBODY_002", "인바디 기록이 없습니다"),
    DUPLICATE_INBODY_DATE(HttpStatus.CONFLICT, "INBODY_003", "해당 날짜에 이미 인바디 기록이 존재합니다"),
    INBODY_ACCESS_DENIED(HttpStatus.FORBIDDEN, "INBODY_004", "본인의 인바디 기록만 접근할 수 있습니다"),
    INVALID_MEASUREMENT_DATE(HttpStatus.BAD_REQUEST, "INBODY_005", "유효하지 않은 측정 날짜입니다"),
    FUTURE_MEASUREMENT_DATE(HttpStatus.BAD_REQUEST, "INBODY_006", "미래 날짜로 인바디 기록을 등록할 수 없습니다"),

    //운동 종목 관련 에러
    EXERCISE_NOT_FOUND(HttpStatus.NOT_FOUND, "EXERCISE_001" ,"운동을 찾을 수 없습니다."),
    // 일반적인 에러
    INVALID_INPUT_VALUE(HttpStatus.BAD_REQUEST, "COMMON_001", "올바르지 않은 입력값입니다"),
    INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "COMMON_002", "서버 내부 오류가 발생했습니다"),
    ACCESS_DENIED(HttpStatus.FORBIDDEN, "COMMON_003", "접근이 거부되었습니다");

    private final HttpStatus httpStatus;
    private final String code;
    private final String message;
}