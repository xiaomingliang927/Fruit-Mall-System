package com.fruitmall.admin.rbac;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/** 后台权限点（V8 建表；code 与前端菜单 perm 对齐） */
@Data
@TableName("permission")
public class Permission {

    /** 1 菜单 2 按钮/接口 */
    public static final int TYPE_MENU = 1;
    public static final int TYPE_ACTION = 2;

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long parentId;

    private String name;

    private String code;

    private Integer type;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
