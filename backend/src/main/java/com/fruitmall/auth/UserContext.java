package com.fruitmall.auth;

import com.fruitmall.common.BizException;
import com.fruitmall.common.ErrorCode;

public final class UserContext {

    private static final ThreadLocal<Long> HOLDER = new ThreadLocal<>();

    private UserContext() {
    }

    public static void set(Long userId) {
        HOLDER.set(userId);
    }

    public static Long getUserId() {
        return HOLDER.get();
    }

    public static Long requireUserId() {
        Long userId = HOLDER.get();
        if (userId == null) {
            throw new BizException(ErrorCode.UNAUTHORIZED);
        }
        return userId;
    }

    public static void clear() {
        HOLDER.remove();
    }
}
