package com.ssafy.backend.s3;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;

import java.io.IOException;
import java.time.Duration;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class S3UploadService {

    private final S3Config s3Config;

    @Value("${cloud.aws.s3.bucket}")
    private String bucket;

    public String uploadAndGetPresignedUrl(MultipartFile file) {
        try {
            String originalFilename = file.getOriginalFilename();
            String uniqueFileName = UUID.randomUUID() + "_" + originalFilename;

            // S3에 파일 업로드
            PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                    .bucket(bucket)
                    .key(uniqueFileName)
                    .contentType(file.getContentType())
                    .build();

            s3Config.getS3Client().putObject(putObjectRequest,
                    RequestBody.fromInputStream(file.getInputStream(), file.getSize()));

            // Presigned URL 생성
            GetObjectRequest getObjectRequest = GetObjectRequest.builder()
                    .bucket(bucket)
                    .key(uniqueFileName)
                    .build();

            GetObjectPresignRequest presignRequest = GetObjectPresignRequest.builder()
                    .signatureDuration(Duration.ofMinutes(10)) //URL 유효 시간 설정, 10분동안 접근 가능, 사용자가 영상 업로드 → S3에 저장되고 10분짜리 presigned URL 생성하고 만료 후엔 다시 백에서 발급해줌
                    .getObjectRequest(getObjectRequest)
                    .build();

            return s3Config.getS3Presigner()
                    .presignGetObject(presignRequest)
                    .url()
                    .toString();

        } catch (IOException e) {
            throw new RuntimeException("S3 파일 업로드 또는 Presigned URL 생성 실패", e);
        }
    }
}


