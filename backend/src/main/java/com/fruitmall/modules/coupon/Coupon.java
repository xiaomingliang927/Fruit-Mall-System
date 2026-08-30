package com.fruitmall.modules.coupon;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/** 优惠券模板（V8 建表） */
@Data
@TableName("coupon")
public class Coupon {

    public static final int TYPE_REDUCE = 1;      // 满减：满 threshold 减 amount
    public static final int TYPE_DISCOUNT = 2;    // 折扣：满 threshold 打 percent/10 折
    public static final int TYPE_NO_THRESHOLD = 3; // 无门槛立减

    @TableId(type = IdType.AUTO)
    private Long id;

    private String name;

    private Integer type;

    /** 使用门槛，分（0=无门槛） */
    private Integer thresholdAmount;

    /** 优惠金额，分（满减/无门槛） */
    private Integer discountAmount;

    /** 折扣率（折扣券，如 90 = 9 折） */
    private Integer discountPercent;

    private Integer totalCount;

    private Integer issuedCount;

    private Integer perUserLimit;

    private LocalDateTime startTime;

    private LocalDateTime endTime;

    /** 领取后有效天数 */
    private Integer validDays;

    private Integer status;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
