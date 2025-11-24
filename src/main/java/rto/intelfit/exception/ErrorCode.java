package rto.intelfit.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum ErrorCode {

    // ==================== 사용자 관련 에러 ====================
    DUPLICATE_USER_ID(HttpStatus.CONFLICT, "USER_001", "이미 사용중인 아이디입니다"),
    PASSWORD_MISMATCH(HttpStatus.BAD_REQUEST, "USER_004", "비밀번호가 일치하지 않습니다"),
    INVALID_USER_DATA(HttpStatus.BAD_REQUEST, "USER_005", "유효하지 않은 사용자 정보입니다"),
    USER_NOT_FOUND_BY_EMAIL(HttpStatus.NOT_FOUND, "USER_006", "해당 이메일로 등록된 사용자를 찾을 수 없습니다"),
    USER_NOT_FOUND(HttpStatus.NOT_FOUND, "USER_007", "사용자를 찾을 수 없습니다"),
    USER_ALREADY_DELETED(HttpStatus.BAD_REQUEST, "USER_008", "이미 탈퇴한 회원입니다"),

    // ==================== 인증 관련 에러 ====================
    INVALID_VERIFICATION_CODE(HttpStatus.BAD_REQUEST, "AUTH_001", "인증코드가 올바르지 않습니다"),
    VERIFICATION_CODE_EXPIRED(HttpStatus.BAD_REQUEST, "AUTH_002", "인증코드가 만료되었습니다"),
    INVALID_LOGIN_CREDENTIALS(HttpStatus.UNAUTHORIZED, "AUTH_003", "아이디 또는 비밀번호가 올바르지 않습니다"),
    INVALID_TEMP_PASSWORD(HttpStatus.BAD_REQUEST, "AUTH_004", "임시 비밀번호가 올바르지 않습니다"),
    TEMP_PASSWORD_EXPIRED(HttpStatus.BAD_REQUEST, "AUTH_005", "임시 비밀번호가 만료되었습니다"),
    AUTHENTICATION_REQUIRED(HttpStatus.UNAUTHORIZED, "AUTH_006", "인증이 필요합니다"),

    // ==================== JWT 토큰 관련 에러 ====================
    INVALID_TOKEN(HttpStatus.UNAUTHORIZED, "JWT_001", "유효하지 않은 토큰입니다"),
    EXPIRED_TOKEN(HttpStatus.UNAUTHORIZED, "JWT_002", "만료된 토큰입니다"),
    INVALID_REFRESH_TOKEN(HttpStatus.UNAUTHORIZED, "JWT_003", "유효하지 않은 리프레시 토큰입니다"),
    EXPIRED_REFRESH_TOKEN(HttpStatus.UNAUTHORIZED, "JWT_004", "만료된 리프레시 토큰입니다"),
    TOKEN_NOT_FOUND(HttpStatus.UNAUTHORIZED, "JWT_005", "토큰이 제공되지 않았습니다"),
    BLACKLISTED_TOKEN(HttpStatus.UNAUTHORIZED, "JWT_006", "블랙리스트에 등록된 토큰입니다"),
    MALFORMED_TOKEN(HttpStatus.UNAUTHORIZED, "JWT_007", "잘못된 형식의 토큰입니다"),

    // ==================== 인바디 관련 에러 ====================
    INBODY_NOT_FOUND(HttpStatus.NOT_FOUND, "INBODY_001", "인바디 기록을 찾을 수 없습니다"),
    INBODY_NO_RECORDS(HttpStatus.NOT_FOUND, "INBODY_002", "인바디 기록이 없습니다"),
    DUPLICATE_INBODY_DATE(HttpStatus.CONFLICT, "INBODY_003", "해당 날짜에 이미 인바디 기록이 존재합니다"),
    INBODY_ACCESS_DENIED(HttpStatus.FORBIDDEN, "INBODY_004", "본인의 인바디 기록만 접근할 수 있습니다"),
    INVALID_MEASUREMENT_DATE(HttpStatus.BAD_REQUEST, "INBODY_005", "유효하지 않은 측정 날짜입니다"),
    FUTURE_MEASUREMENT_DATE(HttpStatus.BAD_REQUEST, "INBODY_006", "미래 날짜로 인바디 기록을 등록할 수 없습니다"),
    INVALID_INBODY_DATA(HttpStatus.BAD_REQUEST, "INBODY_007", "유효하지 않은 인바디 데이터입니다"),

    // ==================== 식단 관련 에러 ====================
    MEAL_NOT_FOUND(HttpStatus.NOT_FOUND, "MEAL_001", "식사 기록을 찾을 수 없습니다"),
    MEAL_ACCESS_DENIED(HttpStatus.FORBIDDEN, "MEAL_002", "본인의 식사 기록만 접근할 수 있습니다"),
    DUPLICATE_MEAL(HttpStatus.CONFLICT, "MEAL_003", "해당 날짜/시간에 이미 식사 기록이 존재합니다"),
    FUTURE_MEAL_DATE(HttpStatus.BAD_REQUEST, "MEAL_004", "미래 날짜의 식사는 등록할 수 없습니다"),
    INVALID_MEAL_DATE(HttpStatus.BAD_REQUEST, "MEAL_005", "유효하지 않은 식사 날짜입니다"),
    INVALID_MEAL_TYPE(HttpStatus.BAD_REQUEST, "MEAL_006", "유효하지 않은 식사 타입입니다"),
    EMPTY_FOOD_LIST(HttpStatus.BAD_REQUEST, "MEAL_007", "음식 목록이 비어있습니다"),
    INVALID_FOOD_DATA(HttpStatus.BAD_REQUEST, "MEAL_008", "유효하지 않은 음식 정보입니다"),
    INVALID_NUTRITION_DATA(HttpStatus.BAD_REQUEST, "MEAL_009", "유효하지 않은 영양 정보입니다"),
    MEAL_ALREADY_EXISTS(HttpStatus.CONFLICT, "MEAL_010", "해당 날짜/타입에 이미 식사가 등록되어 있습니다"),

    // ==================== 영양 목표 관련 에러 ====================
    NUTRITION_GOAL_NOT_FOUND(HttpStatus.NOT_FOUND, "GOAL_001", "영양 목표를 찾을 수 없습니다"),
    NUTRITION_GOAL_ACCESS_DENIED(HttpStatus.FORBIDDEN, "GOAL_002", "본인의 영양 목표만 접근할 수 있습니다"),
    DUPLICATE_NUTRITION_GOAL(HttpStatus.CONFLICT, "GOAL_003", "이미 영양 목표가 설정되어 있습니다"),
    INVALID_NUTRITION_GOAL_DATA(HttpStatus.BAD_REQUEST, "GOAL_004", "유효하지 않은 영양 목표 데이터입니다"),
    INVALID_GOAL_TYPE(HttpStatus.BAD_REQUEST, "GOAL_005", "유효하지 않은 목표 타입입니다"),
    NEGATIVE_NUTRITION_VALUE(HttpStatus.BAD_REQUEST, "GOAL_006", "영양 목표 값은 0보다 커야 합니다"),
    INSUFFICIENT_USER_DATA_FOR_AUTO_GOAL(HttpStatus.BAD_REQUEST, "GOAL_007", "자동 목표 생성을 위한 사용자 정보가 부족합니다"),

    // ==================== 추천 식단 관련 에러 ====================
    RECOMMENDED_PLAN_NOT_FOUND(HttpStatus.NOT_FOUND, "RECOMMEND_001", "추천 식단을 찾을 수 없습니다"),
    RECOMMENDED_PLAN_ACCESS_DENIED(HttpStatus.FORBIDDEN, "RECOMMEND_002", "본인의 추천 식단만 접근할 수 있습니다"),
    RECOMMENDED_PLAN_ALREADY_SAVED(HttpStatus.CONFLICT, "RECOMMEND_003", "이미 저장된 추천 식단입니다"),
    RECOMMENDED_PLAN_NOT_SAVED(HttpStatus.BAD_REQUEST, "RECOMMEND_004", "저장되지 않은 추천 식단입니다"),
    INVALID_RECOMMENDED_PLAN_DATA(HttpStatus.BAD_REQUEST, "RECOMMEND_005", "유효하지 않은 추천 식단 데이터입니다"),
    AI_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "RECOMMEND_006", "AI 서버 오류가 발생했습니다"),
    AI_SERVER_TIMEOUT(HttpStatus.REQUEST_TIMEOUT, "RECOMMEND_007", "AI 서버 응답 시간이 초과되었습니다"),
    AI_SERVER_UNAVAILABLE(HttpStatus.SERVICE_UNAVAILABLE, "RECOMMEND_008", "AI 서버에 접근할 수 없습니다"),
    INVALID_AI_RESPONSE(HttpStatus.INTERNAL_SERVER_ERROR, "RECOMMEND_009", "AI 서버 응답 형식이 올바르지 않습니다"),

    // ==================== 선호 음식 관련 에러 ====================
    FOOD_PREFERENCE_NOT_FOUND(HttpStatus.NOT_FOUND, "PREF_001", "선호 음식을 찾을 수 없습니다"),
    FOOD_PREFERENCE_ACCESS_DENIED(HttpStatus.FORBIDDEN, "PREF_002", "본인의 선호 음식만 접근할 수 있습니다"),
    DUPLICATE_FOOD_PREFERENCE(HttpStatus.CONFLICT, "PREF_003", "이미 등록된 선호 음식입니다"),
    INVALID_FOOD_PREFERENCE_DATA(HttpStatus.BAD_REQUEST, "PREF_004", "유효하지 않은 선호 음식 데이터입니다"),
    INVALID_PREFERENCE_TYPE(HttpStatus.BAD_REQUEST, "PREF_005", "유효하지 않은 선호도 타입입니다"),
    INVALID_PREFERENCE_SCORE(HttpStatus.BAD_REQUEST, "PREF_006", "선호도 점수는 1에서 5 사이여야 합니다"),
    FOOD_NAME_TOO_LONG(HttpStatus.BAD_REQUEST, "PREF_007", "음식 이름이 너무 깁니다 (최대 200자)"),
    EMPTY_FOOD_NAME(HttpStatus.BAD_REQUEST, "PREF_008", "음식 이름을 입력해주세요"),

    // ==================== 이미지 관련 에러 ====================
    IMAGE_UPLOAD_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "IMAGE_001", "이미지 업로드에 실패했습니다"),
    INVALID_IMAGE_FORMAT(HttpStatus.BAD_REQUEST, "IMAGE_002", "지원하지 않는 이미지 형식입니다"),
    IMAGE_SIZE_EXCEEDED(HttpStatus.BAD_REQUEST, "IMAGE_003", "이미지 크기가 너무 큽니다 (최대 10MB)"),
    IMAGE_NOT_FOUND(HttpStatus.NOT_FOUND, "IMAGE_004", "이미지를 찾을 수 없습니다"),
    IMAGE_ANALYSIS_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "IMAGE_005", "이미지 분석에 실패했습니다"),

    // ==================== 파일 관련 에러 ====================
    FILE_UPLOAD_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "FILE_001", "파일 업로드에 실패했습니다"),
    FILE_SIZE_EXCEEDED(HttpStatus.BAD_REQUEST, "FILE_002", "파일 크기가 너무 큽니다"),
    INVALID_FILE_FORMAT(HttpStatus.BAD_REQUEST, "FILE_003", "지원하지 않는 파일 형식입니다"),
    FILE_NOT_FOUND(HttpStatus.NOT_FOUND, "FILE_004", "파일을 찾을 수 없습니다"),
    EMPTY_FILE(HttpStatus.BAD_REQUEST, "FILE_005", "파일이 비어있습니다"),

    // ==================== 날짜/시간 관련 에러 ====================
    INVALID_DATE_FORMAT(HttpStatus.BAD_REQUEST, "DATE_001", "올바르지 않은 날짜 형식입니다"),
    INVALID_DATE_RANGE(HttpStatus.BAD_REQUEST, "DATE_002", "올바르지 않은 날짜 범위입니다"),
    FUTURE_DATE_NOT_ALLOWED(HttpStatus.BAD_REQUEST, "DATE_003", "미래 날짜는 허용되지 않습니다"),
    START_DATE_AFTER_END_DATE(HttpStatus.BAD_REQUEST, "DATE_004", "시작 날짜가 종료 날짜보다 늦을 수 없습니다"),

    // ==================== 이메일 관련 에러 ====================
    EMAIL_SEND_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "EMAIL_001", "이메일 전송에 실패했습니다"),
    INVALID_EMAIL_FORMAT(HttpStatus.BAD_REQUEST, "EMAIL_002", "올바르지 않은 이메일 형식입니다"),
    EMAIL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "EMAIL_003", "이메일 서버 오류가 발생했습니다"),

    // ==================== 외부 API 관련 에러 ====================
    EXTERNAL_API_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "API_001", "외부 API 호출 중 오류가 발생했습니다"),
    EXTERNAL_API_TIMEOUT(HttpStatus.REQUEST_TIMEOUT, "API_002", "외부 API 응답 시간이 초과되었습니다"),
    EXTERNAL_API_UNAVAILABLE(HttpStatus.SERVICE_UNAVAILABLE, "API_003", "외부 API를 사용할 수 없습니다"),

    // ==================== 운동 기록 관련 에러 ====================
    EXERCISE_NOT_FOUND(HttpStatus.NOT_FOUND, "EXERCISE_001", "운동 기록을 찾을 수 없습니다"),
    EXERCISE_ACCESS_DENIED(HttpStatus.FORBIDDEN, "EXERCISE_002", "본인의 운동 기록만 접근할 수 있습니다"),
    INVALID_EXERCISE_DATA(HttpStatus.BAD_REQUEST, "EXERCISE_003", "유효하지 않은 운동 데이터입니다"),
    EMPTY_EXERCISE_SETS(HttpStatus.BAD_REQUEST, "EXERCISE_004", "운동 세트가 비어있습니다"),
    INVALID_EXERCISE_CATEGORY(HttpStatus.BAD_REQUEST, "EXERCISE_005", "유효하지 않은 운동 카테고리입니다"),
    FUTURE_EXERCISE_DATE(HttpStatus.BAD_REQUEST, "EXERCISE_006", "미래 날짜의 운동은 등록할 수 없습니다"),

    // ==================== 추천 운동 관련 에러 ====================
    RECOMMENDED_EXERCISE_PLAN_NOT_FOUND(HttpStatus.NOT_FOUND, "REC_EXERCISE_001", "추천 운동 플랜을 찾을 수 없습니다"),
    RECOMMENDED_EXERCISE_PLAN_ACCESS_DENIED(HttpStatus.FORBIDDEN, "REC_EXERCISE_002", "본인의 추천 운동 플랜만 접근할 수 있습니다"),
    RECOMMENDED_EXERCISE_PLAN_ALREADY_SAVED(HttpStatus.CONFLICT, "REC_EXERCISE_003", "이미 저장된 추천 운동 플랜입니다"),
    INVALID_RECOMMENDED_EXERCISE_DATA(HttpStatus.BAD_REQUEST, "REC_EXERCISE_004", "유효하지 않은 추천 운동 데이터입니다"),

    // ==================== 일반적인 에러 ====================
    INVALID_INPUT_VALUE(HttpStatus.BAD_REQUEST, "COMMON_001", "올바르지 않은 입력값입니다"),
    INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "COMMON_002", "서버 내부 오류가 발생했습니다"),
    ACCESS_DENIED(HttpStatus.FORBIDDEN, "COMMON_003", "접근이 거부되었습니다"),
    RESOURCE_NOT_FOUND(HttpStatus.NOT_FOUND, "COMMON_004", "요청한 리소스를 찾을 수 없습니다"),
    METHOD_NOT_ALLOWED(HttpStatus.METHOD_NOT_ALLOWED, "COMMON_005", "허용되지 않은 HTTP 메서드입니다"),
    UNSUPPORTED_MEDIA_TYPE(HttpStatus.UNSUPPORTED_MEDIA_TYPE, "COMMON_006", "지원하지 않는 미디어 타입입니다"),
    REQUEST_TIMEOUT(HttpStatus.REQUEST_TIMEOUT, "COMMON_007", "요청 시간이 초과되었습니다"),
    TOO_MANY_REQUESTS(HttpStatus.TOO_MANY_REQUESTS, "COMMON_008", "너무 많은 요청입니다. 잠시 후 다시 시도해주세요"),
    SERVICE_UNAVAILABLE(HttpStatus.SERVICE_UNAVAILABLE, "COMMON_009", "서비스를 일시적으로 사용할 수 없습니다"),
    MISSING_REQUIRED_FIELD(HttpStatus.BAD_REQUEST, "COMMON_010", "필수 항목이 누락되었습니다"),
    INVALID_PARAMETER(HttpStatus.BAD_REQUEST, "COMMON_011", "유효하지 않은 파라미터입니다"),
    DATABASE_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "COMMON_012", "데이터베이스 오류가 발생했습니다"),
    NOT_FOUND(HttpStatus.NOT_FOUND, "COMMON_013" ,"리소스를 찾을 수 없습니다."),
    NOT_FOUND_TEMP_MEAL(HttpStatus.NOT_FOUND, "TEMP_MEAL_NOT_FOUND", "임시 저장된 식단이 없습니다."),
    DUPLICATE_EMAIL(HttpStatus.CONFLICT, "COMMMON_000", "이미 사용중인 이메일입니다." ),
    INVALID_STATE(HttpStatus.BAD_REQUEST, "INVALID_STATE", "Invalid workout state");

    private final HttpStatus httpStatus;
    private final String code;
    private final String message;
}