/**
 * 购物车：本地镜像（页面同步读写）+ 后端异步推送（跨端同步）
 *
 * 关键设计：购物车本地以「商品快照」存储（id/名称/价格/图/规格/skuId/itemId/qty），
 * 渲染与增减全部基于本地快照，不再依赖 PRODUCTS 是否包含该 id。
 * 否则一旦购物车里的 id 在 PRODUCTS 中查不到（分页未加载到、或服务端残留），
 * 该商品就会在购物车里「隐身」，表现为减号点不动、数量不跟着变。
 *
 * 登录后通过 syncFromServer 拉取服务端购物车：本地优先合并，
 * 服务端仅用于补充本地没有的项，不会用服务端的旧数量覆盖本次会话内的增减。
 */
import { PRODUCTS } from './data.js'
import { api } from '../api.js'

const CART_KEY = 'fruit_mall_cart'
const SELECT_KEY = 'fruit_mall_selected'

export function getCart() {
	try {
		const cart = uni.getStorageSync(CART_KEY) || {}
		// 兼容旧格式：早期版本把数量直接存成数字
		Object.keys(cart).forEach((k) => {
			if (typeof cart[k] === 'number') {
				cart[k] = { id: Number(k), qty: cart[k], name: '商品', price: 0, img: '', spec: '', skuId: null, itemId: null }
			}
		})
		return cart
	} catch (e) {
		return {}
	}
}
function saveCart(cart) { uni.setStorageSync(CART_KEY, cart) }
export function getSelected() { try { return uni.getStorageSync(SELECT_KEY) || {} } catch (e) { return {} } }
function saveSelected(sel) { uni.setStorageSync(SELECT_KEY, sel) }

function productOf(id) {
	return PRODUCTS.find((x) => x.id === Number(id)) || null
}
function loggedIn() { return !!uni.getStorageSync('token') }

// 角标只能从 TabBar 页面调用，否则 WeChat 抛 "not TabBar page"。
// 四个 TabBar 页在 onShow 都会重算角标，所以非 TabBar 页跳过也始终一致。
const TABBAR_ROUTES = ['pages/index/index', 'pages/category/category', 'pages/cart/cart', 'pages/mine/mine']
function isOnTabBar() {
	try {
		const pages = getCurrentPages()
		const cur = pages[pages.length - 1]
		return !!(cur && TABBAR_ROUTES.indexOf(cur.route) !== -1)
	} catch (e) {
		return false
	}
}

export function updateCartBadge() {
	const count = getCartCount()
	if (!isOnTabBar()) return
	try {
		if (count > 0) {
			uni.setTabBarBadge({ index: 2, text: count > 99 ? '99+' : String(count) })
		} else {
			uni.removeTabBarBadge({ index: 2 })
		}
	} catch (e) { /* 非 TabBar 页场景兜底，忽略 */ }
}

/** 用商品信息造一份快照；拿不到（如 id 不存在）时给兜底值，保证购物车永远能渲染 */
function snapshot(id) {
	const p = productOf(id)
	return {
		id: Number(id),
		name: p ? p.name : '商品',
		price: p ? p.price : 0,
		img: p ? p.img : '',
		spec: p ? p.spec : '',
		skuId: p ? p.skuId : null,
		itemId: null,
		qty: 0
	}
}

export function addToCart(id, qty = 1) {
	const cart = getCart()
	const snap = cart[id] && typeof cart[id] === 'object' ? cart[id] : snapshot(id)
	snap.qty = Math.min((snap.qty || 0) + qty, 99)
	snap.id = Number(id)
	cart[id] = snap
	saveCart(cart)
	const sel = getSelected(); sel[id] = true; saveSelected(sel)
	if (snap.skuId && loggedIn()) api.post('/api/v1/cart/items', { skuId: snap.skuId, quantity: qty }).catch(() => {})
	updateCartBadge()
	return cart
}

export function incQty(id) { return addToCart(id, 1) }

