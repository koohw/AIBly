package com.ssafy.backend.ai.dto.request;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class AiRequestDto {

    private Long videoId;
    private String s3Key;
    private String fileName;
}
//백엔드에서  ai서버로 분석 요청