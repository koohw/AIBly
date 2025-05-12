package com.ssafy.backend.config;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration; // ← 추가
import org.springframework.web.cors.CorsConfigurationSource;        // ← 추가
import org.springframework.web.cors.UrlBasedCorsConfigurationSource; // ← 추가

import java.util.List;  // ← java.util.List 도 필요합니다


import java.util.List;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtTokenProvider jwtProvider;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .cors(Customizer.withDefaults())             // cors().and() 대신
                .csrf(AbstractHttpConfigurer::disable)       // csrf().disable() 대신
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/api/**", "/oauth/**", "/favicon.ico").permitAll() // 일단 api로 다 허용하는 걸로 만듦
                        .anyRequest().authenticated()
                )
                // 5) JWT 필터를 Security 필터 체인에 등록
                .addFilterBefore(
                        new JwtAuthenticationFilter(jwtProvider),
                        UsernamePasswordAuthenticationFilter.class
                );

        return http.build();
    }

    //  ────────────────────────────
    //   CORS 정책 정의
    //  ────────────────────────────
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        // 프론트 개발 서버 주소
//        config.setAllowedOrigins(List.of(
//                "http://localhost:3000",
//                "https://localhost",
//                "capacitor://localhost"));
        config.addAllowedOriginPattern("*");
        // 허용할 HTTP 메서드
        config.setAllowedMethods(List.of("GET", "POST", "OPTIONS"));
        // 모든 헤더 허용
        config.setAllowedHeaders(List.of("*"));
        // 쿠키 인증(필요시)
        config.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        // 모든 경로에 위 설정 적용
        source.registerCorsConfiguration("/**", config);
        return source;
    }
}
