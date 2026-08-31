package com.fruitmall.modules.content;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fruitmall.common.BizException;
import com.fruitmall.common.PageResult;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class BannerService {

    private final BannerMapper bannerMapper;

    public record BannerVO(Long id, String title, String image, Integer linkType, String linkValue,
                           String position, Integer sort, Integer status, LocalDateTime createdAt) {
    }

    public record BannerInput(String title, String image, Integer linkType, String linkValue,
                              String position, Integer sort, Integer status) {
    }

    /** C 端：取某位置启用中的轮播（sort 升序、同排序按 id 升序），匿名可看 */
    public List<BannerVO> listByPosition(String position) {
        String pos = StringUtils.hasText(position) ? position : "home";
        return bannerMapper.selectList(new LambdaQueryWrapper<Banner>()
                        .eq(Banner::getPosition, pos)
                        .eq(Banner::getStatus, Banner.STATUS_ON)
                        .orderByAsc(Banner::getSort)
                        .orderByAsc(Banner::getId))
                .stream().map(this::toVO).toList();
    }

    // ============ 管理端 ============

    public PageResult<BannerVO> adminPage(String position, Integer status, long page, long size) {
        Page<Banner> result = bannerMapper.selectPage(new Page<>(page, Math.min(size, 50)),
                new LambdaQueryWrapper<Banner>()
                        .eq(StringUtils.hasText(position), Banner::getPosition, position)
                        .eq(status != null, Banner::getStatus, status)
                        .orderByAsc(Banner::getSort)
                        .orderByDesc(Banner::getId));
        return PageResult.of(result, this::toVO);
    }

    public Long create(BannerInput input) {
        Banner banner = apply(new Banner(), input);
        bannerMapper.insert(banner);
        return banner.getId();
    }

    public void update(Long id, BannerInput input) {
        bannerMapper.updateById(apply(require(id), input));
    }

    public void updateStatus(Long id, Integer status) {
        Banner banner = require(id);
        banner.setStatus(status != null && status == Banner.STATUS_ON ? Banner.STATUS_ON : Banner.STATUS_OFF);
        bannerMapper.updateById(banner);
    }

    public void delete(Long id) {
        bannerMapper.deleteById(require(id).getId());
    }

    // ============ 内部 ============

    private Banner apply(Banner banner, BannerInput input) {
        if (!StringUtils.hasText(input.title())) throw new BizException("请填写标题");
        if (!StringUtils.hasText(input.image())) throw new BizException("请上传轮播图片");

        int type = input.linkType() == null ? Banner.LINK_NONE : input.linkType();
        if (type < Banner.LINK_NONE || type > Banner.LINK_PAGE) throw new BizException("跳转类型不合法");
        String link = input.linkValue();
        if (type != Banner.LINK_NONE && !StringUtils.hasText(link)) {
            throw new BizException(type == Banner.LINK_PRODUCT ? "请填写商品 ID" : "请填写页面路径");
        }

        banner.setTitle(input.title().trim());
        banner.setImage(input.image().trim());
        banner.setLinkType(type);
        banner.setLinkValue(type == Banner.LINK_NONE ? null : link.trim());
        banner.setPosition(StringUtils.hasText(input.position()) ? input.position().trim() : "home");
        banner.setSort(input.sort() == null ? 0 : input.sort());
        banner.setStatus(input.status() != null && input.status() == Banner.STATUS_OFF
                ? Banner.STATUS_OFF : Banner.STATUS_ON);
        return banner;
    }

    private Banner require(Long id) {
        Banner banner = bannerMapper.selectById(id);
        if (banner == null) throw new BizException("轮播图不存在");
        return banner;
    }

    private BannerVO toVO(Banner b) {
        return new BannerVO(b.getId(), b.getTitle(), b.getImage(), b.getLinkType(), b.getLinkValue(),
                b.getPosition(), b.getSort(), b.getStatus(), b.getCreatedAt());
    }
}
