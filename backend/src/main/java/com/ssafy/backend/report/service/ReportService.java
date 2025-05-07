package com.ssafy.backend.report.service;

import com.ssafy.backend.domain.file.VideoFile;
import com.ssafy.backend.domain.file.VideoFileRepository;
import com.ssafy.backend.domain.report.Report;
import com.ssafy.backend.domain.report.ReportRepository;
import com.ssafy.backend.report.dto.AIAnalysisResultRequestDto;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import com.ssafy.backend.report.service.AccidentInfoResolver.AccidentInfo;

import java.time.LocalDateTime;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ReportService {

    private final ReportRepository reportRepository;
    private final VideoFileRepository videoFileRepository;
    private final AccidentInfoResolver accidentInfoResolver;

    public void generateReport(AIAnalysisResultRequestDto request, Long videoFileId) {

        // 1. 영상 파일 조회
        VideoFile videoFile = videoFileRepository.findById(videoFileId)
                .orElseThrow(() -> new IllegalArgumentException("영상 파일을 찾을 수 없습니다: " + videoFileId));

        // 2. AI 분석 결과에서 사고 유형 번호 가져오기
        int accidentType = request.getPrediction().getAccident_type();

        // 3. 사고 유형에 해당하는 판례/법규 정보 가져오기
        AccidentInfo info = accidentInfoResolver.resolve(accidentType);

        // 4. Report 객체 생성 및 매핑
        Report report = Report.builder()
                .videoFile(videoFile)
                .title(videoFile.getFileName())
                .accidentCode(String.valueOf(accidentType))
                .accidentType(info.getAccidentDescription())
                .carA(info.getCarAFaultRatio())
                .carB(info.getCarBFaultRatio())
                .mainEvidence(formatTimelineLog(request)) // 로그 요약
                .laws(info.getLaws())
                .decisions(info.getPrecedents())
                .createdAt(LocalDateTime.now())
                .build();

        // 5. 저장
        reportRepository.save(report);
    }

    // 로그 요약 텍스트 생성 (시간 + 이벤트)
    private String formatTimelineLog(AIAnalysisResultRequestDto request) {
        if (request.getTimeline_log() == null || request.getTimeline_log().isEmpty()) {
            return "AI 분석 로그 없음";
        }

        return request.getTimeline_log().stream()
                .map(log -> log.getTime() + " - " + String.join(", ", log.getEvents()))
                .collect(Collectors.joining("\n"));
    }
}