package com.ssafy.backend.domain.file;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface VideoFileRepository extends JpaRepository<VideoFile, Long> {

    Optional<VideoFile> findByS3Key(String s3Key); //S3에 업로드된 파일 중에서 s3Key가 일치하는 파일을 DB에서 조회

    Optional<VideoFile> findByUserIdAndFileName(Long userId, String fileName);
}
