package com.fruitmall.modules.favorite;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fruitmall.auth.UserContext;
import com.fruitmall.common.ApiResponse;
import com.fruitmall.common.BizException;
import com.fruitmall.common.PageResult;
import com.fruitmall.modules.product.Product;
import com.fruitmall.modules.product.ProductMapper;
import com.fruitmall.modules.product.ProductSku;
import com.fruitmall.modules.product.ProductSkuMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/favorites")
@RequiredArgsConstructor
public class FavoriteController {

    private final FavoriteMapper favoriteMapper;
    private final ProductMapper productMapper;
    private final ProductSkuMapper skuMapper;

    public record FavoriteVO(Long favoriteId, Long productId, String name, String subtitle,
                             String mainImage, Integer minPrice, Integer sales, Integer status) {
    }

    /** 我的收藏（分页），商品已下架的保留展示并标记 status=0 */
    @GetMapping
    public ApiResponse<PageResult<FavoriteVO>> page(@RequestParam(defaultValue = "1") long page,
                                                    @RequestParam(defaultValue = "10") long size) {
        Long userId = UserContext.requireUserId();
        Page<Favorite> result = favoriteMapper.selectPage(new Page<>(page, Math.min(size, 50)),
                new LambdaQueryWrapper<Favorite>()
                        .eq(Favorite::getUserId, userId)
                        .orderByDesc(Favorite::getId));
        List<Long> productIds = result.getRecords().stream().map(Favorite::getProductId).toList();
        Map<Long, Product> products = productIds.isEmpty() ? Map.of()
                : productMapper.selectBatchIds(productIds).stream()
                        .collect(Collectors.toMap(Product::getId, Function.identity()));
        Map<Long, Integer> minPriceByProduct = productIds.isEmpty() ? Map.of()
                : skuMapper.selectList(new LambdaQueryWrapper<ProductSku>()
                                .in(ProductSku::getProductId, productIds)
                                .eq(ProductSku::getStatus, 1))
                        .stream()
                        .collect(Collectors.toMap(ProductSku::getProductId, ProductSku::getPrice,
                                Integer::min));
        return ApiResponse.ok(PageResult.of(result, f -> {
            Product p = products.get(f.getProductId());
            return new FavoriteVO(f.getId(), f.getProductId(),
                    p == null ? "商品已失效" : p.getName(),
                    p == null ? null : p.getSubtitle(),
                    p == null ? null : p.getMainImage(),
                    minPriceByProduct.getOrDefault(f.getProductId(), 0),
                    p == null ? 0 : p.getSales(),
                    p == null ? 0 : p.getStatus());
        }));
    }

    /** 是否已收藏（详情页按钮状态） */
    @GetMapping("/{productId}/exists")
    public ApiResponse<Boolean> exists(@PathVariable Long productId) {
        Long userId = UserContext.requireUserId();
        return ApiResponse.ok(favoriteMapper.countByUserAndProduct(userId, productId) > 0);
    }

    /** 收藏（重复收藏幂等） */
    @PostMapping("/{productId}")
    public ApiResponse<Void> add(@PathVariable Long productId) {
        Long userId = UserContext.requireUserId();
        if (productMapper.selectById(productId) == null) {
            throw new BizException("商品不存在");
        }
        if (favoriteMapper.countByUserAndProduct(userId, productId) == 0) {
            Favorite f = new Favorite();
            f.setUserId(userId);
            f.setProductId(productId);
            favoriteMapper.insert(f);
        }
        return ApiResponse.ok();
    }

    /** 取消收藏 */
    @DeleteMapping("/{productId}")
    public ApiResponse<Void> remove(@PathVariable Long productId) {
        favoriteMapper.deleteByUserAndProduct(UserContext.requireUserId(), productId);
        return ApiResponse.ok();
    }
}
