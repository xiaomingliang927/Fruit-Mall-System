package com.fruitmall.seckill;

import com.fruitmall.common.BizException;
import com.fruitmall.modules.order.Order;
import com.fruitmall.modules.order.OrderMapper;
import com.fruitmall.modules.product.ProductSku;
import com.fruitmall.modules.product.ProductSkuMapper;
import com.fruitmall.modules.seckill.SeckillActivity;
import com.fruitmall.modules.seckill.SeckillActivityMapper;
import com.fruitmall.modules.seckill.SeckillOrder;
import com.fruitmall.modules.seckill.SeckillOrderMapper;
import com.fruitmall.modules.seckill.SeckillService;
import com.fruitmall.modules.user.User;
import com.fruitmall.modules.user.UserMapper;
import com.fruitmall.modules.user.UserAddress;
import com.fruitmall.modules.user.UserAddressMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * M3 秒杀集成测试：抢购成功链路、每人限购（唯一键拦截）、时间窗拦截、
 * 并发防超卖（20 人抢 5 件，恰好售罄、绝不超发）。
 */
@SpringBootTest
@ActiveProfiles("test")
class SeckillIntegrationTest {

    @Autowired SeckillService seckillService;
    @Autowired SeckillActivityMapper activityMapper;
    @Autowired SeckillOrderMapper seckillOrderMapper;
    @Autowired UserMapper userMapper;
    @Autowired UserAddressMapper addressMapper;
    @Autowired ProductSkuMapper skuMapper;
    @Autowired OrderMapper orderMapper;

    private Long newUser() {
        User u = new User();
        u.setPhone("13" + String.format("%09d", Math.abs(System.nanoTime()) % 1_000_000_000L));
        u.setNickname("秒杀测试");
        u.setLevel(1);
        u.setStatus(1);
        userMapper.insert(u);
        UserAddress a = new UserAddress();
        a.setUserId(u.getId());
        a.setReceiver("测试收货人");
        a.setPhone(u.getPhone());
        a.setProvince("广东省");
        a.setCity("深圳市");
        a.setDistrict("南山区");
        a.setDetail("科技园路1号");
        a.setIsDefault(true);
        addressMapper.insert(a);
        return u.getId();
    }

    private Long newActivity(int totalStock, LocalDateTime start, LocalDateTime end) {
        ProductSku sku = skuMapper.selectList(null).get(0);
        return seckillService.create(new SeckillService.SeckillInput(
                sku.getId(), Math.max(sku.getPrice() / 2, 1), totalStock, 1, start, end, 1));
    }

    @Test
    @DisplayName("抢购成功：订单按秒杀价生成，秒杀与商品库存同步扣减")
    void buy_success() {
        Long userId = newUser();
        ProductSku sku = skuMapper.selectList(null).get(0);
        Long activityId = newActivity(10, LocalDateTime.now().minusMinutes(1), LocalDateTime.now().plusHours(1));
        int stockBefore = skuMapper.selectById(sku.getId()).getStock();

        String orderNo = seckillService.buy(userId, activityId, addressOf(userId), null);

        Order order = orderMapper.selectOne(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<Order>()
                        .eq(Order::getOrderNo, orderNo));
        assertThat(order).isNotNull();
        assertThat(order.getPayAmount()).isEqualTo(Math.max(sku.getPrice() / 2, 1));
        assertThat(activityMapper.selectById(activityId).getAvailableStock()).isEqualTo(9);
        assertThat(skuMapper.selectById(sku.getId()).getStock()).isEqualTo(stockBefore - 1);
    }

    @Test
    @DisplayName("每人限购：同一用户二次抢购被唯一键拦截")
    void buy_rejectSecondAttempt() {
        Long userId = newUser();
        Long activityId = newActivity(10, LocalDateTime.now().minusMinutes(1), LocalDateTime.now().plusHours(1));

        seckillService.buy(userId, activityId, addressOf(userId), null);

        assertThatThrownBy(() -> seckillService.buy(userId, activityId, addressOf(userId), null))
                .isInstanceOf(BizException.class)
                .hasMessageContaining("每人限购");
    }

    @Test
    @DisplayName("时间窗：活动未开始/已结束都抢不了")
    void buy_rejectOutsideWindow() {
        Long userId = newUser();
        Long future = newActivity(10, LocalDateTime.now().plusHours(1), LocalDateTime.now().plusHours(2));
        assertThatThrownBy(() -> seckillService.buy(userId, future, addressOf(userId), null))
                .isInstanceOf(BizException.class);

        Long past = newActivity(10, LocalDateTime.now().minusHours(2), LocalDateTime.now().minusHours(1));
        assertThatThrownBy(() -> seckillService.buy(userId, past, addressOf(userId), null))
                .isInstanceOf(BizException.class);
    }

    @Test
    @DisplayName("并发防超卖：20 用户抢 5 件，恰好 5 单成功且秒杀库存归零")
    void buy_concurrency_noOversell() throws Exception {
        int users = 20, stock = 5;
        List<Long> userIds = new java.util.ArrayList<>();
        for (int i = 0; i < users; i++) userIds.add(newUser());
        Long activityId = newActivity(stock, LocalDateTime.now().minusMinutes(1), LocalDateTime.now().plusHours(1));

        ExecutorService pool = Executors.newFixedThreadPool(users);
        CountDownLatch ready = new CountDownLatch(users);
        CountDownLatch done = new CountDownLatch(users);
        AtomicInteger success = new AtomicInteger();
        for (Long uid : userIds) {
            pool.submit(() -> {
                ready.countDown();
                try {
                    ready.await(5, TimeUnit.SECONDS); // 尽量同时开抢
                    seckillService.buy(uid, activityId, addressOf(uid), null);
                    success.incrementAndGet();
                } catch (Exception ignored) {
                    // 抢失败（限购/售罄）计入失败侧
                } finally {
                    done.countDown();
                }
            });
        }
        assertThat(done.await(30, TimeUnit.SECONDS)).isTrue();
        pool.shutdown();

        assertThat(success.get()).isEqualTo(stock);
        assertThat(activityMapper.selectById(activityId).getAvailableStock()).isZero();
        assertThat(seckillOrderMapper.selectCount(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<SeckillOrder>()
                        .eq(SeckillOrder::getSeckillId, activityId))).isEqualTo(stock);
    }

    private Long addressOf(Long userId) {
        return addressMapper.selectList(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<UserAddress>()
                        .eq(UserAddress::getUserId, userId)).get(0).getId();
    }
}
