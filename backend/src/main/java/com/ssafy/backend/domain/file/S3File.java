package com.ssafy.backend.domain.file;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "s3_files", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"fileName", "contentType", "userId"})
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class S3File {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String s3Key; // S3에 저장된 실제 파일 이름 (f0a1-3b2e.mp4, UUID.mp4 등)

    @Column(nullable = false)
    private String fileName; //사용자가 업로드한 원래 파일 이름

    @Column(nullable = false)
    private String contentType; //video/mp4, application/pdf

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private FileType fileType; //썸네일, 영상, pdf

    private Integer duration; // 영상 길이

    @Column(nullable = false)
    private LocalDateTime uploadDate;

    @Column(nullable = true, length = 128)
    private String fileHash;

    @Column(nullable = false)
    private Long userId;  // 업로드한 유저 ID, s3파일 중복 방지

    @Column(nullable = true)
    private Long size;

    @PrePersist
    public void prePersist() {
        this.uploadDate = LocalDateTime.now();
    }
}