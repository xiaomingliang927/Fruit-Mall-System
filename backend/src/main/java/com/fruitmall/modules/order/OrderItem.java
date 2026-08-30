package com.fruitmall.modules.order;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

/** 订单明细：商品名称/规格/图片/价格均为下单时快照 */
@Data
@TableName("order_item")
public class OrderItem {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long orderId;

    private Long productId;

    private Long skuId;

    private String productName;

    private String skuSpec;

    private String image;

    /** 成交单价，单位：分 */
    private Integer price;

    private Integer quantity;

    /** 小计 = price × quantity，单位：分 */
    private Integer subtotal;
}
