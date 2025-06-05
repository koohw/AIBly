package com.ssafy.backend.domain.file;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface S3FileRepository extends JpaRepository<S3File, Long> {
    boolean existsByFileNameAndContentTypeAndUserId(String fileName, String contentType, Long userId);

    Optional<S3File> findByFileNameAndContentTypeAndUserId(String fileName, String contentType, Long userId);

    //VIDEO,PDF 등 파일 타입으로 구분해서 필터링하기 위한 메서드
    List<S3File> findByFileType(FileType fileType);

    Optional<S3File> findByS3KeyAndUserId(String s3Key, Long userId);

    // S3 Key로 S3File 레코드 삭제
    void deleteByS3Key(String s3Key);

}
// 업로드 완료 후 DB에 파일 정보를 저장할 때 사용