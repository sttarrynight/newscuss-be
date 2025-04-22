package com.example.newscussbe.dto;

import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Message {
    private String role; // "user" 또는 "ai"
    private String content;
    private LocalDateTime timestamp;
}