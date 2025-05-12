package com.ssafy.backend.ai.service;

import com.ssafy.backend.ai.dto.request.AiRequestDto;
import com.ssafy.backend.domain.video.VideoFile;
import com.ssafy.backend.s3.service.S3UploadService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@Service
@RequiredArgsConstructor
@Slf4j
public class AiService {

    private final RestTemplate restTemplate;
    private final S3UploadService s3UploadService; // presigned URL 생성용

    @Value("${ai.server.url}")
    private String aiServerUrl;

    public void requestAnalysis(VideoFile videoFile) {
        // presigned URL 생성
        String presignedUrl = s3UploadService.generatePresignedUrl(videoFile.getS3Key(), videoFile.getContentType());

        AiRequestDto requestDto = new AiRequestDto(
                videoFile.getUser().getId(),
                videoFile.getId(),
                presignedUrl,
                videoFile.getFileName()
        ); //아이디, URL, 파일명 담아서 요청

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON); //ai서버에 json 형태로 보내기 위해 헤더 설정

        //엔티티를 만듦. 이게 RestTemplate의 실제 요청 단위
        HttpEntity<AiRequestDto> requestEntity = new HttpEntity<>(requestDto, headers);

        //AI 서버에 POST 요청 전송,분석이 끝나면 나중에 콜백 URL(/ai-callback)로 결과를 보내기때문에 응답 바디가 필요없음.
        try {
            ResponseEntity<Void> response = restTemplate.postForEntity(aiServerUrl, requestEntity, Void.class);
            log.info("AI 분석 요청 성공. videoId = {}", videoFile.getId());
        } catch (Exception e) {
            log.error("AI 분석 요청 실패. videoId = {}, error = {}", videoFile.getId(), e.getMessage());
        }
    }
}
//백엔드가 AI 서버에 분석 요청을 전송
