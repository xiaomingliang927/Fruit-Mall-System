package com.fruitmall.modules.product;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
@TableName("product_sku")
public class ProductSku {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long productId;

    /** 规格描述，如：5斤装 / 单果80-90mm */
    private String spec;

    /** 价格，单位：分 */
    private Integer price;

    private Integer stock;

    /** 库存预警阈值 */
    private Integer warnStock;

    private String image;

    /** 1 启用 0 停用 */
    private Integer status;
}
