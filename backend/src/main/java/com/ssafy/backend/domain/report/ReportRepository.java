package com.ssafy.backend.domain.report;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ReportRepository extends JpaRepository<Report, Long> {
    List<Report> findByVideoFileUserId(Long userId); //보고서 목록 조회

    Optional<Report> findByIdAndVideoFileUserId(Long reportId, Long userId); //보고서 상세 조회

    void deleteByIdAndVideoFileUserId(Long reportId, Long userId); //보고서 삭제

}
//변수명 다르게 만들면 JPA가 인식하지 못해서 @Query 명시 필요