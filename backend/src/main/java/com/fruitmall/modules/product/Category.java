package com.fruitmall.modules.product;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
@TableName("category")
public class Category {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 顶级分类 parent_id = 0 */
    private Long parentId;

    private String name;

    private Integer sort;

    private String icon;

    /** 1 启用 0 禁用 */
    private Integer status;
}
