package com.fruitmall.modules.favorite;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface FavoriteMapper extends BaseMapper<Favorite> {

    @Select("SELECT COUNT(*) FROM favorite WHERE user_id = #{userId} AND product_id = #{productId}")
    long countByUserAndProduct(@Param("userId") Long userId, @Param("productId") Long productId);

    @Delete("DELETE FROM favorite WHERE user_id = #{userId} AND product_id = #{productId}")
    int deleteByUserAndProduct(@Param("userId") Long userId, @Param("productId") Long productId);
}
