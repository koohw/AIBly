package com.ssafy.backend.domain.file;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface S3FileRepository extends JpaRepository<S3File, Long> {
    // 해시 + 유저 기준으로 중복 체크, 있는지 확인만
    boolean existsByFileHashAndUserId(String fileHash, Long userId);

    // 해시 + 유저 기준으로 파일 조회,파일 상세 정보 필요할 때 (s3Key, 파일명 등)
    Optional<S3File> findByFileHashAndUserId(String fileHash, Long userId);

    //VIDEO,PDF 등 파일 타입으로 구분해서 필터링하기 위한 메서드
    List<S3File> findByFileType(FileType fileType);
}
// 업로드 완료 후 DB에 파일 정보를 저장할 때 사용