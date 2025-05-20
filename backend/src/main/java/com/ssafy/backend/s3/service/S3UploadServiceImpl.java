package com.ssafy.backend.s3.service;

import com.ssafy.backend.domain.file.FileType;
import com.ssafy.backend.domain.file.S3File;
import com.ssafy.backend.domain.file.S3FileRepository;
import com.ssafy.backend.domain.video.VideoFile;
import com.ssafy.backend.domain.video.VideoFileRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import software.amazon.awssdk.core.sync.RequestBody;
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
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class S3UploadServiceImpl implements S3UploadService {

    private final S3Client s3Client;
    private final VideoFileRepository videoFileRepository;
    private final S3Presigner s3Presigner;
    private final S3FileRepository s3FileRepository;

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

    // userId, FileName 으로 videoFile 조회 -> userId, s3Key로 수정
    private VideoFile getVideoFile(Long userId, String s3Key) {
        return videoFileRepository.findByUserIdAndS3Key(userId, s3Key)
                .orElseThrow(() -> new RuntimeException("File not found"));
    }

    @Override
    public void uploadPdf(byte[] pdfBytes, String s3Key, String contentType) {
        s3Client.putObject(PutObjectRequest.builder()
                        .bucket(bucket)
                        .key(s3Key)
                        .contentType(contentType)
                        .build(),
                RequestBody.fromBytes(pdfBytes));
    }

    //s3file db에 저장
    @Override
    public void saveS3File(S3File s3File) {

        s3FileRepository.save(s3File);
    }

    @Override
    public boolean isDuplicateFile(String fileName, String contentType, Long userId) {
        return s3FileRepository.existsByFileNameAndContentTypeAndUserId(fileName, contentType, userId);
    }

    //사용자 PDF 다운로드용
    @Override
    public String generateDownloadPresignedUrl(String s3Key) {
        GetObjectRequest getObjectRequest = GetObjectRequest.builder()
                .bucket(bucket)
                .key(s3Key)
                .build();

        GetObjectPresignRequest presignRequest = GetObjectPresignRequest.builder()

                .signatureDuration(Duration.ofSeconds(expirationInSeconds))
                //.signatureDuration(Duration.ofMinutes(5))
                .getObjectRequest(getObjectRequest)
                .build();

        return s3Presigner.presignGetObject(presignRequest).url().toString();
    }

    @Override
    public Optional<S3File> getS3FileByS3KeyAndUserId(String s3Key, Long userId) {
        return s3FileRepository.findByS3KeyAndUserId(s3Key, userId);
    }


    @Override
    public String getOrCreateS3Key(String fileName, String contentType, Long userId, Integer locationType) {
        Optional<S3File> existing = s3FileRepository.findByFileNameAndContentTypeAndUserId(fileName, contentType, userId);

        if (existing.isPresent()) {
            System.out.println("기존 파일 재사용 - fileName: " + fileName + ", s3Key: " + existing.get().getS3Key());
            return existing.get().getS3Key();
        }

        // 새 S3 Key 생성
        String s3Key = generateS3Key(fileName);

        S3File file = S3File.builder()
                .s3Key(s3Key)
                .fileName(fileName)
                .contentType(contentType)
                .userId(userId)
                .fileType(FileType.VIDEO)
                .locationType(locationType)
                .build();

        System.out.println("새 파일 저장 - fileName: " + fileName + ", s3Key: " + s3Key);
        s3FileRepository.save(file);
        return s3Key;
    }

    @Override
    public void deleteFromS3(String s3Key) {
        DeleteObjectRequest deleteRequest = DeleteObjectRequest.builder()
                .bucket(bucket)
                .key(s3Key)
                .build();

        s3Client.deleteObject(deleteRequest);
    }

    @Override
    public void deleteS3FileEntity(String s3Key) {
        s3FileRepository.deleteByS3Key(s3Key);
    }


}