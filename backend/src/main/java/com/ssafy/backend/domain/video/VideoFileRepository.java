package com.ssafy.backend.domain.video;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface VideoFileRepository extends JpaRepository<VideoFile, Long> {

    Optional<VideoFile> findByUserIdAndS3Key(Long userId, String s3Key);//파일 다운로드/삭제는 userId + s3Key 묶어서 확인
    Optional<VideoFile> findByUserIdAndS3KeyAndFileName(Long userId, String s3Key, String fileName);
    // 전체 영상 목록 조회 (userId 기준)
    List<VideoFile> findByUserId(Long userId);

    // 특정 영상 상세 조회 (videoId + userId)
    Optional<VideoFile> findByIdAndUserId(Long videoId, Long userId);

    //해당 S3 Key를 참조하고 있는 video_files 레코드 개수를 바로 조회
    long countByS3Key(String s3Key);


}
