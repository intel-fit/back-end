package rto.intelfit.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import rto.intelfit.domain.UserFoodPreference;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

public class UserFoodPreferenceDto {

    // ========== 선호 음식 추가 요청 ==========
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "선호 음식 추가 요청")
    public static class FoodPreferenceAddRequest {

        @Schema(description = "음식 이름", example = "닭가슴살")
        @NotBlank(message = "음식 이름을 입력해주세요")
        @Size(max = 200, message = "음식 이름은 200자 이내로 입력해주세요")
        private String foodName;

        @Schema(description = "선호도 타입", example = "LIKE")
        @NotNull(message = "선호도 타입을 선택해주세요")
        private UserFoodPreference.PreferenceType preferenceType;

        @Schema(description = "선호도 점수 (1-5)", example = "5")
        @Min(value = 1, message = "선호도 점수는 1 이상이어야 합니다")
        @Max(value = 5, message = "선호도 점수는 5 이하여야 합니다")
        private Integer preferenceScore;

        @Schema(description = "카테고리", example = "단백질")
        @Size(max = 100, message = "카테고리는 100자 이내로 입력해주세요")
        private String category;

        @Schema(description = "태그 (쉼표로 구분)", example = "고단백,저지방,다이어트")
        @Size(max = 500, message = "태그는 500자 이내로 입력해주세요")
        private String tags;

        @Schema(description = "메모", example = "맛있어요!")
        @Size(max = 500, message = "메모는 500자 이내로 입력해주세요")
        private String memo;
    }

    // ========== 선호 음식 수정 요청 ==========
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "선호 음식 수정 요청")
    public static class FoodPreferenceUpdateRequest {

        @Schema(description = "선호도 타입", example = "FAVORITE")
        private UserFoodPreference.PreferenceType preferenceType;

        @Schema(description = "선호도 점수 (1-5)", example = "5")
        @Min(value = 1, message = "선호도 점수는 1 이상이어야 합니다")
        @Max(value = 5, message = "선호도 점수는 5 이하여야 합니다")
        private Integer preferenceScore;

        @Schema(description = "카테고리", example = "단백질")
        @Size(max = 100, message = "카테고리는 100자 이내로 입력해주세요")
        private String category;

        @Schema(description = "태그 (쉼표로 구분)", example = "고단백,저지방")
        @Size(max = 500, message = "태그는 500자 이내로 입력해주세요")
        private String tags;

        @Schema(description = "메모", example = "정말 맛있어요!")
        @Size(max = 500, message = "메모는 500자 이내로 입력해주세요")
        private String memo;
    }

    // ========== 선호 음식 추가/수정 응답 ==========
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "선호 음식 추가/수정 응답")
    public static class FoodPreferenceResponse {

        @Schema(description = "성공 여부", example = "true")
        private boolean success;

        @Schema(description = "메시지", example = "선호 음식이 추가되었습니다")
        private String message;

        @Schema(description = "선호 음식 정보")
        private FoodPreferenceDetailResponse preference;
    }

    // ========== 선호 음식 상세 응답 ==========
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "선호 음식 상세 정보")
    public static class FoodPreferenceDetailResponse {

        @Schema(description = "선호 음식 ID", example = "1")
        private Long id;

        @Schema(description = "음식 이름", example = "닭가슴살")
        private String foodName;

        @Schema(description = "선호도 타입", example = "LIKE")
        private UserFoodPreference.PreferenceType preferenceType;

        @Schema(description = "선호도 타입 설명", example = "좋아요")
        private String preferenceTypeName;

        @Schema(description = "선호도 점수 (1-5)", example = "5")
        private Integer preferenceScore;

        @Schema(description = "카테고리", example = "단백질")
        private String category;

        @Schema(description = "태그", example = "고단백,저지방,다이어트")
        private String tags;

        @Schema(description = "메모", example = "맛있어요!")
        private String memo;

        @Schema(description = "섭취 횟수", example = "15")
        private Integer consumedCount;

        @Schema(description = "마지막 섭취 일시", example = "2025-01-15T08:30:00")
        @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
        private LocalDateTime lastConsumedAt;

        @Schema(description = "생성 일시", example = "2025-01-10T10:00:00")
        @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
        private LocalDateTime createdAt;

        @Schema(description = "수정 일시", example = "2025-01-15T14:30:00")
        @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
        private LocalDateTime updatedAt;

        public static FoodPreferenceDetailResponse from(UserFoodPreference preference) {
            return FoodPreferenceDetailResponse.builder()
                    .id(preference.getId())
                    .foodName(preference.getFoodName())
                    .preferenceType(preference.getPreferenceType())
                    .preferenceTypeName(preference.getPreferenceType().getDescription())
                    .preferenceScore(preference.getPreferenceScore())
                    .category(preference.getCategory())
                    .tags(preference.getTags())
                    .memo(preference.getMemo())
                    .consumedCount(preference.getConsumedCount())
                    .lastConsumedAt(preference.getLastConsumedAt())
                    .createdAt(preference.getCreatedAt())
                    .updatedAt(preference.getUpdatedAt())
                    .build();
        }
    }

    // ========== 선호 음식 목록 조회 응답 ==========
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "선호 음식 목록 조회 응답")
    public static class FoodPreferenceListResponse {

        @Schema(description = "총 개수", example = "25")
        private int totalCount;

        @Schema(description = "선호 음식 목록")
        private List<FoodPreferenceDetailResponse> preferences;

        public static FoodPreferenceListResponse from(List<UserFoodPreference> preferences) {
            return FoodPreferenceListResponse.builder()
                    .totalCount(preferences.size())
                    .preferences(preferences.stream()
                            .map(FoodPreferenceDetailResponse::from)
                            .collect(Collectors.toList()))
                    .build();
        }
    }

    // ========== 자주 먹는 음식 응답 ==========
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "자주 먹는 음식 정보")
    public static class FrequentFoodResponse {

        @Schema(description = "음식 이름", example = "닭가슴살")
        private String foodName;

        @Schema(description = "섭취 횟수", example = "25")
        private Integer consumedCount;

        @Schema(description = "선호도 점수", example = "5")
        private Integer preferenceScore;

        @Schema(description = "마지막 섭취 일시", example = "2025-01-15T08:30:00")
        @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
        private LocalDateTime lastConsumedAt;

        public static FrequentFoodResponse from(UserFoodPreference preference) {
            return FrequentFoodResponse.builder()
                    .foodName(preference.getFoodName())
                    .consumedCount(preference.getConsumedCount())
                    .preferenceScore(preference.getPreferenceScore())
                    .lastConsumedAt(preference.getLastConsumedAt())
                    .build();
        }
    }

    // ========== AI 추천용 선호 데이터 ==========
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "AI 추천용 선호 데이터")
    public static class PreferenceDataForAI {

        @Schema(description = "좋아하는 음식 목록")
        private List<String> likedFoods;

        @Schema(description = "싫어하는 음식 목록")
        private List<String> dislikedFoods;

        @Schema(description = "자주 먹는 음식 목록")
        private List<String> frequentFoods;

        @Schema(description = "선호 카테고리")
        private List<String> preferredCategories;

        @Schema(description = "선호 태그")
        private List<String> preferredTags;
    }

    // ========== 선호 음식 삭제 응답 ==========
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "선호 음식 삭제 응답")
    public static class FoodPreferenceDeleteResponse {

        @Schema(description = "성공 여부", example = "true")
        private boolean success;

        @Schema(description = "메시지", example = "선호 음식이 삭제되었습니다")
        private String message;
    }
}