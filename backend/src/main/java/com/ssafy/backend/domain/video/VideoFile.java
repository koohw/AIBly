package com.ssafy.backend.domain.video;


import com.ssafy.backend.domain.file.AnalysisStatus;
import com.ssafy.backend.domain.file.FileType;
import com.ssafy.backend.domain.file.UploadType;
import com.ssafy.backend.user.entity.User;
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

    @Builder.Default
    @Enumerated(EnumType.STRING)
    private AnalysisStatus analysisStatus = AnalysisStatus.ANALYZING;
    //업로드 완료 시점에 자동으로 "ANALYZING" 상태를 만듦

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