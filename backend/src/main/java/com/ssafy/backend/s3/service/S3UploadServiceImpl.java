package com.ssafy.backend.s3.service;

import com.ssafy.backend.domain.video.VideoFile;
import com.ssafy.backend.domain.video.VideoFileRepository;
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
    private final VideoFileRepository videoFileRepository;
    private final S3Presigner s3Presigner;

    @Value("${aws.bucketName}")
    private String bucket;

    @Value("${aws.s3.presigned-url.expiration}")
    private int expirationInSeconds; //300초로 설정함

    @Override
    public String generateS3Key(String fileName) {
        String ext = fileName.substring(fileName.lastIndexOf("."));
        return UUID.randomUUID() + ext;
    }//맨 뒤의 확장자 빼고 랜덤으로 uuid 값 만들어서 s3key값 생성.
    // 기존 파일명을 바탕으로 S3에 저장될 고유한 새로운 s3Key를 생성하는 메서드

    @Override
    public String generatePresignedUrl(String s3Key, String contentType) {
        PutObjectRequest objectRequest = PutObjectRequest.builder()
                .bucket(bucket)
                .key(s3Key)
                .contentType(contentType)
                .build();
        //PUT presigned URL을 생성하여 프론트가 직접 s3에 업로드할 때 이런 정보가 필요하다

        PutObjectPresignRequest presignRequest = PutObjectPresignRequest.builder()
                .signatureDuration(Duration.ofSeconds(expirationInSeconds))
                .putObjectRequest(objectRequest)
                .build();

        PresignedPutObjectRequest presignedRequest = s3Presigner.presignPutObject(presignRequest);
        return presignedRequest.url().toString();
        //이 URL을 프론트에 전달하면, 프론트가 S3에 직접 업로드할 수 있음
    }

    //기존 getDownloadURL 변경 -> 다운로드 presigned URL 발급 코드 구현할 때 위해 역할 분리함.
    //사용자 권한 검증은 getVideoFile, URL 생성은 별도 메서드로 분리
    @Override
    public String getDownloadURL(Long userId, String s3Key) {
        VideoFile videoFile = getVideoFile(userId, s3Key); // 검증 수행
        return createPresignedDownloadUrl(videoFile.getS3Key());
    }

    private String createPresignedDownloadUrl(String s3Key) {
        // 역할 분리: 다운로드 URL 생성만 수행하는 전용 메서드
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
    public void deleteS3File(Long userId, String s3Key) {
        // 사용자 검증 후 파일 삭제
        VideoFile videoFile = getVideoFile(userId, s3Key);

        // S3에서 삭제
        DeleteObjectRequest deleteRequest = DeleteObjectRequest.builder()
                .bucket(bucket)
                .key(videoFile.getS3Key())
                .build();
        s3Client.deleteObject(deleteRequest);

        // DB에서 삭제
        videoFileRepository.delete(videoFile);
    }

    // helper methods
    // userId, FileName 으로 videoFile 조회 -> userId, s3Key로 수정
    private VideoFile getVideoFile(Long userId, String s3Key) {
        return videoFileRepository.findByUserIdAndS3Key(userId, s3Key)
                .orElseThrow(() -> new RuntimeException("File not found"));
    }
}