package rto.intelfit.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import rto.intelfit.domain.AIChatMessage;
import rto.intelfit.domain.User;
import rto.intelfit.dto.AIChatMessageDto;
import rto.intelfit.exception.BusinessException;
import rto.intelfit.exception.ErrorCode;
import rto.intelfit.repository.AIChatMessageRepository;
import rto.intelfit.repository.UserRepository;

import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class AIChatService {

    private final AIServerClient aiServerClient;
    private final UserRepository userRepository;
    private final AIChatMessageRepository chatMessageRepository;
    private final ObjectMapper objectMapper;

    /**
     * AI 코치 챗봇과의 대화를 처리하고, 사용자 메시지와 응답을 저장한다.
     */
    @Transactional
    public Map<String, Object> handleChat(Long userId, String message) {
        Map<String, Object> aiResponse = aiServerClient.chatWithCoach(String.valueOf(userId), message);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        AIChatMessage chatMessage = AIChatMessage.builder()
                .user(user)
                .userMessage(message)
                .aiResponse(resolveAiReply(aiResponse))
                .rawResponse(serializeResponse(aiResponse))
                .build();

        chatMessageRepository.save(chatMessage);

        return aiResponse;
    }

    /**
     * 최근 대화 내용 조회 (최대 limit 개).
     */
    @Transactional(readOnly = true)
    public List<AIChatMessageDto> getRecentMessages(Long userId, int limit) {
        return chatMessageRepository.findTop50ByUser_IdOrderByCreatedAtDesc(userId)
                .stream()
                .limit(limit)
                .map(AIChatMessageDto::from)
                .toList();
    }

    private String resolveAiReply(Map<String, Object> response) {
        if (response == null || response.isEmpty()) {
            return "";
        }

        List<String> candidateKeys = List.of("ai_reply", "response", "message", "answer");
        for (String key : candidateKeys) {
            Object value = response.get(key);
            if (value != null) {
                return String.valueOf(value);
            }
        }
        return response.toString();
    }

    private String serializeResponse(Map<String, Object> response) {
        if (response == null || response.isEmpty()) {
            return null;
        }

        try {
            return objectMapper.writeValueAsString(response);
        } catch (JsonProcessingException e) {
            log.warn("AI 응답 직렬화 실패 - {}", e.getMessage());
            return response.toString();
        }
    }
}
