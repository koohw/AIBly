package com.ssafy.backend.ai.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.ssafy.backend.ai.dto.response.AccidentDefinitionDto;
import com.ssafy.backend.domain.file.AnalysisStatus;
import com.ssafy.backend.domain.file.UploadType;
import com.ssafy.backend.domain.report.Report;
import com.ssafy.backend.domain.report.ReportRepository;
import com.ssafy.backend.domain.video.VideoFile;
import com.ssafy.backend.domain.video.VideoFileRepository;
import com.ssafy.backend.fcm.service.FcmService;
import com.ssafy.backend.user.service.UserService;
import com.ssafy.backend.video.service.VideoService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.checkerframework.checker.units.qual.A;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;


@Slf4j
@Service
@RequiredArgsConstructor
public class AiAnalysisService {//AI 서버의 JSON 데이터를 분석, Report 엔티티 생성 및 저장, FCM 발송 (자동 업로드 시만)

    private final VideoFileRepository videoFileRepository;
    private final ReportRepository reportRepository;
    private final AccidentDefinitionLoader accidentDefinitionLoader;
    private final UserService userService;
    private final FcmService fcmService;

    @Transactional
    public Long handleAiCallback(JsonNode json, Long videoId) {
        VideoFile videoFile = videoFileRepository.findById(videoId)
                .orElseThrow(() -> new IllegalArgumentException("VideoFile not found: " + videoId));

        // Idempotent 처리
        Report existingReport = reportRepository.findByVideoFileId(videoId).orElse(null);
        if (existingReport != null) {
            log.info("이미 처리된 Report 존재: videoId={}, reportId={}", videoId, existingReport.getId());
            return existingReport.getId();
        }

        // AI JSON 기반 동적 데이터
        int accidentTypeCode = json.path("accidentType").asInt();
        String carA = json.path("carAProgress").asText("");
        String carB = json.path("carBProgress").asText("");
        String damageLocation = json.path("damageLocation").asText("");
        int faultA = json.path("faultA").asInt();
        int faultB = json.path("faultB").asInt();
        String timelineLog = formatEventTimeline(json.path("eventTimeline"));

        // CSV 기반 정적 데이터
        AccidentDefinitionDto definition = accidentDefinitionLoader.get(accidentTypeCode);

        // Report 생성
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

        // 업로드 타입 직접 조회 (VideoService 대신 Repository 사용)
        UploadType uploadType = videoFile.getUploadType();
        log.info("업로드 타입 확인: videoId={}, uploadType={}", videoId, uploadType);

        if (uploadType == UploadType.AUTO) {
            String fcmToken = userService.getUserFcmTokenByVideoId(videoId);
            if (fcmToken == null || fcmToken.isBlank()) {
                log.info("FCM 토큰 없음 - FCM 발송 없음: videoId={}", videoId);
            } else {
                try {
                    fcmService.sendFCM(fcmToken, report.getId()); //FCM 발송 처리
                    log.info("FCM 발송 성공: videoId={}, reportId={}", videoId, report.getId());
                } catch (Exception e) {
                    log.error("FCM 발송 실패: videoId={}, error={}", videoId, e.getMessage(), e);
                }
            }
        } else {
            log.info("수동 업로드 - FCM 발송 없음: videoId={}", videoId);
        }

        return report.getId();
    }

    private String formatEventTimeline(JsonNode eventTimeline) {
        if (eventTimeline == null || !eventTimeline.isArray()) return "AI 분석 로그 없음";

        return StreamSupport.stream(eventTimeline.spliterator(), false)
                .map(entry -> {
                    String frame = entry.path("frameIdx").asText();
                    String event = entry.path("event").asText();
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


