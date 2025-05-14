package com.ssafy.backend.ai.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.ssafy.backend.ai.dto.response.AccidentDefinitionDto;
import com.ssafy.backend.domain.file.AnalysisStatus;
import com.ssafy.backend.domain.report.Report;
import com.ssafy.backend.domain.report.ReportRepository;
import com.ssafy.backend.domain.video.VideoFile;
import com.ssafy.backend.domain.video.VideoFileRepository;
import lombok.RequiredArgsConstructor;
import org.checkerframework.checker.units.qual.A;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

@Service
@RequiredArgsConstructor
public class AiAnalysisService {

    private final VideoFileRepository videoFileRepository;
    private final ReportRepository reportRepository;
    private final AccidentDefinitionLoader accidentDefinitionLoader;

    @Transactional
    public Long handleAiCallback(JsonNode json, Long videoId) {
        VideoFile videoFile = videoFileRepository.findById(videoId)
                .orElseThrow(() -> new IllegalArgumentException("VideoFile not found: " + videoId));

        // Idempotent 처리
        Report existingReport = reportRepository.findByVideoFileId(videoId).orElse(null);
        if (existingReport != null) {
            return existingReport.getId();
        }

        // AI JSON 기반 동적 데이터
        int accidentTypeCode = json.at("/accident_type").asInt();
        String carA = json.at("/vehicle_A_direction").asText();
        String carB = json.at("/vehicle_B_direction").asText();
        String damageLocation = json.at("/damage_location").asText();
        int faultA = json.at("/negligence_rate/A").asInt();
        int faultB = json.at("/negligence_rate/B").asInt();
        String timelineLog = formatEventTimeline(json);

        // CSV 기반 정적 데이터
        AccidentDefinitionDto definition = accidentDefinitionLoader.get(accidentTypeCode);

        // Report 생성 (정적 + 동적 결합)
        Report report = Report.builder()
                .videoFile(videoFile)
                .accidentCode(String.valueOf(accidentTypeCode))
                .title(definition.getTitle())
                .accidentType(definition.getAccidentFeature())
                .laws(definition.getLaws())
                .decisions(definition.getPrecedents())
                .carA(carA)
                .carB(carB)
                .mainEvidence(timelineLog)
                .createdAt(LocalDateTime.now())
                .analysisStatus(AnalysisStatus.COMPLETED)
                .build();

        reportRepository.save(report);

        videoFile.setAnalysisStatus(AnalysisStatus.COMPLETED);

        return report.getId();
    }

    private String formatEventTimeline(JsonNode json) {
        JsonNode logs = json.at("/event_timeline");
        if (logs.isMissingNode() || !logs.isArray()) return "AI 분석 로그 없음";

        return StreamSupport.stream(logs.spliterator(), false)
                .map(entry -> {
                    String frame = entry.at("/frame_idx").asText();
                    String event = entry.at("/event").asText();
                    return frame + "프레임 - " + event;
                })
                .collect(Collectors.joining("\\n"));
    }

}
//csv:title, accidentFeature, laws, precedents 동적만
/*
accident_type, vehicle_A_direction, vehicle_B_direction,
damage_location, negligence_rate/A, negligence_rate/B, event_timeline
그 외는 모두 ai기반으로 생성. 빈값이면 빈 문자열 "" 반환.
 */
