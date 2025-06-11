package com.example.newscussbe.controller;

import com.example.newscussbe.dto.DiscussionRequestDto;
import com.example.newscussbe.dto.DiscussionResponseDto;
import com.example.newscussbe.dto.FeedbackResponseDto;
import com.example.newscussbe.dto.KeywordSummaryResponseDto;
import com.example.newscussbe.dto.MessageRequestDto;
import com.example.newscussbe.dto.MessageResponseDto;
import com.example.newscussbe.dto.SummaryResponseDto;
import com.example.newscussbe.dto.TopicRequestDto;
import com.example.newscussbe.dto.TopicResponseDto;
import com.example.newscussbe.dto.UrlRequestDto;
import com.example.newscussbe.service.NewscussService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@Slf4j
@RestController
@RequestMapping("/api")
@CrossOrigin(origins = "*") // 모든 출처에서의 요청 허용 (개발 환경용)
@RequiredArgsConstructor
public class NewscussController {

    private final NewscussService newscussService;

    /**
     * URL을 받아 기사 키워드와 요약 정보를 반환
     */
    @PostMapping("/url")
    public ResponseEntity<KeywordSummaryResponseDto> processUrl(@RequestBody UrlRequestDto requestDto) {
        KeywordSummaryResponseDto responseDto = newscussService.processUrl(requestDto.getUrl());
        return ResponseEntity.ok(responseDto);
    }

    /**
     * 토론 주제 생성 요청
     */
    @PostMapping("/topic")
    public ResponseEntity<TopicResponseDto> generateTopic(@RequestBody TopicRequestDto requestDto) {
        TopicResponseDto responseDto = newscussService.generateTopic(
                requestDto.getSessionId(),
                requestDto.getSummary(),
                requestDto.getKeywords()
        );
        return ResponseEntity.ok(responseDto);
    }

    /**
     * 토론 시작 요청
     */
    @PostMapping("/discussion/start")
    public ResponseEntity<DiscussionResponseDto> startDiscussion(@RequestBody DiscussionRequestDto requestDto) {
        DiscussionResponseDto responseDto = newscussService.startDiscussion(
                requestDto.getSessionId(),
                requestDto.getTopic(),
                requestDto.getUserPosition(),
                requestDto.getDifficulty()
        );
        return ResponseEntity.ok(responseDto);
    }

    /**
     * 토론 메시지 전송 (기존 방식 유지)
     */
    @PostMapping("/discussion/message")
    public ResponseEntity<MessageResponseDto> sendMessage(@RequestBody MessageRequestDto requestDto) {
        MessageResponseDto responseDto = newscussService.processMessage(
                requestDto.getSessionId(),
                requestDto.getMessage()
        );
        return ResponseEntity.ok(responseDto);
    }

    /**
     * 토론 메시지 전송 (스트리밍 방식) - 새로 추가
     */
    @PostMapping(value = "/discussion/message/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter streamMessage(@RequestBody MessageRequestDto requestDto) {
        log.info("Starting SSE stream for session: {}", requestDto.getSessionId());

        SseEmitter emitter = new SseEmitter(120000L); // 120초 타임아웃

        // CORS 설정
        emitter.onCompletion(() -> log.info("SSE completed for session: {}", requestDto.getSessionId()));
        emitter.onTimeout(() -> log.warn("SSE timeout for session: {}", requestDto.getSessionId()));
        emitter.onError((ex) -> log.error("SSE error for session: {}", requestDto.getSessionId(), ex));

        try {
            // 백그라운드에서 스트리밍 처리
            newscussService.processMessageStream(requestDto.getSessionId(), requestDto.getMessage(), emitter);
        } catch (Exception e) {
            log.error("Error starting message stream", e);
            emitter.completeWithError(e);
        }

        return emitter;
    }

    /**
     * 토론 요약 요청
     */
    @GetMapping("/discussion/summary/{sessionId}")
    public ResponseEntity<SummaryResponseDto> getSummary(@PathVariable String sessionId) {
        SummaryResponseDto responseDto = newscussService.generateSummary(sessionId);
        return ResponseEntity.ok(responseDto);
    }

    /**
     * 토론 피드백 요청 (새로 추가)
     */
    @GetMapping("/discussion/feedback/{sessionId}")
    public ResponseEntity<FeedbackResponseDto> getFeedback(@PathVariable String sessionId) {
        FeedbackResponseDto responseDto = newscussService.generateFeedback(sessionId);
        return ResponseEntity.ok(responseDto);
    }

    /**
     * 세션 상태 확인 (디버깅용)
     */
    @GetMapping("/session/{sessionId}")
    public ResponseEntity<String> checkSession(@PathVariable String sessionId) {
        return ResponseEntity.ok(newscussService.getSessionStatus(sessionId));
    }
}