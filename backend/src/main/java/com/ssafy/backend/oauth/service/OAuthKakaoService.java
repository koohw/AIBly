package com.ssafy.backend.oauth.service;

import com.ssafy.backend.oauth.dto.KakaoLoginRequest;
import com.ssafy.backend.oauth.dto.KakaoProfileResponse;
import com.ssafy.backend.user.entity.User;
import com.ssafy.backend.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

@Service("OAuthKakaoService")
@RequiredArgsConstructor
public class OAuthKakaoService implements OAuthService {

    @Value("${kakao.rest-api-key}")
    private String restKey;

    @Value("${kakao.redirect-uri}")
    private String redirectUri;

    private final UserRepository userRepository;
    private final RestTemplate restTemplate = new RestTemplate();

    @Override
    public KakaoLoginRequest getToken(String code) {
        String url = "https://kauth.kakao.com/oauth/token";

        // 로그 추가
        System.out.println("카카오 토큰 요청 URL: " + url);
        System.out.println("인가 코드: " + code);
        System.out.println("리다이렉트 URI: " + redirectUri);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add("grant_type", "authorization_code");
        params.add("client_id", restKey);
        params.add("redirect_uri", redirectUri);
        params.add("code", code);

        // 요청 파라미터 로깅
        System.out.println("요청 파라미터: " + params);

        HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(params, headers);

        try {
            ResponseEntity<KakaoLoginRequest> response = restTemplate.exchange(
                    url, HttpMethod.POST, request, KakaoLoginRequest.class
            );
            System.out.println("카카오 응답 성공: " + response.getStatusCode());
            return response.getBody();
        } catch (Exception e) {
            System.err.println("카카오 토큰 요청 실패: " + e.getMessage());
            e.printStackTrace();
            throw e;
        }
    }

    @Override
    public KakaoProfileResponse getProfile(String accessToken) {
        String url = "https://kapi.kakao.com/v2/user/me";
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(accessToken);

        HttpEntity<Void> request = new HttpEntity<>(headers);
        ResponseEntity<KakaoProfileResponse> response = restTemplate.exchange(
                url, HttpMethod.POST, request, KakaoProfileResponse.class
        );
        return response.getBody();
    }

    @Override
    public User saveOrUpdateUser(KakaoProfileResponse profile) {
        String provider = "kakao";
        String providerId = profile.getId().toString();

        return userRepository.findByProviderAndProviderId(provider, providerId)
                .orElseGet(() -> {
                    String email = (String) profile.getKakao_account().get("email");
                    String name  = (String) profile.getProperties().get("nickname");

                    User user = User.builder()
                            .provider(provider)
                            .providerId(providerId)
                            .email(email)
                            .name(name)
                            .build();
                    return userRepository.save(user);
                });
    }
}