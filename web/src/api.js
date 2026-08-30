import { useStore } from './store'
import router from './router'

/** 统一响应处理：code===0 返回 data，401 清 token 跳登录，其余抛错 */
async function request(url, options = {}) {
  const headers = { 'Content-Type': 'application/json', ...(options.headers || {}) }
  if (useStore.token) headers.Authorization = `Bearer ${useStore.token}`
  const res = await fetch(url, { ...options, headers })
  const body = await res.json().catch(() => ({ code: res.status, message: '网络异常' }))
  if (body.code === 0) return body.data
  if (body.code === 401) {
    useStore.token = ''
    if (!location.pathname.startsWith('/login')) {
      router.push({ name: 'login', query: { redirect: location.fullPath || '/' } })
    }
  }
  throw new Error(body.message || '请求失败')
}

export const api = {
  get: (url) => request(url),
  post: (url, data) => request(url, { method: 'POST', body: JSON.stringify(data ?? {}) }),
  put: (url, data) => request(url, { method: 'PUT', body: JSON.stringify(data ?? {}) }),
  delete: (url) => request(url, { method: 'DELETE' }),
}

/** 金额：分 → 元字符串 */
export function yuan(fen) {
  return (fen / 100).toFixed(2).replace(/\.?0+$/, '')
}

/** 时间：ISO → yyyy-MM-dd HH:mm */
export function fmtTime(iso) {
  return iso ? iso.replace('T', ' ').slice(0, 16) : '-'
}

/** 轻量 toast */
let toastTimer = null
export function toast(message, type = 'ok') {
  let el = document.querySelector('.fm-toast')
  if (!el) {
    el = document.createElement('div')
    el.className = 'fm-toast'
    document.body.appendChild(el)
  }
  el.textContent = message
  el.className = `fm-toast show ${type === 'err' ? 'err' : ''}`
  clearTimeout(toastTimer)
  toastTimer = setTimeout(() => el.classList.remove('show'), 2200)
}
