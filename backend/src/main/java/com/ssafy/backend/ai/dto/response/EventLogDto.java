package com.ssafy.backend.ai.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class EventLogDto {
    private String event;
    private String timeInSeconds; // "0.68초"처럼 표현
}
// JSON 배열 그대로 프론트에서 받도록