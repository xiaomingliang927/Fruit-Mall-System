// 后端接口基地址：
//  - H5 版部署在网站同域（:5173/mp/），走相对路径 /api，由 Nginx 反代到 8080，无跨域
//  - 微信开发者工具里 localhost 可用（需在「详情-本地设置」勾选「不校验合法域名」）
//  - 真机预览时改为电脑局域网 IP，如 http://192.168.1.5:8080（手机和电脑需同一 WiFi）
//  - 真机用户可在「设置 → API 地址」自行配置，覆盖默认 localhost
const BASE_URL_KEY = 'fruit_mall_base_url'
// #ifdef H5
const DEFAULT_BASE_URL = ''
// #endif
// #ifndef H5
const DEFAULT_BASE_URL = 'http://localhost:8080'
// #endif

export function getBaseUrl() {
  return uni.getStorageSync(BASE_URL_KEY) || DEFAULT_BASE_URL
}

export function setBaseUrl(url) {
  if (url && url.trim()) {
    uni.setStorageSync(BASE_URL_KEY, url.trim())
  } else {
    uni.removeStorageSync(BASE_URL_KEY)
  }
}

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
      url: getBaseUrl() + url,
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
      url: getBaseUrl() + '/api/v1/upload/image',
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
export const imgUrl = (p) => {
	if (!p) return ''
	if (p.startsWith('http')) return p
	const base = getBaseUrl()
	if (base) return base + p
	// #ifdef H5
	// H5 部署在子路径（/mp/）时，uni-h5 会给 "/" 开头的路径加路由前缀，
	// 后端图片必须用同源绝对 URL 才能绕开（getRealPath 只对 "/" 开头的路径加 base）
	if (typeof window !== 'undefined' && p.startsWith('/')) return window.location.origin + p
	// #endif
	return p
}

/** 金额：分 → 元字符串 */
export const yuan = (fen) => (fen / 100).toFixed(2).replace(/\.?0+$/, '')

export const fmtTime = (iso) => (iso ? iso.replace('T', ' ').slice(0, 16) : '-')
