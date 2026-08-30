package com.fruitmall.auth;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fruitmall.modules.auth.AuthService;
import com.fruitmall.modules.auth.dto.LoginRequest;
import com.fruitmall.modules.user.User;
import com.fruitmall.modules.user.UserMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

/** 登录链路集成测试：短信验证码登录 + 微信开发模式登录 */
@SpringBootTest
@ActiveProfiles("test")
class AuthFlowIntegrationTest {

    @Autowired AuthService authService;
    @Autowired UserMapper userMapper;
    @Autowired JwtUtil jwtUtil;

    @Test
    @DisplayName("短信登录：新手机号自动注册并签发可解析的 token")
    void smsLogin_autoRegister() {
        String phone = "139" + String.valueOf(System.nanoTime()).substring(0, 8);
        AuthService.LoginResponse resp = authService.loginBySms(new LoginRequest(phone, "123456"));

        assertThat(resp.token()).isNotBlank();
        assertThat(jwtUtil.parseUserId(resp.token())).isEqualTo(resp.userId());

        User user = userMapper.selectById(resp.userId());
        assertThat(user.getPhone()).isEqualTo(phone);
        assertThat(user.getStatus()).isEqualTo(1);
    }

    @Test
    @DisplayName("微信开发模式登录：同一 code 两次登录是同一账号，不重复建户")
    void wxLogin_devMode_sameOpenidSameUser() {
        String code = "wx-code-" + System.nanoTime();
        AuthService.LoginResponse first = authService.loginByWechat(code);
        AuthService.LoginResponse second = authService.loginByWechat(code);

        assertThat(second.userId()).isEqualTo(first.userId());

        User user = userMapper.selectOne(new LambdaQueryWrapper<User>()
                .eq(User::getOpenid, "dev-" + code));
        assertThat(user).isNotNull();
        assertThat(user.getOpenid()).isEqualTo("dev-" + code);
    }
}
