package com.ssafy.backend.domain.file;

import com.ssafy.backend.domain.user.User;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "video_files")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class VideoFile {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String fileName;

    @Column(nullable = false, unique = true)
    private String s3Key;

    private String contentType;

    private Long size;

    @Enumerated(EnumType.STRING)
    private UploadType uploadType;

    @Enumerated(EnumType.STRING)
    private FileType fileType;

    @Enumerated(EnumType.STRING)
    private AnalysisStatus analysisStatus;

    private String analysisError;

    //private String thumbnailKey;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user; //사용자는 로그인을 해야 앱 서비스 이용 가능함


    private LocalDateTime uploadedAt;

    @PrePersist //DB에 저장되기 직전에 자동으로 업로드 시간을 현재 시각으로 만들어줌
    public void setUploadTime() {
        this.uploadedAt = LocalDateTime.now();
    }

}