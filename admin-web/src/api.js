import { adminStore } from './store'

async function request(url, options = {}) {
  const headers = { 'Content-Type': 'application/json', ...(options.headers || {}) }
  if (adminStore.token) headers.Authorization = `Bearer ${adminStore.token}`
  const res = await fetch(url, { ...options, headers })
  const body = await res.json().catch(() => ({ code: res.status, message: '网络异常' }))
  if (body.code === 0) return body.data
  if (body.code === 401) {
    adminStore.token = ''
    if (!location.pathname.startsWith('/login')) location.href = '/login'
  }
  throw new Error(body.message || '请求失败')
}

export const api = {
  get: (url) => request(url),
  post: (url, data) => request(url, { method: 'POST', body: JSON.stringify(data ?? {}) }),
  put: (url, data) => request(url, { method: 'PUT', body: JSON.stringify(data ?? {}) }),
  delete: (url) => request(url, { method: 'DELETE' }),
}

/** 金额：分 → 元（数字，用于表格展示与表单回填） */
export function yuan(fen) {
  return fen / 100
}

/** 金额：元 → 分（表单提交） */
export function toFen(y) {
  return Math.round(Number(y) * 100)
}

export function fmtTime(iso) {
  return iso ? iso.replace('T', ' ').slice(0, 16) : '-'
}
