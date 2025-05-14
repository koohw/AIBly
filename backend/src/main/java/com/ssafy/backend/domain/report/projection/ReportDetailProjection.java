package com.ssafy.backend.domain.report.projection;

import java.time.LocalDateTime;

public interface ReportDetailProjection {
    Long getId();
    String getTitle();
    String getAccidentType();
    String getCarA();
    String getCarB();
    String getMainEvidence();
    String getLaws();
    String getDecisions();
    LocalDateTime getCreatedAt();
    String getPdfKey();
    Long getFileId();
}