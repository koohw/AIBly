package com.ssafy.backend.report.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@AllArgsConstructor
@Getter
@Setter
public class ReportPdfResponseDto {
    private String message;
    private String s3Key;
}
