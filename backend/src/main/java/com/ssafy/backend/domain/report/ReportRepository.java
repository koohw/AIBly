package com.ssafy.backend.domain.report;

import com.ssafy.backend.domain.report.projection.ReportDetailProjection;
import com.ssafy.backend.domain.report.projection.ReportListProjection;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ReportRepository extends JpaRepository<Report, Long> {
    // 보고서 목록 조회 (Projection)
    @Query("SELECT r.id AS id, r.title AS title, r.accidentCode AS accidentCode, r.createdAt AS createdAt " +
            "FROM Report r WHERE r.videoFile.user.id = :userId")
    List<ReportListProjection> findAllByUserId(@Param("userId") Long userId);

    // ✅ 상세 조회 (Projection)
    @Query("SELECT r.id AS id, r.title AS title, r.accidentType AS accidentType, " +
            "r.carA AS carA, r.carB AS carB, r.mainEvidence AS mainEvidence, " +
            "r.laws AS laws, r.decisions AS decisions, " +
            "r.createdAt AS createdAt, r.pdfKey AS pdfKey, " +
            "vf.id AS fileId " +
            "FROM Report r JOIN r.videoFile vf " +
            "WHERE r.id = :reportId AND vf.user.id = :userId")
    Optional<ReportDetailProjection> findDetailByIdAndUserId(@Param("reportId") Long reportId, @Param("userId") Long userId);

    // ✅ 삭제 전에 존재 여부만 Projection (Entity 관리 X)
    @Query("SELECT r.id FROM Report r WHERE r.id = :reportId AND r.videoFile.user.id = :userId")
    Optional<Long> findIdByReportIdAndUserId(@Param("reportId") Long reportId, @Param("userId") Long userId);

    // ✅ S3 Video Key Projection
    @Query("SELECT vf.s3Key FROM Report r JOIN r.videoFile vf WHERE r.id = :reportId")
    Optional<String> findVideoS3KeyByReportId(@Param("reportId") Long reportId);

    // ✅ PDF Key Projection
    @Query("SELECT r.pdfKey FROM Report r WHERE r.id = :reportId")
    Optional<String> findPdfKeyByReportId(@Param("reportId") Long reportId);
    //fcm, polling 관련
    Optional<Report> findByVideoFileId(Long videoId);

}
//변수명 다르게 만들면 JPA가 인식하지 못해서 @Query 명시 필요