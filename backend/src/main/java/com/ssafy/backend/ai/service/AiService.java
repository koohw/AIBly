package com.ssafy.backend.ai.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.ssafy.backend.ai.dto.request.AiRequestDto;
import com.ssafy.backend.domain.video.LocationType;
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
    private final S3UploadService s3UploadService;
    private final AiAnalysisService aiAnalysisService;

    @Value("${ai.server.url}")
    private String aiServerUrl;

    public void requestAndHandleAnalysis(VideoFile videoFile) {
        String presignedUrl = s3UploadService.getDownloadURL(videoFile.getUser().getId(), videoFile.getS3Key());

        Integer locationType = videoFile.getLocationType();
        String locationName = LocationType.getDescriptionByCode(locationType);

        AiRequestDto requestDto = new AiRequestDto(
                videoFile.getUser().getId(),
                videoFile.getId(),
                presignedUrl,
                videoFile.getFileName(),
                videoFile.getLocationType(),
                videoFile.getLocationName()
        );

        try {
            JsonNode aiResponse = restTemplate.postForObject(aiServerUrl + "/analyze", requestDto, JsonNode.class);
            log.info("AI 서버 응답 수신 완료: videoId={}", videoFile.getId());

            aiAnalysisService.handleAiCallback(aiResponse, videoFile.getId());

        } catch (Exception e) {
            log.error("AI 분석 요청 또는 처리 실패: videoId={}, error={}", videoFile.getId(), e.getMessage(), e);
            throw new RuntimeException("AI 서버 호출 또는 처리 실패", e);
        }
    }
}


//백엔드가 AI 서버에 분석 요청을 전송
//순환참조 없이 리팩토링하기->AI 호출 + AiAnalysisService 호출만
