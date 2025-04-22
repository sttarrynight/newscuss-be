package com.example.newscussbe.dto;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TopicRequestDto {
    private String sessionId;
    private String summary;
    private List<String> keywords;
}