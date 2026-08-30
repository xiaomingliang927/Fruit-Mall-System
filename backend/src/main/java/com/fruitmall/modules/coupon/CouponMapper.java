package com.fruitmall.modules.coupon;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface CouponMapper extends BaseMapper<Coupon> {

    /** 乐观扣减发放额度：剩余>0 时 +1，返回影响行数（0=已抢完） */
    @Update("UPDATE coupon SET issued_count = issued_count + 1 "
            + "WHERE id = #{id} AND status = 1 AND issued_count < total_count "
            + "AND start_time <= NOW() AND end_time >= NOW()")
    int tryIssue(@Param("id") Long id);
}
