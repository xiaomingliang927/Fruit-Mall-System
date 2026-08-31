package com.fruitmall.load;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fruitmall.common.BizException;
import com.fruitmall.modules.order.OrderItem;
import com.fruitmall.modules.order.OrderItemMapper;
import com.fruitmall.modules.order.OrderService;
import com.fruitmall.modules.order.dto.CreateOrderRequest;
import com.fruitmall.modules.product.Product;
import com.fruitmall.modules.product.ProductMapper;
import com.fruitmall.modules.product.ProductSku;
import com.fruitmall.modules.product.ProductSkuMapper;
import com.fruitmall.modules.user.User;
import com.fruitmall.modules.user.UserAddress;
import com.fruitmall.modules.user.UserAddressMapper;
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
 * 第一期压测（方案 A，纯 Java）——下单关键路径。
 * ① throughput：大库存（10000）下 20 用户 × 10 单 = 200 单全成功，统计 TPS/p95；
 * ② oversell：走完整 createOrder 事务链路复刻防超卖（100 并发抢 50 库存），
 *    断言成功数 = 库存、库存归零、订单明细恰 50 条、零未知异常。
 * 比 OversellConcurrencyTest 更强：那只是 mapper 层单条 UPDATE，这里是完整下单事务。
 */
@SpringBootTest
@ActiveProfiles("test")
class OrderCreateLoadTest {

    @Autowired OrderService orderService;
    @Autowired ProductMapper productMapper;
    @Autowired ProductSkuMapper skuMapper;
    @Autowired UserMapper userMapper;
    @Autowired UserAddressMapper addressMapper;
    @Autowired OrderItemMapper orderItemMapper;

    private Long skuId;
    private final List<Long> userIds = new ArrayList<>();
    private final List<Long> addressIds = new ArrayList<>();

    @BeforeEach
    void setUp() {
        Product p = new Product();
        p.setName("压测下单商品");
        p.setCategoryId(1L);
        p.setStatus(1);
        p.setMainImage("/x.jpg");
        productMapper.insert(p);

        ProductSku sku = new ProductSku();
        sku.setProductId(p.getId());
        sku.setSpec("默认");
        sku.setPrice(100);
        sku.setStatus(1);
        skuMapper.insert(sku);
        skuId = sku.getId();
    }

    private void addUserWithAddress(int i, long phoneBase) {
        User u = new User();
        u.setPhone("137" + String.format("%08d", phoneBase + i));
        u.setNickname("压测用户");
        u.setLevel(1);
        u.setStatus(1);
        userMapper.insert(u);
        userIds.add(u.getId());

        UserAddress a = new UserAddress();
        a.setUserId(u.getId());
        a.setReceiver("压测张三");
        a.setPhone("13800001111");
        a.setProvince("广东省");
        a.setCity("深圳市");
        a.setDistrict("南山区");
        a.setDetail("压测地址 1 号");
        a.setIsDefault(true);
        addressMapper.insert(a);
        addressIds.add(a.getId());
    }

    @Test
    @DisplayName("下单吞吐：库存 10000，20 并发 × 10 单 = 200 单全成功")
    void createOrderThroughput() throws Exception {
        final int threads = 20;
        final int ordersPerThread = 10;
        resetStock(10000);

        long base = System.nanoTime() % 100_000_000L;
        for (int i = 0; i < threads; i++) {
            addUserWithAddress(i, base);
        }

        int expected = threads * ordersPerThread;
        CountDownLatch start = new CountDownLatch(1);
        CountDownLatch done = new CountDownLatch(threads);
        Queue<Long> latencies = new ConcurrentLinkedQueue<>();
        ConcurrentLinkedQueue<String> unexpectedMsgs = new ConcurrentLinkedQueue<>();
        AtomicInteger success = new AtomicInteger();
        AtomicInteger unexpected = new AtomicInteger();

        long t0 = System.nanoTime();
        for (int t = 0; t < threads; t++) {
            final int idx = t;
            new Thread(() -> {
                try {
                    start.await();
                    for (int k = 0; k < ordersPerThread; k++) {
                        long s = System.nanoTime();
                        try {
                            orderService.createOrder(userIds.get(idx),
                                    new CreateOrderRequest(addressIds.get(idx),
                                            List.of(new CreateOrderRequest.Item(skuId, 1)), null, null));
                            success.incrementAndGet();
                        } catch (BizException e) {
                            unexpected.incrementAndGet();
                            unexpectedMsgs.add(e.getMessage());
                        }
                        latencies.add(System.nanoTime() - s);
                    }
                } catch (Exception e) {
                    unexpected.incrementAndGet();
                    unexpectedMsgs.add(e.getClass().getSimpleName() + ": " + e.getMessage());
                } finally {
                    done.countDown();
                }
            }).start();
        }

        start.countDown();
        assertTrue(done.await(120, TimeUnit.SECONDS), "下单吞吐压测超时");
        long wallMs = (System.nanoTime() - t0) / 1_000_000;

        assertThat(unexpectedMsgs).as("不应出现业务失败").isEmpty();
        assertThat(success.get()).isEqualTo(expected);
        assertThat(skuMapper.selectById(skuId).getStock()).isEqualTo(10000 - expected);

        System.out.printf("[ORDER LOAD throughput] threads=%d orders=%d wall=%dms tps=%.0f avg=%.2fms p50=%.2fms p95=%.2fms p99=%.2fms failures=%d%n",
                threads, success.get(), wallMs,
                success.get() * 1000.0 / wallMs,
                avgMs(latencies), pctMs(latencies, 0.50), pctMs(latencies, 0.95), pctMs(latencies, 0.99),
                unexpected.get());
    }

