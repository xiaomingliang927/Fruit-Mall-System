/**
 * 数据层适配器：后端 API → 页面所需的同步数据结构
 *
 * 页面（index/category/cart/detail/mine）保持果小满原始写法不变，
 * 本文件负责从后端拉取数据并填充 PRODUCTS / CATS / FLASH_IDS / HOT_IDS。
 * 数组均为 Vue reactive，加载完成后依赖它们的页面自动刷新。
 */
import { reactive } from 'vue'
import { BASE_URL, imgUrl } from '../api.js'

const photo = (name) => BASE_URL + '/images/products/' + name

/** 页面引用的图片资源（映射到后端静态目录的真实照片） */
export const IMG = {
	banner: photo('gift-box.jpg'),
	apple: photo('aksu-apple.jpg'),
	banana: photo('gannan-orange.jpg'),
	orange: photo('gannan-orange.jpg'),
	grape: photo('chile-cherry.jpg'),
	watermelon: photo('gannan-orange.jpg'),
	strawberry: photo('dandong-strawberry.jpg'),
	mango: photo('thai-durian.jpg'),
	blueberry: photo('yunnan-blueberry.jpg'),
	dragonfruit: photo('gift-box.jpg'),
	pineapple: photo('gannan-orange.jpg')
}

/** 商品列表（后端 /api/v1/products，按销量降序） */
export const PRODUCTS = reactive([])

/** 分类名（后端 /api/v1/categories，首项固定「推荐」） */
export const CATS = reactive(['推荐'])

/** 首页秒杀 = 销量前 4；今日热卖 = 第 5~12 */
export const FLASH_IDS = reactive([])
export const HOT_IDS = reactive([])

let loaded = false
let loading = null
const catNameById = {}

function fetchJson(url) {
	return new Promise((resolve) => {
		uni.request({
			url: BASE_URL + url,
			method: 'GET',
			header: { 'Content-Type': 'application/json' },
			success: (r) => resolve(r.data && r.data.code === 0 ? r.data.data : null),
			fail: () => resolve(null),
		})
	})
}

/** 拉取分类与商品并填充响应式数组；幂等，可在任意页面重复调用 */
export function loadProducts() {
	if (loaded) return Promise.resolve()
	if (loading) return loading
	loading = Promise.all([
		fetchJson('/api/v1/categories'),
		fetchJson('/api/v1/products?page=1&size=50'),
	]).then(([cats, page]) => {
		cats = cats || []
		const products = (page && page.records) || []
		cats.forEach((c) => { catNameById[c.id] = c.name })
		CATS.splice(0, CATS.length, '推荐', ...cats.map((c) => c.name))
		PRODUCTS.splice(0, PRODUCTS.length, ...products.map((p) => ({
			id: p.id,
			name: p.name,
			spec: p.subtitle || '',
			price: p.minPrice / 100,
			unit: p.unit ? '/' + p.unit : '',
			img: imgUrl(p.mainImage),
			tag: (p.tags || '').split(',').filter(Boolean)[0] || '',
			cat: catNameById[p.categoryId] || '',
			sales: p.sales
		})))
		const bySales = [...PRODUCTS].sort((a, b) => b.sales - a.sales)
		FLASH_IDS.splice(0, FLASH_IDS.length, ...bySales.slice(0, 4).map((p) => p.id))
		HOT_IDS.splice(0, HOT_IDS.length, ...bySales.slice(4, 12).map((p) => p.id))
		loaded = true
	}).catch(() => {
		loading = null
		uni.showToast({ title: '数据加载失败，请确认后端已启动', icon: 'none' })
	})
	return loading
}

// 模块加载即预热数据（Tab 首页导入本文件时自动触发）
loadProducts()

// 根据 ID 获取商品（未加载完成时返回占位对象，避免模板报错）
export function getProduct(id) {
	return PRODUCTS.find((p) => p.id === Number(id)) || {
		id: Number(id), name: '加载中…', spec: '', price: 0, unit: '',
		img: IMG.apple, tag: '', cat: '', sales: 0
	}
}

// 根据分类名获取商品（「推荐」返回全部）
export function getProductsByCat(cat) {
	if (cat === '推荐') return PRODUCTS
	return PRODUCTS.filter((p) => p.cat === cat)
}
