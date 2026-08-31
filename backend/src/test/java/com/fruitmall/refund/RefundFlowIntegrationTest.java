package com.fruitmall.refund;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fruitmall.common.BizException;
import com.fruitmall.modules.order.Order;
import com.fruitmall.modules.order.OrderMapper;
import com.fruitmall.modules.order.OrderService;
import com.fruitmall.modules.order.OrderStatus;
import com.fruitmall.modules.order.dto.CreateOrderRequest;
import com.fruitmall.modules.product.ProductSku;
import com.fruitmall.modules.product.ProductSkuMapper;
import com.fruitmall.modules.refund.Refund;
import com.fruitmall.modules.refund.RefundMapper;
import com.fruitmall.modules.refund.RefundService;
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

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** 售后退款集成测试：自动秒审 / 人工审核 / 驳回恢复 / 撤销 / 重复拦截 */
@SpringBootTest
@ActiveProfiles("test")
class RefundFlowIntegrationTest {

    @Autowired RefundService refundService;
    @Autowired OrderService orderService;
    @Autowired OrderMapper orderMapper;
    @Autowired RefundMapper refundMapper;
    @Autowired ProductSkuMapper skuMapper;
    @Autowired UserMapper userMapper;
    @Autowired UserAddressMapper addressMapper;

    Long userId;
    Long addressId;
    Long skuId = 11L; // V6 种子：麒麟西瓜 2990 分，库存 500

