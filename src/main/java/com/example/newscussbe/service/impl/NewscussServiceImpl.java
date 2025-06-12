package com.example.newscussbe.service.impl;

import com.example.newscussbe.client.PythonApiClient;
import com.example.newscussbe.dto.DiscussionResponseDto;
import com.example.newscussbe.dto.FeedbackResponseDto;
import com.example.newscussbe.dto.KeywordSummaryResponseDto;
import com.example.newscussbe.dto.Message;
import com.example.newscussbe.dto.MessageResponseDto;
import com.example.newscussbe.dto.SummaryResponseDto;
import com.example.newscussbe.dto.TopicResponseDto;
import com.example.newscussbe.service.NewscussService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import org.springframework.web.client.RequestCallback;
import org.springframework.web.client.ResponseExtractor;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@Slf4j
@Service
@RequiredArgsConstructor
public class NewscussServiceImpl implements NewscussService {

    private final PythonApiClient pythonApiClient;
    private final ObjectMapper objectMapper;
    private final RestTemplate restTemplate;

    @Value("${python.api.base-url}")
    private String pythonApiBaseUrl;

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
    public void processMessageStream(String sessionId, String message, SseEmitter emitter) {
        log.info("🚀 Processing streaming message for session: {}", sessionId);

        // 비동기로 처리
        CompletableFuture.runAsync(() -> {
            try {
                SessionData sessionData = getSessionData(sessionId);

                // 사용자 메시지 저장
                Message userMessage = Message.builder()
                        .role("user")
                        .content(message)
                        .timestamp(LocalDateTime.now())
                        .build();
                sessionData.getMessages().add(userMessage);

                // Python API 스트리밍 호출 - 최적화된 버전
                streamFromPythonApiOptimized(sessionData, emitter);

            } catch (Exception e) {
                log.error("💥 Error in streaming message processing", e);
                try {
                    // JSON 형태로 에러 데이터 전송
                    emitter.send(SseEmitter.event()
                            .name("message")
                            .data("{\"type\":\"error\",\"message\":\"" + e.getMessage() + "\"}"));
                    emitter.completeWithError(e);
                } catch (Exception sendError) {
                    log.error("💥 Error sending error message", sendError);
                    emitter.completeWithError(sendError);
                }
            }
        });
    }

