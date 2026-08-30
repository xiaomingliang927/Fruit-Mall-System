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

/** 下载文件（Excel 导出等）：带 token 请求 → blob 触发浏览器保存 */
export async function exportFile(url, filename) {
  const headers = {}
  if (adminStore.token) headers.Authorization = `Bearer ${adminStore.token}`
  const res = await fetch(url, { headers })
  if (!res.ok) {
    let message = '导出失败'
    try {
      const body = await res.json()
      message = body.message || message
    } catch { /* 非 JSON 响应 */ }
    throw new Error(message)
  }
  const blob = await res.blob()
  const link = document.createElement('a')
  link.href = URL.createObjectURL(blob)
  link.download = filename
  document.body.appendChild(link)
  link.click()
  link.remove()
  URL.revokeObjectURL(link.href)
}

export function fmtTime(iso) {
  return iso ? iso.replace('T', ' ').slice(0, 16) : '-'
}
