package com.ssafy.backend.report.service;

public interface PdfService {
    String generateAndUploadPdf(Long reportId);
}