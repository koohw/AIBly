package com.ssafy.backend.oauth.dto;

import lombok.Data;
import java.util.Map;

@Data
public class KakaoProfileResponse {
    private Long id;
    private Map<String, Object> kakao_account;
    private Map<String, Object> properties;
}