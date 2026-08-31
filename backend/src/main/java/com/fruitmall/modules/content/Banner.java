package com.fruitmall.modules.content;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/** 轮播图 / 推荐位（V8 建表） */
@Data
@TableName("banner")
public class Banner {

    public static final int LINK_NONE = 0;
    public static final int LINK_PRODUCT = 1;
    public static final int LINK_PAGE = 2;

    public static final int STATUS_OFF = 0;
    public static final int STATUS_ON = 1;

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 标题（后台标识用） */
    private String title;

    private String image;

    /** 0 无跳转 1 商品 2 页面路径 */
    private Integer linkType;

    private String linkValue;

    /** 位置：home 首页轮播 */
    private String position;

    /** 排序，越小越靠前 */
    private Integer sort;

    /** 1 启用 0 禁用 */
    private Integer status;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
