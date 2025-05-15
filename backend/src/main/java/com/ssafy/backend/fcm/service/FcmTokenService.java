package com.ssafy.backend.fcm.service;

//FcmToken 저장/갱신용 Service
public interface FcmTokenService {
    void registerOrUpdateFcmToken(Long userId, String fcmToken);

}
