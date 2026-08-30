package com.fruitmall.coupon;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fruitmall.common.BizException;
import com.fruitmall.modules.coupon.CouponService;
import com.fruitmall.modules.coupon.UserCoupon;
import com.fruitmall.modules.coupon.UserCouponMapper;
import com.fruitmall.modules.order.Order;
import com.fruitmall.modules.order.OrderMapper;
import com.fruitmall.modules.order.OrderService;
import com.fruitmall.modules.order.dto.CreateOrderRequest;
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

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** 优惠券集成测试：领取/限领/抢完/门槛/下单抵扣/取消退回 */
@SpringBootTest
@ActiveProfiles("test")
class CouponFlowIntegrationTest {

    @Autowired CouponService couponService;
    @Autowired OrderService orderService;
    @Autowired OrderMapper orderMapper;
    @Autowired UserCouponMapper userCouponMapper;
    @Autowired UserMapper userMapper;
    @Autowired UserAddressMapper addressMapper;

    Long userId;
    Long addressId;
    Long skuId = 11L; // 麒麟西瓜 2990 分

    @BeforeEach
    void setUp() {
        User user = new User();
        user.setPhone("13" + System.nanoTime() % 1000000000L);
        user.setNickname("券测试");
        user.setLevel(1);
        user.setStatus(1);
        userMapper.insert(user);
        userId = user.getId();

        UserAddress address = new UserAddress();
        address.setUserId(userId);
        address.setReceiver("王五");
        address.setPhone("13700003333");
        address.setProvince("广东省");
        address.setCity("深圳市");
        address.setDistrict("南山区");
        address.setDetail("科技园南路88号");
        address.setIsDefault(true);
        addressMapper.insert(address);
        addressId = address.getId();
    }

    private Long mkCoupon(String name, int type, int threshold, int discountAmount,
                          Integer percent, int total, int perUser) {
        return couponService.create(new CouponService.CouponInput(
                name, type, threshold, discountAmount, percent, total, perUser,
                LocalDateTime.now().minusHours(1), LocalDateTime.now().plusDays(7), 30));
    }

    @Test
    @DisplayName("领取：写入用户券，30 天有效期，发放量 +1")
    void receive_ok() {
        Long cid = mkCoupon("满100减30", CouponService_1.TYPE_REDUCE, 10000, 3000, null, 100, 1);
        couponService.receive(userId, cid);

        List<CouponService.MyCouponVO> mine = couponService.myCoupons(userId, UserCoupon.STATUS_UNUSED);
        assertThat(mine).hasSize(1);
        assertThat(mine.get(0).name()).isEqualTo("满100减30");
        assertThat(mine.get(0).expireAt()).isAfter(LocalDateTime.now().plusDays(29));

        var c = couponService.adminPage(null, 1, 10).getRecords().stream()
                .filter(v -> v.id().equals(cid)).findFirst().orElseThrow();
        assertThat(c.remaining()).isEqualTo(99);
    }

    @Test
    @DisplayName("每人限领：超过限领数量被拒")
    void receive_perUserLimit() {
        Long cid = mkCoupon("限领一张", CouponService_1.TYPE_REDUCE, 0, 500, null, 100, 1);
        couponService.receive(userId, cid);
        assertThatThrownBy(() -> couponService.receive(userId, cid))
                .isInstanceOf(BizException.class)
                .hasMessageContaining("限领");
    }

    @Test
    @DisplayName("抢完：总量 1 张，第二个用户领取失败")
    void receive_soldOut() {
        Long cid = mkCoupon("仅一张", CouponService_1.TYPE_REDUCE, 0, 500, null, 1, 5);
        couponService.receive(userId, cid);

        User other = new User();
        other.setPhone("13" + System.nanoTime() % 1000000000L);
        other.setNickname("第二个");
        other.setLevel(1);
        other.setStatus(1);
        userMapper.insert(other);

        assertThatThrownBy(() -> couponService.receive(other.getId(), cid))
                .isInstanceOf(BizException.class)
                .hasMessageContaining("抢完");
    }

