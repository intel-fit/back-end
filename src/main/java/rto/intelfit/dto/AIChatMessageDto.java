package rto.intelfit.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;
import rto.intelfit.domain.AIChatMessage;

import java.time.LocalDateTime;

@Getter
@Builder
public class AIChatMessageDto {

    @Schema(description = "사용자 메시지")
    private final String userMessage;

    @Schema(description = "AI 응답")
    private final String aiResponse;

    @Schema(description = "대화가 생성된 시간")
    private final LocalDateTime createdAt;

    public static AIChatMessageDto from(AIChatMessage message) {
        return AIChatMessageDto.builder()
                .userMessage(message.getUserMessage())
                .aiResponse(message.getAiResponse())
                .createdAt(message.getCreatedAt())
                .build();
    }
}
