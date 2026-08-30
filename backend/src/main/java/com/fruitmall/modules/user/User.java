package com.fruitmall.modules.user;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("user")
public class User {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 微信 openid，网站端登录为空 */
    private String openid;

    private String phone;

    private String nickname;

    private String avatar;

    /** 会员等级 */
    private Integer level;

    /** 会员卡有效期，NULL = 未开通限期会员 */
    private java.time.LocalDateTime memberExpireAt;

    /** 1 正常 0 禁用 */
    private Integer status;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