    @Test
    @DisplayName("下单抵扣：满100减30，应付 = 总额 - 30")
    void order_withCouponDiscount() {
        Long cid = mkCoupon("满100减30", CouponService_1.TYPE_REDUCE, 10000, 3000, null, 100, 5);
        CouponService.MyCouponVO my = couponService.receive(userId, cid);

        String orderNo = orderService.createOrder(userId,
                new CreateOrderRequest(addressId,
                        List.of(new CreateOrderRequest.Item(skuId, 7)), // 2990×7 = 20930 > 10000 门槛
                        null, my.userCouponId()));

        Order order = orderMapper.selectOne(new LambdaQueryWrapper<Order>()
                .eq(Order::getOrderNo, orderNo));
        assertThat(order.getTotalAmount()).isEqualTo(2990 * 7);
        assertThat(order.getPayAmount()).isEqualTo(2990 * 7 - 3000);
        assertThat(order.getUserCouponId()).isEqualTo(my.userCouponId());

        // 券已核销
        UserCoupon uc = userCouponMapper.selectById(my.userCouponId());
        assertThat(uc.getStatus()).isEqualTo(UserCoupon.STATUS_USED);
        assertThat(uc.getOrderNo()).isEqualTo(orderNo);
    }

    @Test
    @DisplayName("门槛校验：未满足满减门槛被拒")
    void order_threshold() {
        Long cid = mkCoupon("满100减30", CouponService_1.TYPE_REDUCE, 10000, 3000, null, 100, 5);
        CouponService.MyCouponVO my = couponService.receive(userId, cid);

        assertThatThrownBy(() -> orderService.createOrder(userId,
                new CreateOrderRequest(addressId,
                        List.of(new CreateOrderRequest.Item(skuId, 1)), // 2990 < 10000
                        null, my.userCouponId())))
                .isInstanceOf(BizException.class)
                .hasMessageContaining("门槛");
    }

    @Test
    @DisplayName("折扣券：9 折按比例抵扣")
    void discountCoupon() {
        Long cid = mkCoupon("整单9折", CouponService_1.TYPE_DISCOUNT, 0, 0, 90, 100, 5);
        CouponService.MyCouponVO my = couponService.receive(userId, cid);

        String orderNo = orderService.createOrder(userId,
                new CreateOrderRequest(addressId,
                        List.of(new CreateOrderRequest.Item(skuId, 10)), // 29900
                        null, my.userCouponId()));

        Order order = orderMapper.selectOne(new LambdaQueryWrapper<Order>()
                .eq(Order::getOrderNo, orderNo));
        assertThat(order.getPayAmount()).isEqualTo(29900 * 90 / 100);
    }

    @Test
    @DisplayName("取消订单：券退回为未使用，可再次下单使用")
    void cancelOrder_returnsCoupon() {
        Long cid = mkCoupon("满100减30", CouponService_1.TYPE_REDUCE, 10000, 3000, null, 100, 5);
        CouponService.MyCouponVO my = couponService.receive(userId, cid);

        String orderNo = orderService.createOrder(userId,
                new CreateOrderRequest(addressId,
                        List.of(new CreateOrderRequest.Item(skuId, 7)), null, my.userCouponId()));
        orderService.cancelOrder(userId, orderNo);

        UserCoupon uc = userCouponMapper.selectById(my.userCouponId());
        assertThat(uc.getStatus()).isEqualTo(UserCoupon.STATUS_UNUSED);
        assertThat(uc.getOrderNo()).isNull();
    }

    /** 引用常量的辅助类型（避免静态导入过长） */
    interface CouponService_1 {
        int TYPE_REDUCE = 1;
        int TYPE_DISCOUNT = 2;
    }
}
