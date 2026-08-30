package com.fruitmall.admin;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fruitmall.auth.JwtUtil;
import com.fruitmall.common.BizException;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.HexFormat;

@Service
@RequiredArgsConstructor
public class AdminService implements ApplicationRunner {

    private final AdminUserMapper adminUserMapper;
    private final JwtUtil jwtUtil;

    public record LoginResponse(String token, Long adminId, String username, String realName) {
    }

    public LoginResponse login(String username, String password) {
        AdminUser admin = adminUserMapper.selectOne(new LambdaQueryWrapper<AdminUser>()
                .eq(AdminUser::getUsername, username));
        if (admin == null || !verify(password, admin.getSalt(), admin.getPasswordHash())) {
            throw new BizException("用户名或密码错误");
        }
        if (admin.getStatus() == null || admin.getStatus() == 0) {
            throw new BizException("账号已被禁用");
        }
        return new LoginResponse(jwtUtil.generateAdmin(admin.getId()),
                admin.getId(), admin.getUsername(), admin.getRealName());
    }

    /** 首次启动自动创建默认管理员 admin / admin123（生产环境务必首登改密） */
    @Override
    public void run(ApplicationArguments args) {
        if (adminUserMapper.selectCount(null) > 0) {
            return;
        }
        AdminUser admin = new AdminUser();
        admin.setUsername("admin");
        admin.setRealName("超级管理员");
        admin.setStatus(1);
        String salt = randomSalt();
        admin.setSalt(salt);
        admin.setPasswordHash(sha256(salt + "admin123"));
        adminUserMapper.insert(admin);
    }

    private boolean verify(String rawPassword, String salt, String expectedHash) {
        return sha256(salt + rawPassword).equals(expectedHash);
    }

    private String randomSalt() {
        byte[] bytes = new byte[16];
        new SecureRandom().nextBytes(bytes);
        return HexFormat.of().formatHex(bytes);
    }

    private String sha256(String text) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(text.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 不可用", e);
        }
    }
}
