package com.fruitmall.modules.review;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.Map;

@Mapper
public interface ReviewMapper extends BaseMapper<Review> {

    /** 商品评分聚合（仅可见评价）：count + 平均分×10（整数，如 48 = 4.8） */
    @Select("SELECT COUNT(*) AS cnt, IFNULL(ROUND(AVG(rating) * 10), 0) AS avgX10 "
            + "FROM review WHERE product_id = #{productId} AND status = 1")
    Map<String, Object> ratingAggByProduct(@Param("productId") Long productId);
}
