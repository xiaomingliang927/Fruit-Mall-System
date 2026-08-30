package com.fruitmall.admin;

/** 当前登录管理员（由 AdminInterceptor 写入） */
public final class AdminContext {

    private static final ThreadLocal<Long> HOLDER = new ThreadLocal<>();

    private AdminContext() {
    }

    public static void set(Long adminId) {
        HOLDER.set(adminId);
    }

    public static Long requireAdminId() {
		Long id = HOLDER.get();
		if (id == null) {
			throw new com.fruitmall.common.BizException(com.fruitmall.common.ErrorCode.UNAUTHORIZED);
		}
		return id;
	}

	public static Long getAdminId() {
        return HOLDER.get();
    }

    public static void clear() {
        HOLDER.remove();
    }
}
