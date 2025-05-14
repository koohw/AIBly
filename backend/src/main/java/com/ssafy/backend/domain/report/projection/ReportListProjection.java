package com.ssafy.backend.domain.report.projection;

import java.time.LocalDateTime;

public interface ReportListProjection {
    Long getId();
    String getTitle();
    String getAccidentCode();
    LocalDateTime getCreatedAt();
}
