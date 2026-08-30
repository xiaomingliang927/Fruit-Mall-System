package com.fruitmall.modules.product;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fruitmall.common.BizException;
import com.fruitmall.common.PageResult;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ProductService {

    private final CategoryMapper categoryMapper;
    private final ProductMapper productMapper;
    private final ProductSkuMapper skuMapper;
    private final com.fruitmall.modules.review.ReviewMapper reviewMapper;

    /** 最低价 SKU 价格，用于列表展示「xx元起」；firstSkuId 供购物车直接加购 */
    public record ProductListVO(Long id, Long categoryId, String name, String subtitle, String mainImage,
                                String origin, String unit, String tags, Integer sales, Integer minPrice,
                                Long firstSkuId) {
    }

    public record CategoryNode(Long id, String name, String icon, List<CategoryNode> children) {
    }

    public List<CategoryNode> categoryTree() {
        List<Category> all = categoryMapper.selectList(new LambdaQueryWrapper<Category>()
                .eq(Category::getStatus, 1)
                .orderByAsc(Category::getSort));
        Map<Long, List<Category>> byParent = all.stream()
                .collect(Collectors.groupingBy(Category::getParentId));
        return all.stream()
                .filter(c -> c.getParentId() == 0)
                .map(top -> toNode(top, byParent))
                .toList();
    }

    private CategoryNode toNode(Category category, Map<Long, List<Category>> byParent) {
        List<CategoryNode> children = byParent.getOrDefault(category.getId(), List.of()).stream()
                .map(c -> toNode(c, byParent))
                .toList();
        return new CategoryNode(category.getId(), category.getName(), category.getIcon(), children);
    }

    public PageResult<ProductListVO> pageProducts(Long categoryId, String keyword, long page, long size) {
        LambdaQueryWrapper<Product> wrapper = new LambdaQueryWrapper<Product>()
                .eq(Product::getStatus, 1)
                .eq(categoryId != null, Product::getCategoryId, categoryId)
                .like(StringUtils.hasText(keyword), Product::getName, keyword)
                .orderByDesc(Product::getSales);
        Page<Product> result = productMapper.selectPage(new Page<>(page, size), wrapper);
        List<Long> productIds = result.getRecords().stream().map(Product::getId).toList();
        // 批量取每个商品的默认 SKU（优先有库存中 ID 最小者）
        Map<Long, Long> firstSkuByProduct = productIds.isEmpty() ? Map.of()
                : skuMapper.selectList(new LambdaQueryWrapper<ProductSku>()
                                .in(ProductSku::getProductId, productIds)
                                .orderByAsc(ProductSku::getId))
                        .stream()
                        .collect(Collectors.toMap(ProductSku::getProductId, ProductSku::getId,
                                (a, b) -> a));
        return PageResult.of(result, p -> new ProductListVO(
                p.getId(), p.getCategoryId(), p.getName(), p.getSubtitle(), p.getMainImage(),
                p.getOrigin(), p.getUnit(), p.getTags(), p.getSales(), minPrice(p.getId()),
                firstSkuByProduct.get(p.getId())));
    }

    private final Map<Long, Integer> minPriceCache = new java.util.concurrent.ConcurrentHashMap<>();

    private Integer minPrice(Long productId) {
        return minPriceCache.computeIfAbsent(productId, id ->
                skuMapper.selectList(new LambdaQueryWrapper<ProductSku>()
                                .eq(ProductSku::getProductId, id)
                                .eq(ProductSku::getStatus, 1))
                        .stream()
                        .mapToInt(ProductSku::getPrice)
                        .min()
                        .orElse(0));
    }

    public record SkuVO(Long id, String spec, Integer price, Integer stock) {
    }

    /** 大小写不敏感的聚合取值 */
    private static long num(Map<String, Object> map, String key) {
        if (map == null) return 0;
        for (var e : map.entrySet()) {
            if (key.equalsIgnoreCase(e.getKey()) && e.getValue() instanceof Number n) return n.longValue();
        }
        return 0;
    }

    public record ProductDetailVO(Long id, String name, String subtitle, String mainImage, String origin,
                                  String unit, String tags, Integer sales, List<SkuVO> skus,
                                  String ratingAvg, Long reviewCount) {
    }

    public ProductDetailVO detail(Long id) {
        Product product = productMapper.selectById(id);
        if (product == null || product.getStatus() == 0) {
            throw new BizException("商品不存在或已下架");
        }
        List<SkuVO> skus = skuMapper.selectList(new LambdaQueryWrapper<ProductSku>()
                        .eq(ProductSku::getProductId, id)
                        .eq(ProductSku::getStatus, 1))
                .stream()
                .map(s -> new SkuVO(s.getId(), s.getSpec(), s.getPrice(), s.getStock()))
                .toList();
        // 评分聚合（仅可见评价）：均分与条数
        Map<String, Object> agg = reviewMapper.ratingAggByProduct(id);
        long reviewCount = num(agg, "cnt");
        String ratingAvg = reviewCount == 0 ? "5.0" : String.valueOf(num(agg, "avgX10") / 10.0);
        return new ProductDetailVO(product.getId(), product.getName(), product.getSubtitle(),
                product.getMainImage(), product.getOrigin(), product.getUnit(), product.getTags(),
                product.getSales(), skus, ratingAvg, reviewCount);
    }
}
