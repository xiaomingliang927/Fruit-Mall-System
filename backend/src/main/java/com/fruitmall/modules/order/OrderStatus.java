package com.fruitmall.modules.order;

/** 订单状态机：与设计文档 4.3 节保持一致 */
public final class OrderStatus {

    public static final int PENDING_PAY = 10;
    public static final int PENDING_SHIP = 20;
    public static final int PENDING_RECEIVE = 30;
    public static final int COMPLETED = 40;
    public static final int CANCELLED = 50;
    public static final int REFUNDING = 60;
    public static final int REFUNDED = 70;

    private OrderStatus() {
    }

    public static String textOf(int status) {
        return switch (status) {
            case PENDING_PAY -> "待支付";
            case PENDING_SHIP -> "待发货";
            case PENDING_RECEIVE -> "待收货";
            case COMPLETED -> "已完成";
            case CANCELLED -> "已取消";
            case REFUNDING -> "售后中";
            case REFUNDED -> "已退款";
            default -> "未知状态";
        };
    }
}
