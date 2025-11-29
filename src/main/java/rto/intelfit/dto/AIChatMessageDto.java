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

    @Schema(description = "챗봇 모드(auto/nutrition/exercise)")
    private final String mode;

    @Schema(description = "코치 스타일(default/pro/friend/soft/drill)")
    private final String coachStyle;

    @Schema(description = "감정 분석 결과")
    private final String emotionDetected;

    @Schema(description = "대화가 생성된 시간")
    private final LocalDateTime createdAt;

    public static AIChatMessageDto from(AIChatMessage message) {
        return AIChatMessageDto.builder()
                .userMessage(message.getUserMessage())
                .aiResponse(message.getAiResponse())
                .mode(message.getMode())
                .coachStyle(message.getCoachStyle())
                .emotionDetected(message.getEmotionDetected())
                .createdAt(message.getCreatedAt())
                .build();
    }
}
