package com.example.newscussbe.service;

import com.example.newscussbe.dto.DiscussionResponseDto;
import com.example.newscussbe.dto.FeedbackResponseDto;
import com.example.newscussbe.dto.KeywordSummaryResponseDto;
import com.example.newscussbe.dto.MessageResponseDto;
import com.example.newscussbe.dto.SummaryResponseDto;
import com.example.newscussbe.dto.TopicResponseDto;
import java.util.List;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

public interface NewscussService {

    /**
     * URL을 받아 기사 키워드와 요약 정보를 반환
     */
    KeywordSummaryResponseDto processUrl(String url);

    /**
     * 토론 주제 생성
     */
    TopicResponseDto generateTopic(String sessionId, String summary, List<String> keywords);

    /**
     * 토론 시작
     */
    DiscussionResponseDto startDiscussion(String sessionId, String topic, String userPosition, String difficulty);

    /**
     * 사용자 메시지 처리 및 AI 응답 생성 (기존 방식)
     */
    MessageResponseDto processMessage(String sessionId, String message);

    /**
     * 사용자 메시지 처리 및 AI 응답 생성 (스트리밍 방식) - 새로 추가
     */
    void processMessageStream(String sessionId, String message, SseEmitter emitter);

    /**
     * 토론 요약 생성
     */
    SummaryResponseDto generateSummary(String sessionId);

    /**
     * 토론 피드백 생성 (새로 추가)
     */
    FeedbackResponseDto generateFeedback(String sessionId);

    /**
     * 세션 상태 확인 (디버깅용)
     */
    String getSessionStatus(String sessionId);
}