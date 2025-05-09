package com.ssafy.backend.s3.service;

import com.ssafy.backend.domain.file.VideoFile;
import com.ssafy.backend.domain.file.VideoFileRepository;
import com.ssafy.backend.s3.model.dto.PresignedUrlRequestDto;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;
import software.amazon.awssdk.services.s3.presigner.model.PresignedPutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.model.PutObjectPresignRequest;

import java.net.URL;
import java.time.Duration;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class S3UploadServiceImpl implements S3UploadService {

    private final S3Client s3Client;
    @Value("${aws.bucketName}")
    private String bucket;

    @Value("${aws.s3.presigned-url.expiration}")
    private int expirationInSeconds; //300초로 설정함

    private final VideoFileRepository videoFileRepository;
    private final S3Presigner s3Presigner;

    @Override
    public String generateS3Key(String FileName) {
        String ext = FileName.substring(FileName.lastIndexOf("."));
        return UUID.randomUUID() + ext;
    }//맨 뒤의 확장자 빼고 랜덤으로 uuid 값 만들어서 s3key값 생성. 이게 s3에 저장되는 파일명이 됨.

    @Override //AWS SDK로 PresignedUrl 생성함
    public String generatePresignedUrl(String s3Key, String contentType) {
        PutObjectRequest objectRequest = PutObjectRequest.builder()
                .bucket(bucket)
                .key(s3Key)
                .contentType(contentType)
                .build(); //s3에 업로드할 때 이런 정보가 필요하다

        PutObjectPresignRequest presignRequest = PutObjectPresignRequest.builder()
                .signatureDuration(Duration.ofSeconds(expirationInSeconds))
                .putObjectRequest(objectRequest)
                .build();

        PresignedPutObjectRequest presignedRequest = s3Presigner.presignPutObject(presignRequest);
        return presignedRequest.url().toString(); //이 URL을 프론트에 전달하면, 프론트가 S3에 직접 업로드할 수 있음
    }


    // 다운로드용 URL
    @Override
    public String getDownloadURL(Long userId, String fileName) {
        String s3Key = getVideoFile(userId, fileName).getS3Key();
        GetObjectRequest getObjectRequest = GetObjectRequest.builder()
                .bucket(bucket)
                .key(s3Key)
                .build();

        GetObjectPresignRequest presignRequest = GetObjectPresignRequest.builder()
                .signatureDuration(Duration.ofSeconds(expirationInSeconds))
                .getObjectRequest(getObjectRequest)
                .build();
        URL presignedUrl = s3Presigner.presignGetObject(presignRequest).url();
        return presignedUrl.toString();
    }


    // S3에 업로드된 파일 삭제
    @Override
    public void deleteS3File(Long userId, String fileName) {
        VideoFile videoFile = getVideoFile(userId, fileName);

        // S3에서 삭제
        DeleteObjectRequest deleteObjectRequest = DeleteObjectRequest.builder()
                .bucket(bucket)
                .key(videoFile.getS3Key())
                .build();
        s3Client.deleteObject(deleteObjectRequest);

        // DB에서 삭제
        videoFileRepository.delete(videoFile);
    }

    // helper methods

    // userId, FileName 으로 videoFile 조회
    private VideoFile getVideoFile(Long userId, String fileName) {
        return videoFileRepository.findByUserIdAndFileName(userId, fileName)
                .orElseThrow(() -> new RuntimeException("file not found"));
    }
}