package com.fruitmall.modules.seckill;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("seckill_order")
public class SeckillOrder {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long seckillId;

    private Long userId;

    /** 生成的订单号（下单成功后回填） */
    private String orderNo;

    private Integer quantity;

    /** 成交秒杀价，分 */
    private Integer seckillPrice;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
