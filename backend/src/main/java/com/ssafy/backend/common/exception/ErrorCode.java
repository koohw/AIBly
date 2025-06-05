package com.ssafy.backend.common.exception;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum ErrorCode {

    // 인증/인가 관련
    INVALID_TOKEN(HttpStatus.UNAUTHORIZED, "유효하지 않은 토큰입니다"),
    ACCESS_DENIED(HttpStatus.FORBIDDEN, "접근 권한이 없습니다"),

    // 사용자 관련
    USER_NOT_FOUND(HttpStatus.NOT_FOUND, "존재하지 않는 사용자입니다"),
    USER_ALREADY_EXISTS(HttpStatus.CONFLICT, "이미 존재하는 사용자입니다"),

    // 영상 관련
    VIDEO_NOT_FOUND(HttpStatus.NOT_FOUND, "존재하지 않는 영상입니다"),
    VIDEO_ALREADY_ANALYZED(HttpStatus.BAD_REQUEST, "해당 영상은 이미 분석되었습니다"),
    VIDEO_UPLOAD_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "영상 업로드에 실패했습니다"),
    VIDEO_DELETE_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "영상 삭제에 실패했습니다"),

    // 보고서 관련
    REPORT_NOT_FOUND(HttpStatus.NOT_FOUND, "존재하지 않는 보고서입니다"),
    REPORT_ALREADY_EXISTS(HttpStatus.CONFLICT, "이미 보고서가 존재합니다"),
    PDF_ALREADY_EXIST(HttpStatus.CONFLICT, "이미 PDF가 생성된 보고서입니다"),
    PDF_GENERATE_FAIL(HttpStatus.INTERNAL_SERVER_ERROR, "PDF 생성에 실패했습니다"),

    // S3 관련
    S3_DELETE_FAIL(HttpStatus.INTERNAL_SERVER_ERROR, "S3 파일 삭제에 실패했습니다"),
    S3_UPLOAD_FAIL(HttpStatus.INTERNAL_SERVER_ERROR, "S3 파일 업로드에 실패했습니다"),
    FILE_NOT_FOUND(HttpStatus.NOT_FOUND, "존재하지 않는 파일입니다"),

    PDF_NOT_FOUND(HttpStatus.BAD_REQUEST, "PDF가 아직 생성되지 않았습니다."),
    // 공통
    INVALID_INPUT_VALUE(HttpStatus.BAD_REQUEST, "잘못된 입력값입니다"),
    INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "내부 서버 오류가 발생했습니다");

    private final HttpStatus httpStatus;
    private final String message;
}