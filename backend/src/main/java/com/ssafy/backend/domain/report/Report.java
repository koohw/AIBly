package com.ssafy.backend.domain.report;

import com.ssafy.backend.domain.file.VideoFile;
import jakarta.persistence.*;
import lombok.Builder;

import java.time.LocalDateTime;

@Builder
@Entity
public class Report {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "file_id", nullable = false)
    private VideoFile videoFile;

    @Column(length = 300, nullable = false)
    private String title;

    @Column(length = 100, nullable = false)
    private String accidentCode;

    @Column(columnDefinition = "TEXT")
    private String accidentType;

    @Column(columnDefinition = "TEXT")
    private String carA;

    @Column(columnDefinition = "TEXT")
    private String carB;

    @Column(columnDefinition = "TEXT")
    private String mainEvidence;

    @Column(columnDefinition = "TEXT")
    private String laws;

    @Column(columnDefinition = "TEXT")
    private String decisions;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    //@Builder로 jpa 생성자 만듦
    protected Report() {
    }
}