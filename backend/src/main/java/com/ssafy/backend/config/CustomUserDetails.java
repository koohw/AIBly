package com.ssafy.backend.config;

import com.ssafy.backend.user.entity.User;
import lombok.Getter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.Collections;

//JWT에서 추출한 사용자 정보를 담을 CustomUserDetails

@Getter
public class CustomUserDetails implements UserDetails {

    private final Long userId;
    private final String email;
    private final String provider;
    private final String providerId;

    public CustomUserDetails(User user) {
        this.userId = user.getId();
        this.email = user.getEmail();
        this.provider = user.getProvider();
        this.providerId = user.getProviderId();
    }

    // ✅ from 메서드 추가
    public static CustomUserDetails from(User user) {
        return new CustomUserDetails(user);
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        // 권한이 있다면 반환
        return Collections.emptyList();
    }

    @Override
    public String getPassword() {
        return null; // 소셜 로그인 기반이면 비밀번호 null
    }

    @Override
    public String getUsername() {
        return provider + ":" + providerId; // subject 일관성 유지
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return true;
    }
}