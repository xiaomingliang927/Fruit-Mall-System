package com.fruitmall.review;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fruitmall.common.BizException;
import com.fruitmall.modules.order.Order;
import com.fruitmall.modules.order.OrderItem;
import com.fruitmall.modules.order.OrderItemMapper;
import com.fruitmall.modules.order.OrderMapper;
import com.fruitmall.modules.order.OrderService;
import com.fruitmall.modules.order.dto.CreateOrderRequest;
import com.fruitmall.modules.product.ProductService;
import com.fruitmall.modules.review.Review;
import com.fruitmall.modules.review.ReviewMapper;
import com.fruitmall.modules.review.ReviewService;
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

/** 评价集成测试：完成单评价 / 防重复 / 商品聚合 / 后台回复与隐藏 */
@SpringBootTest
@ActiveProfiles("test")
class ReviewFlowIntegrationTest {

    @Autowired ReviewService reviewService;
    @Autowired com.fruitmall.admin.AdminOrderService adminOrderService;
    @Autowired OrderService orderService;
    @Autowired OrderMapper orderMapper;
    @Autowired OrderItemMapper orderItemMapper;
    @Autowired ReviewMapper reviewMapper;
    @Autowired ProductService productService;
    @Autowired UserMapper userMapper;
    @Autowired UserAddressMapper addressMapper;

    Long userId;
    Long addressId;
    Long skuId = 11L; // 麒麟西瓜

    @BeforeEach
    void setUp() {
        User user = new User();
        user.setPhone("13" + System.nanoTime() % 1000000000L);
        user.setNickname("评价测试");
        user.setLevel(1);
        user.setStatus(1);
        userMapper.insert(user);
        userId = user.getId();

        UserAddress address = new UserAddress();
        address.setUserId(userId);
        address.setReceiver("赵六");
        address.setPhone("13600004444");
        address.setProvince("广东省");
        address.setCity("深圳市");
        address.setDistrict("南山区");
        address.setDetail("科技园南路88号");
        address.setIsDefault(true);
        addressMapper.insert(address);
        addressId = address.getId();
    }

    /** 创建并完成一笔订单（1 件），返回 orderNo */
    private String completedOrder() {
        String orderNo = orderService.createOrder(userId,
                new CreateOrderRequest(addressId,
                        List.of(new CreateOrderRequest.Item(skuId, 1)), null, null));
        orderService.markPaid(orderNo, "TX-" + orderNo, "MOCK");
        // 发货（待发货 → 待收货）再确认收货 → 已完成
        adminOrderService.ship(orderOf(orderNo).getId(), "SF-TEST-0001");
        orderService.confirmOrder(userId, orderNo);
        return orderNo;
    }

    private Order orderOf(String orderNo) {
        return orderMapper.selectOne(new LambdaQueryWrapper<Order>().eq(Order::getOrderNo, orderNo));
    }

    private OrderItem firstItem(String orderNo) {
        return orderItemMapper.selectList(new LambdaQueryWrapper<OrderItem>()
                        .eq(OrderItem::getOrderId, orderOf(orderNo).getId()))
                .get(0);
    }

    @Test
    @DisplayName("完成单评价成功：默认可见，商品详情出现评分聚合")
    void createReview_ok() {
        String orderNo = completedOrder();
        OrderItem item = firstItem(orderNo);

        ReviewService.ReviewVO vo = reviewService.create(userId, orderNo, item.getId(), 5, "很新鲜，好评！", false);

        assertThat(vo.id()).isNotNull();
        List<ReviewService.ReviewVO> productReviews =
                reviewService.productReviews(11L, 1, 10).getRecords();
        // 商品 11 的评价列表应包含本条
        assertThat(productReviews.stream().anyMatch((r) -> r.id().equals(vo.id()))).isTrue();

        // 详情页评分聚合
        ProductService.ProductDetailVO detail = productService.detail(11L);
        assertThat(detail.reviewCount()).isGreaterThanOrEqualTo(1L);
    }

    @Test
    @DisplayName("防重复：同一订单明细只能评价一次")
    void duplicate_blocked() {
        String orderNo = completedOrder();
        OrderItem item = firstItem(orderNo);
        reviewService.create(userId, orderNo, item.getId(), 5, "第一次评价", false);

        assertThatThrownBy(() -> reviewService.create(userId, orderNo, item.getId(), 4, "第二次", false))
                .isInstanceOf(BizException.class)
                .hasMessageContaining("已评价过");
    }

    @Test
    @DisplayName("未完成订单不可评价")
    void notCompleted_blocked() {
        String orderNo = orderService.createOrder(userId,
                new CreateOrderRequest(addressId,
                        List.of(new CreateOrderRequest.Item(skuId, 1)), null, null));
        OrderItem item = orderItemMapper.selectList(new LambdaQueryWrapper<OrderItem>()
                        .eq(OrderItem::getOrderId, orderOf(orderNo).getId()))
                .get(0);

        assertThatThrownBy(() -> reviewService.create(userId, orderNo, item.getId(), 5, "还没收货就想评", false))
                .isInstanceOf(BizException.class)
                .hasMessageContaining("完成后");
    }

    @Test
    @DisplayName("后台隐藏评价：商品页不再展示，评分聚合同步下降")
    void adminHide_removesFromList() {
        String orderNo = completedOrder();
        OrderItem item = firstItem(orderNo);
        ReviewService.ReviewVO vo = reviewService.create(userId, orderNo, item.getId(), 2, "一般般", false);
        Long reviewId = vo.id();

        ProductService.ProductDetailVO before = productService.detail(11L);
        reviewService.adminSetStatus(1L, reviewId, Review.STATUS_HIDDEN);

        List<ReviewService.ReviewVO> visible = reviewService.productReviews(11L, 1, 10).getRecords();
        assertThat(visible.stream().noneMatch((r) -> r.id().equals(reviewId))).isTrue();

        ProductService.ProductDetailVO after = productService.detail(11L);
        assertThat(after.reviewCount()).isEqualTo(before.reviewCount() - 1);
    }

    @Test
    @DisplayName("商家回复：回复内容写入评价")
    void adminReply() {
        String orderNo = completedOrder();
        OrderItem item = firstItem(orderNo);
        ReviewService.ReviewVO vo = reviewService.create(userId, orderNo, item.getId(), 5, "好吃", false);

        reviewService.adminReply(1L, vo.id(), "感谢认可，欢迎回购！");
        Review review = reviewMapper.selectById(vo.id());
        assertThat(review.getAdminReply()).contains("欢迎回购");
    }
}
