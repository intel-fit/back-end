package rto.intelfit.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

import java.util.List;

public class UserFoodPreferenceDto {

    @Getter @Setter
    @NoArgsConstructor @AllArgsConstructor @Builder
    @Schema(description = "음식 선호/비선호 조회 응답")
    public static class Response {
        private List<String> preferredFoods;
        private List<String> dislikedFoods;
    }

    @Getter @Setter
    @NoArgsConstructor @AllArgsConstructor @Builder
    @Schema(description = "비선호 음식 추가 요청")
    public static class AddDislikedFoodRequest {
        @Schema(example = "돼지고기")
        private String foodName;
    }
}
