package com.example.newscussbe.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class DiscussionRequestDto {
    private String sessionId;
    private String topic;
    private String userPosition; // "찬성" 또는 "반대"
    private String difficulty;   // "초급", "중급", "고급"
}