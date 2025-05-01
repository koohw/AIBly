package com.ssafy.backend.common.exception;

import lombok.Getter;

@Getter
public class CustomException extends RuntimeException {

    private final ErrorCode errorCode;

    public CustomException(ErrorCode errorCode) {
        super(errorCode.getMessage()); // 메시지를 RuntimeException에도 전달
        this.errorCode = errorCode;
    }

    public int getStatus() {
        return errorCode.getHttpStatus().value();
    }

    public String getErrorMessage() {
        return errorCode.getMessage();
    }
}