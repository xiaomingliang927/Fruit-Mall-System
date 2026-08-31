package com.fruitmall.modules.user;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.Map;

/** C 端会员概览用的单用户消费聚合（管理端另有整页批量版本 AdminMemberMapper） */
@Mapper
public interface MemberStatMapper {

    /** 单个用户的已支付订单数与累计消费（分）；无订单时两项均返回 0 */
    @Select("SELECT COUNT(*) AS orderCount, IFNULL(SUM(pay_amount), 0) AS totalGmv "
            + "FROM `order` WHERE status IN (20, 30, 40) AND user_id = #{userId}")
    Map<String, Object> orderAggByUser(@Param("userId") Long userId);
}
