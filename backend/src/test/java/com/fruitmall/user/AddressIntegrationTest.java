package com.fruitmall.user;

import com.fruitmall.auth.JwtUtil;
import com.fruitmall.modules.user.User;
import com.fruitmall.modules.user.UserMapper;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;

/**
 * 收货地址增改接口（POST / PUT /api/v1/users/me/addresses）集成测试。
 * 覆盖：修改回读、默认地址唯一性（新增/修改两条路径都只保留一条默认）、
 * 越权修改他人地址被拒。小程序地址页编辑功能依赖 PUT 端点。
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AddressIntegrationTest {

    @Autowired MockMvc mockMvc;
    @Autowired JwtUtil jwtUtil;
    @Autowired UserMapper userMapper;

    private Long newUser() {
        User u = new User();
        u.setPhone("13" + String.format("%09d", Math.abs(System.nanoTime()) % 1_000_000_000L));
        u.setNickname("地址测试");
        u.setLevel(1);
        u.setStatus(1);
        userMapper.insert(u);
        return u.getId();
    }

    private String body(String receiver, String detail, boolean isDefault) {
        return """
                {"receiver":"%s","phone":"13800000001","province":"广东省","city":"深圳市",
                 "district":"南山区","detail":"%s","isDefault":%s}
                """.formatted(receiver, detail, isDefault).replaceAll("\\s+", "");
    }

    private String perform(Long userId, MockHttpServletRequestBuilder builder, String json) throws Exception {
        return mockMvc.perform(builder
                        .header("Authorization", "Bearer " + jwtUtil.generate(userId))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
    }

    private Long addAddress(Long userId, String receiver, boolean isDefault) throws Exception {
        String json = perform(userId, post("/api/v1/users/me/addresses"), body(receiver, "科技园路1号", isDefault));
        return ((Number) JsonPath.read(json, "$.data")).longValue();
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> list(Long userId) throws Exception {
        String json = mockMvc.perform(get("/api/v1/users/me/addresses")
                        .header("Authorization", "Bearer " + jwtUtil.generate(userId)))
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
        return JsonPath.read(json, "$.data");
    }

    @Test
    @DisplayName("修改地址：字段更新生效，列表回读一致")
    void updateAddress_fieldsPersisted() throws Exception {
        Long userId = newUser();
        Long id = addAddress(userId, "张三", false);

        String json = perform(userId, put("/api/v1/users/me/addresses/" + id),
                body("李四", "欢乐谷路8号", false));

        assertThat((int) JsonPath.read(json, "$.code")).isZero();
        List<Map<String, Object>> addresses = list(userId);
        assertThat(addresses).hasSize(1);
        assertThat(addresses.get(0).get("receiver")).isEqualTo("李四");
        assertThat(addresses.get(0).get("detail")).isEqualTo("欢乐谷路8号");
    }

    @Test
    @DisplayName("修改设为默认：旧默认被清除，全局仅剩一条默认")
    void updateAddress_defaultExclusivity() throws Exception {
        Long userId = newUser();
        Long first = addAddress(userId, "张三", true);
        Long second = addAddress(userId, "李四", false);

        String json = perform(userId, put("/api/v1/users/me/addresses/" + second),
                body("李四", "科技园路1号", true));

        assertThat((int) JsonPath.read(json, "$.code")).isZero();
        List<Map<String, Object>> addresses = list(userId);
        long defaults = addresses.stream().filter(a -> Boolean.TRUE.equals(a.get("isDefault"))).count();
        assertThat(defaults).isEqualTo(1);
        assertThat(addresses.stream().filter(a -> ((Number) a.get("id")).longValue() == second)
                .findFirst().orElseThrow().get("isDefault")).isEqualTo(true);
        assertThat(addresses.stream().filter(a -> ((Number) a.get("id")).longValue() == first)
                .findFirst().orElseThrow().get("isDefault")).isEqualTo(false);
    }

    @Test
    @DisplayName("新增默认地址：旧默认同样被清除")
    void addAddress_defaultExclusivity() throws Exception {
        Long userId = newUser();
        addAddress(userId, "张三", true);
        addAddress(userId, "李四", true);

        List<Map<String, Object>> addresses = list(userId);
        long defaults = addresses.stream().filter(a -> Boolean.TRUE.equals(a.get("isDefault"))).count();
        assertThat(defaults).isEqualTo(1);
    }

    @Test
    @DisplayName("越权：用户 B 修改用户 A 的地址被拒")
    void updateAddress_rejectForeignOwner() throws Exception {
        Long userA = newUser();
        Long userB = newUser();
        Long idA = addAddress(userA, "张三", false);

        String json = perform(userB, put("/api/v1/users/me/addresses/" + idA),
                body("黑客", "不该改成功", false));

        assertThat((int) JsonPath.read(json, "$.code")).isNotZero();
        // A 的地址原样未动
        List<Map<String, Object>> addresses = list(userA);
        assertThat(addresses.get(0).get("receiver")).isEqualTo("张三");
    }
}
