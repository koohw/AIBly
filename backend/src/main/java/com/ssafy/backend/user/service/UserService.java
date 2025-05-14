package com.ssafy.backend.user.service;

public interface UserService {
    /**
     * @param userId DB에 저장된 User의 PK
     */
    void deleteUserById(Long userId);

    // videoId 기준 FCM 토큰 조회
    String getUserFcmTokenByVideoId(Long videoId);

    //fcm 토큰
    void updateUserFcmToken(Long userId, String fcmToken);

}
