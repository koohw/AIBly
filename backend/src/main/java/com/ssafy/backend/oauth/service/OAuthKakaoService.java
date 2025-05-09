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
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add("grant_type", "authorization_code");
        params.add("client_id", restKey);
        params.add("redirect_uri", redirectUri);
        params.add("code", code);

        HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(params, headers);
        ResponseEntity<KakaoLoginRequest> response = restTemplate.exchange(
                url, HttpMethod.POST, request, KakaoLoginRequest.class
        );
        return response.getBody();
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