package com.fruitmall.modules.seckill;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fruitmall.common.BizException;
import com.fruitmall.common.PageResult;
import com.fruitmall.modules.order.OrderService;
import com.fruitmall.modules.product.Product;
import com.fruitmall.modules.product.ProductMapper;
import com.fruitmall.modules.product.ProductSku;
import com.fruitmall.modules.product.ProductSkuMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * M3 秒杀。
 * 防超卖三道闸（单事务内，任一失败整体回滚）：
 * 1. seckill_activity 条件更新原子扣减（含时间窗/状态/库存）；
 * 2. seckill_order 唯一键 uk_seckill_user 拦并发重复参与；
 * 3. 秒杀订单仍走真实商品库存扣减，与普通订单同一套乐观锁。
 * 单实例部署下无需 Redis；多实例可在此层加 Redis 预扣（接口保持不变）。
 */
@Service
@RequiredArgsConstructor
public class SeckillService {

    private final SeckillActivityMapper activityMapper;
    private final SeckillOrderMapper seckillOrderMapper;
    private final ProductMapper productMapper;
    private final ProductSkuMapper skuMapper;
    private final OrderService orderService;

    public record SeckillVO(Long id, Long productId, String productName, String image, String skuSpec,
                            Integer originalPrice, Integer seckillPrice,
                            Integer totalStock, Integer availableStock, Integer limitPerUser,
                            LocalDateTime startTime, LocalDateTime endTime, Integer status) {
    }

    // ============ C 端 ============

    /** 进行中/即将开始的秒杀（含商品快照），匿名可看 */
    public List<SeckillVO> activeList() {
        LocalDateTime now = LocalDateTime.now();
        List<SeckillActivity> list = activityMapper.selectList(new LambdaQueryWrapper<SeckillActivity>()
                .eq(SeckillActivity::getStatus, SeckillActivity.STATUS_ON)
                .ge(SeckillActivity::getEndTime, now)
                .orderByAsc(SeckillActivity::getStartTime)
                .orderByDesc(SeckillActivity::getId)
                .last("LIMIT 20"));
        return toVOs(list);
    }

    public SeckillVO detail(Long id) {
        SeckillActivity activity = activityMapper.selectById(id);
        if (activity == null) {
            throw new BizException("秒杀活动不存在");
        }
        return toVOs(List.of(activity)).get(0);
    }

    /** 抢购：三道闸全过 → 生成秒杀订单（待支付），返回订单号 */
    @Transactional
    public String buy(Long userId, Long seckillId, Long addressId, Integer quantity) {
        int qty = quantity == null ? 1 : quantity;
        if (qty < 1) {
            throw new BizException("购买件数不合法");
        }
        // 闸 1：原子扣减秒杀库存（时间窗/状态/库存全在 SQL 条件里）
        if (activityMapper.decreaseStock(seckillId, qty, LocalDateTime.now()) == 0) {
            throw new BizException("手慢了，本场已抢完或不在活动时间内");
        }
        SeckillActivity activity = activityMapper.selectById(seckillId);
        if (qty > activity.getLimitPerUser()) {
            throw new BizException("每人限购 " + activity.getLimitPerUser() + " 件");
        }
        // 闸 2：参与记录，唯一键拦截并发重复参与
        SeckillOrder record = new SeckillOrder();
        record.setSeckillId(seckillId);
        record.setUserId(userId);
        record.setQuantity(qty);
        record.setSeckillPrice(activity.getSeckillPrice());
        try {
            seckillOrderMapper.insert(record);
        } catch (DuplicateKeyException e) {
            throw new BizException("每人限购 " + activity.getLimitPerUser() + " 件，本场已参与");
        }
        // 闸 3：真实订单（秒杀价 + 商品库存扣减）
        ProductSku sku = skuMapper.selectById(activity.getSkuId());
        if (sku == null || sku.getStatus() == 0) {
            throw new BizException("秒杀规格不存在或已停售");
        }
        String orderNo = orderService.createSeckillOrder(userId, addressId, sku, qty, activity.getSeckillPrice());
        record.setOrderNo(orderNo);
        seckillOrderMapper.updateById(record);
        return orderNo;
    }

    // ============ 管理端 ============

    public PageResult<SeckillVO> adminPage(Integer status, long page, long size) {
        Page<SeckillActivity> result = activityMapper.selectPage(new Page<>(page, Math.min(size, 50)),
                new LambdaQueryWrapper<SeckillActivity>()
                        .eq(status != null, SeckillActivity::getStatus, status)
                        .orderByDesc(SeckillActivity::getId));
        return PageResult.of(result, a -> toVOs(List.of(a)).get(0));
    }

