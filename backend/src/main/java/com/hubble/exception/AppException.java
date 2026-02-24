package com.hubble.exception;

import lombok.Getter;

@Getter
public class AppException extends RuntimeException {

    final ErrorCode errorCode;

    public AppException(ErrorCode errorCode) {
        super(errorCode.getMessage());
        this.errorCode = errorCode;
    }
}
