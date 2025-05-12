package com.ssafy.backend.report.controller;

import com.ssafy.backend.common.controller.BaseController;
import com.ssafy.backend.config.JwtTokenProvider;
import com.ssafy.backend.report.dto.response.ReportDetailResponseDto;
import com.ssafy.backend.report.dto.response.ReportListResponseDto;
import com.ssafy.backend.report.service.ReportService;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/my/reports")
@RequiredArgsConstructor
public class MyReportController extends BaseController {

    private final ReportService reportService;

    //내 보고서 목록 조회
    @GetMapping
    public ResponseEntity<List<ReportListResponseDto>> getMyReports(HttpServletRequest request) {
        Long userId = getCurrentUserId(request);
        List<ReportListResponseDto> reports = reportService.getMyReports(userId);
        return ResponseEntity.ok(reports);
    }

    //상세 보고서
    @GetMapping("/{reportId}")
    public ResponseEntity<ReportDetailResponseDto> getMyReportDetail(@PathVariable Long reportId, HttpServletRequest request) {
        Long userId = getCurrentUserId(request);
        ReportDetailResponseDto report = reportService.getMyReportDetail(userId, reportId);
        return ResponseEntity.ok(report);
    }


    //내 보고서 삭제 (PDF + 영상 +Report DB 삭제까지 서비스 내부에서 수행)
    @DeleteMapping("/{reportId}")
    public ResponseEntity<Void> deleteMyReport(@PathVariable Long reportId, HttpServletRequest request) {
        Long userId = getCurrentUserId(request);
        reportService.deleteMyReport(userId, reportId);
        return ResponseEntity.noContent().build();
    }

     //JWT 기반 User ID 추출 (CustomUserDetails 사용 예시) 지금은 principal이 String으로 저장되고 있어서 못씀
 /**
    private Long getCurrentUserId() {
        return ((CustomUserDetails) SecurityContextHolder.getContext().getAuthentication().getPrincipal()).getUserId();
    }

 */
}
