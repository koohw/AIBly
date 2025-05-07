package com.ssafy.backend.s3.service;

import com.ssafy.backend.s3.model.dto.PresignedUrlRequestDto;
import com.ssafy.backend.s3.model.dto.PresignedUrlResponseDto;

public interface S3UploadService {

    //파일 이름에서 UUID로 s3Key 생성
    String generateS3Key(String FileName);

    // presigned URL 생성, PresignedUrlRequestDto에 있던 것들 가져옴
    String generatePresignedUrl(String s3Key, String contentType);
}

//Presigned URL API는 아직 DB에 저장할 게 없어서 Repository를 안 씀
//왜냐면 presined url 생성 시점에는 아직 파일이 s3에 존재하지 않아서 s3file에 db 저장안함.