package com.ssafy.backend.s3.repository;

import com.ssafy.backend.domain.file.FileType;
import com.ssafy.backend.s3.model.entity.S3File;

import java.util.List;
import java.util.Optional;

public interface S3FileRepository {
    // 파일 이름으로 중복 업로드 방지 or 조회
    Optional<S3File> findByFileName(String fileName);

    //IMAGE, VIDEO, THUMBNAIL, PDF 등 파일 종류별로 구분해서 필터링하기 위한 메서드인데 필요할지 모르겠음.
    //일단 지금은 필요없음.
    List<S3File> findByFileType(FileType fileType);
}
// 업로드 완료 후 DB에 파일 정보를 저장할 때 사용