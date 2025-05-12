package com.ssafy.backend.config;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;

//JWT에서 추출한 사용자 정보를 담을 CustomUserDetails
@Getter
@AllArgsConstructor
public class CustomUserDetails implements UserDetails {

    private Long userId;
    private String provider;
    private String providerId;

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(); // 필요하면 권한 추가
    }

    @Override
    public String getPassword() {
        return null; // 비밀번호 인증 안쓰므로 null
    }

    @Override
    public String getUsername() {
        return provider + ":" + providerId;
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