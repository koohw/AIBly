package com.ssafy.backend.video.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
@AllArgsConstructor
public class MyVideoResponseDto {

    private Long videoId;
    private String fileName;
    private String videoUrl;   // Presigned URL
    private LocalDateTime uploadedAt;
    private Long size;
}
