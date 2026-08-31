package com.fruitmall.security;

import com.fruitmall.auth.JwtUtil;
import com.fruitmall.common.BizException;
import com.fruitmall.modules.auth.SmsCodeService;
import com.fruitmall.modules.order.Order;
import com.fruitmall.modules.order.OrderItem;
import com.fruitmall.modules.order.OrderItemMapper;
import com.fruitmall.modules.order.OrderMapper;
import com.fruitmall.modules.order.OrderService;
import com.fruitmall.modules.order.OrderStatus;
import com.fruitmall.modules.order.dto.CreateOrderRequest;
import com.fruitmall.modules.product.Product;
import com.fruitmall.modules.product.ProductMapper;
import com.fruitmall.modules.product.ProductSku;
import com.fruitmall.modules.product.ProductSkuMapper;
import com.fruitmall.modules.review.Review;
import com.fruitmall.modules.review.ReviewMapper;
import com.fruitmall.modules.review.ReviewService;
import com.fruitmall.modules.user.User;
import com.fruitmall.modules.user.UserAddress;
import com.fruitmall.modules.user.UserAddressMapper;
import com.fruitmall.modules.user.UserMapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

/**
 * 安全加固集成测试：覆盖未登录拦截、C 端/管理端 token 隔离、C 端资源归属越权、
 * 短信验证码风控、XSS 清洗、JWT 续期。
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class SecurityHardeningIntegrationTest {

    @Autowired MockMvc mockMvc;
    @Autowired JwtUtil jwtUtil;
    @Autowired ObjectMapper objectMapper;
    @Autowired OrderService orderService;
    @Autowired OrderMapper orderMapper;
    @Autowired OrderItemMapper orderItemMapper;
    @Autowired UserMapper userMapper;
    @Autowired UserAddressMapper addressMapper;
    @Autowired ReviewService reviewService;
    @Autowired ReviewMapper reviewMapper;
    @Autowired ProductMapper productMapper;
    @Autowired ProductSkuMapper skuMapper;
    @Autowired SmsCodeService smsCodeService;

    private Long newUser() {
        User u = new User();
        u.setPhone("13" + String.format("%09d", Math.abs(System.nanoTime()) % 1_000_000_000L));
        u.setNickname("安全测试用户");
        u.setLevel(1);
        u.setStatus(1);
        userMapper.insert(u);
        return u.getId();
    }

    private Long newAddress(Long userId) {
        UserAddress a = new UserAddress();
        a.setUserId(userId);
        a.setReceiver("李四");
        a.setPhone("13800002222");
        a.setProvince("广东省");
        a.setCity("深圳市");
        a.setDistrict("南山区");
        a.setDetail("科技园北路1号");
        a.setIsDefault(true);
        addressMapper.insert(a);
        return a.getId();
    }

    private String cToken(Long userId) {
        return "Bearer " + jwtUtil.generate(userId);
    }

    private String aToken(Long adminId) {
        return "Bearer " + jwtUtil.generateAdmin(adminId);
    }

    private int codeOf(String json) {
        return JsonPath.read(json, "$.code");
    }

    private String validPhone() {
        return "13" + String.format("%09d", Math.abs(System.nanoTime() + Thread.currentThread().getId()) % 1_000_000_000L);
    }

    @Test
    @DisplayName("未登录访问 C 端受保护接口返回 401")
    void unauthorized_returns401() throws Exception {
        String json = mockMvc.perform(get("/api/v1/users/me"))
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
        assertThat(codeOf(json)).isEqualTo(401);
    }

    @Test
    @DisplayName("管理端 token 不能访问 C 端接口（token 隔离），返 401")
    void adminTokenRejectedOnCClient_returns401() throws Exception {
        Long adminId = 1L; // 任意 id 即可，关键在 role=admin 声明
        String json = mockMvc.perform(get("/api/v1/users/me").header("Authorization", aToken(adminId)))
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
        assertThat(codeOf(json)).isEqualTo(401);
    }

    @Test
    @DisplayName("C 端资源归属：用户 B 不能读取用户 A 的订单，抛「订单不存在」")
    void crossUserOrderAccess_denied() {
        Long ua = newUser();
        Long addrA = newAddress(ua);
        Long ub = newUser();

        String orderNo = orderService.createOrder(ua,
                new CreateOrderRequest(addrA, List.of(new CreateOrderRequest.Item(11L, 1)), null, null));

        // 本人可读
        assertThat(orderService.orderDetail(ua, orderNo).orderNo()).isEqualTo(orderNo);
        // 他人访问被拒（不泄露存在性）
        assertThatThrownBy(() -> orderService.orderDetail(ub, orderNo))
                .isInstanceOf(BizException.class)
                .hasMessageContaining("订单不存在");
    }

    @Test
    @DisplayName("短信验证码：同手机号 60s 内重发被风控拦截")
    void smsRateLimit_blocksRapidResend() {
        String phone = validPhone();
        smsCodeService.send(phone, "127.0.0.1"); // 首次成功
        assertThatThrownBy(() -> smsCodeService.send(phone, "127.0.0.1"))
                .isInstanceOf(BizException.class)
                .hasMessageContaining("频繁");
    }

    @Test
    @DisplayName("短信验证码：开发模式万能码可用、错误码累加、生成码可校验")
    void smsDevCodeAndVerify() {
        // 万能码 123456 直接通过
        String p1 = validPhone();
        smsCodeService.send(p1, "127.0.0.1");
        smsCodeService.verify(p1, "123456");

        // 错误码累加尝试次数
        String p2 = validPhone();
        smsCodeService.send(p2, "127.0.0.1");
        assertThatThrownBy(() -> smsCodeService.verify(p2, "000000"))
                .isInstanceOf(BizException.class)
                .hasMessageContaining("验证码错误");

        // 真实生成的验证码可校验通过
        String p3 = validPhone();
        SmsCodeService.SendResult r = smsCodeService.send(p3, "127.0.0.1");
        smsCodeService.verify(p3, r.code());
    }

    @Test
    @DisplayName("XSS：评价内容中的 <script> 等标签在存储前被清洗")
    void xssReviewContentSanitized() {
        Long u = newUser();
        Product p = new Product();
        p.setName("XSS测试果");
        p.setCategoryId(1L);
        p.setStatus(1);
        p.setMainImage("/x.jpg");
        productMapper.insert(p);

        ProductSku sku = new ProductSku();
        sku.setProductId(p.getId());
        sku.setSpec("默认");
        sku.setPrice(100);
        sku.setStock(10);
        sku.setStatus(1);
        skuMapper.insert(sku);

        Order o = new Order();
        o.setOrderNo("XT" + System.nanoTime());
        o.setUserId(u);
        o.setStatus(OrderStatus.COMPLETED);
        o.setTotalAmount(100);
        o.setPayAmount(100);
        o.setFreight(0);
        o.setDeliveryType(1);
        orderMapper.insert(o);

        OrderItem it = new OrderItem();
        it.setOrderId(o.getId());
        it.setProductId(p.getId());
        it.setSkuId(sku.getId());
        it.setProductName("XSS测试果");
        it.setPrice(100);
        it.setQuantity(1);
        it.setSubtotal(100);
        orderItemMapper.insert(it);

        reviewService.create(u, o.getOrderNo(), it.getId(), 5,
                "<script>alert(1)</script><img src=x onerror=alert(2)>很好吃", false);

        Review saved = reviewMapper.selectOne(new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<Review>()
                .eq(Review::getOrderItemId, it.getId()));
        assertThat(saved.getContent())
                .doesNotContain("<script>")
                .doesNotContain("<img")
                .doesNotContain("onerror")
                .doesNotContain("javascript:")
                .contains("很好吃");
    }

    @Test
    @DisplayName("JWT 续期：有效 token 换发新 token；无效 token 被拒")
    void refreshEndpoint() throws Exception {
        Long u = newUser();
        String token = jwtUtil.generate(u);

        String ok = mockMvc.perform(post("/api/v1/auth/refresh").header("Authorization", "Bearer " + token))
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
        assertThat(codeOf(ok)).isEqualTo(0);
        assertThat((String) JsonPath.read(ok, "$.data.token")).isNotBlank();

        // 伪造 token 续期被拒（非 0 即失败）
        String bad = mockMvc.perform(post("/api/v1/auth/refresh").header("Authorization", "Bearer not-a-real-token"))
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
        assertThat(codeOf(bad)).isNotEqualTo(0);
    }
}
