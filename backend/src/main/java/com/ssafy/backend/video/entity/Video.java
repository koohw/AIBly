package com.ssafy.backend.video.entity;

import com.ssafy.backend.member.entity.User;
import com.ssafy.backend.video.entity.enums.VideoSource;
import com.ssafy.backend.video.entity.enums.VideoStatus;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Video {

    @Id //pk
    @GeneratedValue // 자동 생성
    private Long id;

    @Column(length = 1000) //presigned URL 길이가 길어서 추가 또는 text로 바꾸기
    private String videoUrl;

    @Enumerated(EnumType.STRING) //enum 타입 string로 db저장
    private VideoStatus status;

    @Enumerated(EnumType.STRING)
    private VideoSource source;

    @CreationTimestamp // 저장 시 자동으로 현재 시간 입력
    @Column(updatable = false)
    private LocalDateTime createAt;

    private Boolean accident; //Boolean은 null 가능

    @ManyToOne(fetch = FetchType.LAZY) //여러개의 영상이 하나의 사용자에게 소속되는 다대일 관계. LAZY: 필요한 순간에 user을 db에서 가져옴.
    private User user;

}
