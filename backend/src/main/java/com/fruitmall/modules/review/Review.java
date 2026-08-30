package com.fruitmall.modules.review;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/** 商品评价（V8 建表；订单明细粒度，一明细一评） */
@Data
@TableName("review")
public class Review {

    public static final int STATUS_HIDDEN = 0;
    public static final int STATUS_VISIBLE = 1;

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long orderId;

    private String orderNo;

    private Long orderItemId;

    private Long userId;

    private Long productId;

    /** 评分 1~5 */
    private Integer rating;

    private String content;

    /** 晒图 URL JSON 数组 */
    private String images;

    private Boolean isAnonymous;

    /** 0 隐藏 1 显示 */
    private Integer status;

    private String adminReply;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
