package com.ssafy.backend.s3.dto.request;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class PresignedUrlRequestDto {
    private String fileName; // 사용자가 업로드한 원본 파일명
    private String contentType;      // MIME 타입 (video/mp4)
    private Integer locationType;  // 장소 id(0,1,2,3)

}

//클라이언트가 presigned URL 생성을 요청할 때 보내는 데이터