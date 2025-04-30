package com.ssafy.backend.video.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class VideoUploadResponse {
    private Long videoId;
    private String videoUrl;
}
//@RequestParam MultipartFile file로 파일을 받기때문에 request 없음.