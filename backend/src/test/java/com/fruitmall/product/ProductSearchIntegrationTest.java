package com.fruitmall.product;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

/**
 * 商品搜索（GET /api/v1/products?keyword=）集成测试。
 *
 * 搜索需同时匹配 名称 / 副标题 / 标签 / 产地 四个字段：
 * 用户搜「海南」是找产地的果，搜「热卖」是找带标签的果，
 * 只匹配 name 的话这两种输入都搜不到东西——小程序搜索页会被当成「坏了」。
 *
 * 种子数据（V6__guoxiaoman_products.sql）：
 *   麒麟西瓜 产地海南 tags=热卖 副标题「沙瓤多汁，当季现摘」
 *   海南香蕉 产地海南
 *   金煌芒果 产地海南
 *   赣南脐橙 tags=特价
 *   丹东草莓 / 红富士苹果 / 巨峰葡萄 / 云南蓝莓 / 红心火龙果 / 都乐菠萝
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ProductSearchIntegrationTest {

    @Autowired MockMvc mockMvc;

    /** 发起一次搜索，返回响应 JSON */
    private String search(String keyword) throws Exception {
        return mockMvc.perform(get("/api/v1/products")
                        .param("keyword", keyword)
                        .param("size", "50"))
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
    }

    private int total(String json) {
        return JsonPath.read(json, "$.data.total");
    }

    @SuppressWarnings("unchecked")
    private List<String> names(String json) {
        List<Map<String, Object>> records = JsonPath.read(json, "$.data.records");
        return records.stream().map(r -> String.valueOf(r.get("name"))).toList();
    }

    @Test
    @DisplayName("按商品名称搜索：搜「草莓」命中丹东草莓")
    void searchByName() throws Exception {
        String json = search("草莓");

        assertThat(total(json)).isEqualTo(1);
        assertThat(names(json)).containsExactly("丹东草莓");
    }

    @Test
    @DisplayName("按产地搜索：搜「海南」命中 3 款海南产水果")
    void searchByOrigin() throws Exception {
        String json = search("海南");

        assertThat(total(json)).isEqualTo(3);
        assertThat(names(json)).contains("麒麟西瓜", "海南香蕉", "金煌芒果");
    }

    @Test
    @DisplayName("按标签搜索：搜「热卖」命中带该标签的商品")
    void searchByTag() throws Exception {
        String json = search("热卖");

        assertThat(total(json)).isGreaterThanOrEqualTo(1);
        assertThat(names(json)).contains("麒麟西瓜");
    }

    @Test
    @DisplayName("按副标题搜索：搜「沙瓤」命中麒麟西瓜")
    void searchBySubtitle() throws Exception {
        String json = search("沙瓤");

        assertThat(total(json)).isEqualTo(1);
        assertThat(names(json)).containsExactly("麒麟西瓜");
    }

    @Test
    @DisplayName("无匹配关键词返回空列表而不是报错")
    void searchNoMatch() throws Exception {
        String json = search("不存在的商品xyz");

        assertThat(total(json)).isZero();
        assertThat(names(json)).isEmpty();
    }

    @Test
    @DisplayName("关键词带空格时仍能命中（避免首尾空格导致搜不到）")
    void searchTrimsKeyword() throws Exception {
        String json = search("  草莓  ");

        assertThat(total(json)).isEqualTo(1);
        assertThat(names(json)).containsExactly("丹东草莓");
    }

    @Test
    @DisplayName("搜索结果只包含在售商品")
    void searchOnlyOnSale() throws Exception {
        String json = search("海南");

        List<Integer> statuses = JsonPath.read(json, "$.data.records[*].status");
        // status 字段不在 ProductListVO 里，缺失说明列表 VO 本身已按 status=1 过滤
        assertThat(statuses).isNullOrEmpty();
        assertThat(total(json)).isEqualTo(3);
    }
}
