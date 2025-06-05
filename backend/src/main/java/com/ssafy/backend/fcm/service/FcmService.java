package com.ssafy.backend.fcm.service;

//자동 업로드 시 FCM 발송
//reportId    보고서 ID (Data 메시지에 포함)
//모든 report의 값을 넣어서 보낼 필요없음
public interface FcmService {
    void sendFCM(String targetToken, Long reportId);
}

//BE -> Google FCM 서버로 직접 발송(프론트 앱한테 발송x)