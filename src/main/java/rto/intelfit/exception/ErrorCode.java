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
    USER_NOT_FOUND(HttpStatus.NOT_FOUND, "USER_003", "사용자를 찾을 수 없습니다"),
    PASSWORD_MISMATCH(HttpStatus.BAD_REQUEST, "USER_004", "비밀번호가 일치하지 않습니다"),

    // 인증 관련 에러
    INVALID_VERIFICATION_CODE(HttpStatus.BAD_REQUEST, "AUTH_001", "인증코드가 올바르지 않습니다"),
    VERIFICATION_CODE_EXPIRED(HttpStatus.BAD_REQUEST, "AUTH_002", "인증코드가 만료되었습니다"),
    INVALID_LOGIN_CREDENTIALS(HttpStatus.UNAUTHORIZED, "AUTH_003", "아이디 또는 비밀번호가 올바르지 않습니다"),
    INVALID_TEMP_PASSWORD(HttpStatus.BAD_REQUEST, "AUTH_004", "임시 비밀번호가 올바르지 않습니다"),
    TEMP_PASSWORD_EXPIRED(HttpStatus.BAD_REQUEST, "AUTH_005", "임시 비밀번호가 만료되었습니다"),

    // 일반적인 에러
    INVALID_INPUT_VALUE(HttpStatus.BAD_REQUEST, "COMMON_001", "올바르지 않은 입력값입니다"),
    INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "COMMON_002", "서버 내부 오류가 발생했습니다");

    private final HttpStatus httpStatus;
    private final String code;
    private final String message;
}