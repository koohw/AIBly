package com.ssafy.backend.domain.user;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    // 카카오 로그인 시: provider = "kakao", providerId = 카카오 userId
    Optional<User> findByProviderAndProviderId(String provider, String providerId);
}