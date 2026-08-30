package com.fruitmall.common;

import lombok.Getter;

@Getter
public class BizException extends RuntimeException {

    private final int code;

    public BizException(String message) {
        this(ErrorCode.BIZ_ERROR.getCode(), message);
    }

    public BizException(ErrorCode errorCode) {
        this(errorCode.getCode(), errorCode.getMessage());
    }

    public BizException(int code, String message) {
        super(message);
        this.code = code;
    }
}
