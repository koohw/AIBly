package com.ssafy.backend.s3.service;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Configuration;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.PresignedPutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.model.PutObjectPresignRequest;

import java.net.URL;
import java.time.Duration;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class S3UploadServiceImpl implements S3UploadService {

    @Value("${aws.bucketName}")
    private String bucket;

    @Value("${aws.region}")
    private String region;

    @Value("${aws.accessKeyId}")
    private String accessKey;

    @Value("${aws.secretKey}")
    private String secretKey;

    @Value("${aws.s3.presigned-url.expiration}")
    private int expirationInSeconds; //300초로 설정함

    @Override
    public String generateS3Key(String FileName) {
        String ext = FileName.substring(FileName.lastIndexOf("."));
        return UUID.randomUUID() + ext;
    }//맨 뒤의 확장자 빼고 랜덤으로 uuid 값 만들어서 s3key값 생성. 이게 s3에 저장되는 파일명이 됨.

    @Override //AWS SDK로 PresignedUrl 생성함
    public String generatePresignedUrl(String s3Key, String contentType) {
        AwsBasicCredentials credentials = AwsBasicCredentials.create(accessKey, secretKey);

        try (S3Presigner presigner = S3Presigner.builder()
                .region(Region.of(region))
                .credentialsProvider(StaticCredentialsProvider.create(credentials))
                .serviceConfiguration(S3Configuration.builder().build())
                .build()) {

            PutObjectRequest objectRequest = PutObjectRequest.builder()
                    .bucket(bucket)
                    .key(s3Key)
                    .contentType(contentType)
                    .build(); //s3에 업로드할 때 이런 정보가 필요하다

            PresignedPutObjectRequest presignedRequest = presigner.presignPutObject(
                    builder -> builder
                            .signatureDuration(Duration.ofSeconds(expirationInSeconds))
                            .putObjectRequest(objectRequest) //300초 간 유효한 put url 생성
            );

            return presignedRequest.url().toString(); //이 URL을 프론트에 전달하면, 프론트가 S3에 직접 업로드할 수 있음
        }
    }
}