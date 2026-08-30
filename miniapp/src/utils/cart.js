/**
 * 购物车：本地镜像（页面同步读写）+ 后端异步推送（跨端同步）
 * 登录后通过 syncFromServer 拉取服务端购物车覆盖本地
 */
import { PRODUCTS } from './data.js'
import { api } from '../api.js'

const CART_KEY = 'fruit_mall_cart'
const SELECT_KEY = 'fruit_mall_selected'

export function getCart() { try { return uni.getStorageSync(CART_KEY) || {} } catch (e) { return {} } }
function saveCart(cart) { uni.setStorageSync(CART_KEY, cart) }
export function getSelected() { try { return uni.getStorageSync(SELECT_KEY) || {} } catch (e) { return {} } }
function saveSelected(sel) { uni.setStorageSync(SELECT_KEY, sel) }

function skuOf(id) {
	const p = PRODUCTS.find((x) => x.id === Number(id))
	return p && p.skuId ? p.skuId : null
}
function loggedIn() { return !!uni.getStorageSync('token') }

export function updateCartBadge() {
	const count = getCartCount()
	if (count > 0) {
		uni.setTabBarBadge({ index: 2, text: count > 99 ? '99+' : String(count) })
	} else {
		uni.removeTabBarBadge({ index: 2 })
	}
}

export function addToCart(id, qty = 1) {
	const cart = getCart()
	cart[id] = (cart[id] || 0) + qty
	saveCart(cart)
	const sel = getSelected(); sel[id] = true; saveSelected(sel)
	const skuId = skuOf(id)
	if (skuId && loggedIn()) api.post('/api/v1/cart/items', { skuId, quantity: qty }).catch(() => {})
	updateCartBadge()
	return cart
}

export function incQty(id) { return addToCart(id, 1) }

export function decQty(id) {
	const cart = getCart()
	cart[id] = (cart[id] || 0) - 1
	const skuId = skuOf(id)
	if (cart[id] <= 0) {
		delete cart[id]
		const sel = getSelected(); delete sel[id]; saveSelected(sel)
		if (skuId && loggedIn()) api.delete('/api/v1/cart/items/' + skuId).catch(() => {})
	} else {
		saveCart(cart)
		if (skuId && loggedIn()) api.put('/api/v1/cart/items/' + skuId, { quantity: cart[id] }).catch(() => {})
	}
	updateCartBadge()
	return cart
}

export function removeFromCart(id) {
	const cart = getCart(); delete cart[id]; saveCart(cart)
	const sel = getSelected(); delete sel[id]; saveSelected(sel)
	const skuId = skuOf(id)
	if (skuId && loggedIn()) api.delete('/api/v1/cart/items/' + skuId).catch(() => {})
	updateCartBadge()
	return cart
}

export function toggleSelect(id) {
	const sel = getSelected(); sel[id] = !sel[id]; saveSelected(sel)
	const p = PRODUCTS.find((x) => x.id === Number(id))
	if (p && p.skuId && loggedIn()) api.put('/api/v1/cart/items/' + p.skuId, { checked: !!sel[id] }).catch(() => {})
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
			const p = PRODUCTS.find((x) => x.id === Number(id))
			if (p && p.skuId) api.put('/api/v1/cart/items/' + p.skuId, { checked: !allSelected }).catch(() => {})
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
	return Object.values(cart).reduce((a, b) => a + b, 0)
}

export function getCartList() {
	const cart = getCart()
	return PRODUCTS.filter((p) => cart[p.id]).map((p) => ({ ...p, qty: cart[p.id] }))
}

export function getCartTotal() {
	const sel = getSelected()
	return getCartList().filter((p) => sel[p.id]).reduce((s, p) => s + p.price * p.qty, 0).toFixed(2)
}

export function clearCart() { saveCart({}); saveSelected({}) }

/** 登录后/启动时：服务端购物车覆盖本地镜像（其他平台加的购物车同步过来） */
export async function syncFromServer() {
	if (!uni.getStorageSync('token')) return
	try {
		const data = await api.get('/api/v1/cart')
		const cart = {}; const sel = {}
		;(data.items || []).forEach((it) => {
			if (!PRODUCTS.find((x) => x.id === it.productId)) return
			cart[it.productId] = it.quantity
			sel[it.productId] = !!it.checked
		})
		saveCart(cart); saveSelected(sel)
	} catch (e) { /* 静默 */ }
}

/** 退出登录：清空本地镜像与角标 */
export function clearLocalMirror() {
	saveCart({}); saveSelected({})
	uni.removeTabBarBadge({ index: 2 })
}
