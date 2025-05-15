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
    private String accidentCode;  // 사고 유형 번호

    @Column(length = 300, nullable = false)
    private String title;         // 사고 제목 (목록/카드에서 사용)

    @Column(columnDefinition = "TEXT")
    private String accidentType;  // 사고 유형 상세 설명 (사고장소 특징)

    @Column(columnDefinition = "TEXT")
    private String carA;          // 차량 A 진행 방향 (AI JSON carAProgress)

    @Column(columnDefinition = "TEXT")
    private String carB;          // 차량 B 진행 방향

    @Column(nullable = false)
    private int faultA;           // A 차량 과실

    @Column(nullable = false)
    private int faultB;           // B 차량 과실

    @Column(length = 100)
    private String damageLocation;  // 충돌 위치

    @Column(columnDefinition = "TEXT")
    private String mainEvidence;    // 주요 증거 (AI eventTimeline)

    @Column(columnDefinition = "TEXT")
    private String laws;            // 법령 (CSV)

    @Column(columnDefinition = "TEXT")
    private String decisions;       // 판례 (CSV)

    @Column(nullable = false)
    private LocalDateTime createdAt;

    //사용자에게 보여줄 Report 생성/분석 상태 (ANALYZING, COMPLETED, FAILED)
    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private AnalysisStatus analysisStatus;

    //PDF 저장 후 S3에 업로드할 때, 파일의 S3 key를 저장
    //나중에 다운로드 URL 발급 시 이 key로 presigned URL을 생성
    @Column(length = 500)
    private String pdfKey;

    @Builder
    public Report(VideoFile videoFile, String accidentCode, String title, String accidentType,
                  String carA, String carB, int faultA, int faultB,
                  String damageLocation, String mainEvidence,
                  String laws, String decisions,
                  LocalDateTime createdAt, AnalysisStatus analysisStatus) {

        this.videoFile = videoFile;
        this.accidentCode = accidentCode;
        this.title = title;
        this.accidentType = accidentType;
        this.carA = carA;
        this.carB = carB;
        this.faultA = faultA;
        this.faultB = faultB;
        this.damageLocation = damageLocation;
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
