package com.ssafy.backend.common.exception;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum ErrorCode {

    //인증/인가 관련
    INVALID_TOKEN(HttpStatus.UNAUTHORIZED, "유효하지 않은 토큰입니다"),
    ACCESS_DENIED(HttpStatus.FORBIDDEN, "접근 권한이 없습니다"),

    //사용자 관련
    USER_NOT_FOUND(HttpStatus.NOT_FOUND, "존재하지 않는 사용자입니다"),
    USER_ALREADY_EXISTS(HttpStatus.CONFLICT, "이미 존재하는 사용자입니다"),

    //영상 관련
    VIDEO_NOT_FOUND(HttpStatus.NOT_FOUND, "존재하지 않는 영상입니다"),
    VIDEO_ALREADY_ANALYZED(HttpStatus.BAD_REQUEST, "해당 영상은 이미 분석되었습니다"),

    //보고서 관련
    DOCUMENT_NOT_FOUND(HttpStatus.NOT_FOUND, "존재하지 않는 보고서입니다"),
    PDF_GENERATION_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "PDF 생성에 실패했습니다"),

    //공통
    INVALID_INPUT_VALUE(HttpStatus.BAD_REQUEST, "잘못된 입력값입니다");

    private final HttpStatus httpStatus;
    private final String message;
}