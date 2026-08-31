package com.fruitmall.admin.rbac;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/** 后台角色（V8 建表） */
@Data
@TableName("role")
public class Role {

    /** 内置超级管理员角色：拥有全部权限，不做逐点校验 */
    public static final String CODE_SUPER = "super";

    @TableId(type = IdType.AUTO)
    private Long id;

    private String name;

    private String code;

    private String remark;

    /** 1 启用 0 禁用 */
    private Integer status;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
