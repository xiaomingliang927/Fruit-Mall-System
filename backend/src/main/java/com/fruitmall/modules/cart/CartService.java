package com.fruitmall.modules.cart;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fruitmall.common.BizException;
import com.fruitmall.modules.product.Product;
import com.fruitmall.modules.product.ProductMapper;
import com.fruitmall.modules.product.ProductSku;
import com.fruitmall.modules.product.ProductSkuMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CartService {

    private static final int MAX_QUANTITY = 99;

    private final CartMapper cartMapper;
    private final ProductSkuMapper skuMapper;
    private final ProductMapper productMapper;

    public record CartVO(Long itemId, Long productId, Long skuId, String productName, String image,
                         String spec, Integer price, Integer quantity, Integer stock,
                         Boolean checked, Integer subtotal) {
    }

    public record CartSummary(List<CartVO> items, int totalCount, int totalAmount) {
    }

    public CartSummary myCart(Long userId) {
        List<CartItem> items = cartMapper.selectList(new LambdaQueryWrapper<CartItem>()
                .eq(CartItem::getUserId, userId)
                .orderByDesc(CartItem::getId));
        if (items.isEmpty()) {
            return new CartSummary(List.of(), 0, 0);
        }
        Map<Long, ProductSku> skuMap = skuMapper.selectBatchIds(
                        items.stream().map(CartItem::getSkuId).toList()).stream()
                .collect(Collectors.toMap(ProductSku::getId, Function.identity()));
        Map<Long, Product> productMap = productMapper.selectBatchIds(
                        skuMap.values().stream().map(ProductSku::getProductId).toList()).stream()
                .collect(Collectors.toMap(Product::getId, Function.identity()));

        List<CartVO> vos = items.stream().map(item -> {
            ProductSku sku = skuMap.get(item.getSkuId());
            Product product = sku == null ? null : productMap.get(sku.getProductId());
            boolean valid = product != null && product.getStatus() == 1
                    && sku != null && sku.getStatus() == 1;
            return new CartVO(item.getId(),
                    product != null ? product.getId() : null,
                    item.getSkuId(),
                    product != null ? product.getName() : "商品已失效",
                    product != null ? product.getMainImage() : null,
                    sku != null ? sku.getSpec() : null,
                    sku != null ? sku.getPrice() : 0,
                    item.getQuantity(),
                    sku != null ? sku.getStock() : 0,
                    item.getChecked(),
                    sku != null ? sku.getPrice() * item.getQuantity() : 0);
        }).toList();

        int totalCount = vos.stream()
                .filter(v -> Boolean.TRUE.equals(v.checked()) && v.stock() > 0)
                .mapToInt(CartVO::quantity).sum();
        int totalAmount = vos.stream()
                .filter(v -> Boolean.TRUE.equals(v.checked()) && v.stock() > 0)
                .mapToInt(CartVO::subtotal).sum();
        return new CartSummary(vos, totalCount, totalAmount);
    }

    public void addItem(Long userId, Long skuId, Integer quantity) {
        if (quantity < 1 || quantity > MAX_QUANTITY) {
            throw new BizException("购买数量必须在 1~99 之间");
        }
        ProductSku sku = skuMapper.selectById(skuId);
        if (sku == null || sku.getStatus() == 0) {
            throw new BizException("商品规格不存在或已停售");
        }
        Product product = productMapper.selectById(sku.getProductId());
        if (product == null || product.getStatus() == 0) {
            throw new BizException("商品已下架");
        }
        CartItem existing = cartMapper.selectOne(new LambdaQueryWrapper<CartItem>()
                .eq(CartItem::getUserId, userId)
                .eq(CartItem::getSkuId, skuId));
        if (existing != null) {
            int newQty = Math.min(existing.getQuantity() + quantity, MAX_QUANTITY);
            existing.setQuantity(newQty);
            cartMapper.updateById(existing);
        } else {
            CartItem item = new CartItem();
            item.setUserId(userId);
            item.setSkuId(skuId);
            item.setQuantity(quantity);
            item.setChecked(true);
            cartMapper.insert(item);
        }
    }

    public void updateItem(Long userId, Long itemId, Integer quantity, Boolean checked) {
        CartItem item = cartMapper.selectById(itemId);
        if (item == null || !item.getUserId().equals(userId)) {
            throw new BizException("购物车项不存在");
        }
        if (quantity != null) {
            if (quantity < 1 || quantity > MAX_QUANTITY) {
                throw new BizException("购买数量必须在 1~99 之间");
            }
            item.setQuantity(quantity);
        }
        if (checked != null) {
            item.setChecked(checked);
        }
        cartMapper.updateById(item);
    }

    public void removeItem(Long userId, Long itemId) {
        CartItem item = cartMapper.selectById(itemId);
        if (item == null || !item.getUserId().equals(userId)) {
            throw new BizException("购物车项不存在");
        }
        cartMapper.deleteById(itemId);
    }

    /** 清空已勾选商品（下单成功后调用，未勾选/失效商品保留） */
    public void clearChecked(Long userId) {
        cartMapper.delete(new LambdaQueryWrapper<CartItem>()
                .eq(CartItem::getUserId, userId)
                .eq(CartItem::getChecked, true));
    }
}
