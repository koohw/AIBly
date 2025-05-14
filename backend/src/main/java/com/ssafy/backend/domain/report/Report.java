package com.ssafy.backend.domain.report;

import com.ssafy.backend.domain.file.AnalysisStatus;
import com.ssafy.backend.domain.video.VideoFile;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

//AI 분석 결과 + CSV 설명을 합쳐 사용자에게 보여줄 사고 보고서를 저장
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
public class Report {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "file_id", nullable = false)
    private VideoFile videoFile;

    @Column(length = 100, nullable = false)
    private String accidentCode;

    @Column(length = 300, nullable = false)
    private String title;

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

    //PDF 저장 후 S3에 업로드할 때, 파일의 S3 key를 저장
    //나중에 다운로드 URL 발급 시 이 key로 presigned URL을 생성
    @Column(length = 500)
    private String pdfKey;

    //사용자에게 보여줄 Report 생성/분석 상태 (ANALYZING, COMPLETED, FAILED)
    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private AnalysisStatus analysisStatus;

    @Builder
    public Report(VideoFile videoFile, String title, String accidentCode,
                  String accidentType, String carA, String carB,
                  String mainEvidence, String laws, String decisions,
                  LocalDateTime createdAt, AnalysisStatus analysisStatus) {
        this.videoFile = videoFile;
        this.title = title;
        this.accidentCode = accidentCode;
        this.accidentType = accidentType;
        this.carA = carA;
        this.carB = carB;
        this.mainEvidence = mainEvidence;
        this.laws = laws;
        this.decisions = decisions;
        this.createdAt = createdAt;
        this.analysisStatus = analysisStatus;
    }

    //초기상태 지정
    @PrePersist
    public void prePersist() {
        this.createdAt = LocalDateTime.now();
        if (this.analysisStatus == null) {
            this.analysisStatus = AnalysisStatus.ANALYZING;
        }
    }

}
