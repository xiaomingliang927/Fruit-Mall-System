package com.fruitmall.modules.refund;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fruitmall.common.BizException;
import com.fruitmall.common.PageResult;
import com.fruitmall.modules.order.Order;
import com.fruitmall.modules.order.OrderItem;
import com.fruitmall.modules.order.OrderItemMapper;
import com.fruitmall.modules.order.OrderMapper;
import com.fruitmall.modules.order.OrderStatus;
import com.fruitmall.modules.product.ProductSkuMapper;
import com.fruitmall.modules.user.User;
import com.fruitmall.modules.user.UserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class RefundService {

    /** 坏果包赔自动秒审阈值：50 元 */
    public static final int AUTO_APPROVE_THRESHOLD = 5000;

    private final RefundMapper refundMapper;
    private final OrderMapper orderMapper;
    private final OrderItemMapper orderItemMapper;
    private final ProductSkuMapper skuMapper;
    private final UserMapper userMapper;

    public record RefundVO(Long id, String refundNo, String orderNo, String typeText, String statusText,
                           Integer type, Integer status, String reason, Integer amount,
                           String auditRemark, LocalDateTime createdAt,
                           Long productId, String productName, String productImage) {
    }

    // ============ 用户侧 ============

    /** 申请售后：整单退款；≤50 元自动秒审退款，否则进入人工审核 */
    @Transactional
    public RefundVO apply(Long userId, String orderNo, Integer type, String reason) {
        if (type == null || (type != Refund.TYPE_ONLY_REFUND && type != Refund.TYPE_RETURN_REFUND)) {
            throw new BizException("请选择售后类型");
        }
        if (!org.springframework.util.StringUtils.hasText(reason)) {
            throw new BizException("请填写申请原因");
        }
        Order order = orderMapper.selectOne(new LambdaQueryWrapper<Order>()
                .eq(Order::getOrderNo, orderNo));
        if (order == null || !order.getUserId().equals(userId)) {
            throw new BizException("订单不存在");
        }
        if (order.getStatus() != OrderStatus.PENDING_SHIP
                && order.getStatus() != OrderStatus.PENDING_RECEIVE
                && order.getStatus() != OrderStatus.COMPLETED
                && order.getStatus() != OrderStatus.REFUNDING) {
            throw new BizException("当前订单状态不可申请售后");
        }
        Long active = refundMapper.selectCount(new LambdaQueryWrapper<Refund>()
                .eq(Refund::getOrderNo, orderNo)
                .in(Refund::getStatus, Refund.STATUS_PENDING, Refund.STATUS_APPROVED, Refund.STATUS_WAIT_RETURN));
        if (active > 0) {
            throw new BizException("该订单已有进行中的售后申请，请勿重复提交");
        }

        Refund refund = new Refund();
        refund.setRefundNo(generateRefundNo());
        refund.setOrderNo(orderNo);
        refund.setUserId(userId);
        refund.setType(type);
        refund.setReason(reason);
        refund.setAmount(order.getPayAmount());
        refund.setStatus(Refund.STATUS_PENDING);
        refund.setPrevStatus(order.getStatus());
        refundMapper.insert(refund);

        order.setStatus(OrderStatus.REFUNDING);
        orderMapper.updateById(order);

        if (refund.getAmount() <= AUTO_APPROVE_THRESHOLD) {
            approveInternal(refund, null, "坏果包赔自动退款");
        }
        return toVO(refund, orderItemMapper.selectList(new LambdaQueryWrapper<OrderItem>()
                .eq(OrderItem::getOrderId, order.getId())));
    }

    /** 用户撤销待审核的售后申请，订单恢复原状态 */
    @Transactional
    public void withdraw(Long userId, String refundNo) {
        Refund refund = getOwn(userId, refundNo);
        if (refund.getStatus() != Refund.STATUS_PENDING) {
            throw new BizException("当前售后进度不可撤销");
        }
        refund.setStatus(Refund.STATUS_WITHDRAWN);
        refund.setAuditRemark("用户撤销");
        refund.setAuditTime(LocalDateTime.now());
        refundMapper.updateById(refund);
        restoreOrderStatus(refund);
    }

    public PageResult<RefundVO> myRefunds(Long userId, long page, long size) {
        Page<Refund> result = refundMapper.selectPage(new Page<>(page, Math.min(size, 50)),
                new LambdaQueryWrapper<Refund>()
                        .eq(Refund::getUserId, userId)
                        .orderByDesc(Refund::getId));
        return PageResult.of(result, this::toVOBasic);
    }

    public RefundVO detail(Long userId, String refundNo) {
        Refund refund = getOwn(userId, refundNo);
        return toVO(refund, itemsOf(refund.getOrderNo()));
    }

    // ============ 管理侧 ============

    public record AdminRefundVO(Long id, String refundNo, String orderNo, Integer type, String typeText,
                                Integer status, String statusText, String reason, Integer amount,
                                String auditRemark, LocalDateTime createdAt,
                                Long userId, String userNickname, String userPhone) {
    }

    public PageResult<AdminRefundVO> adminPage(Integer status, String keyword, long page, long size) {
        Page<Refund> result = refundMapper.selectPage(new Page<>(page, Math.min(size, 50)),
                new LambdaQueryWrapper<Refund>()
                        .eq(status != null, Refund::getStatus, status)
                        .and(org.springframework.util.StringUtils.hasText(keyword), w -> w
                                .like(Refund::getRefundNo, keyword)
                                .or().like(Refund::getOrderNo, keyword))
                        .orderByDesc(Refund::getId));
        List<Refund> refunds = result.getRecords();
        Map<Long, User> userMap = refunds.isEmpty() ? Map.of()
                : userMapper.selectBatchIds(refunds.stream().map(Refund::getUserId).distinct().toList())
                        .stream().collect(Collectors.toMap(User::getId, u -> u));
        return PageResult.of(result, r -> {
            User u = userMap.get(r.getUserId());
            return new AdminRefundVO(r.getId(), r.getRefundNo(), r.getOrderNo(), r.getType(),
                    typeText(r.getType()), r.getStatus(), statusText(r.getStatus()), r.getReason(),
                    r.getAmount(), r.getAuditRemark(), r.getCreatedAt(),
                    r.getUserId(), u == null ? null : u.getNickname(), u == null ? null : u.getPhone());
        });
    }

    /** 审核通过：完成退款（M2 为模拟打款），订单转已退款，回补库存 */
    @Transactional
    public void adminApprove(Long adminId, Long id, String remark) {
        Refund refund = refundMapper.selectById(id);
        if (refund == null) throw new BizException("售后单不存在");
        if (refund.getStatus() != Refund.STATUS_PENDING) throw new BizException("该售后单当前状态不可审核");
        approveInternal(refund, adminId, StringUtilsOrDefault(remark, "审核通过"));
    }

    /** 审核驳回：订单恢复申请前状态 */
    @Transactional
    public void adminReject(Long adminId, Long id, String remark) {
        Refund refund = refundMapper.selectById(id);
        if (refund == null) throw new BizException("售后单不存在");
        if (refund.getStatus() != Refund.STATUS_PENDING) throw new BizException("该售后单当前状态不可审核");
        refund.setStatus(Refund.STATUS_REJECTED);
        refund.setAuditBy(adminId);
        refund.setAuditTime(LocalDateTime.now());
        refund.setAuditRemark(org.springframework.util.StringUtils.hasText(remark) ? remark : "不符合退款条件");
        refundMapper.updateById(refund);
        restoreOrderStatus(refund);
    }

    // ============ 内部 ============

    private void approveInternal(Refund refund, Long adminId, String remark) {
        refund.setStatus(Refund.STATUS_REFUNDED);
        refund.setAuditBy(adminId);
        refund.setAuditTime(LocalDateTime.now());
        refund.setAuditRemark(remark);
        refund.setRefundTime(LocalDateTime.now());
        refundMapper.updateById(refund);

        Order order = orderMapper.selectOne(new LambdaQueryWrapper<Order>()
                .eq(Order::getOrderNo, refund.getOrderNo()));
        if (order != null && order.getStatus() == OrderStatus.REFUNDING) {
            order.setStatus(OrderStatus.REFUNDED);
            orderMapper.updateById(order);
        }
        // 回补库存（未发货退款/退货完成，货可再售）
        List<OrderItem> items = itemsOf(refund.getOrderNo());
        for (OrderItem item : items) {
            skuMapper.increaseStock(item.getSkuId(), item.getQuantity());
        }
    }

    private void restoreOrderStatus(Refund refund) {
        Order order = orderMapper.selectOne(new LambdaQueryWrapper<Order>()
                .eq(Order::getOrderNo, refund.getOrderNo()));
        if (order != null && order.getStatus() == OrderStatus.REFUNDING && refund.getPrevStatus() != null) {
            order.setStatus(refund.getPrevStatus());
            orderMapper.updateById(order);
        }
    }

    private List<OrderItem> itemsOf(String orderNo) {
		Order order = orderMapper.selectOne(new LambdaQueryWrapper<Order>()
				.eq(Order::getOrderNo, orderNo));
		return orderItemMapper.selectList(new LambdaQueryWrapper<OrderItem>()
				.eq(OrderItem::getOrderId, order.getId()));
	}

	private Refund getOwn(Long userId, String refundNo) {
        Refund refund = refundMapper.selectOne(new LambdaQueryWrapper<Refund>()
                .eq(Refund::getRefundNo, refundNo));
        if (refund == null || !refund.getUserId().equals(userId)) {
            throw new BizException("售后单不存在");
        }
        return refund;
    }

    private RefundVO toVOBasic(Refund r) {
        return new RefundVO(r.getId(), r.getRefundNo(), r.getOrderNo(), typeText(r.getType()),
                statusText(r.getStatus()), r.getType(), r.getStatus(), r.getReason(), r.getAmount(),
                r.getAuditRemark(), r.getCreatedAt(), null, null, null);
    }

    private RefundVO toVO(Refund r, List<OrderItem> items) {
        OrderItem first = items.isEmpty() ? null : items.get(0);
        return new RefundVO(r.getId(), r.getRefundNo(), r.getOrderNo(), typeText(r.getType()),
                statusText(r.getStatus()), r.getType(), r.getStatus(), r.getReason(), r.getAmount(),
                r.getAuditRemark(), r.getCreatedAt(),
                first == null ? null : first.getProductId(),
                first == null ? null : first.getProductName(),
                first == null ? null : first.getImage());
    }

    public static String typeText(Integer type) {
        return type != null && type == Refund.TYPE_RETURN_REFUND ? "退货退款" : "仅退款";
    }

    public static String statusText(Integer status) {
        if (status == null) return "-";
        return switch (status) {
            case Refund.STATUS_PENDING -> "待审核";
            case Refund.STATUS_APPROVED -> "已同意待退款";
            case Refund.STATUS_REJECTED -> "已拒绝";
            case Refund.STATUS_WAIT_RETURN -> "待用户寄回";
            case Refund.STATUS_REFUNDED -> "已退款";
            case Refund.STATUS_WITHDRAWN -> "已撤销";
            default -> "未知";
        };
    }

    private String generateRefundNo() {
        return "RF" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyMMddHHmmss"))
                + String.format("%04d", ThreadLocalRandom.current().nextInt(10000));
    }

    private static String StringUtilsOrDefault(String v, String def) {
        return org.springframework.util.StringUtils.hasText(v) ? v : def;
    }
}
