package com.fruitmall.member;

import com.fruitmall.auth.JwtUtil;
import com.fruitmall.modules.user.User;
import com.fruitmall.modules.user.UserMapper;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

/**
 * C 端会员卡概览接口（GET /api/v1/users/me/member）集成测试。
 * 覆盖四种会员状态与未登录拦截；小程序会员中心页依赖这些字段渲染。
 *
 * 注意：项目开启 jackson default-property-inclusion=non_null，
 * 值为 null 的字段不会出现在 JSON 里，因此用 $.data 整包 Map 来判断
 * 字段“为空/缺失”，而不是对 null 字段单独走 JsonPath（会抛 PathNotFound）。
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class MemberOverviewIntegrationTest {

    @Autowired MockMvc mockMvc;
    @Autowired JwtUtil jwtUtil;
    @Autowired UserMapper userMapper;

    /** 造一个用户，expire 传 null 表示未开通会员卡 */
    private Long newUser(LocalDateTime expire, int level) {
        User u = new User();
        u.setPhone("13" + String.format("%09d", Math.abs(System.nanoTime()) % 1_000_000_000L));
        u.setNickname("会员概览测试");
        u.setLevel(level);
        u.setStatus(1);
        u.setMemberExpireAt(expire);
        userMapper.insert(u);
        return u.getId();
    }

    private String fetch(Long userId) throws Exception {
        return mockMvc.perform(get("/api/v1/users/me/member")
                        .header("Authorization", "Bearer " + jwtUtil.generate(userId)))
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
    }

    /** 整包 data 读成 Map，规避 non_null 下 null 字段缺失导致的 PathNotFound */
    @SuppressWarnings("unchecked")
    private Map<String, Object> data(String json) {
        return JsonPath.read(json, "$.data");
    }

    /**
     * JsonPath.read 返回泛型 T，直接塞给 assertThat 会在
     * assertThat(IntPredicate) 与 assertThat(Predicate&lt;T&gt;) 之间产生歧义，
     * 先落到 int 局部变量把类型定下来。
     */
    private int intAt(String json, String path) {
        return JsonPath.read(json, path);
    }

    @Test
    @DisplayName("未开通会员卡：状态 NONE，到期日与剩余天数为空/缺失")
    void none_whenNoCard() throws Exception {
        String json = fetch(newUser(null, 1));

        assertThat(intAt(json, "$.code")).isZero();
        assertThat((String) data(json).get("memberStatus")).isEqualTo("NONE");
        assertThat(data(json).get("memberExpireAt")).isNull();
        assertThat(data(json).get("remainDays")).isNull();
        assertThat(intAt(json, "$.data.level")).isEqualTo(1);
    }

    @Test
    @DisplayName("生效中：状态 ACTIVE，返回到期日与剩余天数")
    void active_whenFarFromExpire() throws Exception {
        String json = fetch(newUser(LocalDateTime.now().plusDays(100), 3));

        assertThat((String) data(json).get("memberStatus")).isEqualTo("ACTIVE");
        assertThat((String) data(json).get("memberExpireAt")).isNotBlank();
        // 边界：跨过午夜时 ChronoUnit.DAYS 可能算成 99 天
        assertThat(((Number) data(json).get("remainDays")).longValue()).isBetween(99L, 100L);
        assertThat(intAt(json, "$.data.level")).isEqualTo(3);
    }

    @Test
    @DisplayName("临期：7 天内到期时状态为 EXPIRING")
    void expiring_within7Days() throws Exception {
        String json = fetch(newUser(LocalDateTime.now().plusDays(3), 2));

        assertThat((String) data(json).get("memberStatus")).isEqualTo("EXPIRING");
        assertThat(((Number) data(json).get("remainDays")).longValue()).isBetween(2L, 3L);
    }

    @Test
    @DisplayName("已过期：状态 EXPIRED，保留到期日但剩余天数为空/缺失")
    void expired_whenPastDue() throws Exception {
        String json = fetch(newUser(LocalDateTime.now().minusDays(5), 2));

        assertThat((String) data(json).get("memberStatus")).isEqualTo("EXPIRED");
        assertThat((String) data(json).get("memberExpireAt")).isNotBlank();
        assertThat(data(json).get("remainDays")).isNull();
    }

    @Test
    @DisplayName("无消费记录时订单数与累计消费返回 0 而不是 null")
    void zeroStats_whenNoOrders() throws Exception {
        String json = fetch(newUser(null, 1));

        assertThat(((Number) data(json).get("orderCount")).longValue()).isZero();
        assertThat(((Number) data(json).get("totalGmv")).longValue()).isZero();
    }

    @Test
    @DisplayName("未登录访问会员概览返回 401")
    void unauthorized_returns401() throws Exception {
        String json = mockMvc.perform(get("/api/v1/users/me/member"))
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);

        assertThat(intAt(json, "$.code")).isEqualTo(401);
    }
}