    @Test
    @DisplayName("防超卖（完整下单链路）：100 并发抢 50 库存，恰好 50 成功、库存归零、明细恰 50 条")
    void createOrderNoOversellFullPath() throws Exception {
        final int threads = 100;
        final int stock = 50;
        resetStock(stock);

        long base = System.nanoTime() % 100_000_000L;
        for (int i = 0; i < threads; i++) {
            addUserWithAddress(i, base);
        }

        CountDownLatch start = new CountDownLatch(1);
        CountDownLatch done = new CountDownLatch(threads);
        Queue<Long> latencies = new ConcurrentLinkedQueue<>();
        ConcurrentLinkedQueue<String> unexpectedMsgs = new ConcurrentLinkedQueue<>();
        AtomicInteger success = new AtomicInteger();
        AtomicInteger stockShortage = new AtomicInteger();
        AtomicInteger unexpected = new AtomicInteger();

        long t0 = System.nanoTime();
        for (int t = 0; t < threads; t++) {
            final int idx = t;
            new Thread(() -> {
                try {
                    start.await();
                    long s = System.nanoTime();
                    try {
                        orderService.createOrder(userIds.get(idx),
                                new CreateOrderRequest(addressIds.get(idx),
                                        List.of(new CreateOrderRequest.Item(skuId, 1)), null, null));
                        success.incrementAndGet();
                    } catch (BizException e) {
                        if (e.getMessage().contains("库存不足")) {
                            stockShortage.incrementAndGet();
                        } else {
                            unexpected.incrementAndGet();
                            unexpectedMsgs.add(e.getMessage());
                        }
                    }
                    latencies.add(System.nanoTime() - s);
                } catch (Exception e) {
                    unexpected.incrementAndGet();
                    unexpectedMsgs.add(e.getClass().getSimpleName() + ": " + e.getMessage());
                } finally {
                    done.countDown();
                }
            }).start();
        }

        start.countDown();
        assertTrue(done.await(120, TimeUnit.SECONDS), "防超卖压测超时");
        long wallMs = (System.nanoTime() - t0) / 1_000_000;

        assertThat(unexpectedMsgs).as("未知异常数应为 0").isEmpty();
        assertThat(success.get()).as("成功数必须恰好等于初始库存（不超卖）").isEqualTo(stock);
        assertThat(stockShortage.get()).as("库存不足拦截数 = 并发数 - 库存").isEqualTo(threads - stock);
        assertThat(skuMapper.selectById(skuId).getStock()).isZero();
        Long itemCount = orderItemMapper.selectCount(new LambdaQueryWrapper<OrderItem>()
                .eq(OrderItem::getSkuId, skuId));
        assertThat(itemCount).as("订单明细必须恰好 %d 条（防重复落单）", stock).isEqualTo(stock);

        System.out.printf("[ORDER LOAD oversell] threads=%d stock=%d success=%d shortage=%d wall=%dms tps=%.0f avg=%.2fms p50=%.2fms p95=%.2fms p99=%.2fms unexpected=%d%n",
                threads, stock, success.get(), stockShortage.get(), wallMs,
                success.get() * 1000.0 / wallMs,
                avgMs(latencies), pctMs(latencies, 0.50), pctMs(latencies, 0.95), pctMs(latencies, 0.99),
                unexpected.get());
    }

    private void resetStock(int stock) {
        ProductSku sku = skuMapper.selectById(skuId);
        sku.setStock(stock);
        skuMapper.updateById(sku);
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
