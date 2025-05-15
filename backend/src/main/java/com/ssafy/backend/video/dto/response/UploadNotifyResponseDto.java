package com.ssafy.backend.video.dto.response;

import com.ssafy.backend.domain.file.AnalysisStatus;
import com.ssafy.backend.domain.file.FileType;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class UploadNotifyResponseDto {
    private Long videoId; //저장된 VideoFile ID
    private FileType fileType; //VIDEO, PDF
    private AnalysisStatus analysisStatus; //ANALYZING, COMPLETED, FAILED
}

//프론트에서 지금 어떤 영상이 등록되어 있고, 분석 중인지 아닌지 확인