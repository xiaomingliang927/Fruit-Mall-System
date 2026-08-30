/**
 * 水果商城 - 商品数据与分类
 * 图片使用 CDN 远程 URL，正式项目可下载到 /static 目录后替换为本地路径
 */

// 图片资源（CDN 远程地址）
export const IMG = {
	banner: 'https://aka.doubaocdn.com/s/MDyIsn52kp',
	apple: 'https://aka.doubaocdn.com/s/EbFefw1nFr',
	banana: 'https://aka.doubaocdn.com/s/1Stqtu279Y',
	orange: 'https://aka.doubaocdn.com/s/wvAE58fbUY',
	grape: 'https://aka.doubaocdn.com/s/aEKu5VuWc7',
	watermelon: 'https://aka.doubaocdn.com/s/oCGvQrDwyS',
	strawberry: 'https://aka.doubaocdn.com/s/8MEBywSEHw',
	mango: 'https://aka.doubaocdn.com/s/fiXuV7mHkW',
	blueberry: 'https://aka.doubaocdn.com/s/AsxcyYuq10',
	dragonfruit: 'https://aka.doubaocdn.com/s/iemApzkMqY',
	pineapple: 'https://aka.doubaocdn.com/s/7VvdMsuNsO'
}

// 商品列表
export const PRODUCTS = [
	{ id: 1, name: '麒麟西瓜 · 沙瓤多汁', spec: '约4kg/个', price: 29.9, unit: '/个', img: IMG.watermelon, tag: '热卖', cat: '时令鲜果', sales: 1200 },
	{ id: 2, name: '红富士苹果', spec: '500g', price: 8.8, unit: '/份', img: IMG.apple, tag: '', cat: '苹果', sales: 2300 },
	{ id: 3, name: '海南香蕉', spec: '500g', price: 5.9, unit: '/份', img: IMG.banana, tag: '', cat: '香蕉', sales: 3100 },
	{ id: 4, name: '赣南脐橙', spec: '500g', price: 9.9, unit: '/份', img: IMG.orange, tag: '特价', cat: '柑橘橙柚', sales: 1800 },
	{ id: 5, name: '巨峰葡萄 · 果粒饱满', spec: '500g', price: 12.8, unit: '/份', img: IMG.grape, tag: '', cat: '时令鲜果', sales: 960 },
	{ id: 6, name: '丹东草莓 · 奶油甜', spec: '250g', price: 19.9, unit: '/盒', img: IMG.strawberry, tag: '新品', cat: '浆果莓类', sales: 780 },
	{ id: 7, name: '金煌芒果', spec: '500g', price: 13.9, unit: '/份', img: IMG.mango, tag: '', cat: '热带水果', sales: 1500 },
	{ id: 8, name: '云南蓝莓 · 当季', spec: '125g', price: 15.9, unit: '/盒', img: IMG.blueberry, tag: '', cat: '浆果莓类', sales: 640 },
	{ id: 9, name: '红心火龙果', spec: '约450g/个', price: 7.9, unit: '/个', img: IMG.dragonfruit, tag: '', cat: '热带水果', sales: 1100 },
	{ id: 10, name: '都乐菠萝', spec: '约1kg/个', price: 12.9, unit: '/个', img: IMG.pineapple, tag: '', cat: '热带水果', sales: 880 }
]

// 分类
export const CATS = ['推荐', '时令鲜果', '苹果', '柑橘橙柚', '香蕉', '热带水果', '浆果莓类', '果切礼盒']

// 首页秒杀商品 ID
export const FLASH_IDS = [5, 6, 8, 1]

// 首页热卖商品 ID
export const HOT_IDS = [2, 3, 4, 7, 9, 10, 6, 5]

// 根据 ID 获取商品
export function getProduct(id) {
	return PRODUCTS.find(p => p.id === Number(id))
}

// 根据分类获取商品
export function getProductsByCat(cat) {
	if (cat === '推荐') return PRODUCTS
	return PRODUCTS.filter(p => p.cat === cat)
}
