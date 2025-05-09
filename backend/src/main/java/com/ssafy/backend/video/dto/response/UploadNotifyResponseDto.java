package com.ssafy.backend.video.dto.response;

import com.ssafy.backend.domain.file.AnalysisStatus;
import com.ssafy.backend.domain.file.FileType;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class UploadNotifyResponseDto {
    private Long fileId;
    private FileType fileType;
    private AnalysisStatus analysisStatus;
}