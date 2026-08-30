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

    /** 在售商品按一级分类的销量占比 */
    @Select("SELECT c.name AS name, IFNULL(SUM(p.sales), 0) AS value FROM product p "
            + "JOIN category c ON p.category_id = c.id WHERE p.status = 1 "
            + "GROUP BY c.id, c.name ORDER BY value DESC")
    List<Map<String, Object>> categorySales();

    /** 各状态订单数量分布 */
    @Select("SELECT status AS status, COUNT(*) AS count FROM `order` GROUP BY status")
    List<Map<String, Object>> statusDistribution();
}
