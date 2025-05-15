package com.ssafy.backend.domain.report;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ReportRepository extends JpaRepository<Report, Long> {
    // 목록 조회 (Entity 리턴, 필요한 경우 where 조건만 유지)
    @Query("SELECT r FROM Report r WHERE r.videoFile.user.id = :userId")
    List<Report> findAllByUserId(@Param("userId") Long userId);

    // 상세 조회 (Entity 직접 리턴)
    @Query("SELECT r FROM Report r JOIN FETCH r.videoFile vf WHERE r.id = :reportId AND vf.user.id = :userId")
    Optional<Report> findDetailByIdAndUserId(@Param("reportId") Long reportId, @Param("userId") Long userId);

    // 삭제 여부 확인 (Entity 직접 리턴 가능)
    Optional<Report> findByIdAndVideoFileUserId(@Param("reportId") Long reportId, @Param("userId") Long userId);

    // Video S3 Key (간단한 Projection은 유지 가능)
    @Query("SELECT vf.s3Key FROM Report r JOIN r.videoFile vf WHERE r.id = :reportId")
    Optional<String> findVideoS3KeyByReportId(@Param("reportId") Long reportId);

    // PDF Key (간단 Projection 유지)
    @Query("SELECT r.pdfKey FROM Report r WHERE r.id = :reportId")
    Optional<String> findPdfKeyByReportId(@Param("reportId") Long reportId);

    // fcm, polling 관련 (Entity 그대로 유지)
    Optional<Report> findByVideoFileId(Long videoId);
}

//변수명 다르게 만들면 JPA가 인식하지 못해서 @Query 명시 필요