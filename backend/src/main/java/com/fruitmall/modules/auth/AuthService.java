package com.fruitmall.modules.auth;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fruitmall.auth.JwtUtil;
import com.fruitmall.common.BizException;
import com.fruitmall.modules.auth.dto.LoginRequest;
import com.fruitmall.modules.user.User;
import com.fruitmall.modules.user.UserMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserMapper userMapper;
    private final JwtUtil jwtUtil;
    private final ObjectMapper objectMapper;
    private final SmsCodeService smsCodeService;

    @Value("${app.wechat.appid:}")
    private String wxAppid;

    @Value("${app.wechat.secret:}")
    private String wxSecret;

    /** 下发短信验证码（带频率限制/试错上限），供前端「获取验证码」按钮调用 */
    public SmsCodeService.SendResult sendSmsCode(String phone, String clientIp) {
        return smsCodeService.send(phone, clientIp);
    }

    public LoginResponse loginBySms(LoginRequest request) {
        smsCodeService.verify(request.phone(), request.code());
        User user = userMapper.selectOne(new LambdaQueryWrapper<User>()
                .eq(User::getPhone, request.phone()));
        if (user == null) {
            user = new User();
            user.setPhone(request.phone());
            user.setNickname("果友" + request.phone().substring(request.phone().length() - 4));
            user.setLevel(1);
            user.setStatus(1);
            userMapper.insert(user);
        }
        return checkAndIssue(user);
    }

    /**
     * 小程序微信登录：wx.login 的 code 换 openid 后自动注册/登录。
     * 未配置 appid/secret 时进入开发模式（openid = dev-<code>），方便本地联调。
     * TODO(M2): 引导用户 getPhoneNumber 快捷绑定手机号。
     */
    public LoginResponse loginByWechat(String code) {
        if (code == null || code.isBlank()) {
            throw new BizException("code 不能为空");
        }
        String openid = resolveOpenid(code);
        User user = userMapper.selectOne(new LambdaQueryWrapper<User>()
                .eq(User::getOpenid, openid));
        if (user == null) {
            user = new User();
            user.setOpenid(openid);
            user.setPhone("wx_" + Math.abs(openid.hashCode()) % 1000000000);
            user.setNickname("果友" + ThreadLocalRandom.current().nextInt(1000, 9999));
            user.setLevel(1);
            user.setStatus(1);
            userMapper.insert(user);
        }
        return checkAndIssue(user);
    }

    /** 已配置密钥则调微信 code2Session，否则开发模式直接派生 openid */
    private String resolveOpenid(String code) {
        if (wxSecret == null || wxSecret.isBlank() || wxAppid == null || wxAppid.isBlank()) {
            return "dev-" + code;
        }
        try {
            HttpClient client = HttpClient.newBuilder()
                    .connectTimeout(Duration.ofSeconds(5)).build();
            String url = "https://api.weixin.qq.com/sns/jscode2session?appid="
                    + URLEncoder.encode(wxAppid, StandardCharsets.UTF_8)
                    + "&secret=" + URLEncoder.encode(wxSecret, StandardCharsets.UTF_8)
                    + "&js_code=" + URLEncoder.encode(code, StandardCharsets.UTF_8)
                    + "&grant_type=authorization_code";
            HttpResponse<String> resp = client.send(HttpRequest.newBuilder(URI.create(url)).GET().build(),
                    HttpResponse.BodyHandlers.ofString());
            Map<String, Object> body = objectMapper.readValue(resp.body(), Map.class);
            if (body.get("openid") == null) {
                log.error("code2Session 失败: {}", resp.body());
                throw new BizException("微信登录失败，请稍后重试");
            }
            return String.valueOf(body.get("openid"));
        } catch (BizException e) {
            throw e;
        } catch (Exception e) {
            log.error("code2Session 请求异常", e);
            throw new BizException("微信登录失败，请稍后重试");
        }
    }

    private LoginResponse checkAndIssue(User user) {
        if (user.getStatus() == null || user.getStatus() == 0) {
            throw new BizException("账号已被禁用，请联系客服");
        }
        return new LoginResponse(jwtUtil.generate(user.getId()),
                user.getId(), user.getNickname(), user.getAvatar());
    }

    public record LoginResponse(String token, Long userId, String nickname, String avatar) {
    }
}