    /**
     * 최적화된 스트리밍 메서드 - 안정적인 연결 관리
     */
    private void streamFromPythonApiOptimized(SessionData sessionData, SseEmitter emitter) {
        try {
            String endpoint = pythonApiBaseUrl + "/discussion/message/stream";

            Map<String, Object> requestMap = new HashMap<>();
            requestMap.put("topic", sessionData.getTopic());
            requestMap.put("userPosition", sessionData.getUserPosition());
            requestMap.put("aiPosition", sessionData.getAiPosition());
            requestMap.put("difficulty", sessionData.getDifficulty());
            requestMap.put("messages", sessionData.getMessages());

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("Accept", "text/event-stream");

            HttpEntity<Map<String, Object>> request = new HttpEntity<>(requestMap, headers);

            log.info("📡 Starting optimized streaming for session");

            RequestCallback requestCallback = restTemplate.httpEntityCallback(request, String.class);

            ResponseExtractor<Void> responseExtractor = response -> {
                StringBuilder accumulatedMessage = new StringBuilder();
                boolean hasCompleted = false;

                try (BufferedReader reader = new BufferedReader(new InputStreamReader(response.getBody()))) {
                    String line;

                    while ((line = reader.readLine()) != null && !hasCompleted) {
                        if (line.trim().isEmpty()) {
                            continue;
                        }

                        if (line.startsWith("data: ")) {
                            String jsonData = line.substring(6).trim();

                            if (jsonData.isEmpty() || jsonData.equals("{}")) {
                                continue;
                            }

                            try {
                                JsonNode dataNode = objectMapper.readTree(jsonData);
                                String type = dataNode.get("type").asText();

                                if ("chunk".equals(type)) {
                                    String content = dataNode.get("content").asText();
                                    accumulatedMessage.append(content);

                                    // 청크만 전송
                                    String chunkJson = String.format(
                                            "{\"type\":\"chunk\",\"content\":\"%s\"}",
                                            content.replace("\"", "\\\"").replace("\n", "\\n").replace("\r", "")
                                    );

                                    emitter.send(SseEmitter.event()
                                            .name("message")
                                            .data(chunkJson));

                                } else if ("end".equals(type)) {
                                    String finalMessage = dataNode.has("final_message")
                                            ? dataNode.get("final_message").asText()
                                            : accumulatedMessage.toString();

                                    log.info("🏁 Stream completed. Message length: {}", finalMessage.length());

                                    // 세션에 저장
                                    Message aiMessage = Message.builder()
                                            .role("ai")
                                            .content(finalMessage)
                                            .timestamp(LocalDateTime.now())
                                            .build();
                                    sessionData.getMessages().add(aiMessage);

                                    // 완료 신호 전송
                                    emitter.send(SseEmitter.event()
                                            .name("message")
                                            .data("{\"type\":\"end\"}"));

                                    hasCompleted = true;
                                    emitter.complete();
                                    return null;

                                } else if ("error".equals(type)) {
                                    String errorMessage = dataNode.get("message").asText();
                                    log.error("❌ Error from Python API: {}", errorMessage);

                                    String errorJson = String.format(
                                            "{\"type\":\"error\",\"message\":\"%s\"}",
                                            errorMessage.replace("\"", "\\\"")
                                    );

                                    emitter.send(SseEmitter.event()
                                            .name("message")
                                            .data(errorJson));

                                    hasCompleted = true;
                                    emitter.completeWithError(new RuntimeException(errorMessage));
                                    return null;
                                }

                            } catch (Exception parseError) {
                                log.error("🚫 Error parsing SSE data: {}", jsonData, parseError);
                                // 파싱 에러는 무시하고 계속 진행
                                continue;
                            }
                        }
                    }

                    // 정상적으로 완료되지 않은 경우 강제 완료
                    if (!hasCompleted && accumulatedMessage.length() > 0) {
                        log.warn("⚠️ Stream ended without proper completion signal, forcing completion");

                        String finalMessage = accumulatedMessage.toString();

                        Message aiMessage = Message.builder()
                                .role("ai")
                                .content(finalMessage)
                                .timestamp(LocalDateTime.now())
                                .build();
                        sessionData.getMessages().add(aiMessage);

                        emitter.send(SseEmitter.event()
                                .name("message")
                                .data("{\"type\":\"end\"}"));
                    }

                    if (!hasCompleted) {
                        emitter.complete();
                    }

                } catch (Exception streamError) {
                    log.error("💥 Error in stream processing", streamError);
                    if (!hasCompleted) {
                        try {
                            emitter.send(SseEmitter.event()
                                    .name("message")
                                    .data("{\"type\":\"error\",\"message\":\"스트리밍 처리 중 오류가 발생했습니다\"}"));
                            emitter.completeWithError(streamError);
                        } catch (Exception e) {
                            log.error("Failed to send error message", e);
                            emitter.completeWithError(streamError);
                        }
                    }
                }

                return null;
            };

            restTemplate.execute(endpoint, HttpMethod.POST, requestCallback, responseExtractor);

        } catch (Exception e) {
            log.error("💥 Error streaming from Python API", e);
            try {
                emitter.send(SseEmitter.event()
                        .name("message")
                        .data("{\"type\":\"error\",\"message\":\"연결 오류가 발생했습니다\"}"));
                emitter.completeWithError(e);
            } catch (Exception sendError) {
                log.error("💥 Error sending error message", sendError);
                emitter.completeWithError(e);
            }
        }
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
    public FeedbackResponseDto generateFeedback(String sessionId) {
        log.info("Generating feedback for session: {}", sessionId);

        SessionData sessionData = getSessionData(sessionId);

        // 메시지가 충분히 있는지 확인 (최소 2개 이상의 사용자 메시지)
        long userMessageCount = sessionData.getMessages().stream()
                .filter(msg -> "user".equals(msg.getRole()))
                .count();

        if (userMessageCount < 2) {
            log.warn("Insufficient user messages for feedback generation: {}", userMessageCount);
            // 기본 피드백 반환
            Map<String, Object> defaultFeedback = Map.of(
                    "논리적_사고력", Map.of("점수", 50, "코멘트", "토론 참여가 부족하여 정확한 평가가 어렵습니다"),
                    "근거와_증거_활용", Map.of("점수", 50, "코멘트", "토론 참여가 부족하여 정확한 평가가 어렵습니다"),
                    "의사소통_능력", Map.of("점수", 50, "코멘트", "토론 참여가 부족하여 정확한 평가가 어렵습니다"),
                    "토론_태도와_매너", Map.of("점수", 50, "코멘트", "토론 참여가 부족하여 정확한 평가가 어렵습니다"),
                    "창의성과_통찰력", Map.of("점수", 50, "코멘트", "토론 참여가 부족하여 정확한 평가가 어렵습니다"),
                    "총점", 50,
                    "종합_코멘트", "더 활발한 토론 참여를 통해 다양한 능력을 보여주세요!"
            );

            return FeedbackResponseDto.builder()
                    .feedback(defaultFeedback)
                    .build();
        }

        // Python API 호출: 토론 피드백 생성
        Map<String, Object> feedback = pythonApiClient.generateFeedback(
                sessionData.getTopic(),
                sessionData.getUserPosition(),
                sessionData.getAiPosition(),
                sessionData.getMessages()
        );

        return FeedbackResponseDto.builder()
                .feedback(feedback)
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
    @Data
    static class SessionData {
        private String summary;
        private List<String> keywords;
        private String topic;
        private String topicDescription;
        private String userPosition;
        private String aiPosition;
        private String difficulty;
        private List<Message> messages;
    }
}