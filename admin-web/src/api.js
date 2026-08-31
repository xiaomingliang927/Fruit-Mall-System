import { adminStore } from './store'

async function request(url, options = {}) {
  const headers = { 'Content-Type': 'application/json', ...(options.headers || {}) }
  if (adminStore.token) headers.Authorization = `Bearer ${adminStore.token}`
  let res
  try {
    res = await fetch(url, { ...options, headers })
  } catch (e) {
    // fetch 抛错 = 网络层不通（后端未启动 / 断网 / 代理拒绝）
    throw new Error('无法连接服务器，请确认后端服务已启动')
  }
  const body = await res.json().catch(() => {
    // 收到响应但不是 JSON（多为 Nginx 502/504 页面 = 后端瞬时不可用）
    throw new Error(res.status >= 500 ? `服务暂不可用（${res.status}），请稍后重试` : `请求失败（${res.status}）`)
  })
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

/** 图片上传（管理端，multipart）→ 返回 /uploads/ 访问路径 */
export async function uploadImage(file) {
  const fd = new FormData()
  fd.append('file', file)
  const headers = {}
  if (adminStore.token) headers.Authorization = `Bearer ${adminStore.token}`
  const res = await fetch('/api/admin/upload/image', { method: 'POST', headers, body: fd })
  const body = await res.json().catch(() => ({ code: res.status, message: '上传失败' }))
  if (body.code === 0) return body.data.url
  throw new Error(body.message || '上传失败')
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