    public record SeckillInput(Long skuId, Integer seckillPrice, Integer totalStock, Integer limitPerUser,
                               LocalDateTime startTime, LocalDateTime endTime, Integer status) {
    }

    public Long create(SeckillInput input) {
        SeckillActivity activity = apply(new SeckillActivity(), input);
        activity.setTotalStock(input.totalStock());
        activity.setAvailableStock(input.totalStock());
        activityMapper.insert(activity);
        return activity.getId();
    }

    public void update(Long id, SeckillInput input) {
        SeckillActivity activity = require(id);
        // 已扣减的量 = total - available，调整总量时保持已扣减量不变
        int sold = activity.getTotalStock() - activity.getAvailableStock();
        apply(activity, input);
        activity.setTotalStock(input.totalStock());
        activity.setAvailableStock(Math.max(input.totalStock() - sold, 0));
        activityMapper.updateById(activity);
    }

    public void updateStatus(Long id, Integer status) {
        SeckillActivity activity = require(id);
        activity.setStatus(status != null && status == SeckillActivity.STATUS_ON
                ? SeckillActivity.STATUS_ON : SeckillActivity.STATUS_OFF);
        activityMapper.updateById(activity);
    }

    public void delete(Long id) {
        activityMapper.deleteById(require(id).getId());
    }

    // ============ 内部 ============

    private SeckillActivity require(Long id) {
        SeckillActivity activity = activityMapper.selectById(id);
        if (activity == null) {
            throw new BizException("秒杀活动不存在");
        }
        return activity;
    }

    private SeckillActivity apply(SeckillActivity activity, SeckillInput input) {
        if (input.skuId() == null) {
            throw new BizException("请选择秒杀规格");
        }
        ProductSku sku = skuMapper.selectById(input.skuId());
        if (sku == null || sku.getStatus() == 0) {
            throw new BizException("秒杀规格不存在或已停售");
        }
        if (input.seckillPrice() == null || input.seckillPrice() < 1) {
            throw new BizException("秒杀价必须大于 0");
        }
        if (input.seckillPrice() > sku.getPrice()) {
            throw new BizException("秒杀价不能高于原价（" + sku.getPrice() + " 分）");
        }
        if (input.totalStock() == null || input.totalStock() < 1) {
            throw new BizException("秒杀总量必须大于 0");
        }
        if (input.startTime() == null || input.endTime() == null
                || !input.endTime().isAfter(input.startTime())) {
            throw new BizException("活动结束时间必须晚于开始时间");
        }
        activity.setProductId(sku.getProductId());
        activity.setSkuId(sku.getId());
        activity.setSeckillPrice(input.seckillPrice());
        activity.setLimitPerUser(input.limitPerUser() == null || input.limitPerUser() < 1
                ? 1 : Math.min(input.limitPerUser(), 99));
        activity.setStartTime(input.startTime());
        activity.setEndTime(input.endTime());
        activity.setStatus(input.status() != null && input.status() == SeckillActivity.STATUS_OFF
                ? SeckillActivity.STATUS_OFF : SeckillActivity.STATUS_ON);
        return activity;
    }

    private List<SeckillVO> toVOs(List<SeckillActivity> list) {
        if (list.isEmpty()) {
            return List.of();
        }
        List<Long> skuIds = list.stream().map(SeckillActivity::getSkuId).toList();
        List<Long> productIds = list.stream().map(SeckillActivity::getProductId).distinct().toList();
        Map<Long, ProductSku> skuById = skuMapper.selectBatchIds(skuIds).stream()
                .collect(Collectors.toMap(ProductSku::getId, s -> s, (a, b) -> a));
        Map<Long, Product> productById = productIds.isEmpty() ? Map.of()
                : productMapper.selectBatchIds(productIds).stream()
                .collect(Collectors.toMap(Product::getId, p -> p, (a, b) -> a));
        return list.stream().map(a -> {
            Product product = productById.get(a.getProductId());
            ProductSku sku = skuById.get(a.getSkuId());
            return new SeckillVO(a.getId(), a.getProductId(),
                    product == null ? "" : product.getName(),
                    product == null ? "" : product.getMainImage(),
                    sku == null ? "" : sku.getSpec(),
                    sku == null ? null : sku.getPrice(),
                    a.getSeckillPrice(), a.getTotalStock(), a.getAvailableStock(), a.getLimitPerUser(),
                    a.getStartTime(), a.getEndTime(), a.getStatus());
        }).toList();
    }
}
