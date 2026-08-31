package com.fruitmall.modules.auth;

import com.fruitmall.auth.JwtUtil;
import com.fruitmall.common.ApiResponse;
import com.fruitmall.common.BizException;
import com.fruitmall.modules.auth.dto.LoginRequest;
import io.jsonwebtoken.Claims;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final JwtUtil jwtUtil;

    /** 手机号 + 短信验证码登录（验证码默认走风控校验；开发模式万能码 123456 仍可用） */
    @PostMapping("/sms-login")
    public ApiResponse<AuthService.LoginResponse> smsLogin(@Valid @RequestBody LoginRequest request) {
        return ApiResponse.ok(authService.loginBySms(request));
    }

    /** 下发短信验证码：带频率限制与试错上限，防刷防爆破 */
    @PostMapping("/sms-code")
    public ApiResponse<Void> sendSmsCode(@Valid @RequestBody SmsCodeRequest request, HttpServletRequest httpRequest) {
        authService.sendSmsCode(request.phone(), httpRequest.getRemoteAddr());
        return ApiResponse.ok();
    }

    /** 小程序微信登录：wx.login 的 code 换 token */
    @PostMapping("/wx-login")
    public ApiResponse<AuthService.LoginResponse> wxLogin(@RequestBody WxLoginRequest request) {
        return ApiResponse.ok(authService.loginByWechat(request.code()));
    }

    /**
     * 续期：用未过期的旧 token 换发新 token（接入刷新机制，前端可忽略）。
     * 旧 token 失效/过期则要求重新登录。
     */
    @PostMapping("/refresh")
    public ApiResponse<Map<String, Object>> refresh(HttpServletRequest httpRequest) {
        String auth = httpRequest.getHeader("Authorization");
        if (auth == null || !auth.startsWith("Bearer ")) {
            throw new BizException("未登录");
        }
        Claims claims = jwtUtil.parseClaims(auth.substring(7));
        if (claims == null) {
            throw new BizException("登录已过期，请重新登录");
        }
        Long subject = Long.valueOf(claims.getSubject());
        String role = claims.get("role", String.class);
        String token = "admin".equals(role) ? jwtUtil.generateAdmin(subject) : jwtUtil.generate(subject);
        return ApiResponse.ok(Map.of("token", token, "userId", subject));
    }

    public record SmsCodeRequest(@NotBlank(message = "手机号不能为空") String phone) {
    }

    public record WxLoginRequest(String code) {
    }
}
