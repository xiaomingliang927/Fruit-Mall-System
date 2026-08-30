package com.fruitmall.admin;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fruitmall.common.BizException;
import com.fruitmall.common.PageResult;
import com.fruitmall.modules.order.Order;
import com.fruitmall.modules.order.OrderItem;
import com.fruitmall.modules.order.OrderItemMapper;
import com.fruitmall.modules.order.OrderMapper;
import com.fruitmall.modules.order.OrderStatus;
import com.fruitmall.modules.user.User;
import com.fruitmall.modules.user.UserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AdminOrderService {

    private final OrderMapper orderMapper;
    private final OrderItemMapper orderItemMapper;
    private final UserMapper userMapper;
    private final com.fruitmall.modules.product.ProductSkuMapper skuMapper;

    public record OrderVO(Long id, String orderNo, Long userId, String userNickname, String userPhone,
                          Integer status, String statusText, Integer totalAmount, Integer payAmount,
                          String trackingNo, LocalDateTime createdAt, LocalDateTime paidAt) {
    }

    public record ItemVO(String productName, String skuSpec, String image,
                         Integer price, Integer quantity, Integer subtotal) {
    }

    public record OrderDetailVO(Long id, String orderNo, Integer status, String statusText,
                                Integer totalAmount, Integer payAmount, Integer freight, String remark,
                                String trackingNo, String addressSnapshot, LocalDateTime createdAt,
                                LocalDateTime paidAt, List<ItemVO> items) {
    }

    public PageResult<OrderVO> page(Integer status, String keyword, long page, long size) {
        Page<Order> result = orderMapper.selectPage(new Page<>(page, size),
                new LambdaQueryWrapper<Order>()
                        .eq(status != null, Order::getStatus, status)
                        .like(StringUtils.hasText(keyword), Order::getOrderNo, keyword)
                        .orderByDesc(Order::getId));
        List<Order> orders = result.getRecords();
        if (orders.isEmpty()) {
            return PageResult.of(result, o -> toVO(o, Map.of()));
        }
        Map<Long, User> userMap = userMapper.selectBatchIds(
                        orders.stream().map(Order::getUserId).distinct().toList()).stream()
                .collect(Collectors.toMap(User::getId, u -> u));
        return PageResult.of(result, o -> toVO(o, userMap));
    }

    private OrderVO toVO(Order o, Map<Long, User> userMap) {
        User user = userMap.get(o.getUserId());
        return new OrderVO(o.getId(), o.getOrderNo(), o.getUserId(),
                user != null ? user.getNickname() : null,
                user != null ? user.getPhone() : null,
                o.getStatus(), OrderStatus.textOf(o.getStatus()),
                o.getTotalAmount(), o.getPayAmount(), o.getTrackingNo(),
                o.getCreatedAt(), o.getPaidAt());
    }

    /** 导出用：不分页拉取订单（与列表同筛选条件，上限 5000 条防内存滥用） */
    public List<OrderVO> listForExport(Integer status, String keyword) {
        List<Order> orders = orderMapper.selectList(new LambdaQueryWrapper<Order>()
                .eq(status != null, Order::getStatus, status)
                .like(StringUtils.hasText(keyword), Order::getOrderNo, keyword)
                .orderByDesc(Order::getId)
                .last("LIMIT 5000"));
        if (orders.isEmpty()) {
            return List.of();
        }
        Map<Long, User> userMap = userMapper.selectBatchIds(
                        orders.stream().map(Order::getUserId).distinct().toList()).stream()
                .collect(Collectors.toMap(User::getId, u -> u));
        return orders.stream().map(o -> toVO(o, userMap)).toList();
    }

    public OrderDetailVO detail(Long id) {
        Order order = orderMapper.selectById(id);
        if (order == null) {
            throw new BizException("订单不存在");
        }
        List<ItemVO> items = orderItemMapper.selectList(new LambdaQueryWrapper<OrderItem>()
                        .eq(OrderItem::getOrderId, order.getId()))
                .stream()
                .map(i -> new ItemVO(i.getProductName(), i.getSkuSpec(), i.getImage(),
                        i.getPrice(), i.getQuantity(), i.getSubtotal()))
                .toList();
        return new OrderDetailVO(order.getId(), order.getOrderNo(), order.getStatus(),
                OrderStatus.textOf(order.getStatus()), order.getTotalAmount(), order.getPayAmount(),
                order.getFreight(), order.getRemark(), order.getTrackingNo(),
                order.getAddressSnapshot(), order.getCreatedAt(), order.getPaidAt(), items);
    }

    /** 发货：待发货 → 待收货，录入运单号 */
    @Transactional
    public void ship(Long id, String trackingNo) {
        if (!StringUtils.hasText(trackingNo)) {
            throw new BizException("请填写快递运单号");
        }
        Order order = orderMapper.selectById(id);
        if (order == null) {
            throw new BizException("订单不存在");
        }
        if (order.getStatus() != OrderStatus.PENDING_SHIP) {
            throw new BizException("只有「待发货」订单可以发货");
        }
        order.setStatus(OrderStatus.PENDING_RECEIVE);
        order.setTrackingNo(trackingNo);
        order.setShippedAt(LocalDateTime.now());
        orderMapper.updateById(order);
    }

    /** 管理员取消：仅限未支付订单（已支付订单走售后流程，M2 开发），取消后回补库存 */
    @Transactional
    public void cancel(Long id) {
        Order order = orderMapper.selectById(id);
        if (order == null) {
            throw new BizException("订单不存在");
        }
        if (order.getStatus() != OrderStatus.PENDING_PAY) {
            throw new BizException("已支付订单不能直接取消，请走售后流程（M2 提供）");
        }
        order.setStatus(OrderStatus.CANCELLED);
        orderMapper.updateById(order);
        List<OrderItem> items = orderItemMapper.selectList(new LambdaQueryWrapper<OrderItem>()
                .eq(OrderItem::getOrderId, order.getId()));
        for (OrderItem item : items) {
            skuMapper.increaseStock(item.getSkuId(), item.getQuantity());
        }
    }
}