export function decQty(id) {
	const cart = getCart()
	const snap = cart[id]
	if (!snap || typeof snap !== 'object') return cart
	snap.qty = (snap.qty || 0) - 1
	if (snap.qty <= 0) {
		delete cart[id]
		const sel = getSelected(); delete sel[id]; saveSelected(sel)
		const serverId = snap.itemId || snap.skuId
		if (serverId && loggedIn()) api.delete('/api/v1/cart/items/' + serverId).catch(() => {})
	} else {
		saveCart(cart)
		const serverId = snap.itemId || snap.skuId
		if (serverId && loggedIn()) api.put('/api/v1/cart/items/' + serverId, { quantity: snap.qty }).catch(() => {})
	}
	updateCartBadge()
	return cart
}

export function removeFromCart(id) {
	const cart = getCart(); const snap = cart[id]; delete cart[id]; saveCart(cart)
	const sel = getSelected(); delete sel[id]; saveSelected(sel)
	const serverId = snap && (snap.itemId || snap.skuId)
	if (serverId && loggedIn()) api.delete('/api/v1/cart/items/' + serverId).catch(() => {})
	updateCartBadge()
	return cart
}

export function toggleSelect(id) {
	const sel = getSelected(); sel[id] = !sel[id]; saveSelected(sel)
	const snap = getCart()[id]
	const serverId = snap && (snap.itemId || snap.skuId)
	if (serverId && loggedIn()) api.put('/api/v1/cart/items/' + serverId, { checked: !!sel[id] }).catch(() => {})
	return sel
}

export function toggleSelectAll() {
	const cart = getCart(); const sel = getSelected()
	const ids = Object.keys(cart)
	const allSelected = ids.length > 0 && ids.every((id) => sel[id])
	ids.forEach((id) => { sel[id] = !allSelected })
	saveSelected(sel)
	if (loggedIn()) {
		ids.forEach((id) => {
			const snap = cart[id]
			const serverId = snap && (snap.itemId || snap.skuId)
			if (serverId) api.put('/api/v1/cart/items/' + serverId, { checked: !allSelected }).catch(() => {})
		})
	}
	return sel
}

export function isAllSelected() {
	const cart = getCart(); const sel = getSelected()
	const ids = Object.keys(cart)
	return ids.length > 0 && ids.every((id) => sel[id])
}

export function getCartCount() {
	const cart = getCart()
	return Object.values(cart).reduce((a, c) => a + ((c && c.qty) || 0), 0)
}

/** 直接返回本地快照，不依赖 PRODUCTS；任何购物车项都能渲染出来 */
export function getCartList() {
	const cart = getCart()
	return Object.keys(cart).map((id) => ({ ...cart[id], id: Number(id) }))
}

export function getCartTotal() {
	const sel = getSelected()
	return getCartList().filter((p) => sel[p.id]).reduce((s, p) => s + (p.price || 0) * (p.qty || 0), 0).toFixed(2)
}

export function clearCart() { saveCart({}); saveSelected({}) }

/** 登录后/启动时：服务端购物车补充进本地（本地优先，避免覆盖本次会话内的增减） */
export async function syncFromServer() {
	if (!uni.getStorageSync('token')) return
	try {
		const data = await api.get('/api/v1/cart')
		const remote = ((data && data.items) || []).filter((it) => it.productId != null)
		const cart = getCart()
		const sel = getSelected()
		remote.forEach((it) => {
			const pid = it.productId
			if (!cart[pid] || typeof cart[pid] !== 'object') {
				// 本地没有该项（如其他端加的），按服务端快照补进来
				cart[pid] = {
					id: pid,
					name: it.productName || '商品',
					price: it.price || 0,
					img: it.image || '',
					spec: it.spec || '',
					skuId: it.skuId,
					itemId: it.itemId,
					qty: it.quantity
				}
			} else {
				// 本地已有：仅记录服务端 itemId，便于后续增量同步；数量以本地为准
				cart[pid].itemId = it.itemId
			}
			sel[pid] = !!it.checked
		})
		saveCart(cart); saveSelected(sel)
	} catch (e) { /* 静默 */ }
}

/** 退出登录：清空本地镜像与角标 */
export function clearLocalMirror() {
	saveCart({}); saveSelected({})
	if (isOnTabBar()) {
		try { uni.removeTabBarBadge({ index: 2 }) } catch (e) { /* 忽略 */ }
	}
}
