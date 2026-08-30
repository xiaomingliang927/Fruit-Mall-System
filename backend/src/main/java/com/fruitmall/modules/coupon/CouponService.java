package com.fruitmall.modules.coupon;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fruitmall.common.BizException;
import com.fruitmall.common.PageResult;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CouponService {

    private final CouponMapper couponMapper;
    private final UserCouponMapper userCouponMapper;

    public record CouponInput(String name, Integer type, Integer thresholdAmount,
                              Integer discountAmount, Integer discountPercent,
                              Integer totalCount, Integer perUserLimit,
                              LocalDateTime startTime, LocalDateTime endTime, Integer validDays) {
    }

    public record CouponVO(Long id, String name, Integer type, String typeText,
                           Integer thresholdAmount, Integer discountAmount, Integer discountPercent,
                           Integer remaining, Integer perUserLimit,
                           LocalDateTime startTime, LocalDateTime endTime) {
    }

    /** 我的券（含模板信息），status 空则惰性过期后返回未使用的 */
    public record MyCouponVO(Long userCouponId, Long couponId, String name, Integer type, String typeText,
                             Integer thresholdAmount, Integer discountAmount, Integer discountPercent,
                             Integer status, String statusText, LocalDateTime expireAt) {
    }

    // ============ 管理侧 ============

    public Long create(CouponInput input) {
        validateInput(input);
        Coupon c = new Coupon();
        c.setName(input.name());
        c.setType(input.type());
        c.setThresholdAmount(nz(input.thresholdAmount()));
        c.setDiscountAmount(nz(input.discountAmount()));
        c.setDiscountPercent(input.discountPercent());
        c.setTotalCount(input.totalCount());
        c.setIssuedCount(0);
        c.setPerUserLimit(input.perUserLimit() == null ? 1 : input.perUserLimit());
        c.setStartTime(input.startTime());
        c.setEndTime(input.endTime());
        c.setValidDays(input.validDays() == null ? 30 : input.validDays());
        c.setStatus(1);
        couponMapper.insert(c);
        return c.getId();
    }

    public PageResult<CouponVO> adminPage(String keyword, long page, long size) {
        Page<Coupon> result = couponMapper.selectPage(new Page<>(page, Math.min(size, 50)),
                new LambdaQueryWrapper<Coupon>()
                        .like(org.springframework.util.StringUtils.hasText(keyword), Coupon::getName, keyword)
                        .orderByDesc(Coupon::getId));
        return PageResult.of(result, c -> new CouponVO(c.getId(), c.getName(), c.getType(),
                typeText(c.getType()), c.getThresholdAmount(), c.getDiscountAmount(),
                c.getDiscountPercent(), Math.max(0, c.getTotalCount() - c.getIssuedCount()),
                c.getPerUserLimit(), c.getStartTime(), c.getEndTime()));
    }

    public void updateStatus(Long id, Integer status) {
        Coupon c = couponMapper.selectById(id);
        if (c == null) throw new BizException("优惠券不存在");
        c.setStatus(status);
        couponMapper.updateById(c);
    }

    // ============ 用户侧 ============

    /** 可领券列表（进行中的） */
    public List<CouponVO> listReceivable() {
        List<Coupon> list = couponMapper.selectList(new LambdaQueryWrapper<Coupon>()
                .eq(Coupon::getStatus, 1)
                .le(Coupon::getStartTime, LocalDateTime.now())
                .ge(Coupon::getEndTime, LocalDateTime.now())
                .orderByDesc(Coupon::getId));
        return list.stream().map(c -> new CouponVO(c.getId(), c.getName(), c.getType(),
                typeText(c.getType()), c.getThresholdAmount(), c.getDiscountAmount(),
                c.getDiscountPercent(), Math.max(0, c.getTotalCount() - c.getIssuedCount()),
                c.getPerUserLimit(), c.getStartTime(), c.getEndTime())).toList();
    }

    /** 领取：时间窗 + 每人限领 + 乐观扣减发放额度（防超发） */
    @Transactional
    public MyCouponVO receive(Long userId, Long couponId) {
        Coupon c = couponMapper.selectById(couponId);
        if (c == null || c.getStatus() != 1) throw new BizException("优惠券不存在或已下架");
        LocalDateTime now = LocalDateTime.now();
        if (now.isBefore(c.getStartTime()) || now.isAfter(c.getEndTime())) {
            throw new BizException("不在领取时间范围内");
        }
        Long mine = userCouponMapper.selectCount(new LambdaQueryWrapper<UserCoupon>()
                .eq(UserCoupon::getCouponId, couponId)
                .eq(UserCoupon::getUserId, userId));
        if (mine >= c.getPerUserLimit()) throw new BizException("已达每人限领数量");

        if (couponMapper.tryIssue(couponId) == 0) throw new BizException("手慢了，券已抢完");

        UserCoupon uc = new UserCoupon();
        uc.setCouponId(couponId);
        uc.setUserId(userId);
        uc.setStatus(UserCoupon.STATUS_UNUSED);
        uc.setReceivedAt(now);
        uc.setExpireAt(now.plusDays(c.getValidDays()));
        userCouponMapper.insert(uc);
        return toMyVO(uc, c);
    }

    /** 我的优惠券：先惰性过期，再按状态返回 */
    public List<MyCouponVO> myCoupons(Long userId, Integer status) {
        // 惰性过期
        userCouponMapper.selectList(new LambdaQueryWrapper<UserCoupon>()
                        .eq(UserCoupon::getUserId, userId)
                        .eq(UserCoupon::getStatus, UserCoupon.STATUS_UNUSED)
                        .lt(UserCoupon::getExpireAt, LocalDateTime.now()))
                .forEach(uc -> {
                    uc.setStatus(UserCoupon.STATUS_EXPIRED);
                    userCouponMapper.updateById(uc);
                });
        List<UserCoupon> ucs = userCouponMapper.selectList(new LambdaQueryWrapper<UserCoupon>()
                .eq(UserCoupon::getUserId, userId)
                .eq(status != null, UserCoupon::getStatus, status)
                .orderByDesc(UserCoupon::getId));
        Map<Long, Coupon> tplById = ucs.isEmpty() ? Map.of()
                : couponMapper.selectBatchIds(ucs.stream().map(UserCoupon::getCouponId).distinct().toList())
                        .stream().collect(Collectors.toMap(Coupon::getId, c -> c));
        return ucs.stream().map(uc -> toMyVO(uc, tplById.get(uc.getCouponId()))).toList();
    }

    /** 结算页：该订单金额下可用的券（含可抵扣金额） */
    public List<MyCouponVO> usableForOrder(Long userId, int orderAmount) {
        expireLazy(userId);
        List<MyCouponVO> all = myCoupons(userId, UserCoupon.STATUS_UNUSED);
        return all.stream()
                .filter(v -> discountOf(v.type(), v.thresholdAmount(), v.discountAmount(),
                        v.discountPercent(), orderAmount) > 0
                        || (v.thresholdAmount() != null && v.thresholdAmount() <= orderAmount))
                .filter(v -> v.thresholdAmount() == null || v.thresholdAmount() <= orderAmount)
                .toList();
    }

    /** 下单用券：校验归属/状态/有效期/门槛，返回抵扣金额并核销 */
    @Transactional
    public int useForOrder(Long userId, Long userCouponId, int orderAmount, String orderNo) {
        UserCoupon uc = userCouponMapper.selectById(userCouponId);
        if (uc == null || !uc.getUserId().equals(userId)) throw new BizException("优惠券不存在");
        if (uc.getStatus() != UserCoupon.STATUS_UNUSED) throw new BizException("优惠券不可用（已使用或已过期）");
        if (uc.getExpireAt().isBefore(LocalDateTime.now())) throw new BizException("优惠券已过期");
        Coupon c = couponMapper.selectById(uc.getCouponId());
        if (c == null) throw new BizException("优惠券信息异常");
        int discount = discountOf(c.getType(), c.getThresholdAmount(), c.getDiscountAmount(),
                c.getDiscountPercent(), orderAmount);
        if (c.getThresholdAmount() != null && c.getThresholdAmount() > orderAmount) {
            throw new BizException("未满足优惠券使用门槛");
        }
        uc.setStatus(UserCoupon.STATUS_USED);
        uc.setOrderNo(orderNo);
        uc.setUsedAt(LocalDateTime.now());
        userCouponMapper.updateById(uc);
        return discount;
    }

    /** 取消订单：券退回（未过期恢复可用，已过期保持过期） */
    @Transactional
    public void returnCoupon(Long userCouponId) {
        if (userCouponId == null) return;
        UserCoupon uc = userCouponMapper.selectById(userCouponId);
        if (uc == null || uc.getStatus() != UserCoupon.STATUS_USED) return;
        // updateById 忽略 null 字段，退回需显式置空 order_no/used_at
        userCouponMapper.update(null, new com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper<UserCoupon>()
                .eq(UserCoupon::getId, uc.getId())
                .set(UserCoupon::getStatus, UserCoupon.STATUS_UNUSED)
                .set(UserCoupon::getOrderNo, null)
                .set(UserCoupon::getUsedAt, null));
    }

    // ============ 内部 ============

    private MyCouponVO toMyVO(UserCoupon uc, Coupon c) {
        return new MyCouponVO(uc.getId(), uc.getCouponId(),
                c == null ? "优惠券" : c.getName(),
                c == null ? Coupon.TYPE_REDUCE : c.getType(),
                c == null ? "满减" : typeText(c.getType()),
                c == null ? 0 : c.getThresholdAmount(),
                c == null ? 0 : c.getDiscountAmount(),
                c == null ? null : c.getDiscountPercent(),
                uc.getStatus(), statusText(uc.getStatus()), uc.getExpireAt());
    }

    /** 计算抵扣金额（分）；门槛不满足返回 0 */
    public static int discountOf(Integer type, Integer threshold, Integer discountAmount,
                                 Integer discountPercent, int orderAmount) {
        if (threshold != null && threshold > orderAmount) return 0;
        if (type != null && type == Coupon.TYPE_DISCOUNT) {
            int percent = discountPercent == null ? 100 : discountPercent;
            return Math.max(0, orderAmount * (100 - percent) / 100);
        }
        return Math.min(nz(discountAmount), orderAmount);
    }

    private void expireLazy(Long userId) {
        userCouponMapper.selectList(new LambdaQueryWrapper<UserCoupon>()
                        .eq(UserCoupon::getUserId, userId)
                        .eq(UserCoupon::getStatus, UserCoupon.STATUS_UNUSED)
                        .lt(UserCoupon::getExpireAt, LocalDateTime.now()))
                .forEach(uc -> {
                    uc.setStatus(UserCoupon.STATUS_EXPIRED);
                    userCouponMapper.updateById(uc);
                });
    }

    private void validateInput(CouponInput input) {
        if (!org.springframework.util.StringUtils.hasText(input.name())) throw new BizException("请填写券名");
        if (input.type() == null || input.type() < Coupon.TYPE_REDUCE || input.type() > Coupon.TYPE_NO_THRESHOLD) {
            throw new BizException("券类型不合法");
        }
        if (input.type() == Coupon.TYPE_DISCOUNT) {
            if (input.discountPercent() == null || input.discountPercent() < 1 || input.discountPercent() > 99) {
                throw new BizException("折扣率须在 1~99 之间");
            }
        } else if (input.discountAmount() == null || input.discountAmount() <= 0) {
            throw new BizException("请填写优惠金额");
        }
        if (input.totalCount() == null || input.totalCount() <= 0) throw new BizException("发放总量须大于 0");
        if (input.startTime() == null || input.endTime() == null || input.startTime().isAfter(input.endTime())) {
            throw new BizException("领取时间范围不合法");
        }
    }

    private static String typeText(Integer type) {
        if (type == null) return "-";
        return switch (type) {
            case Coupon.TYPE_DISCOUNT -> "折扣券";
            case Coupon.TYPE_NO_THRESHOLD -> "无门槛券";
            default -> "满减券";
        };
    }

    private static String statusText(Integer status) {
        if (status == null) return "-";
        return switch (status) {
            case UserCoupon.STATUS_USED -> "已使用";
            case UserCoupon.STATUS_EXPIRED -> "已过期";
            default -> "未使用";
        };
    }

    private static int nz(Integer v) {
        return v == null ? 0 : v;
    }
}
