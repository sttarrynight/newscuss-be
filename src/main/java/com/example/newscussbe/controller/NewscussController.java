package com.example.newscussbe.controller;

import com.example.newscussbe.dto.DiscussionRequestDto;
import com.example.newscussbe.dto.DiscussionResponseDto;
import com.example.newscussbe.dto.KeywordSummaryResponseDto;
import com.example.newscussbe.dto.MessageRequestDto;
import com.example.newscussbe.dto.MessageResponseDto;
import com.example.newscussbe.dto.SummaryResponseDto;
import com.example.newscussbe.dto.TopicRequestDto;
import com.example.newscussbe.dto.TopicResponseDto;
import com.example.newscussbe.dto.UrlRequestDto;
import com.example.newscussbe.service.NewscussService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

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
     * 토론 메시지 전송
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
     * 토론 요약 요청
     */
    @GetMapping("/discussion/summary/{sessionId}")
    public ResponseEntity<SummaryResponseDto> getSummary(@PathVariable String sessionId) {
        SummaryResponseDto responseDto = newscussService.generateSummary(sessionId);
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