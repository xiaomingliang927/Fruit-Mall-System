const TOKEN_KEY = 'fm_token'

/** 读取/写入本地 token */
export const useStore = {
  get token() {
    return localStorage.getItem(TOKEN_KEY) || ''
  },
  set token(v) {
    if (v) localStorage.setItem(TOKEN_KEY, v)
    else localStorage.removeItem(TOKEN_KEY)
  },
}

/** 全局响应式状态：登录用户 + 购物车数量角标 */
export const globalState = reactive({
  nickname: '',
  cartCount: 0,
})

import { reactive } from 'vue'
import { api } from './api'

/** 拉取当前用户信息与购物车数量（登录后调用） */
export async function refreshMe() {
  if (!useStore.token) {
    globalState.nickname = ''
    globalState.cartCount = 0
    return
  }
  try {
    const me = await api.get('/api/v1/users/me')
    globalState.nickname = me.nickname
    const cart = await api.get('/api/v1/cart')
    globalState.cartCount = cart.totalCount || 0
  } catch {
    /* token 失效时由 api 层统一跳登录 */
  }
}
