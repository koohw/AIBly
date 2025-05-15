package com.ssafy.backend.fcm.repository;

import com.ssafy.backend.fcm.entity.FcmToken;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface FcmTokenRepository extends JpaRepository<FcmToken, Long> {
    Optional<FcmToken> findByUserId(Long userId); //해당 userId로 등록된 FCM 토큰이 없다면 Optional.empty() 리턴
}