package com.ssafy.backend.s3.service;

import com.ssafy.backend.domain.file.S3File;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.util.Optional;

public interface S3UploadService {

    //파일 이름에서 UUID로 s3Key 생성
    String generateS3Key(String fileName);

    // presigned URL 생성, PresignedUrlRequestDto에 있던 것들 가져옴
    String generatePresignedUrl(String s3Key, String contentType);

    // presinged URL 생성, 다운로드 용도
    String getDownloadURL(Long userId, String s3Key);

    //유저가 pdf파일 다운받을 때
    String generateDownloadPresignedUrl(String s3Key);

    //S3에 올라간 영상 파일 삭제(S3 presigned URL로 업로드한 진짜 파일을 제거)
    void deleteFromS3(String s3Key);

    //s3_files 레코드 삭제
    void deleteS3FileEntity(String s3Key);

    // 한글 PDF 업로드 기능
    void uploadPdf(byte[] pdfBytes, String s3Key, String contentType);

    //s3 file 저장
    void saveS3File(S3File s3File);

    //유저가 같은 파일을 올렸는지 파악함
    boolean isDuplicateFile(String fileName, String contentType, Long userId);

    String getOrCreateS3Key(String fileName, String contentType, Long userId, Integer locationType);

    //userId + s3Key로 S3File을 찾는 메서드
    Optional<S3File> getS3FileByS3KeyAndUserId(String s3Key, Long userId);

}

//Presigned URL API는 아직 DB에 저장할 게 없어서 Repository를 안 씀
//왜냐면 presined url 생성 시점에는 아직 파일이 s3에 존재하지 않아서 s3file에 db 저장안함.