    @BeforeEach
    void setUp() {
        User user = new User();
        user.setPhone("13" + System.nanoTime() % 1000000000L);
        user.setNickname("售后测试");
        user.setLevel(1);
        user.setStatus(1);
        userMapper.insert(user);
        userId = user.getId();

        UserAddress address = new UserAddress();
        address.setUserId(userId);
        address.setReceiver("李四");
        address.setPhone("13900002222");
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

    /** 创建已支付订单（待发货，可申请售后） */
    private String paidOrder(int qty) {
        String orderNo = orderService.createOrder(userId,
                new CreateOrderRequest(addressId, List.of(new CreateOrderRequest.Item(skuId, qty)), null, null));
        orderService.markPaid(orderNo, "TX-" + orderNo, "MOCK");
        return orderNo;
    }

    private String apply(String orderNo, int type) {
        return refundService.apply(userId, orderNo, type, "坏果包赔测试", null).refundNo();
    }

    @Test
    @DisplayName("凭证图闭环：上传后的凭证 URL 落库，并在用户端/管理端列表原样返回")
    void imagesRoundTrip() {
        String orderNo = paidOrder(2); // >50 元保持「待审核」
        String imagesJson = "[\"/uploads/2026/08/a.jpg\",\"/uploads/2026/08/b.jpg\"]";
        String refundNo = refundService
                .apply(userId, orderNo, Refund.TYPE_ONLY_REFUND, "坏果拍照为证", imagesJson).refundNo();

        // 落库
        assertThat(refundOf(refundNo).getImages()).isEqualTo(imagesJson);

        // 用户端详情
        assertThat(refundService.detail(userId, refundNo).images()).isEqualTo(imagesJson);

        // 管理端审核列表（后台凭证展示取这个字段）
        RefundService.AdminRefundVO vo = refundService.adminPage(null, refundNo, 1, 10)
                .getRecords().stream()
                .filter(v -> v.refundNo().equals(refundNo))
                .findFirst().orElseThrow();
        assertThat(vo.images()).isEqualTo(imagesJson);
    }

    @Test
    @DisplayName("超额售后：>50 元进入人工审核，订单转售后中")
    void apply_overThreshold_pending() {
        String orderNo = paidOrder(2);
        int before = sku().getStock();
        String refundNo = apply(orderNo, Refund.TYPE_ONLY_REFUND);

        Refund refund = refundMapper.selectOne(new LambdaQueryWrapper<Refund>()
                .eq(Refund::getRefundNo, refundNo));
        assertThat(refund.getStatus()).isEqualTo(Refund.STATUS_PENDING);
        assertThat(refund.getAmount()).isEqualTo(2990 * 2);
        assertThat(refund.getPrevStatus()).isEqualTo(OrderStatus.PENDING_SHIP);

        Order order = orderMapper.selectOne(new LambdaQueryWrapper<Order>()
                .eq(Order::getOrderNo, orderNo));
        assertThat(order.getStatus()).isEqualTo(OrderStatus.REFUNDING);
        assertThat(sku().getStock()).isEqualTo(before); // 审核中不动库存
    }

    @Test
    @DisplayName("小额售后：≤50 元自动秒审退款，订单转已退款，库存回补")
    void apply_autoApprove() {
        int before = sku().getStock();
        String orderNo = paidOrder(1); // 2990 ≤ 5000
        String refundNo = apply(orderNo, Refund.TYPE_ONLY_REFUND);

        Refund refund = refundMapper.selectOne(new LambdaQueryWrapper<Refund>()
                .eq(Refund::getRefundNo, refundNo));
        assertThat(refund.getStatus()).isEqualTo(Refund.STATUS_REFUNDED);
        assertThat(refund.getRefundTime()).isNotNull();

        Order order = orderMapper.selectOne(new LambdaQueryWrapper<Order>()
                .eq(Order::getOrderNo, orderNo));
        assertThat(order.getStatus()).isEqualTo(OrderStatus.REFUNDED);
        assertThat(sku().getStock()).isEqualTo(before); // 扣1又回补
    }

    @Test
    @DisplayName("人工审核：通过后订单转已退款并回补库存")
    void adminApprove() {
        int before = sku().getStock();
        String orderNo = paidOrder(2);
        String refundNo = apply(orderNo, Refund.TYPE_RETURN_REFUND);

        refundService.adminApprove(1L, refundOf(refundNo).getId(), "凭证有效，同意退款");

        Refund refund = refundOf(refundNo);
        assertThat(refund.getStatus()).isEqualTo(Refund.STATUS_REFUNDED);
        assertThat(refund.getAuditBy()).isEqualTo(1L);
        Order order = orderMapper.selectOne(new LambdaQueryWrapper<Order>()
                .eq(Order::getOrderNo, orderNo));
        assertThat(order.getStatus()).isEqualTo(OrderStatus.REFUNDED);
        assertThat(sku().getStock()).isEqualTo(before);
    }

    @Test
    @DisplayName("审核驳回：订单恢复申请前状态（待发货）")
    void adminReject_restoresOrder() {
        String orderNo = paidOrder(2);
        String refundNo = apply(orderNo, Refund.TYPE_ONLY_REFUND);

        refundService.adminReject(1L, refundOf(refundNo).getId(), "凭证不足");

        Refund refund = refundOf(refundNo);
        assertThat(refund.getStatus()).isEqualTo(Refund.STATUS_REJECTED);
        Order order = orderMapper.selectOne(new LambdaQueryWrapper<Order>()
                .eq(Order::getOrderNo, orderNo));
        assertThat(order.getStatus()).isEqualTo(OrderStatus.PENDING_SHIP);
    }

    @Test
    @DisplayName("重复申请拦截：同一订单存在进行中售后时不可重复申请")
    void apply_duplicate_blocked() {
        String orderNo = paidOrder(2); // >50 元保持「待审核」
        apply(orderNo, Refund.TYPE_ONLY_REFUND);

        assertThatThrownBy(() -> apply(orderNo, Refund.TYPE_RETURN_REFUND))
                .isInstanceOf(BizException.class)
                .hasMessageContaining("进行中的售后");
    }

    @Test
    @DisplayName("撤销：待审核申请撤销后订单恢复，可重新申请")
    void withdraw_restoresAndAllowsReapply() {
        String orderNo = paidOrder(2); // >50 元保持「待审核」
        String refundNo = apply(orderNo, Refund.TYPE_ONLY_REFUND);

        refundService.withdraw(userId, refundNo);
        assertThat(refundOf(refundNo).getStatus()).isEqualTo(Refund.STATUS_WITHDRAWN);
        Order order = orderMapper.selectOne(new LambdaQueryWrapper<Order>()
                .eq(Order::getOrderNo, orderNo));
        assertThat(order.getStatus()).isEqualTo(OrderStatus.PENDING_SHIP);

        // 撤销后可重新申请
        String again = apply(orderNo, Refund.TYPE_RETURN_REFUND);
        assertThat(again).isNotBlank();
    }

    private Refund refundOf(String refundNo) {
        return refundMapper.selectOne(new LambdaQueryWrapper<Refund>()
                .eq(Refund::getRefundNo, refundNo));
    }
}
