package com.ssafy.backend.oauth.service;

import com.ssafy.backend.oauth.dto.KakaoLoginRequest;
import com.ssafy.backend.oauth.dto.KakaoProfileResponse;
import com.ssafy.backend.user.entity.User;

public interface OAuthService {
    KakaoLoginRequest getToken(String code);
    KakaoProfileResponse getProfile(String accessToken);
    User saveOrUpdateUser(KakaoProfileResponse profile);
}