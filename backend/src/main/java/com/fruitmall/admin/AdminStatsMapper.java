package com.fruitmall.admin;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;
import java.util.Map;

@Mapper
public interface AdminStatsMapper {

    @Select("SELECT IFNULL(SUM(pay_amount), 0) FROM `order` WHERE status IN (20, 30, 40) AND paid_at >= CURDATE()")
    long todayGmv();

    @Select("SELECT COUNT(*) FROM `order` WHERE paid_at >= CURDATE()")
    long todayPaidOrders();

    @Select("SELECT COUNT(*) FROM `user`")
    long userCount();

    @Select("SELECT COUNT(*) FROM product WHERE status = 1")
    long onSaleProductCount();

    @Select("SELECT COUNT(*) FROM `order` WHERE status = 20")
    long pendingShipCount();

    @Select("SELECT id, name, sales, main_image AS mainImage FROM product ORDER BY sales DESC LIMIT 10")
    List<Map<String, Object>> topProducts();

    /** 近 7 日每日销售额与支付订单数（可能缺天，由调用方补零） */
    @Select("SELECT DATE_FORMAT(paid_at, '%m-%d') AS day, IFNULL(SUM(pay_amount), 0) AS gmv, COUNT(*) AS orders "
            + "FROM `order` WHERE status IN (20, 30, 40) AND paid_at >= DATE_SUB(CURDATE(), INTERVAL 6 DAY) "
            + "GROUP BY day ORDER BY MIN(paid_at)")
    List<Map<String, Object>> salesTrend();

    /** 各状态订单数量分布 */
    @Select("SELECT status AS status, COUNT(*) AS count FROM `order` GROUP BY status")
    List<Map<String, Object>> statusDistribution();

    /** 已支付订单按一级分类的营业额（分）与订单数 */
    @Select("SELECT c.name AS name, IFNULL(SUM(oi.subtotal), 0) AS gmv, COUNT(DISTINCT o.id) AS orders "
            + "FROM order_item oi "
            + "JOIN product p ON oi.product_id = p.id "
            + "JOIN category c ON p.category_id = c.id "
            + "JOIN `order` o ON oi.order_id = o.id "
            + "WHERE o.status IN (20, 30, 40) "
            + "GROUP BY c.id, c.name ORDER BY gmv DESC")
    List<Map<String, Object>> categoryGmv();

    /** 营业额日明细（含起止日期，闭区间） */
    @Select("SELECT DATE_FORMAT(paid_at, '%Y-%m-%d') AS day, COUNT(*) AS orders, "
            + "IFNULL(SUM(pay_amount), 0) AS gmv "
            + "FROM `order` WHERE status IN (20, 30, 40) "
            + "AND paid_at >= #{from} AND paid_at < DATE_ADD(#{to}, INTERVAL 1 DAY) "
            + "GROUP BY day ORDER BY day")
    List<Map<String, Object>> revenueDaily(@org.apache.ibatis.annotations.Param("from") String from,
                                           @org.apache.ibatis.annotations.Param("to") String to);

    /** 营业额区间汇总 */
    @Select("SELECT COUNT(*) AS orders, IFNULL(SUM(pay_amount), 0) AS gmv "
            + "FROM `order` WHERE status IN (20, 30, 40) "
            + "AND paid_at >= #{from} AND paid_at < DATE_ADD(#{to}, INTERVAL 1 DAY)")
    Map<String, Object> revenueTotal(@org.apache.ibatis.annotations.Param("from") String from,
                                     @org.apache.ibatis.annotations.Param("to") String to);
}
