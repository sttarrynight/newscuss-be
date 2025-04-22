package com.example.newscussbe.dto;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class KeywordSummaryResponseDto {
    private String sessionId;
    private List<String> keywords;
    private String summary;
}