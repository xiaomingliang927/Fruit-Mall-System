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
}
