package com.fruitmall.content;

import com.fruitmall.common.BizException;
import com.fruitmall.modules.content.BannerService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** 轮播图：C 端只取启用项并排序；管理端增删改与状态切换即时生效；必填校验 */
@SpringBootTest
@ActiveProfiles("test")
class BannerIntegrationTest {

    @Autowired
    BannerService bannerService;

    @Test
    @DisplayName("C 端：只返回启用中的轮播，按 sort 升序（V12 种子 3 条）")
    void listOnlyEnabledOrderBySort() {
        List<BannerService.BannerVO> list = bannerService.listByPosition("home");

        assertThat(list).isNotEmpty();
        assertThat(list).allMatch(v -> v.status() == 1);
        assertThat(list).allMatch(v -> "home".equals(v.position()));
        assertThat(list).extracting(BannerService.BannerVO::sort).isSorted();
    }

    @Test
    @DisplayName("新建（禁用）不进 C 端 → 启用后立即出现且按 sort 排最前 → 删除后消失")
    void crudFlow() {
        int before = bannerService.listByPosition("home").size();

        Long id = bannerService.create(new BannerService.BannerInput(
                "测试轮播", "/uploads/test/banner.jpg", 0, null, "home", 5, 0));
        assertThat(id).isNotNull();
        assertThat(bannerService.listByPosition("home")).hasSize(before); // 禁用态不展示

        bannerService.updateStatus(id, 1);
        List<BannerService.BannerVO> afterEnable = bannerService.listByPosition("home");
        assertThat(afterEnable).hasSize(before + 1);
        assertThat(afterEnable.get(0).title()).isEqualTo("测试轮播"); // sort=5 最小，排最前

        bannerService.update(id, new BannerService.BannerInput(
                "改后标题", "/uploads/test/banner2.jpg", 1, "11", "home", 5, 1));
        BannerService.BannerVO updated = bannerService.listByPosition("home").get(0);
        assertThat(updated.title()).isEqualTo("改后标题");
        assertThat(updated.linkType()).isEqualTo(1);
        assertThat(updated.linkValue()).isEqualTo("11");

        bannerService.delete(id);
        assertThat(bannerService.listByPosition("home")).hasSize(before);
        assertThat(bannerService.adminPage("home", null, 1, 50).getRecords())
                .noneMatch(v -> v.id().equals(id));
    }

    @Test
    @DisplayName("校验：标题/图片必填，选择跳转时必须填写目标值")
    void validation() {
        assertThatThrownBy(() -> bannerService.create(
                new BannerService.BannerInput(null, "/uploads/a.jpg", 0, null, "home", 0, 1)))
                .isInstanceOf(BizException.class).hasMessageContaining("标题");

        assertThatThrownBy(() -> bannerService.create(
                new BannerService.BannerInput("无图", " ", 0, null, "home", 0, 1)))
                .isInstanceOf(BizException.class).hasMessageContaining("图片");

        assertThatThrownBy(() -> bannerService.create(
                new BannerService.BannerInput("缺商品", "/uploads/a.jpg", 1, null, "home", 0, 1)))
                .isInstanceOf(BizException.class).hasMessageContaining("商品 ID");

        assertThatThrownBy(() -> bannerService.create(
                new BannerService.BannerInput("缺路径", "/uploads/a.jpg", 2, "", "home", 0, 1)))
                .isInstanceOf(BizException.class).hasMessageContaining("页面路径");
    }
}
