package com.ssafy.backend.report.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@AllArgsConstructor
@Getter
@Setter
public class ReportPdfDownloadUrlResponseDto {
    private String downloadUrl;
    private int expiresIn;
}
