package com.fruitmall.modules.order;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fruitmall.common.BizException;
import com.fruitmall.common.PageResult;
import com.fruitmall.modules.order.dto.CreateOrderRequest;
import com.fruitmall.modules.pay.Payment;
import com.fruitmall.modules.pay.PaymentMapper;
import com.fruitmall.modules.product.Product;
import com.fruitmall.modules.product.ProductMapper;
import com.fruitmall.modules.product.ProductSku;
import com.fruitmall.modules.product.ProductSkuMapper;
import com.fruitmall.modules.user.UserAddress;
import com.fruitmall.modules.user.UserAddressMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

@Service
@RequiredArgsConstructor
public class OrderService {

    private final OrderMapper orderMapper;
    private final OrderItemMapper orderItemMapper;
    private final ProductSkuMapper skuMapper;
    private final ProductMapper productMapper;
    private final UserAddressMapper addressMapper;
    private final PaymentMapper paymentMapper;
    private final ObjectMapper objectMapper;

    public record OrderListVO(Long id, String orderNo, Integer status, String statusText,
                              Integer totalAmount, Integer payAmount, LocalDateTime createdAt) {
    }

    public record ItemVO(String productName, String skuSpec, String image,
                         Integer price, Integer quantity, Integer subtotal) {
    }

    public record OrderDetailVO(Long id, String orderNo, Integer status, String statusText,
                                Integer totalAmount, Integer payAmount, Integer freight, String remark,
                                String trackingNo, String addressSnapshot, LocalDateTime createdAt,
                                LocalDateTime paidAt, List<ItemVO> items) {
    }

    /** 下单：校验地址与商品 → 乐观锁库存（防超卖）→ 生成订单与明细快照 */
    @Transactional
    public String createOrder(Long userId, CreateOrderRequest request) {
        UserAddress address = addressMapper.selectById(request.addressId());
        if (address == null || !address.getUserId().equals(userId)) {
            throw new BizException("收货地址不存在");
        }
        int totalAmount = 0;
        List<OrderItem> items = new ArrayList<>();
        for (CreateOrderRequest.Item reqItem : request.items()) {
            ProductSku sku = skuMapper.selectById(reqItem.skuId());
            if (sku == null || sku.getStatus() == 0) {
                throw new BizException("商品规格不存在或已停售");
            }
            Product product = productMapper.selectById(sku.getProductId());
            if (product == null || product.getStatus() == 0) {
                throw new BizException("商品已下架：" + (product == null ? sku.getProductId() : product.getName()));
            }
            if (skuMapper.decreaseStock(sku.getId(), reqItem.quantity()) == 0) {
                throw new BizException("「" + product.getName() + " " + sku.getSpec() + "」库存不足");
            }
            OrderItem item = new OrderItem();
            item.setProductId(product.getId());
            item.setSkuId(sku.getId());
            item.setProductName(product.getName());
            item.setSkuSpec(sku.getSpec());
            item.setImage(product.getMainImage());
            item.setPrice(sku.getPrice());
            item.setQuantity(reqItem.quantity());
            item.setSubtotal(sku.getPrice() * reqItem.quantity());
            items.add(item);
            totalAmount += item.getSubtotal();
        }
        Order order = new Order();
        order.setOrderNo(generateOrderNo());
        order.setUserId(userId);
        order.setStatus(OrderStatus.PENDING_PAY);
        order.setTotalAmount(totalAmount);
        order.setFreight(0);
        order.setPayAmount(totalAmount);
        order.setDeliveryType(1);
        order.setRemark(request.remark());
        order.setAddressSnapshot(toAddressJson(address));
        orderMapper.insert(order);
        for (OrderItem item : items) {
            item.setOrderId(order.getId());
            orderItemMapper.insert(item);
        }
        return order.getOrderNo();
    }

    /** 取消订单：仅待支付可取消，回补库存 */
    @Transactional
    public void cancelOrder(Long userId, String orderNo) {
        Order order = getOwnOrder(userId, orderNo);
        if (order.getStatus() != OrderStatus.PENDING_PAY) {
            throw new BizException("当前订单状态不可取消");
        }
        order.setStatus(OrderStatus.CANCELLED);
        orderMapper.updateById(order);
        List<OrderItem> items = orderItemMapper.selectList(new LambdaQueryWrapper<OrderItem>()
                .eq(OrderItem::getOrderId, order.getId()));
        for (OrderItem item : items) {
            skuMapper.increaseStock(item.getSkuId(), item.getQuantity());
        }
    }

