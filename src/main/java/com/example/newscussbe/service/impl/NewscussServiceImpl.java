package com.example.newscussbe.service.impl;

import com.example.newscussbe.client.PythonApiClient;
import com.example.newscussbe.dto.DiscussionResponseDto;
import com.example.newscussbe.dto.KeywordSummaryResponseDto;
import com.example.newscussbe.dto.Message;
import com.example.newscussbe.dto.MessageResponseDto;
import com.example.newscussbe.dto.SummaryResponseDto;
import com.example.newscussbe.dto.TopicResponseDto;
import com.example.newscussbe.service.NewscussService;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class NewscussServiceImpl implements NewscussService {

    private final PythonApiClient pythonApiClient;
    private final ObjectMapper objectMapper;

    // 세션 데이터 저장을 위한 ConcurrentHashMap
    private final ConcurrentHashMap<String, SessionData> sessionStore = new ConcurrentHashMap<>();

    @Override
    public KeywordSummaryResponseDto processUrl(String url) {
        log.info("Processing URL: {}", url);

        // 세션 ID 생성
        String sessionId = UUID.randomUUID().toString();

        // Python API 호출: URL에서 키워드와 요약 추출
        KeywordSummaryResponseDto result = pythonApiClient.extractKeywordsAndSummary(url);
        result.setSessionId(sessionId);

        // 세션 데이터 저장
        SessionData sessionData = new SessionData();
        sessionData.setSummary(result.getSummary());
        sessionData.setKeywords(result.getKeywords());
        sessionStore.put(sessionId, sessionData);

        return result;
    }

    @Override
    public TopicResponseDto generateTopic(String sessionId, String summary, List<String> keywords) {
        log.info("Generating topic for session: {}", sessionId);

        SessionData sessionData = getSessionData(sessionId);

        // Python API 호출: 토론 주제 생성
        TopicResponseDto topicResponse = pythonApiClient.generateTopic(summary, keywords);

        // 세션 데이터 업데이트
        sessionData.setTopic(topicResponse.getTopic());
        sessionData.setTopicDescription(topicResponse.getDescription());

        return topicResponse;
    }

    @Override
    public DiscussionResponseDto startDiscussion(String sessionId, String topic, String userPosition, String difficulty) {
        log.info("Starting discussion for session: {}, topic: {}, position: {}, difficulty: {}",
                sessionId, topic, userPosition, difficulty);

        SessionData sessionData = getSessionData(sessionId);

        // 세션 데이터 업데이트
        sessionData.setUserPosition(userPosition);
        sessionData.setDifficulty(difficulty);

        // AI 입장 설정 (사용자와 반대)
        String aiPosition = "찬성".equals(userPosition) ? "반대" : "찬성";
        sessionData.setAiPosition(aiPosition);

        // 메시지 리스트 초기화
        sessionData.setMessages(new ArrayList<>());

        // Python API 호출: 토론 시작 및 AI의 첫 메시지 얻기
        String aiFirstMessage = pythonApiClient.startDiscussion(topic, userPosition, aiPosition, difficulty);

        // AI 첫 메시지 저장
        Message aiMessage = Message.builder()
                .role("ai")
                .content(aiFirstMessage)
                .timestamp(LocalDateTime.now())
                .build();
        sessionData.getMessages().add(aiMessage);

        return DiscussionResponseDto.builder()
                .aiMessage(aiFirstMessage)
                .aiPosition(aiPosition)
                .build();
    }

    @Override
    public MessageResponseDto processMessage(String sessionId, String message) {
        log.info("Processing message for session: {}", sessionId);

        SessionData sessionData = getSessionData(sessionId);

        // 사용자 메시지 저장
        Message userMessage = Message.builder()
                .role("user")
                .content(message)
                .timestamp(LocalDateTime.now())
                .build();
        sessionData.getMessages().add(userMessage);

        // Python API 호출: 메시지에 대한 AI 응답 얻기
        String aiResponseMessage = pythonApiClient.getAiResponse(
                sessionData.getTopic(),
                sessionData.getUserPosition(),
                sessionData.getAiPosition(),
                sessionData.getDifficulty(),
                sessionData.getMessages()
        );

        // AI 응답 메시지 저장
        Message aiMessage = Message.builder()
                .role("ai")
                .content(aiResponseMessage)
                .timestamp(LocalDateTime.now())
                .build();
        sessionData.getMessages().add(aiMessage);

        return MessageResponseDto.builder()
                .aiMessage(aiResponseMessage)
                .build();
    }

    @Override
    public SummaryResponseDto generateSummary(String sessionId) {
        log.info("Generating summary for session: {}", sessionId);

        SessionData sessionData = getSessionData(sessionId);

        // Python API 호출: 토론 요약 생성
        String summary = pythonApiClient.generateSummary(
                sessionData.getTopic(),
                sessionData.getUserPosition(),
                sessionData.getAiPosition(),
                sessionData.getMessages()
        );

        return SummaryResponseDto.builder()
                .summary(summary)
                .build();
    }

    @Override
    public String getSessionStatus(String sessionId) {
        SessionData sessionData = sessionStore.get(sessionId);
        if (sessionData == null) {
            return "Session not found";
        }

        try {
            return objectMapper.writeValueAsString(sessionData);
        } catch (Exception e) {
            log.error("Error serializing session data", e);
            return "Error getting session data: " + e.getMessage();
        }
    }

    private SessionData getSessionData(String sessionId) {
        SessionData sessionData = sessionStore.get(sessionId);
        if (sessionData == null) {
            throw new IllegalArgumentException("Session not found: " + sessionId);
        }
        return sessionData;
    }

    // 세션 데이터 내부 클래스
    static class SessionData {
        private String summary;
        private List<String> keywords;
        private String topic;
        private String topicDescription;
        private String userPosition;
        private String aiPosition;
        private String difficulty;
        private List<Message> messages;

        // Getters and Setters
        public String getSummary() {
            return summary;
        }

        public void setSummary(String summary) {
            this.summary = summary;
        }

        public List<String> getKeywords() {
            return keywords;
        }

        public void setKeywords(List<String> keywords) {
            this.keywords = keywords;
        }

        public String getTopic() {
            return topic;
        }

        public void setTopic(String topic) {
            this.topic = topic;
        }

        public String getTopicDescription() {
            return topicDescription;
        }

        public void setTopicDescription(String topicDescription) {
            this.topicDescription = topicDescription;
        }

        public String getUserPosition() {
            return userPosition;
        }

        public void setUserPosition(String userPosition) {
            this.userPosition = userPosition;
        }

        public String getAiPosition() {
            return aiPosition;
        }

        public void setAiPosition(String aiPosition) {
            this.aiPosition = aiPosition;
        }

        public String getDifficulty() {
            return difficulty;
        }

        public void setDifficulty(String difficulty) {
            this.difficulty = difficulty;
        }

        public List<Message> getMessages() {
            return messages;
        }

        public void setMessages(List<Message> messages) {
            this.messages = messages;
        }
    }
}