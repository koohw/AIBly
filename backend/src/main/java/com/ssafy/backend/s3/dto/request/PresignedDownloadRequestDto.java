package com.ssafy.backend.s3.dto.request;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class PresignedDownloadRequestDto {
    private String s3Key; // fileName -> s3key 로 변경
} 