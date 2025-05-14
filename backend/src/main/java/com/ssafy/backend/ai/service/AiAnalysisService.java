package com.ssafy.backend.ai.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.ssafy.backend.ai.dto.response.AccidentDefinitionDto;
import com.ssafy.backend.domain.file.AnalysisStatus;
import com.ssafy.backend.domain.report.Report;
import com.ssafy.backend.domain.report.ReportRepository;
import com.ssafy.backend.domain.video.VideoFile;
import com.ssafy.backend.domain.video.VideoFileRepository;
import lombok.RequiredArgsConstructor;
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
                .orElseThrow(() -> new IllegalArgumentException("VideoFile not found"));

        int accidentTypeCode = json.at("/prediction/accident_type").asInt();
        String carA = json.at("/prediction/vehicle_a_progress_info").asText();
        String carB = json.at("/prediction/vehicle_b_progress_info").asText();

        AccidentDefinitionDto definition = accidentDefinitionLoader.get(accidentTypeCode);


        Report report = Report.builder()
                .videoFile(videoFile)
                .accidentCode(String.valueOf(accidentTypeCode))
                .title(definition.getTitle())
                .accidentType(definition.getAccidentFeature())
                .laws(definition.getLaws())
                .decisions(definition.getPrecedents())
                .carA(carA)
                .carB(carB)
                .mainEvidence(formatTimelineLog(json)) // ✅ 요약된 타임로그 저장
                .createdAt(LocalDateTime.now())
                .build();
        reportRepository.save(report);

        videoFile.setAnalysisStatus(AnalysisStatus.COMPLETED);
        return report.getId();
    }

    private String formatTimelineLog(JsonNode json) {
        JsonNode logs = json.at("/timeline_log");
        if (logs.isMissingNode() || !logs.isArray()) return "AI 분석 로그 없음";

        return StreamSupport.stream(logs.spliterator(), false)
                .map(entry -> {
                    String time = entry.at("/time").asText();
                    List<String> events = new ArrayList<>();
                    entry.at("/events").forEach(e -> events.add(e.asText()));
                    return time + " - " + String.join(", ", events);
                })
                .collect(Collectors.joining("\\n"));
    }
}
