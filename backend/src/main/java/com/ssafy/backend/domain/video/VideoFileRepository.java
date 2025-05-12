package com.ssafy.backend.domain.video;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface VideoFileRepository extends JpaRepository<VideoFile, Long> {

    Optional<VideoFile> findByUserIdAndS3Key(Long userId, String s3Key);//파일 다운로드/삭제는 userId + s3Key 묶어서 확인
}
