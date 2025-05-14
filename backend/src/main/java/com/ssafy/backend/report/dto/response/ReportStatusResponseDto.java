package com.ssafy.backend.report.dto.response;

import com.ssafy.backend.domain.file.AnalysisStatus;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class ReportStatusResponseDto {
    private AnalysisStatus status;  // AnalysisStatus 직접 반환 (ANALYZING, COMPLETED, FAILED)
    private Long reportId;          // 완료 시 reportId 반환, 아니면 null
}