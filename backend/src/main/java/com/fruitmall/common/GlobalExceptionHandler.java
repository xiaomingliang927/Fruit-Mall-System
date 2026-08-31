package com.fruitmall.common;

import lombok.extern.slf4j.Slf4j;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(BizException.class)
    public ApiResponse<Void> handleBiz(BizException e) {
        return ApiResponse.error(e.getCode(), e.getMessage());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ApiResponse<Void> handleValid(MethodArgumentNotValidException e) {
        FieldError fieldError = e.getBindingResult().getFieldError();
        String msg = fieldError != null ? fieldError.getDefaultMessage() : "参数错误";
        return ApiResponse.error(ErrorCode.PARAM_ERROR.getCode(), msg);
    }

    /** 超出 multipart 大小限制时给中文提示，避免落到 500 兜底文案 */
    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ApiResponse<Void> handleUploadTooLarge(MaxUploadSizeExceededException e) {
        return ApiResponse.error(ErrorCode.PARAM_ERROR.getCode(), "图片不能超过 5MB");
    }

    @ExceptionHandler(Exception.class)
    public ApiResponse<Void> handleOther(Exception e) {
        log.error("未处理异常", e);
        return ApiResponse.error(500, "服务器开小差了，请稍后重试");
    }
}
