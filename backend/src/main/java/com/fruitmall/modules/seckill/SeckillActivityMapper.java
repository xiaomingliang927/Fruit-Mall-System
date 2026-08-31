package com.fruitmall.modules.seckill;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

import java.time.LocalDateTime;

@Mapper
public interface SeckillActivityMapper extends BaseMapper<SeckillActivity> {

    /**
     * 原子扣减秒杀库存：影响行数为 0 表示库存不足 / 活动停用 / 不在活动时间窗。
     * 与 product_sku.decreaseStock 同款「条件更新」防超卖模式，
     * 单实例下等价于 Redis 预扣；多实例部署仍是安全的（行锁串行化）。
     */
    @Update("""
            UPDATE seckill_activity
            SET available_stock = available_stock - #{quantity}
            WHERE id = #{id}
              AND status = 1
              AND available_stock >= #{quantity}
              AND start_time <= #{now}
              AND end_time > #{now}
            """)
    int decreaseStock(@Param("id") Long id, @Param("quantity") int quantity, @Param("now") LocalDateTime now);
}
