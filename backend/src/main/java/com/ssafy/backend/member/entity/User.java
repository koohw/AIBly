package com.ssafy.backend.member.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity // 이게 있어야 테이블 생성해줌. 엔티티에는 필수로 적기
public class User {

    @Id
    @GeneratedValue
    private Long id;
    private String name;
    private String email;

    @CreationTimestamp // 저장 시점 자동 입력
    @Column(updatable = false) // 수정 시 변경 금지
    private LocalDateTime createAt;

}
