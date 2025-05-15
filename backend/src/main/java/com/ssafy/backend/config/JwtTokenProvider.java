package com.ssafy.backend.config;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;
import org.springframework.security.core.Authentication;

import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.util.Date;
import java.util.List;




@Component
public class JwtTokenProvider {

    // application.properties 에서 읽어옵니다.
    @Value("${jwt.secret}")
    private String secretKey;

    @Value("${jwt.expiration-ms}")
    private long validityInMilliseconds;

    // provider + providerId 로 토큰 생성
    public String generateToken(String provider, String providerId, Long userId) {
        Date now = new Date();
        Date expiry = new Date(now.getTime() + validityInMilliseconds);

        return Jwts.builder()
                .setSubject(provider + ":" + providerId)
                .claim("userId", userId)
                .setIssuedAt(now)
                .setExpiration(expiry)
                .signWith(getSigningKey(), SignatureAlgorithm.HS256)
                .compact();
    }

    // 2) 토큰 유효성 검사
    public boolean validateToken(String token) {
        try {
            Jwts.parserBuilder()                                  // parser() → parserBuilder()
                    .setSigningKey(getSigningKey())                  // 키 세팅
                    .build()                                         // 빌드해서
                    .parseClaimsJws(token);                          // 파싱
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    // 3) 토큰에서 Authentication 객체 생성
    public Authentication getAuthentication(String token) {
        Claims claims = Jwts.parserBuilder()
                .setSigningKey(getSigningKey())
                .build()
                .parseClaimsJws(token)
                .getBody();

        String principalName = claims.getSubject();
        // subject="provider:providerId" 이므로 그대로 사용하거나, UserDetailsService 로 로드

        // 예시: principalName 그대로 UserDetails 로 쓰는 간단 구현
        return new UsernamePasswordAuthenticationToken(
                principalName,
                "",
                List.of()   // 권한이 있다면 여기에 담아 주세요
        );
    }

    // 공통으로 쓰일 서명키 생성 메서드
    private Key getSigningKey() {
        // secretKey 는 Base64 인코딩된 문자열이라 가정
        byte[] keyBytes = secretKey.getBytes(StandardCharsets.UTF_8);
        return Keys.hmacShaKeyFor(keyBytes);
    }

    public Long getUserIdFromToken(String token) {
        Claims claims = Jwts.parserBuilder()
                .setSigningKey(getSigningKey())
                .build()
                .parseClaimsJws(token)
                .getBody();

        Object userIdObj = claims.get("userId");
        if (userIdObj instanceof Number) {
            return ((Number) userIdObj).longValue();
        } else if (userIdObj instanceof String) {
            return Long.parseLong((String) userIdObj);
        }
        throw new IllegalArgumentException("토큰에 userId 클레임이 없습니다.");
    }

    //JWT 파싱 중복 제거
    public Claims getClaims(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(getSigningKey())
                .build()
                .parseClaimsJws(token)
                .getBody();
    }

}


