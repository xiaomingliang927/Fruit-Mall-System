package com.fruitmall.common;

import lombok.Getter;

@Getter
public enum ErrorCode {

    UNAUTHORIZED(401, "未登录或登录已过期"),
    PARAM_ERROR(400, "参数错误"),
    NOT_FOUND(404, "资源不存在"),
    BIZ_ERROR(1000, "业务处理失败");

    private final int code;
    private final String message;

    ErrorCode(int code, String message) {
        this.code = code;
        this.message = message;
    }
}
