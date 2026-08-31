package com.fruitmall.modules.seckill;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("seckill_activity")
public class SeckillActivity {

    /** 启用 */
    public static final int STATUS_ON = 1;
    /** 停用 */
    public static final int STATUS_OFF = 0;

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long productId;

    private Long skuId;

    /** 秒杀价，分 */
    private Integer seckillPrice;

    private Integer totalStock;

    private Integer availableStock;

    /** 每人限购（当前模型：一场一单，件数不超过该值） */
    private Integer limitPerUser;

    private LocalDateTime startTime;

    private LocalDateTime endTime;

    private Integer status;

    private LocalDateTime createdAt;
}
