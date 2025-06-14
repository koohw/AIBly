package com.ssafy.backend.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class CorsConfig implements WebMvcConfigurer {
    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**")
//                .allowedOrigins("https://k12e203.p.ssafy.io") //프론트 도메인으로 변경
                .allowedOrigins(
                        "https://localhost",      // 개발 중인 WebView origin
                        "capacitor://localhost",   // 실제 앱에서의 origin
                        "https://k12e203.p.ssafy.io"
                ) // 테스트 환경에서 임시로 설정
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                .allowedHeaders("*")
                .allowCredentials(false) //나중에 로그인 구현해서 JWT, OAuth2 인증 시 true로 변경하기
                .maxAge(3600);
    }
}
