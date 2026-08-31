package com.fruitmall.modules.review;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fruitmall.common.BizException;
import com.fruitmall.common.HtmlSanitizer;
import com.fruitmall.common.PageResult;
import com.fruitmall.modules.order.Order;
import com.fruitmall.modules.order.OrderItem;
import com.fruitmall.modules.order.OrderItemMapper;
import com.fruitmall.modules.order.OrderMapper;
import com.fruitmall.modules.order.OrderStatus;
import com.fruitmall.modules.product.Product;
import com.fruitmall.modules.product.ProductMapper;
import com.fruitmall.modules.user.User;
import com.fruitmall.modules.user.UserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ReviewService {

    private final ReviewMapper reviewMapper;
    private final OrderMapper orderMapper;
    private final OrderItemMapper orderItemMapper;
    private final UserMapper userMapper;
    private final ProductMapper productMapper;

    public record ReviewVO(Long id, Integer rating, String content, Boolean isAnonymous,
                           String userNickname, String adminReply, String createdAt,
                           Long productId, String productName) {
    }

    public record OrderReviewItemVO(Long orderItemId, String productName, String skuSpec,
                                    Boolean reviewed) {
    }

    // ============ 用户侧 ============

    /** 评价：仅订单已完成；一明细一评；先发后审（默认可见，后台可隐藏） */
    public ReviewVO create(Long userId, String orderNo, Long orderItemId,
                           Integer rating, String content, Boolean isAnonymous) {
        if (rating == null || rating < 1 || rating > 5) throw new BizException("请给出 1~5 星评分");
        if (!org.springframework.util.StringUtils.hasText(content)) throw new BizException("请填写评价内容");
        Order order = orderMapper.selectOne(new LambdaQueryWrapper<Order>()
                .eq(Order::getOrderNo, orderNo));
        if (order == null || !order.getUserId().equals(userId)) throw new BizException("订单不存在");
        if (order.getStatus() != OrderStatus.COMPLETED) throw new BizException("订单完成后才能评价");

        OrderItem item = orderItemMapper.selectById(orderItemId);
        if (item == null || !item.getOrderId().equals(order.getId())) throw new BizException("订单明细不存在");
        Long reviewed = reviewMapper.selectCount(new LambdaQueryWrapper<Review>()
                .eq(Review::getOrderItemId, orderItemId));
        if (reviewed > 0) throw new BizException("该商品已评价过");

        Review review = new Review();
        review.setOrderId(order.getId());
        review.setOrderNo(orderNo);
        review.setOrderItemId(orderItemId);
        review.setUserId(userId);
        review.setProductId(item.getProductId());
        review.setRating(rating);
        review.setContent(HtmlSanitizer.sanitize(content));
        review.setIsAnonymous(Boolean.TRUE.equals(isAnonymous));
        review.setStatus(Review.STATUS_VISIBLE);
        reviewMapper.insert(review);
        return toVO(review, nicknameOf(userId, isAnonymous));
    }

    /** 我的订单评价进度：每明细是否已评 */
    public List<OrderReviewItemVO> orderReviewSummary(Long userId, String orderNo) {
        Order order = orderMapper.selectOne(new LambdaQueryWrapper<Order>()
                .eq(Order::getOrderNo, orderNo));
        if (order == null || !order.getUserId().equals(userId)) throw new BizException("订单不存在");
        List<OrderItem> items = orderItemMapper.selectList(new LambdaQueryWrapper<OrderItem>()
                .eq(OrderItem::getOrderId, order.getId()));
        Set<Long> reviewedItemIds = reviewMapper.selectList(new LambdaQueryWrapper<Review>()
                        .eq(Review::getOrderId, order.getId()))
                .stream().map(Review::getOrderItemId).collect(Collectors.toSet());
        return items.stream().map((it) -> new OrderReviewItemVO(
                it.getId(), it.getProductName(), it.getSkuSpec(),
                reviewedItemIds.contains(it.getId()))).toList();
    }

    /** 商品评价列表（公开，仅可见） */
    public PageResult<ReviewVO> productReviews(Long productId, long page, long size) {
        Page<Review> result = reviewMapper.selectPage(new Page<>(page, Math.min(size, 20)),
                new LambdaQueryWrapper<Review>()
                        .eq(Review::getProductId, productId)
                        .eq(Review::getStatus, Review.STATUS_VISIBLE)
                        .orderByDesc(Review::getId));
        return PageResult.of(result, r -> toVO(r, nicknameOf(r.getUserId(), r.getIsAnonymous())));
    }

    // ============ 管理侧 ============

    public record AdminReviewVO(Long id, Integer rating, String content, Boolean isAnonymous,
                                String userNickname, String productName, Integer status,
                                String adminReply, String createdAt) {
    }

    public PageResult<AdminReviewVO> adminPage(Integer status, Long productId, String keyword, long page, long size) {
        Page<Review> result = reviewMapper.selectPage(new Page<>(page, Math.min(size, 50)),
                new LambdaQueryWrapper<Review>()
                        .eq(status != null, Review::getStatus, status)
                        .eq(productId != null, Review::getProductId, productId)
                        .like(org.springframework.util.StringUtils.hasText(keyword), Review::getContent, keyword)
                        .orderByDesc(Review::getId));
        List<Review> reviews = result.getRecords();
        Map<Long, User> userMap = reviews.isEmpty() ? Map.of()
                : userMapper.selectBatchIds(reviews.stream().map(Review::getUserId).distinct().toList())
                        .stream().collect(Collectors.toMap(User::getId, u -> u));
        Map<Long, Product> productMap = reviews.isEmpty() ? Map.of()
                : productMapper.selectBatchIds(reviews.stream().map(Review::getProductId).distinct().toList())
                        .stream().collect(Collectors.toMap(Product::getId, p -> p));
        return PageResult.of(result, r -> {
            User u = userMap.get(r.getUserId());
            Product p = productMap.get(r.getProductId());
            return new AdminReviewVO(r.getId(), r.getRating(), r.getContent(), r.getIsAnonymous(),
                    u == null ? null : u.getNickname(),
                    p == null ? null : p.getName(),
                    r.getStatus(), r.getAdminReply(), fmtDateTime(r.getCreatedAt()));
        });
    }

    /** 商家回复 */
    public void adminReply(Long adminId, Long id, String reply) {
        Review review = getReview(id);
        review.setAdminReply(reply);
        reviewMapper.updateById(review);
    }

    /** 显示/隐藏 */
    public void adminSetStatus(Long adminId, Long id, Integer status) {
        if (status == null || (status != Review.STATUS_HIDDEN && status != Review.STATUS_VISIBLE)) {
            throw new BizException("status 不合法");
        }
        Review review = getReview(id);
        review.setStatus(status);
        reviewMapper.updateById(review);
    }

    private Review getReview(Long id) {
        Review review = reviewMapper.selectById(id);
        if (review == null) throw new BizException("评价不存在");
        return review;
    }

    private String fmtDateTime(LocalDateTime t) {
		return t == null ? null : t.format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"));
	}

	private String nicknameOf(Long userId, Boolean anonymous) {
        if (Boolean.TRUE.equals(anonymous)) return "匿名用户";
        User u = userMapper.selectById(userId);
        if (u == null) return "果友";
        String n = u.getNickname();
        return n == null ? "果友" : n.charAt(0) + "**";
    }

    private ReviewVO toVO(Review r, String nickname) {
        Product p = productMapper.selectById(r.getProductId());
        return new ReviewVO(r.getId(), r.getRating(), r.getContent(), r.getIsAnonymous(),
                nickname, r.getAdminReply(),
                r.getCreatedAt() == null ? null : r.getCreatedAt().toLocalDate().toString(),
                r.getProductId(), p == null ? null : p.getName());
    }
}
