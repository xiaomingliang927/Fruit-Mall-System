package com.fruitmall.modules.pay;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/** 支付流水 */
@Data
@TableName("payment")
public class Payment {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String orderNo;

    /** 渠道交易号：微信支付为 transaction_id */
    private String transactionId;

    /** MOCK / WECHAT_JSAPI / WECHAT_NATIVE */
    private String channel;

    /** 实付金额，单位：分 */
    private Integer amount;

    /** 1 支付成功 */
    private Integer status;

    private LocalDateTime paidAt;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
