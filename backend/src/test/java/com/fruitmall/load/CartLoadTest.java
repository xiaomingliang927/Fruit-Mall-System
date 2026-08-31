package com.fruitmall.load;

import com.fruitmall.common.BizException;
import com.fruitmall.modules.cart.CartService;
import com.fruitmall.modules.cart.CartService.CartSummary;
import com.fruitmall.modules.cart.CartService.CartVO;
import com.fruitmall.modules.user.User;
import com.fruitmall.modules.user.UserMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.ArrayList;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 第一期压测（方案 A，纯 Java）——购物车读写混合并发。
 * 20 用户 × 20 轮（加购→查购物车→改数量→删除）× 4 操作 = 1600 次操作，
 * 每用户独立，无跨线程数据竞争；统计 TPS 与 p50/p95/p99。
 * 注意：跑在 H2 内存库 + 默认 HikariCP(10) 上，数字只作相对基线。
 */
@SpringBootTest
@ActiveProfiles("test")
class CartLoadTest {

    @Autowired CartService cartService;
    @Autowired UserMapper userMapper;

    private static final Long SKU_ID = 11L; // V6 种子：麒麟西瓜，库存 500
    private static final int THREADS = 20;
    private static final int ITERATIONS = 20;

    private final List<Long> userIds = new ArrayList<>();

    @BeforeEach
    void setUp() {
        userIds.clear();
        long base = System.nanoTime() % 100_000_000L;
        for (int i = 0; i < THREADS; i++) {
            User u = new User();
            u.setPhone("136" + String.format("%08d", base + i));
            u.setNickname("压测用户");
            u.setLevel(1);
            u.setStatus(1);
            userMapper.insert(u);
            userIds.add(u.getId());
        }
    }

    @Test
    @DisplayName("购物车读写混合并发：1600 次操作零失败，输出 TPS/p50/p95/p99")
    void cartReadWriteMix() throws Exception {
        int expectedOps = THREADS * ITERATIONS * 4;
        CountDownLatch start = new CountDownLatch(1);
        CountDownLatch done = new CountDownLatch(THREADS);
        Queue<Long> latencies = new ConcurrentLinkedQueue<>();
        AtomicInteger errors = new AtomicInteger();

        long t0 = System.nanoTime();
        for (int t = 0; t < THREADS; t++) {
            final int idx = t;
            new Thread(() -> {
                try {
                    start.await();
                    Long userId = userIds.get(idx);
                    for (int it = 0; it < ITERATIONS && errors.get() == 0; it++) {
                        long s = System.nanoTime();
                        cartService.addItem(userId, SKU_ID, 1);
                        latencies.add(System.nanoTime() - s);

                        s = System.nanoTime();
                        CartSummary summary = cartService.myCart(userId);
                        latencies.add(System.nanoTime() - s);
                        CartVO vo = summary.items().get(0); // 本轮刚加购，必存在

                        s = System.nanoTime();
                        cartService.updateItem(userId, vo.itemId(), 2, true);
                        latencies.add(System.nanoTime() - s);

                        s = System.nanoTime();
                        cartService.removeItem(userId, vo.itemId());
                        latencies.add(System.nanoTime() - s);
                    }
                } catch (BizException e) {
                    errors.incrementAndGet();
                } catch (Exception e) {
                    errors.incrementAndGet();
                } finally {
                    done.countDown();
                }
            }).start();
        }

        start.countDown();
        assertTrue(done.await(120, TimeUnit.SECONDS), "购物车压测超时");
        long wallMs = (System.nanoTime() - t0) / 1_000_000;

        assertThat(errors.get()).as("业务异常/未知异常数应为 0").isZero();
        assertThat(latencies).as("操作数应为 %d", expectedOps).hasSize(expectedOps);

        System.out.printf("[CART LOAD] threads=%d ops=%d wall=%dms tps=%.0f avg=%.2fms p50=%.2fms p95=%.2fms p99=%.2fms errors=%d%n",
                THREADS, latencies.size(), wallMs,
                latencies.size() * 1000.0 / wallMs,
                avgMs(latencies), pctMs(latencies, 0.50), pctMs(latencies, 0.95), pctMs(latencies, 0.99),
                errors.get());
    }

    private static double avgMs(Queue<Long> q) {
        long sum = 0;
        for (long v : q) sum += v;
        return sum / 1_000_000.0 / q.size();
    }

    private static double pctMs(Queue<Long> q, double p) {
        List<Long> sorted = new ArrayList<>(q);
        sorted.sort(Long::compareTo);
        int i = (int) Math.ceil(sorted.size() * p) - 1;
        return sorted.get(Math.max(0, Math.min(i, sorted.size() - 1))) / 1_000_000.0;
    }
}
