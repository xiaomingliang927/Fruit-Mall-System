/**
 * 购物车状态管理
 * 基于 uni.setStorageSync 持久化，跨页面共享
 */

const CART_KEY = 'fruit_mall_cart'
const SELECT_KEY = 'fruit_mall_selected'

// 获取购物车 { id: qty }
export function getCart() {
	try {
		return uni.getStorageSync(CART_KEY) || {}
	} catch (e) {
		return {}
	}
}

// 保存购物车
function saveCart(cart) {
	uni.setStorageSync(CART_KEY, cart)
}

// 获取勾选状态 { id: true/false }
export function getSelected() {
	try {
		return uni.getStorageSync(SELECT_KEY) || {}
	} catch (e) {
		return {}
	}
}

function saveSelected(sel) {
	uni.setStorageSync(SELECT_KEY, sel)
}

// 添加商品到购物车
export function addToCart(id, qty = 1) {
	const cart = getCart()
	cart[id] = (cart[id] || 0) + qty
	saveCart(cart)
	const sel = getSelected()
	sel[id] = true
	saveSelected(sel)
	return cart
}

// 增加数量
export function incQty(id) {
	return addToCart(id, 1)
}

// 减少数量
export function decQty(id) {
	const cart = getCart()
	cart[id] = (cart[id] || 0) - 1
	if (cart[id] <= 0) {
		delete cart[id]
		const sel = getSelected()
		delete sel[id]
		saveSelected(sel)
	}
	saveCart(cart)
	return cart
}

// 移除商品
export function removeFromCart(id) {
	const cart = getCart()
	delete cart[id]
	saveCart(cart)
	const sel = getSelected()
	delete sel[id]
	saveSelected(sel)
	return cart
}

// 切换勾选
export function toggleSelect(id) {
	const sel = getSelected()
	sel[id] = !sel[id]
	saveSelected(sel)
	return sel
}

// 全选 / 取消全选
export function toggleSelectAll() {
	const cart = getCart()
	const sel = getSelected()
	const ids = Object.keys(cart)
	const allSelected = ids.length > 0 && ids.every(id => sel[id])
	ids.forEach(id => { sel[id] = !allSelected })
	saveSelected(sel)
	return sel
}

// 是否全选
export function isAllSelected() {
	const cart = getCart()
	const sel = getSelected()
	const ids = Object.keys(cart)
	return ids.length > 0 && ids.every(id => sel[id])
}

// 购物车商品总数
export function getCartCount() {
	const cart = getCart()
	return Object.values(cart).reduce((a, b) => a + b, 0)
}

// 购物车商品列表（带商品详情）
export function getCartList() {
	const cart = getCart()
	const { PRODUCTS } = require('./data.js')
	return PRODUCTS.filter(p => cart[p.id]).map(p => ({
		...p,
		qty: cart[p.id]
	}))
}

// 勾选商品总价
export function getCartTotal() {
	const cart = getCart()
	const sel = getSelected()
	const { PRODUCTS } = require('./data.js')
	const total = PRODUCTS
		.filter(p => cart[p.id] && sel[p.id])
		.reduce((sum, p) => sum + p.price * cart[p.id], 0)
	return total.toFixed(2)
}

// 清空购物车
export function clearCart() {
	saveCart({})
	saveSelected({})
}
