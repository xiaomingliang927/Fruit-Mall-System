const TOKEN_KEY = 'fm_admin_token'

export const adminStore = {
  get token() {
    return localStorage.getItem(TOKEN_KEY) || ''
  },
  set token(v) {
    if (v) localStorage.setItem(TOKEN_KEY, v)
    else localStorage.removeItem(TOKEN_KEY)
  },
  get name() {
    return localStorage.getItem('fm_admin_name') || ''
  },
  set name(v) {
    if (v) localStorage.setItem('fm_admin_name', v)
    else localStorage.removeItem('fm_admin_name')
  },
  /** 权限点编码列表（登录后写入，用于菜单显隐） */
  get permissions() {
    try {
      const raw = localStorage.getItem('fm_admin_perms')
      const arr = raw ? JSON.parse(raw) : []
      return Array.isArray(arr) ? arr : []
    } catch {
      return []
    }
  },
  set permissions(v) {
    if (Array.isArray(v) && v.length) localStorage.setItem('fm_admin_perms', JSON.stringify(v))
    else localStorage.removeItem('fm_admin_perms')
  },
  /** 是否超级管理员（跳过逐点校验） */
  get superAdmin() {
    return localStorage.getItem('fm_admin_super') === '1'
  },
  set superAdmin(v) {
    if (v) localStorage.setItem('fm_admin_super', '1')
    else localStorage.removeItem('fm_admin_super')
  },
}
