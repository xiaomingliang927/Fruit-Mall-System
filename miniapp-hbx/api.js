// 后端接口基地址：
//  - 微信开发者工具里 localhost 可用（需在「详情-本地设置」勾选「不校验合法域名」）
//  - 真机预览时改为电脑局域网 IP，如 http://192.168.1.5:8080
export const BASE_URL = 'http://localhost:8080'

export function getToken() {
  return uni.getStorageSync('token') || ''
}

export function setToken(t) {
  if (t) uni.setStorageSync('token', t)
  else uni.removeStorageSync('token')
}

function request(url, method = 'GET', data) {
  return new Promise((resolve, reject) => {
    const header = { 'Content-Type': 'application/json' }
    const token = getToken()
    if (token) header.Authorization = `Bearer ${token}`
    uni.request({
      url: BASE_URL + url,
      method,
      data,
      header,
      success: (res) => {
        const body = res.data || {}
        if (body.code === 0) return resolve(body.data)
        if (body.code === 401) {
          setToken('')
          uni.showToast({ title: '请先登录', icon: 'none' })
          uni.navigateTo({ url: '/pages/login/login' })
          return reject(new Error(body.message))
        }
        reject(new Error(body.message || '请求失败'))
      },
      fail: () => reject(new Error('网络异常，请确认后端已启动并勾选「不校验合法域名」')),
    })
  })
}

export const api = {
  get: (url) => request(url),
  post: (url, data) => request(url, 'POST', data ?? {}),
  put: (url, data) => request(url, 'PUT', data ?? {}),
  delete: (url) => request(url, 'DELETE'),
}

/** 凭证图上传：uni.uploadFile → 返回后端相对路径（展示时用 imgUrl 拼绝对地址） */
export function uploadImage(filePath) {
  return new Promise((resolve, reject) => {
    const header = {}
    const token = getToken()
    if (token) header.Authorization = `Bearer ${token}`
    uni.uploadFile({
      url: BASE_URL + '/api/v1/upload/image',
      filePath,
      name: 'file',
      header,
      success: (res) => {
        let body = res.data
        if (typeof body === 'string') {
          try { body = JSON.parse(body) } catch (e) { body = {} }
        }
        if (body && body.code === 0 && body.data) return resolve(body.data.url)
        if (body && body.code === 401) {
          setToken('')
          uni.navigateTo({ url: '/pages/login/login' })
        }
        reject(new Error((body && body.message) || '上传失败'))
      },
      fail: () => reject(new Error('上传失败，请确认后端已启动')),
    })
  })
}

/** 商品图：数据库存相对路径，小程序需拼绝对地址 */
export const imgUrl = (p) => (p ? (p.startsWith('http') ? p : BASE_URL + p) : '')

/** 金额：分 → 元字符串 */
export const yuan = (fen) => (fen / 100).toFixed(2).replace(/\.?0+$/, '')

export const fmtTime = (iso) => (iso ? iso.replace('T', ' ').slice(0, 16) : '-')
