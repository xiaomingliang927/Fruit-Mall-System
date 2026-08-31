// 小程序：订单页评价入口 + 详情页评价区 补丁（v2：匹配 options API 的 detail.vue）
const fs = require('fs');

// 1) 订单页（script setup 组合式）
const op = 'C:/Users/季/Desktop/fruit/miniapp/src/pages/orders/orders.vue';
let o = fs.readFileSync(op, 'utf8');
if (!o.includes('goReview')) {
  o = o.replace(
    `<view v-if="o.status === 30" class="act main" @tap="confirmOrder(o)">确认收货</view>`,
    `<view v-if="o.status === 30" class="act main" @tap="confirmOrder(o)">确认收货</view>
        <view v-if="o.status === 40" class="act main" @tap="goReview(o)">评价</view>`);
  o = o.replace(`async function pay(o) {`, `function goReview(o) {
  uni.navigateTo({ url: '/pages/review/review?orderNo=' + o.orderNo })
}

async function pay(o) {`);
  fs.writeFileSync(op, o);
}
console.log('orders goReview:', o.includes('goReview'));

// 2) 详情页（options API，鲜果集原版结构）
const dp = 'C:/Users/季/Desktop/fruit/miniapp/src/pages/detail/detail.vue';
let d = fs.readFileSync(dp, 'utf8');

// 2a. 模板：评价卡片插到「选择规格」前
if (!d.includes('rv-item')) {
  const reviewsCard = `			<view class="card sec">
				<view class="lab">用户评价（{{ reviews.length }}）<text class="rv-avg" v-if="reviews.length">评分 {{ avgRating }}</text></view>
				<view v-if="!reviews.length" class="rv-empty">暂无评价</view>
				<view v-for="r in reviews" :key="r.id" class="rv-item">
					<view class="rv-head">
						<text class="rv-user">{{ r.userNickname }}</text>
						<text class="rv-stars">
							<text v-for="n in 5" :key="n" class="rv-star" :class="{ on: n <= r.rating }">★</text>
						</text>
						<text class="rv-date">{{ (r.createdAt || '').slice(0, 10) }}</text>
					</view>
					<view class="rv-content">{{ r.content }}</view>
					<view v-if="r.adminReply" class="rv-reply">商家回复：{{ r.adminReply }}</view>
				</view>
			</view>

`;
  d = d.replace(`			<view class="card sec">
				<view class="lab">选择规格</view>`, reviewsCard + `			<view class="card sec">
				<view class="lab">选择规格</view>`);
  // 样式
  d = d.replace(`</style>`, `	.rv-avg { color: #f24e3e; font-weight: 700; margin-left: 12rpx; }
	.rv-empty { color: #a5aca1; font-size: 24rpx; padding: 8rpx 0 12rpx; }
	.rv-item { border-bottom: 1rpx dashed #f0f2f4; padding: 12rpx 0; }
	.rv-item:last-child { border-bottom: none; }
	.rv-head { display: flex; align-items: center; gap: 12rpx; }
	.rv-user { font-size: 24rpx; font-weight: 600; }
	.rv-star { font-size: 22rpx; color: #e2e5e9; }
	.rv-star.on { color: #f7ba2a; }
	.rv-date { margin-left: auto; font-size: 20rpx; color: #a5aca1; }
	.rv-content { font-size: 24rpx; color: #4b5563; margin-top: 8rpx; line-height: 1.7; }
	.rv-reply {
		background: #f7f9f5; border-left: 4rpx solid #2e9e6b; border-radius: 6rpx;
		padding: 10rpx 16rpx; font-size: 22rpx; color: #6b7269; margin-top: 10rpx;
	}
</style>`);
  fs.writeFileSync(dp, d);
}
console.log('detail template ✓', d.includes('rv-item'));

// 3) 脚本：data 加 reviews/avgRating，onLoad 拉取（需引入 api）
if (!d.includes('reviews: []')) {
  d = d.replace(`			statusBarHeight: 20,`, `			reviews: [],
			avgRating: '5.0',
			statusBarHeight: 20,`);
  fs.writeFileSync(dp, d);
}
if (!d.includes("import { api } from '../../api'")) {
  d = d.replace(`<script>`, `<script>
	import { api } from '../../api'`);
  fs.writeFileSync(dp, d);
}
// onLoad 里 product 加载后拉评价
if (!d.includes('/reviews?page=1')) {
  d = d.replace(`	onLoad(options) {
		this.productId = Number(options.id) || 1`, `	onLoad(options) {
		this.productId = Number(options.id) || 1
		api.get('/api/v1/products/' + this.productId + '/reviews?page=1&size=3')
			.then((r) => { this.reviews = r.records; this.avgRating = r.records.length ? this.calcAvg(r.records) : '5.0' })
			.catch(() => {})`);
  fs.writeFileSync(dp, d);
}
// methods 加 calcAvg
if (!d.includes('calcAvg(')) {
  d = d.replace(`	methods: {`, `	methods: {
		calcAvg(list) {
			const sum = list.reduce((s, r) => s + r.rating, 0)
			return (sum / list.length).toFixed(1)
		},`);
  fs.writeFileSync(dp, d);
}
console.log('detail script ✓ reviews:', d.includes('reviews: []'), '| api import:', d.includes("import { api }"));
