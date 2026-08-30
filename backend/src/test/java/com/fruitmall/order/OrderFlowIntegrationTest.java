package com.fruitmall.order;

import com.fruitmall.common.BizException;
import com.fruitmall.modules.order.Order;
import com.fruitmall.modules.order.OrderItem;
import com.fruitmall.modules.order.OrderItemMapper;
import com.fruitmall.modules.order.OrderMapper;
import com.fruitmall.modules.order.OrderService;
import com.fruitmall.modules.order.OrderStatus;
import com.fruitmall.modules.order.dto.CreateOrderRequest;
import com.fruitmall.modules.pay.Payment;
import com.fruitmall.modules.pay.PaymentMapper;
import com.fruitmall.modules.product.ProductSku;
import com.fruitmall.modules.product.ProductSkuMapper;
import com.fruitmall.modules.user.User;
import com.fruitmall.modules.user.UserAddress;
import com.fruitmall.modules.user.UserAddressMapper;
import com.fruitmall.modules.user.UserMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * 订单核心链路集成测试（H2 内存库，Flyway 建库 + 种子数据）。
 * 覆盖三条最值钱的不变量：下单锁库存、支付幂等、取消回补库存。
 */
@SpringBootTest
@ActiveProfiles("test")
class OrderFlowIntegrationTest {

    @Autowired OrderService orderService;
    @Autowired OrderMapper orderMapper;
    @Autowired OrderItemMapper orderItemMapper;
    @Autowired ProductSkuMapper skuMapper;
    @Autowired PaymentMapper paymentMapper;
    @Autowired UserMapper userMapper;
    @Autowired UserAddressMapper addressMapper;
    @Autowired TransactionTemplate transactionTemplate;

    Long userId;
    Long addressId;
    Long skuId = 11L; // V6 种子：麒麟西瓜 2990 分，库存 500

    @BeforeEach
    void setUp() {
        User user = new User();
        user.setPhone("13" + System.nanoTime() % 1000000000L);
        user.setNickname("测试用户");
        user.setLevel(1);
        user.setStatus(1);
        userMapper.insert(user);
        userId = user.getId();

        UserAddress address = new UserAddress();
        address.setUserId(userId);
        address.setReceiver("张三");
        address.setPhone("13800001111");
        address.setProvince("广东省");
        address.setCity("深圳市");
        address.setDistrict("南山区");
        address.setDetail("科技园南路88号");
        address.setIsDefault(true);
        addressMapper.insert(address);
        addressId = address.getId();
    }

    private ProductSku sku() {
        return skuMapper.selectById(skuId);
    }

    private String createOrder(int quantity) {
        return orderService.createOrder(userId,
                new CreateOrderRequest(addressId, List.of(new CreateOrderRequest.Item(skuId, quantity)), null, null));
    }

    @Test
    @DisplayName("下单锁定库存：创建订单后 SKU 库存按数量扣减，订单为待支付")
    void createOrder_locksStock() {
        int before = sku().getStock();
        String orderNo = createOrder(2);

        assertThat(sku().getStock()).isEqualTo(before - 2);
        Order order = orderMapper.selectOne(new LambdaQueryWrapper<Order>()
                .eq(Order::getOrderNo, orderNo));
        assertThat(order.getStatus()).isEqualTo(OrderStatus.PENDING_PAY);
        assertThat(order.getTotalAmount()).isEqualTo(2990 * 2);

        List<OrderItem> items = orderItemMapper.selectList(new LambdaQueryWrapper<OrderItem>()
                .eq(OrderItem::getOrderId, order.getId()));
        assertThat(items).hasSize(1);
        assertThat(items.get(0).getProductName()).isEqualTo("麒麟西瓜");
        assertThat(items.get(0).getPrice()).isEqualTo(2990);
    }

    @Test
    @DisplayName("支付回调幂等：重复回调只落一条支付流水，订单状态不被回退")
    void markPaid_isIdempotent() {
        String orderNo = createOrder(1);
        orderService.markPaid(orderNo, "TX-001", "MOCK");
        orderService.markPaid(orderNo, "TX-002", "MOCK"); // 模拟重复回调

        Order order = orderMapper.selectOne(new LambdaQueryWrapper<Order>()
                .eq(Order::getOrderNo, orderNo));
        assertThat(order.getStatus()).isEqualTo(OrderStatus.PENDING_SHIP);
        assertThat(order.getPaidAt()).isNotNull();

        Long paymentCount = paymentMapper.selectCount(new LambdaQueryWrapper<Payment>()
                .eq(Payment::getOrderNo, orderNo));
        assertThat(paymentCount).isEqualTo(1);
    }

    @Test
    @DisplayName("取消订单回补库存：库存恢复原值，订单转已取消")
    void cancelOrder_restoresStock() {
        int before = sku().getStock();
        String orderNo = createOrder(3);
        assertThat(sku().getStock()).isEqualTo(before - 3);

        orderService.cancelOrder(userId, orderNo);

        assertThat(sku().getStock()).isEqualTo(before);
        Order order = orderMapper.selectOne(new LambdaQueryWrapper<Order>()
                .eq(Order::getOrderNo, orderNo));
        assertThat(order.getStatus()).isEqualTo(OrderStatus.CANCELLED);
    }

    @Test
    @DisplayName("防超卖：库存不足时下单抛异常，事务回滚库存不变、订单不落库")
    void createOrder_oversell_rollsBack() {
        int before = sku().getStock();

        assertThatThrownBy(() -> createOrder(before + 1))
                .isInstanceOf(BizException.class)
                .hasMessageContaining("库存不足");

        assertThat(sku().getStock()).isEqualTo(before);
        Long orderCount = orderMapper.selectCount(new LambdaQueryWrapper<Order>()
                .eq(Order::getUserId, userId));
        assertThat(orderCount).isZero();
    }
}
