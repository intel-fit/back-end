package rto.intelfit.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ChatbotMessageRequest {

    @Schema(description = "사용자 메시지", example = "오늘 먹은 식단을 분석해줘")
    private String message;
}
