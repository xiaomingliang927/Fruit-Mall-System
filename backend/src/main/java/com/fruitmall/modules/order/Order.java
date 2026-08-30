package com.fruitmall.modules.order;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("`order`")
public class Order {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 业务订单号：FM + yyMMddHHmmss + 4位随机 */
    private String orderNo;

    private Long userId;

    /** 见 OrderStatus */
    private Integer status;

    /** 商品总额，单位：分 */
    private Integer totalAmount;

    /** 实付金额，单位：分 */
    private Integer payAmount;

    /** 运费，单位：分（M1 统一包邮） */
    private Integer freight;

    /** 收货地址 JSON 快照，下单后不受改址影响 */
    private String addressSnapshot;

    /** 1 快递 2 同城配送 3 门店自提（M1 仅快递） */
    private Integer deliveryType;

    /** 快递运单号，发货时录入 */
    private String trackingNo;

    /** 使用的用户优惠券ID（取消订单时退回） */
    private Long userCouponId;

    private String remark;

    private LocalDateTime paidAt;

    private LocalDateTime shippedAt;

    private LocalDateTime completedAt;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
