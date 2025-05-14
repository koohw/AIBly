package com.ssafy.backend.fcm.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.auth.oauth2.GoogleCredentials;
import com.ssafy.backend.fcm.dto.FcmV1Message;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.FileInputStream;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Collections;

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
        try {
            // 1. Access Token 발급
            String accessToken = getAccessToken();

            // 2. 메시지 구성 (v1 포맷)
            String message = getMessageBody(targetToken, reportId);

            // 3. HTTP 요청 전송
            URL url = new URL(String.format(FCM_API_URL_TEMPLATE, projectId));
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setUseCaches(false);
            conn.setDoInput(true);
            conn.setDoOutput(true);
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Authorization", "Bearer " + accessToken);
            conn.setRequestProperty("Content-Type", "application/json; UTF-8");

            conn.getOutputStream().write(message.getBytes("UTF-8"));
            int responseCode = conn.getResponseCode();
            if (responseCode != 200) {
                throw new RuntimeException("FCM v1 메시지 전송 실패 : " + responseCode);
            }

        } catch (Exception e) {
            throw new RuntimeException("FCM v1 호출 에러", e);
        }
    }

    // ✅ Access Token 발급 (OAuth2)
    private String getAccessToken() throws IOException {
        GoogleCredentials googleCredentials = GoogleCredentials
                .fromStream(new FileInputStream(serviceAccountJsonPath))
                .createScoped(Collections.singletonList("https://www.googleapis.com/auth/cloud-platform"));
        googleCredentials.refreshIfExpired();
        return googleCredentials.getAccessToken().getTokenValue();
    }

    // ✅ 메시지 Body 구성 (v1 JSON 포맷)
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