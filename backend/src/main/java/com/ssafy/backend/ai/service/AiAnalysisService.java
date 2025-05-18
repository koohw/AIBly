package com.ssafy.backend.ai.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ssafy.backend.ai.dto.response.AccidentDefinitionDto;
import com.ssafy.backend.ai.dto.response.EventLogDto;
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
    private final ObjectMapper objectMapper;

    @Transactional
    public Long handleAiCallback(JsonNode json, Long videoId) {
        VideoFile videoFile = videoFileRepository.findById(videoId)
                .orElseThrow(() -> new IllegalArgumentException("VideoFile not found: " + videoId));

        String fileName = videoFile.getFileName();

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
        String timelineJson = convertEventTimelineToJson(json.path("eventTimeline"));  // ✅ 여기서 JSON으로 변환

        // CSV 기반 정적 데이터
        AccidentDefinitionDto definition = accidentDefinitionLoader.get(accidentTypeCode);

        // CSV 기반 과실비율로 대체
        int faultA = definition.getFaultA();
        int faultB = definition.getFaultB();

        // Report 생성
        Report report = Report.builder()
                .videoFile(videoFile)
                .fileName(fileName)
                .accidentCode(String.valueOf(accidentTypeCode))
                .title(definition.getAccidentFeature())  // 장소 특징으로 제목 설정
                .accidentType(definition.getTitle())     // 기존 title을 accidentType으로 이동
                .laws(definition.getLaws())
                .decisions(definition.getPrecedents())
                .carA(carA)
                .carB(carB)
                .mainEvidence(timelineJson)
                .createdAt(LocalDateTime.now())
                .analysisStatus(AnalysisStatus.COMPLETED)
                .build();

        reportRepository.save(report);
        videoFile.setAnalysisStatus(AnalysisStatus.COMPLETED);

        // 업로드 타입 직접 조회 (VideoService 대신 Repository 사용)
        //FCM 발송 처리
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

    //프론트에게 넘기는 파일 [] 이렇게 덩어리로 보내기.
    private String convertEventTimelineToJson(JsonNode eventTimeline) {
        if (eventTimeline == null || !eventTimeline.isArray()) {
            return "[]";  // 빈 배열로 저장
        }

        List<EventLogDto> timelineList = StreamSupport.stream(eventTimeline.spliterator(), false)
                .map(entry -> {
                    String event = entry.path("event").asText();
                    int frame = entry.path("frameIdx").asInt();
                    double seconds = Math.round(frame * 0.68 * 100.0) / 100.0; // 소수점 둘째 자리 반올림
                    return new EventLogDto(event, seconds + "초");
                })
                .collect(Collectors.toList());

        try {
            return objectMapper.writeValueAsString(timelineList);
        } catch (Exception e) {
            log.error("EventTimeline JSON 변환 실패", e);
            return "[]";
        }
    }

}

//csv:title, accidentFeature, laws, precedents 동적만
/*
accident_type, vehicle_A_direction, vehicle_B_direction,
damage_location, negligence_rate/A, negligence_rate/B, event_timeline
그 외는 모두 ai기반으로 생성. 빈값이면 빈 문자열 "" 반환.
 */


