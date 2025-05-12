package com.ssafy.backend.ai.dto.request;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Setter
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class AiRequestDto {

    private Long userId;
    private Long videoId;
    private String presignedUrl;
    private String fileName;
}
//백엔드에서  ai서버로 분석 요청