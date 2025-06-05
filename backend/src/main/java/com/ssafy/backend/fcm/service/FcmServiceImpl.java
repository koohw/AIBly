package com.ssafy.backend.fcm.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.auth.oauth2.GoogleCredentials;
import com.ssafy.backend.fcm.dto.FcmV1Message;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.FileInputStream;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.io.File;

@Slf4j
@Service
@RequiredArgsConstructor
public class FcmServiceImpl implements FcmService {

    @Value("${fcm.service-account.path}")
    private String serviceAccountJsonPath;

    @Value("${fcm.project-id}")
    private String projectId;

    private static final String FCM_API_URL_TEMPLATE = "https://fcm.googleapis.com/v1/projects/%s/messages:send";

    @Override
    public void sendFCM(String targetToken, Long reportId) {
        log.info("FCM 발송 시도: targetToken={}, reportId={}", targetToken, reportId);

        try {
            String accessToken = getAccessToken();
            String message = getMessageBody(targetToken, reportId);

            URL url = new URL(String.format(FCM_API_URL_TEMPLATE, projectId));
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setUseCaches(false);
            conn.setDoInput(true);
            conn.setDoOutput(true);
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Authorization", "Bearer " + accessToken);
            conn.setRequestProperty("Content-Type", "application/json; UTF-8");

            conn.getOutputStream().write(message.getBytes(StandardCharsets.UTF_8));
            int responseCode = conn.getResponseCode();

            if (responseCode == 200) {
                log.info("✅ FCM 발송 성공: reportId={}", reportId);
            } else {
                String errorResponse = new String(conn.getErrorStream().readAllBytes(), StandardCharsets.UTF_8);
                log.error("FCM 발송 실패 - HTTP 응답 코드: {}, 내용: {}, reportId={}", responseCode, errorResponse, reportId);
                throw new RuntimeException("FCM v1 메시지 전송 실패: HTTP " + responseCode + " / " + errorResponse);
            }

        } catch (Exception e) {
            log.error("FCM 발송 예외 발생: reportId={}, error={}", reportId, e.getMessage(), e);
            throw new RuntimeException("FCM v1 호출 에러", e);
        }
    }

    private String getAccessToken() throws IOException {
        log.info("FCM 서비스 계정 경로: {}", serviceAccountJsonPath);
        
        File pathFile = new File(serviceAccountJsonPath);
        String actualPath = serviceAccountJsonPath;
        
        if (pathFile.isDirectory()) {
            actualPath = new File(pathFile, "ablri-b137e-638caf50f016.json").getAbsolutePath();
            log.info("FCM 서비스 계정 파일 전체 경로로 수정: {}", actualPath);
        }
        
        try (FileInputStream serviceAccountStream = new FileInputStream(actualPath)) {
            GoogleCredentials googleCredentials = GoogleCredentials
                    .fromStream(serviceAccountStream)
                    .createScoped(Collections.singletonList("https://www.googleapis.com/auth/cloud-platform"));
            googleCredentials.refreshIfExpired();
            return googleCredentials.getAccessToken().getTokenValue();
        }
    }

    private String getMessageBody(String targetToken, Long reportId) throws IOException {
        ObjectMapper objectMapper = new ObjectMapper();
        var message = new FcmV1Message(
                new FcmV1Message.Message(
                        new FcmV1Message.Notification("AI 분석 완료", "보고서가 생성되었습니다."),
                        targetToken,
                        Collections.singletonMap("reportId", reportId.toString())
                )
        );
        return objectMapper.writeValueAsString(message);
    }
}

//FCM 발송 메시지는 최소한으로 (title, body, reportId만)
//보고서 내용 전체는 넣지 않음 (불필요 + 성능 저하)