package com.fruitmall.modules.auth;

import com.fruitmall.common.ApiResponse;
import com.fruitmall.modules.auth.dto.LoginRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    /** 手机号 + 短信验证码登录（M1 开发模式：验证码固定 123456，自动注册） */
    @PostMapping("/sms-login")
    public ApiResponse<AuthService.LoginResponse> smsLogin(@Valid @RequestBody LoginRequest request) {
        return ApiResponse.ok(authService.loginBySms(request));
    }

    /** 小程序微信登录：wx.login 的 code 换 token */
    @PostMapping("/wx-login")
    public ApiResponse<AuthService.LoginResponse> wxLogin(@RequestBody WxLoginRequest request) {
        return ApiResponse.ok(authService.loginByWechat(request.code()));
    }

    public record WxLoginRequest(String code) {
    }
}
