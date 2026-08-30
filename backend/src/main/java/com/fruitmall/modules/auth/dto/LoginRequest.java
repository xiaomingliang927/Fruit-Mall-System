package com.fruitmall.modules.auth.dto;

import jakarta.validation.constraints.NotBlank;

public record LoginRequest(
        @NotBlank(message = "手机号不能为空") String phone,
        @NotBlank(message = "验证码不能为空") String code
) {
}
