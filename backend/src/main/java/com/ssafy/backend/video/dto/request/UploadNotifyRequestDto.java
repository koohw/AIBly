package com.ssafy.backend.video.dto.request;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UploadNotifyRequestDto {

    private String fileName; // 사용자가 업로드한 원래 파일명
    private String s3Key;            // presigned로 S3에 저장된 키
    private String contentType;      // MIME 타입 (ex: video/mp4)
    private Long size;               // 파일 크기 (bytes)

}
//프론트에서 백엔드로 업로드 완료 알림에 사용