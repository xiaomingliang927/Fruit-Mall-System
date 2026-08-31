/**
 * 数据层适配器：后端 API → 页面所需的同步数据结构（鲜果集界面专用）
 * 商品/分类/图片全部来自后端；PRODUCTS 携带 skuId 供购物车直接同步
 */
import { reactive } from 'vue'
import { BASE_URL, imgUrl } from '../api.js'

const photo = (name) => BASE_URL + '/images/products/' + name

export const IMG = {
	banner: photo('fruit-banner.jpg'),
	apple: photo('fruit-apple.jpg'),
	banana: photo('fruit-banana.jpg'),
	orange: photo('fruit-orange.jpg'),
	grape: photo('fruit-grape.jpg'),
	watermelon: photo('fruit-watermelon.jpg'),
	strawberry: photo('fruit-strawberry.jpg'),
	mango: photo('fruit-mango.jpg'),
	blueberry: photo('fruit-blueberry.jpg'),
	dragonfruit: photo('fruit-dragonfruit.jpg'),
	pineapple: photo('fruit-pineapple.jpg')
}

export const PRODUCTS = reactive([])
export const CATS = reactive(['推荐'])
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
			fail: () => resolve(null)
		})
	})
}

export function loadProducts() {
	if (loaded) return Promise.resolve()
	if (loading) return loading
	loading = Promise.all([
		fetchJson('/api/v1/categories'),
		fetchJson('/api/v1/products?page=1&size=50')
	]).then(([cats, page]) => {
		cats = cats || []
		const products = (page && page.records) || []
		cats.forEach((c) => { catNameById[c.id] = c.name })
		CATS.splice(0, CATS.length, '推荐', ...cats.map((c) => c.name))
		PRODUCTS.splice(0, PRODUCTS.length, ...products.map((p) => ({
			id: p.id,
			skuId: p.firstSkuId,
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
	})
	return loading
}

loadProducts()

export function getProduct(id) {
	return PRODUCTS.find((p) => p.id === Number(id)) || {
		id: Number(id), skuId: null, name: '加载中…', spec: '', price: 0, unit: '',
		img: IMG.apple, tag: '', cat: '', sales: 0
	}
}

export function getProductsByCat(cat) {
	if (cat === '推荐') return PRODUCTS
	return PRODUCTS.filter((p) => p.cat === cat)
}
