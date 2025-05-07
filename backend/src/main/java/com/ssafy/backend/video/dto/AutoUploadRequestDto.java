package com.ssafy.backend.video.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class AutoUploadRequestDto {

    private String FileName; // 사용자가 업로드한 원래 파일명
    private String s3Key;            // presigned로 S3에 저장된 키
    private String contentType;      // MIME 타입 (ex: video/mp4)
    private Long size;               // 파일 크기 (bytes)

}