    /** 支付成功回调（幂等）：待支付 → 待发货，写入支付流水 */
    @Transactional
    public void markPaid(String orderNo, String transactionId, String channel) {
        Order order = orderMapper.selectOne(new LambdaQueryWrapper<Order>()
                .eq(Order::getOrderNo, orderNo));
        if (order == null) {
            throw new BizException("订单不存在");
        }
        if (order.getStatus() != OrderStatus.PENDING_PAY) {
            return; // 重复回调直接忽略，保证幂等
        }
        order.setStatus(OrderStatus.PENDING_SHIP);
        order.setPaidAt(LocalDateTime.now());
        orderMapper.updateById(order);

        Payment payment = new Payment();
        payment.setOrderNo(orderNo);
        payment.setTransactionId(transactionId);
        payment.setChannel(channel);
        payment.setAmount(order.getPayAmount());
        payment.setStatus(1);
        payment.setPaidAt(order.getPaidAt());
        paymentMapper.insert(payment);
    }

    /** 确认收货：待收货 → 已完成 */
    @Transactional
    public void confirmOrder(Long userId, String orderNo) {
        Order order = getOwnOrder(userId, orderNo);
        if (order.getStatus() != OrderStatus.PENDING_RECEIVE) {
            throw new BizException("当前订单状态不可确认收货");
        }
        order.setStatus(OrderStatus.COMPLETED);
        order.setCompletedAt(LocalDateTime.now());
        orderMapper.updateById(order);
    }

    public PageResult<OrderListVO> pageMyOrders(Long userId, Integer status, long page, long size) {
        Page<Order> result = orderMapper.selectPage(new Page<>(page, size),
                new LambdaQueryWrapper<Order>()
                        .eq(Order::getUserId, userId)
                        .eq(status != null, Order::getStatus, status)
                        .orderByDesc(Order::getId));
        return PageResult.of(result, o -> new OrderListVO(
                o.getId(), o.getOrderNo(), o.getStatus(), OrderStatus.textOf(o.getStatus()),
                o.getTotalAmount(), o.getPayAmount(), o.getCreatedAt()));
    }

    public OrderDetailVO orderDetail(Long userId, String orderNo) {
        Order order = getOwnOrder(userId, orderNo);
        List<ItemVO> items = orderItemMapper.selectList(new LambdaQueryWrapper<OrderItem>()
                        .eq(OrderItem::getOrderId, order.getId()))
                .stream()
                .map(i -> new ItemVO(i.getProductName(), i.getSkuSpec(), i.getImage(),
                        i.getPrice(), i.getQuantity(), i.getSubtotal()))
                .toList();
        return new OrderDetailVO(order.getId(), order.getOrderNo(), order.getStatus(),
                OrderStatus.textOf(order.getStatus()), order.getTotalAmount(), order.getPayAmount(),
                order.getFreight(), order.getRemark(), order.getTrackingNo(),
                order.getAddressSnapshot(),
                order.getCreatedAt(), order.getPaidAt(), items);
    }

    private Order getOwnOrder(Long userId, String orderNo) {
        Order order = orderMapper.selectOne(new LambdaQueryWrapper<Order>()
                .eq(Order::getOrderNo, orderNo));
        if (order == null || !order.getUserId().equals(userId)) {
            throw new BizException("订单不存在");
        }
        return order;
    }

    private String generateOrderNo() {
        return "FM" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyMMddHHmmss"))
                + String.format("%04d", ThreadLocalRandom.current().nextInt(10000));
    }

    private String toAddressJson(UserAddress address) {
        Map<String, Object> snapshot = new LinkedHashMap<>();
        snapshot.put("receiver", address.getReceiver());
        snapshot.put("phone", address.getPhone());
        snapshot.put("province", address.getProvince());
        snapshot.put("city", address.getCity());
        snapshot.put("district", address.getDistrict());
        snapshot.put("detail", address.getDetail());
        try {
            return objectMapper.writeValueAsString(snapshot);
        } catch (JsonProcessingException e) {
            throw new BizException("收货地址序列化失败");
        }
    }
}
