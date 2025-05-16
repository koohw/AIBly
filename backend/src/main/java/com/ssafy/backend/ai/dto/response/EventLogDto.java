package com.ssafy.backend.ai.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class EventLogDto {
    private String event;
    private int frameIdx;
}
// JSON 배열 그대로 프론트에서 받도록