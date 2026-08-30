package com.fruitmall.admin;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;
import java.util.Map;

/** 会员中心聚合查询 */
@Mapper
public interface AdminMemberMapper {

    /** 按用户聚合已支付订单数与累计消费（分） */
    @Select("<script>SELECT user_id AS userId, COUNT(*) AS orderCount, IFNULL(SUM(pay_amount), 0) AS totalGmv "
            + "FROM `order` WHERE status IN (20, 30, 40) AND user_id IN "
            + "<foreach collection='userIds' item='id' open='(' separator=',' close=')'>#{id}</foreach> "
            + "GROUP BY user_id</script>")
    List<Map<String, Object>> orderAggByUsers(@org.apache.ibatis.annotations.Param("userIds") List<Long> userIds);
}
