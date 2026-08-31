package com.fruitmall.security;

import com.fruitmall.modules.product.Product;
import com.fruitmall.modules.product.ProductMapper;
import com.fruitmall.modules.product.ProductSku;
import com.fruitmall.modules.product.ProductSkuMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 防超卖并发验证：库存靠 `UPDATE ... SET stock=stock-? WHERE id=? AND stock>=?` 的原子条件更新保障，
 * 本测试用 100 个并发线程抢购同一 SKU（初始库存 50），断言最终恰好售罄、绝不超发。
 */
@SpringBootTest
@ActiveProfiles("test")
class OversellConcurrencyTest {

    @Autowired ProductMapper productMapper;
    @Autowired ProductSkuMapper skuMapper;

    private static final int INITIAL_STOCK = 50;
    private Long skuId;

    @BeforeEach
    void setUp() {
        Product p = new Product();
        p.setName("并发防超卖测试商品");
        p.setCategoryId(1L);
        p.setStatus(1);
        p.setMainImage("/x.jpg");
        productMapper.insert(p);

        ProductSku sku = new ProductSku();
        sku.setProductId(p.getId());
        sku.setSpec("默认");
        sku.setPrice(100);
        sku.setStock(INITIAL_STOCK);
        sku.setStatus(1);
        skuMapper.insert(sku);
        skuId = sku.getId();
    }

    @Test
    @DisplayName("100 并发各买 1 件（初始 50）：成功数=库存，最终库存 0，不超发")
    void noOversellUnderConcurrency() throws Exception {
        int threads = 100;
        CountDownLatch start = new CountDownLatch(1);
        CountDownLatch done = new CountDownLatch(threads);
        AtomicInteger success = new AtomicInteger(0);

        for (int i = 0; i < threads; i++) {
            new Thread(() -> {
                try {
                    start.await();
                    if (skuMapper.decreaseStock(skuId, 1) == 1) {
                        success.incrementAndGet();
                    }
                } catch (Exception ignored) {
                    // 极端锁竞争下个别线程异常也只会影响该次扣减，不破坏不变量
                } finally {
                    done.countDown();
                }
            }).start();
        }

        start.countDown();
        assertTrue(done.await(30, TimeUnit.SECONDS), "并发扣减超时");

        assertThat(success.get()).isEqualTo(INITIAL_STOCK);
        assertThat(skuMapper.selectById(skuId).getStock()).isEqualTo(0);
    }
}
