package com.ssafy.backend.video.service;

import com.ssafy.backend.s3.S3UploadService;
import com.ssafy.backend.video.dto.response.VideoUploadResponse;
import com.ssafy.backend.video.entity.Video;
import com.ssafy.backend.video.entity.enums.VideoSource;
import com.ssafy.backend.video.entity.enums.VideoStatus;
import com.ssafy.backend.video.repository.VideoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
@RequiredArgsConstructor
public class VideoService {

    private final S3UploadService s3UploadService;
    private final VideoRepository videoRepository;

    public VideoUploadResponse autoUpload(MultipartFile file) {
        String videoUrl = s3UploadService.uploadAndGetPresignedUrl(file); //s3에 영상 업로드

        Video video = Video.builder()
                .videoUrl(videoUrl)
                .status(VideoStatus.uploading)
                .source(VideoSource.auto)
                .accident(null)
                .build();
        videoRepository.save(video); //db저장

        return new VideoUploadResponse(video.getId(), videoUrl); // 결과 리턴
    }
}

