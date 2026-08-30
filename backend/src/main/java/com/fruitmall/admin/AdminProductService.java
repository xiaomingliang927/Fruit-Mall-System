package com.fruitmall.admin;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fruitmall.common.BizException;
import com.fruitmall.common.PageResult;
import com.fruitmall.modules.product.Category;
import com.fruitmall.modules.product.CategoryMapper;
import com.fruitmall.modules.product.Product;
import com.fruitmall.modules.product.ProductMapper;
import com.fruitmall.modules.product.ProductSku;
import com.fruitmall.modules.product.ProductSkuMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AdminProductService {

    private final ProductMapper productMapper;
    private final ProductSkuMapper skuMapper;
    private final CategoryMapper categoryMapper;

    public record SkuInput(Long id, String spec, Integer price, Integer stock, Integer status) {
    }

    public record ProductInput(Long categoryId, String name, String subtitle, String mainImage,
                               String origin, String unit, String tags, List<SkuInput> skus) {
    }

    public record SkuVO(Long id, String spec, Integer price, Integer stock, Integer status) {
    }

    public record ProductVO(Long id, Long categoryId, String categoryName, String name, String subtitle,
                            String mainImage, String origin, String unit, String tags,
                            Integer status, Integer sales, List<SkuVO> skus) {
    }

    public PageResult<ProductVO> page(Long categoryId, String keyword, long page, long size) {
        Page<Product> result = productMapper.selectPage(new Page<>(page, size),
                new LambdaQueryWrapper<Product>()
                        .eq(categoryId != null, Product::getCategoryId, categoryId)
                        .like(StringUtils.hasText(keyword), Product::getName, keyword)
                        .orderByDesc(Product::getId));
        List<Product> products = result.getRecords();
        if (products.isEmpty()) {
            return PageResult.of(result, p -> toVO(p, List.of(), Map.of()));
        }
        List<Long> productIds = products.stream().map(Product::getId).toList();
        Map<Long, List<ProductSku>> skusByProduct = skuMapper.selectList(new LambdaQueryWrapper<ProductSku>()
                        .in(ProductSku::getProductId, productIds)).stream()
                .collect(Collectors.groupingBy(ProductSku::getProductId));
        Map<Long, String> categoryNames = categoryMapper.selectBatchIds(
                        products.stream().map(Product::getCategoryId).distinct().toList()).stream()
                .collect(Collectors.toMap(Category::getId, Category::getName));
        return PageResult.of(result, p -> toVO(p,
                skusByProduct.getOrDefault(p.getId(), List.of()), categoryNames));
    }

    private ProductVO toVO(Product p, List<ProductSku> skus, Map<Long, String> categoryNames) {
        return new ProductVO(p.getId(), p.getCategoryId(), categoryNames.get(p.getCategoryId()),
                p.getName(), p.getSubtitle(), p.getMainImage(), p.getOrigin(), p.getUnit(), p.getTags(),
                p.getStatus(), p.getSales(),
                skus.stream().map(s -> new SkuVO(s.getId(), s.getSpec(), s.getPrice(),
                        s.getStock(), s.getStatus())).toList());
    }

    @Transactional
    public Long create(ProductInput input) {
        validateSkus(input.skus());
        Product product = new Product();
        copyInput(input, product);
        product.setStatus(1);
        product.setSales(0);
        productMapper.insert(product);
        for (SkuInput sku : input.skus()) {
            insertSku(product.getId(), sku);
        }
        return product.getId();
    }

    /** 编辑：更新商品字段；SKU 仅支持「改已有 + 新增」，不做删除以保护历史订单引用 */
    @Transactional
    public void update(Long id, ProductInput input) {
        validateSkus(input.skus());
        Product product = productMapper.selectById(id);
        if (product == null) {
            throw new BizException("商品不存在");
        }
        copyInput(input, product);
        productMapper.updateById(product);
        for (SkuInput sku : input.skus()) {
            if (sku.id() == null) {
                insertSku(id, sku);
                continue;
            }
            ProductSku existing = skuMapper.selectById(sku.id());
            if (existing == null || !existing.getProductId().equals(id)) {
                throw new BizException("SKU 不存在或不属于该商品");
            }
            existing.setSpec(sku.spec());
            existing.setPrice(sku.price());
            existing.setStock(sku.stock());
            existing.setStatus(sku.status());
            skuMapper.updateById(existing);
        }
    }

    /** 上下架 */
    public void updateStatus(Long id, Integer status) {
        if (status == null || (status != 0 && status != 1)) {
            throw new BizException("status 只能为 0 或 1");
        }
        Product product = productMapper.selectById(id);
        if (product == null) {
            throw new BizException("商品不存在");
        }
        product.setStatus(status);
        productMapper.updateById(product);
    }

    private void validateSkus(List<SkuInput> skus) {
        if (skus == null || skus.isEmpty()) {
            throw new BizException("至少需要一个 SKU 规格");
        }
        for (SkuInput sku : skus) {
            if (!StringUtils.hasText(sku.spec())) {
                throw new BizException("SKU 规格名不能为空");
            }
            if (sku.price() == null || sku.price() <= 0) {
                throw new BizException("SKU 价格必须大于 0");
            }
            if (sku.stock() == null || sku.stock() < 0) {
                throw new BizException("SKU 库存不能为负数");
            }
        }
    }

    private void insertSku(Long productId, SkuInput sku) {
        ProductSku entity = new ProductSku();
        entity.setProductId(productId);
        entity.setSpec(sku.spec());
        entity.setPrice(sku.price());
        entity.setStock(sku.stock());
        entity.setStatus(sku.status() == null ? 1 : sku.status());
        entity.setWarnStock(10);
        skuMapper.insert(entity);
    }

    private void copyInput(ProductInput input, Product product) {
        if (input.categoryId() == null || categoryMapper.selectById(input.categoryId()) == null) {
            throw new BizException("分类不存在");
        }
        product.setCategoryId(input.categoryId());
        product.setName(input.name());
        product.setSubtitle(input.subtitle());
        product.setMainImage(input.mainImage());
        product.setOrigin(input.origin());
        product.setUnit(input.unit());
        product.setTags(input.tags());
    }
}
