package com.fruitmall.modules.refund;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/** 售后退款单（V8 建表，V9 补 prev_status） */
@Data
@TableName("refund")
public class Refund {

    public static final int TYPE_ONLY_REFUND = 1;
    public static final int TYPE_RETURN_REFUND = 2;

    public static final int STATUS_PENDING = 0;
    public static final int STATUS_APPROVED = 1;
    public static final int STATUS_REJECTED = 2;
    public static final int STATUS_WAIT_RETURN = 3;
    public static final int STATUS_REFUNDED = 4;
    public static final int STATUS_WITHDRAWN = 5;

    @TableId(type = IdType.AUTO)
    private Long id;

    private String refundNo;

    private String orderNo;

    private Long userId;

    /** 1 仅退款 2 退货退款 */
    private Integer type;

    private String reason;

    /** 凭证图 URL JSON 数组（预留上传能力） */
    private String images;

    /** 申请退款金额，分（M2 为整单退款） */
    private Integer amount;

    private Integer status;

    /** 申请前订单状态（驳回/撤销时恢复） */
    private Integer prevStatus;

    private Long auditBy;

    private LocalDateTime auditTime;

    private String auditRemark;

    private LocalDateTime refundTime;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
