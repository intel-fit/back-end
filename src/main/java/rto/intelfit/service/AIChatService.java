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
import rto.intelfit.service.MembershipService;

import java.time.LocalDate;
import java.time.LocalDateTime;
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
    private final MembershipService membershipService;

    /**
     * AI 코치 챗봇과의 대화를 처리하고, 사용자 메시지와 응답을 저장한다.
     */
    @Transactional
    public Map<String, Object> handleChat(Long userId, String message, String mode, String coachStyle) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        // 멤버십 상태 최신화 (만료 처리 포함)
        membershipService.syncMembership(user.getUserId());
        user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        if (user.getMembershipType() == User.MembershipType.FREE) {
            resetChatbotTokensIfNeeded(user);
            if (user.getChatbotTokens() == null || user.getChatbotTokens() <= 0) {
                throw new BusinessException(ErrorCode.CHATBOT_TOKENS_EXHAUSTED, "무료 사용자의 일일 챗봇 토큰이 부족합니다");
            }
            user.setChatbotTokens(user.getChatbotTokens() - 1);
            userRepository.save(user);
        }

        Map<String, Object> aiResponse = aiServerClient.chatWithCoach(user.getUserId(), message, mode, coachStyle);

        AIChatMessage chatMessage = AIChatMessage.builder()
                .user(user)
                .userMessage(message)
                .aiResponse(resolveAiReply(aiResponse))
                .mode(mode)
                .coachStyle(coachStyle)
                .emotionDetected(resolveEmotion(aiResponse))
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

        List<String> candidateKeys = List.of("reply", "ai_reply", "response", "message", "answer");
        for (String key : candidateKeys) {
            Object value = response.get(key);
            if (value != null) {
                return String.valueOf(value);
            }
        }
        return response.toString();
    }

    private String resolveEmotion(Map<String, Object> response) {
        if (response == null || response.isEmpty()) {
            return null;
        }
        Object value = response.get("emotion_detected");
        return value != null ? String.valueOf(value) : null;
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

    private void resetChatbotTokensIfNeeded(User user) {
        LocalDate today = LocalDate.now();
        LocalDate lastResetDate = user.getChatbotLastReset() != null ? user.getChatbotLastReset().toLocalDate() : null;
        if (lastResetDate == null || lastResetDate.isBefore(today)) {
            user.setChatbotTokens(3);
            user.setChatbotLastReset(LocalDateTime.now());
        }
    }
}
