package com.example.newscussbe.client;

import com.example.newscussbe.dto.KeywordSummaryResponseDto;
import com.example.newscussbe.dto.Message;
import com.example.newscussbe.dto.TopicResponseDto;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

@Slf4j
@Component
@RequiredArgsConstructor
public class PythonApiClient {

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    @Value("${python.api.base-url}")
    private String pythonApiBaseUrl;

    /**
     * URL에서 키워드와 요약 추출
     */
    public KeywordSummaryResponseDto extractKeywordsAndSummary(String url) {
        String endpoint = pythonApiBaseUrl + "/extract";

        Map<String, Object> requestMap = new HashMap<>();
        requestMap.put("url", url);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<Map<String, Object>> request = new HttpEntity<>(requestMap, headers);

        log.info("Calling Python API: {} with URL: {}", endpoint, url);

        try {
            Map<String, Object> response = restTemplate.postForObject(endpoint, request, Map.class);

            if (response != null) {
                @SuppressWarnings("unchecked")
                List<String> keywords = (List<String>) response.get("keywords");
                String summary = (String) response.get("summary");

                return KeywordSummaryResponseDto.builder()
                        .keywords(keywords)
                        .summary(summary)
                        .build();
            } else {
                log.error("Empty response from Python API");
                throw new RuntimeException("Failed to get response from Python API");
            }
        } catch (Exception e) {
            log.error("Error calling Python API", e);
            throw new RuntimeException("Failed to call Python API", e);
        }
    }

    /**
     * 토론 주제 생성
     */
    public TopicResponseDto generateTopic(String summary, List<String> keywords) {
        String endpoint = pythonApiBaseUrl + "/topic";

        Map<String, Object> requestMap = new HashMap<>();
        requestMap.put("summary", summary);
        requestMap.put("keywords", keywords);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<Map<String, Object>> request = new HttpEntity<>(requestMap, headers);

        log.info("Calling Python API: {} for topic generation", endpoint);

        try {
            Map<String, Object> response = restTemplate.postForObject(endpoint, request, Map.class);

            if (response != null) {
                String topic = (String) response.get("topic");
                String description = (String) response.get("description");

                return TopicResponseDto.builder()
                        .topic(topic)
                        .description(description)
                        .build();
            } else {
                log.error("Empty response from Python API");
                throw new RuntimeException("Failed to get response from Python API");
            }
        } catch (Exception e) {
            log.error("Error calling Python API", e);
            throw new RuntimeException("Failed to call Python API", e);
        }
    }

    /**
     * 토론 시작
     */
    public String startDiscussion(String topic, String userPosition, String aiPosition, String difficulty) {
        String endpoint = pythonApiBaseUrl + "/discussion/start";

        Map<String, Object> requestMap = new HashMap<>();
        requestMap.put("topic", topic);
        requestMap.put("userPosition", userPosition);
        requestMap.put("aiPosition", aiPosition);
        requestMap.put("difficulty", difficulty);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<Map<String, Object>> request = new HttpEntity<>(requestMap, headers);

        log.info("Calling Python API: {} to start discussion", endpoint);

        try {
            Map<String, Object> response = restTemplate.postForObject(endpoint, request, Map.class);

            if (response != null) {
                return (String) response.get("message");
            } else {
                log.error("Empty response from Python API");
                throw new RuntimeException("Failed to get response from Python API");
            }
        } catch (Exception e) {
            log.error("Error calling Python API", e);
            throw new RuntimeException("Failed to call Python API", e);
        }
    }

    /**
     * AI 응답 생성
     */
    public String getAiResponse(String topic, String userPosition, String aiPosition,
                                String difficulty, List<Message> messages) {
        String endpoint = pythonApiBaseUrl + "/discussion/message";

        Map<String, Object> requestMap = new HashMap<>();
        requestMap.put("topic", topic);
        requestMap.put("userPosition", userPosition);
        requestMap.put("aiPosition", aiPosition);
        requestMap.put("difficulty", difficulty);
        requestMap.put("messages", messages);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<Map<String, Object>> request = new HttpEntity<>(requestMap, headers);

        log.info("Calling Python API: {} for AI response", endpoint);

        try {
            Map<String, Object> response = restTemplate.postForObject(endpoint, request, Map.class);

            if (response != null) {
                return (String) response.get("message");
            } else {
                log.error("Empty response from Python API");
                throw new RuntimeException("Failed to get response from Python API");
            }
        } catch (Exception e) {
            log.error("Error calling Python API", e);
            throw new RuntimeException("Failed to call Python API", e);
        }
    }

    /**
     * 토론 요약 생성
     */
    public String generateSummary(String topic, String userPosition, String aiPosition, List<Message> messages) {
        String endpoint = pythonApiBaseUrl + "/discussion/summary";

        Map<String, Object> requestMap = new HashMap<>();
        requestMap.put("topic", topic);
        requestMap.put("userPosition", userPosition);
        requestMap.put("aiPosition", aiPosition);
        requestMap.put("messages", messages);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<Map<String, Object>> request = new HttpEntity<>(requestMap, headers);

        log.info("Calling Python API: {} for discussion summary", endpoint);

        try {
            Map<String, Object> response = restTemplate.postForObject(endpoint, request, Map.class);

            if (response != null) {
                return (String) response.get("summary");
            } else {
                log.error("Empty response from Python API");
                throw new RuntimeException("Failed to get response from Python API");
            }
        } catch (Exception e) {
            log.error("Error calling Python API", e);
            throw new RuntimeException("Failed to call Python API", e);
        }
    }
}