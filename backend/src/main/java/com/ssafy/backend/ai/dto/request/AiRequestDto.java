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
    private String downloadUrl; //AI 서버는 presigned URL을 통해 S3에서 영상을 가져가서 분석
    private String fileName;
}
//백엔드에서  ai서버로 분석 요청