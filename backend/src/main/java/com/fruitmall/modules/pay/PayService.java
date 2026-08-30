package com.fruitmall.modules.pay;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fruitmall.common.BizException;
import com.fruitmall.modules.order.Order;
import com.fruitmall.modules.order.OrderMapper;
import com.fruitmall.modules.order.OrderService;
import com.fruitmall.modules.order.OrderStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class PayService {

    private final OrderMapper orderMapper;
    private final OrderService orderService;

    /**
     * M1 开发模式：模拟支付成功，驱动订单「待支付 → 待发货」。
     * TODO(M2): 接入微信支付
     *   1) prepay：小程序 JSAPI（需 openid）/ 网站 Native 扫码，调用微信统一下单；
     *   2) notify：支付回调验签 + 幂等处理（复用 OrderService.markPaid）；
     *   3) 对账：每日下载对账单核对 payment 流水。
     */
    public void mockPay(Long userId, String orderNo) {
        Order order = orderMapper.selectOne(new LambdaQueryWrapper<Order>()
                .eq(Order::getOrderNo, orderNo));
        if (order == null || !order.getUserId().equals(userId)) {
            throw new BizException("订单不存在");
        }
        if (order.getStatus() != OrderStatus.PENDING_PAY) {
            throw new BizException("订单当前状态不可支付");
        }
        orderService.markPaid(orderNo, "MOCK-" + orderNo, "MOCK");
    }
}
