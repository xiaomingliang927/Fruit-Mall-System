package com.fruitmall.modules.auth;

import com.fruitmall.common.BizException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.locks.ReentrantLock;

/**
 * 短信验证码风控：生成/存储/校验 + 频率限制 + 试错上限。
 *
 * 设计要点：
 * - 内存存储（demo 期足够；生产接 Redis 即可水平扩展，接口不变）
 * - 同手机号 60s 内不可重发、1 小时内最多 10 次（防刷）
 * - 验证码 5 分钟过期，最多试错 5 次后作废（防爆破）
 * - 开发模式保留万能码 123456，方便联调；接入真实短信网关后关闭 dev-mode 即可
 */
@Slf4j
@Service
public class SmsCodeService {

    private final boolean devMode;
    private final long codeTtlSeconds;
    private final long resendSeconds;
    private final int maxPerHour;
    private final int maxAttempts;
    private final String devCode = "123456";

    private final Map<String, Entry> store = new ConcurrentHashMap<>();
    private final Map<String, ReentrantLock> locks = new ConcurrentHashMap<>();
    private final Map<String, List<Long>> sendLog = new ConcurrentHashMap<>();

    public SmsCodeService(@Value("${app.sms.dev-mode:true}") boolean devMode,
                          @Value("${app.sms.code-ttl-seconds:300}") long codeTtlSeconds,
                          @Value("${app.sms.resend-seconds:60}") long resendSeconds,
                          @Value("${app.sms.max-per-hour:10}") int maxPerHour,
                          @Value("${app.sms.max-attempts:5}") int maxAttempts) {
        this.devMode = devMode;
        this.codeTtlSeconds = codeTtlSeconds;
        this.resendSeconds = resendSeconds;
        this.maxPerHour = maxPerHour;
        this.maxAttempts = maxAttempts;
    }

    public record SendResult(String code, boolean devMode) {
    }

    /** 下发验证码（带风控）。开发模式会把验证码打到日志，方便前端/联调取用。 */
    public SendResult send(String phone, String clientIp) {
        validatePhone(phone);
        ReentrantLock lock = locks.computeIfAbsent(phone, k -> new ReentrantLock());
        lock.lock();
        try {
            long now = System.currentTimeMillis();

            Entry existing = store.get(phone);
            if (existing != null && now - existing.createdAt < resendSeconds * 1000L) {
                long waitSec = (resendSeconds * 1000L - (now - existing.createdAt)) / 1000L + 1;
                throw new BizException("验证码发送过于频繁，请 " + waitSec + " 秒后重试");
            }

            List<Long> sendTimes = sendLog.computeIfAbsent(phone, k -> new ArrayList<>());
            sendTimes.removeIf(t -> now - t > 3_600_000L);
            if (sendTimes.size() >= maxPerHour) {
                throw new BizException("该手机号发送次数过多，请稍后再试");
            }

            String code = generateCode();
            store.put(phone, new Entry(code, now, 0));
            sendTimes.add(now);

            if (devMode) {
                log.info("【DEV】短信验证码 phone={} code={} clientIp={}", phone, code, clientIp);
            }
            return new SendResult(code, devMode);
        } finally {
            lock.unlock();
        }
    }

    /** 校验验证码：通过即销毁；错误累加尝试次数；过期/超限直接作废。 */
    public void verify(String phone, String code) {
        if (code == null || code.isBlank()) {
            throw new BizException("请输入验证码");
        }
        if (devMode && devCode.equals(code)) {
            return; // 开发模式万能码
        }
        ReentrantLock lock = locks.computeIfAbsent(phone, k -> new ReentrantLock());
        lock.lock();
        try {
            Entry e = store.get(phone);
            if (e == null) {
                throw new BizException("验证码不存在或已失效，请重新获取");
            }
            if (System.currentTimeMillis() - e.createdAt > codeTtlSeconds * 1000L) {
                store.remove(phone);
                throw new BizException("验证码已过期，请重新获取");
            }
            if (e.attempts >= maxAttempts) {
                store.remove(phone);
                throw new BizException("验证码错误次数过多，请重新获取");
            }
            if (!e.code.equals(code)) {
                e.attempts++;
                throw new BizException("验证码错误");
            }
            store.remove(phone); // 使用成功即销毁，防重放
        } finally {
            lock.unlock();
        }
    }

    private void validatePhone(String phone) {
        if (phone == null || !phone.matches("^1[3-9]\\d{9}$")) {
            throw new BizException("手机号格式不正确");
        }
    }

    private String generateCode() {
        return String.format("%06d", ThreadLocalRandom.current().nextInt(1_000_000));
    }

    private static class Entry {
        final String code;
        final long createdAt;
        int attempts;

        Entry(String code, long createdAt, int attempts) {
            this.code = code;
            this.createdAt = createdAt;
            this.attempts = attempts;
        }
    }
}
