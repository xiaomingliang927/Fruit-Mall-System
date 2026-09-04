<template>
	<view class="page">
		<!-- 红色渐变顶部导航 -->
		<view class="nav-home" :style="{ paddingTop: statusBarHeight + 'px' }">
			<view class="nav-topline">
				<view class="brand">
					<view class="logo"><text class="logo-text">果</text></view>
					<text>果上选</text>
				</view>
				<view class="loc"><image src="/static/icon/png/map-pin-white.png" mode="aspectFit" class="loc-icon"></image><text>湘西州·滨江公寓</text></view>
				<view class="head-icons">
					<view class="icon-btn" @click="showToast('暂无新消息')"><image src="/static/icon/png/bell-white.png" mode="aspectFit" class="bell-icon"></image></view>
				</view>
			</view>
			<view class="searchbar" @click="showToast('搜索功能开发中')">
				<text class="cat-icon">☰</text>
				<text class="cat-text">分类</text>
				<text class="search-txt">搜索新鲜水果</text>
				<view class="search-btn">搜索</view>
			</view>
		</view>

		<view class="content">
			<!-- 限时秒杀 -->
			<view class="flash">
				<view class="flash-head">
					<view class="flash-t"><text class="flash-icon">⚡</text>限时秒杀<text class="flash-hot">爆品天天有</text></view>
					<view class="flash-cd">距结束 <text class="cd-num">{{ countdownText }}</text></view>
				</view>
				<scroll-view scroll-x class="flash-track" show-scrollbar="false">
					<view class="flash-card" v-for="id in FLASH_IDS" :key="id" @click="goDetail(id)">
						<view class="flash-img">
							<image :src="getProduct(id).img" mode="aspectFill" class="flash-img-inner"></image>
							<text class="flash-off">{{ getProduct(id).id === 6 ? '尝鲜' : '直降' }}</text>
						</view>
						<text class="flash-nm">{{ getProduct(id).name }}</text>
						<view class="flash-pr"><text class="flash-price">¥{{ getProduct(id).price }}</text><text class="flash-unit">{{ getProduct(id).unit }}</text></view>
						<text class="flash-sales">已售{{ getProduct(id).sales }}</text>
					</view>
				</scroll-view>
			</view>


			<!-- 金刚区（5列圆形图） -->
			<view class="grid-v2">
				<view class="grid-row" v-for="(row, ri) in gridRows" :key="ri">
					<view class="grid-cell" v-for="c in row" :key="c.lb" @click="goCatCell(c)">
						<view class="grid-ic"><image :src="c.img" mode="aspectFill" class="grid-ic-img"></image></view>
						<text class="grid-lb">{{ c.lb }}</text>
					</view>
				</view>
				<view class="grid-dots"><text class="dot on"></text><text class="dot"></text></view>
			</view>

			<!-- 首页轮播（后台「营销中心 → 轮播图」配置；无数据时回落静态图） -->
			<swiper v-if="banners.length" class="banner-sw" :indicator-dots="true"
			        indicator-color="rgba(255,255,255,.55)" indicator-active-color="#f24e3e"
			        :autoplay="true" :interval="4000" :circular="true">
				<swiper-item v-for="b in banners" :key="b.id" @click="onBanner(b)">
					<image :src="imgUrl(b.image)" mode="aspectFill" class="banner-img"></image>
				</swiper-item>
			</swiper>
			<view v-else class="promo-banner" @click="showToast('活动详情（原型占位）')">
				<image :src="IMG.banner" mode="aspectFill" class="promo-img"></image>
				<view class="promo-tag"><text class="promo-tag-t">免费领</text><text class="promo-tag-s">福利中心</text></view>
			</view>

			<!-- 今日热卖 -->
			<view class="hot-sec">
				<view class="hot-hd"><text class="hot-t">今日热卖</text><text class="hot-sub">产地直采 · 坏果包赔</text></view>
				<view class="hot-grid">
					<view class="p-card" v-for="id in HOT_IDS" :key="id" @click="goDetail(id)">
						<view class="p-img">
							<image :src="getProduct(id).img" mode="aspectFill" class="p-img-inner"></image>
							<text class="p-tag" v-if="getProduct(id).tag">{{ getProduct(id).tag }}</text>
						</view>
						<view class="p-bd">
							<text class="p-nm">{{ getProduct(id).name }}</text>
							<text class="p-sp">{{ getProduct(id).spec }}{{ getProduct(id).unit }}</text>
							<view class="p-btm">
								<view class="p-pr"><text class="p-price">¥{{ getProduct(id).price }}</text><text class="p-unit">{{ getProduct(id).unit }}</text></view>
								<view class="p-add" @click.stop="addCart(id)"><text class="add-icon">+</text><view v-if="cartCounts[id]" class="add-badge">{{ cartCounts[id] }}</view></view>
							</view>
						</view>
					</view>
				</view>
			</view>
			<view style="height: 40rpx;"></view>
		</view>
	</view>
</template>

<script>
	import { IMG, PRODUCTS, FLASH_IDS, HOT_IDS, getProduct, CAT_LINK } from '@/utils/data.js'
	import { addToCart, getCartCounts, updateCartBadge } from '@/utils/cart.js'
	import { api, imgUrl } from '@/api'

	export default {
		data() {
			return {
				IMG,
				FLASH_IDS,
				HOT_IDS,
				cartCounts: {},
				banners: [],
				statusBarHeight: 20,
				countdown: 2 * 3600 + 59 * 60 + 59,
				countdownText: '02:59:59',
				timer: null,
				gridCats: [
					{ lb: '时令鲜果', img: IMG.apple, to: '时令鲜果' },
					{ lb: '苹果专区', img: IMG.apple, to: '国产精品' },
					{ lb: '柑橘橙柚', img: IMG.orange, to: '国产精品' },
					{ lb: '热带水果', img: IMG.mango, to: '进口水果' },
					{ lb: '浆果莓类', img: IMG.strawberry, to: '进口水果' },
					{ lb: '瓜类精选', img: IMG.watermelon, to: '国产精品' },
					{ lb: '果切礼盒', img: IMG.pineapple, to: '精品礼盒' },
					{ lb: '果汁饮品', img: IMG.grape, to: '时令鲜果' },
					{ lb: '进口优选', img: IMG.blueberry, to: '进口水果' },
					{ lb: '会员专享', img: IMG.dragonfruit, to: '精品礼盒' }
				]
			}
		},
		computed: {
			gridRows() {
				const rows = []
				for (let i = 0; i < this.gridCats.length; i += 5) {
					rows.push(this.gridCats.slice(i, i + 5))
				}
				return rows
			}
		},
		onLoad() {
			const sysInfo = uni.getSystemInfoSync()
			this.statusBarHeight = sysInfo.statusBarHeight || 20
			this.startCountdown()
			this.loadBanners()
		},
		onShow() {
			this.cartCounts = getCartCounts()
			this.updateTabBar()
		},
		onUnload() {
			if (this.timer) clearInterval(this.timer)
		},
		methods: {
			getProduct,
			imgUrl,
			loadBanners() {
				api.get('/api/v1/banners?position=home')
					.then((list) => { this.banners = Array.isArray(list) ? list : [] })
					.catch(() => {})
			},
			onBanner(b) {
				if (!b) return
				if (b.linkType === 1 && b.linkValue) {
					uni.navigateTo({ url: '/pages/detail/detail?id=' + b.linkValue })
				} else if (b.linkType === 2 && b.linkValue) {
					if (b.linkValue.indexOf('/pages/') === 0) uni.navigateTo({ url: b.linkValue })
					else this.showToast(b.title || '活动详情')
				}
			},
			updateTabBar() {
				if (typeof this.getTabBar === 'function' && this.getTabBar()) {
					this.getTabBar().selected = 0
					this.getTabBar().refreshCartCount()
				}
			},
			startCountdown() {
				this.timer = setInterval(() => {
					this.countdown--
					if (this.countdown < 0) this.countdown = 2 * 3600 + 59 * 60 + 59
					const h = Math.floor(this.countdown / 3600)
					const m = Math.floor((this.countdown % 3600) / 60)
					const s = this.countdown % 60
					this.countdownText = [h, m, s].map(x => String(x).padStart(2, '0')).join(':')
				}, 1000)
			},
			goLive() {
				uni.navigateTo({ url: '/pages/live/live' })
			},
			goDetail(id) {
				uni.navigateTo({ url: '/pages/detail/detail?id=' + id })
			},
			addCart(id) {
				addToCart(id)
				this.cartCounts = getCartCounts()
				updateCartBadge()
				this.updateTabBar()
				uni.showToast({ title: '已加入购物车', icon: 'none' })
			},
			/** 分类宫格：跳到分类页并选中对应分类 */
			goCatCell(c) {
				CAT_LINK.pending = c.to || '推荐'
				uni.switchTab({ url: '/pages/category/category' })
			},
			showToast(msg) {
				uni.showToast({ title: msg, icon: 'none' })
			}
		}
	}
</script>

<style scoped>
	.page { min-height: 100vh; background: #f5f5f5; }
	.nav-home { background: linear-gradient(180deg, #ff4d4d 0%, #ff6b5e 55%, #f5f5f5 100%); padding: 0 28rpx 20rpx; color: #fff; }
	.nav-topline { display: flex; align-items: center; justify-content: space-between; padding: 20rpx 0; }
	.brand { display: flex; align-items: center; gap: 12rpx; font-weight: 800; font-size: 40rpx; }
	.logo { width: 48rpx; height: 48rpx; border-radius: 14rpx; background: #fff; display: flex; align-items: center; justify-content: center; }
	.logo-text { color: #d9363e; font-size: 28rpx; font-weight: 700; }
	.loc-icon { width: 26rpx; height: 26rpx; flex: none; }
		.loc { display: flex; align-items: center; gap: 6rpx; font-size: 24rpx; font-weight: 500; opacity: .95; max-width: 280rpx; overflow: hidden; white-space: nowrap; text-overflow: ellipsis; }
	.head-icons { display: flex; }
	.icon-btn { width: 64rpx; height: 64rpx; border-radius: 50%; background: rgba(255,255,255,.18); display: flex; align-items: center; justify-content: center; }
		.bell-icon { width: 34rpx; height: 34rpx; }
	.searchbar { display: flex; align-items: center; gap: 12rpx; background: #fff; border-radius: 44rpx; padding: 10rpx 10rpx 10rpx 24rpx; }
	.cat-icon { color: #333; font-size: 28rpx; }
	.cat-text { color: #666; font-size: 26rpx; border-right: 1rpx solid #eee; padding-right: 16rpx; }
	.search-txt { flex: 1; color: #999; font-size: 26rpx; overflow: hidden; white-space: nowrap; text-overflow: ellipsis; }
	.search-btn { background: #2e9e6b; color: #fff; border-radius: 34rpx; padding: 12rpx 32rpx; font-size: 26rpx; font-weight: 600; }
	.content { padding-bottom: 120rpx; }
	.add-icon { font-size: 32rpx; line-height: 1; font-weight: 300; }
	.grid-v2 { margin: 20rpx 24rpx 0; background: #fff; border-radius: 28rpx; padding: 28rpx 16rpx 20rpx; }
	.grid-row { display: grid; grid-template-columns: repeat(5, 1fr); gap: 8rpx; }
	.grid-cell { display: flex; flex-direction: column; align-items: center; gap: 12rpx; padding: 12rpx 0; }
	.grid-ic { width: 104rpx; height: 104rpx; border-radius: 50%; overflow: hidden; background: #f5f5f5; }
	.grid-ic-img { width: 100%; height: 100%; }
	.grid-lb { font-size: 23rpx; color: #333; }
	.grid-dots { display: flex; justify-content: center; gap: 8rpx; margin-top: 16rpx; }
	.dot { width: 8rpx; height: 8rpx; border-radius: 4rpx; background: #ddd; }
	.dot.on { width: 24rpx; background: #2e9e6b; }
	.banner-sw { margin: 20rpx 24rpx 0; border-radius: 28rpx; overflow: hidden; height: 220rpx; }
	.banner-img { width: 100%; height: 100%; display: block; }
	.promo-banner { margin: 20rpx 24rpx 0; border-radius: 28rpx; overflow: hidden; height: 220rpx; position: relative; }
	.promo-img { width: 100%; height: 100%; }
	.promo-tag { position: absolute; right: 0; top: 0; bottom: 0; width: 128rpx; background: linear-gradient(180deg, #ffd700, #ff8c00); display: flex; flex-direction: column; align-items: center; justify-content: center; color: #fff; }
	.promo-tag-t { font-size: 36rpx; font-weight: 800; writing-mode: vertical-rl; letter-spacing: 4rpx; }
	.promo-tag-s { font-size: 20rpx; margin-top: 8rpx; }
	.flash { margin: 20rpx 24rpx 0; background: #fff; border-radius: 28rpx; padding: 24rpx 0 28rpx; }
	.flash-head { display: flex; align-items: center; padding: 0 24rpx 20rpx; }
	.flash-t { display: flex; align-items: center; gap: 10rpx; font-size: 32rpx; font-weight: 800; color: #222; }
	.flash-icon { font-size: 30rpx; }
	.flash-hot { font-size: 20rpx; background: #f24e3e; color: #fff; padding: 4rpx 14rpx; border-radius: 16rpx; font-weight: 600; margin-left: 8rpx; }
	.flash-cd { margin-left: auto; display: flex; align-items: center; gap: 8rpx; font-size: 22rpx; color: #888; }
	.cd-num { background: #222; color: #fff; font-weight: 600; border-radius: 8rpx; padding: 4rpx 10rpx; font-size: 22rpx; font-variant-numeric: tabular-nums; }
	.flash-track { white-space: nowrap; padding: 4rpx 24rpx 0; }
	.flash-card { display: inline-block; width: 216rpx; margin-right: 20rpx; vertical-align: top; }
	.flash-img { position: relative; width: 216rpx; height: 216rpx; border-radius: 24rpx; overflow: hidden; background: #fafafa; }
	.flash-img-inner { width: 100%; height: 100%; }
	.flash-off { position: absolute; left: 12rpx; top: 12rpx; background: #f24e3e; color: #fff; font-size: 20rpx; padding: 4rpx 14rpx; border-radius: 16rpx; font-weight: 600; }
	.flash-nm { font-size: 24rpx; color: #333; margin-top: 12rpx; display: block; overflow: hidden; white-space: nowrap; text-overflow: ellipsis; }
	.flash-pr { display: flex; align-items: baseline; gap: 4rpx; margin-top: 6rpx; }
	.flash-price { color: #f24e3e; font-size: 30rpx; font-weight: 700; }
	.flash-unit { font-size: 20rpx; color: #bbb; }
	.flash-sales { font-size: 20rpx; color: #bbb; margin-top: 2rpx; display: block; }
	.hot-sec { margin: 20rpx 24rpx 32rpx; }
	.hot-hd { display: flex; align-items: center; gap: 16rpx; padding: 8rpx 4rpx 20rpx; }
	.hot-t { font-size: 32rpx; font-weight: 800; color: #222; }
	.hot-sub { font-size: 23rpx; color: #999; }
	.hot-grid { display: grid; grid-template-columns: 1fr 1fr; gap: 20rpx; }
	.p-card { background: #fff; border-radius: 28rpx; overflow: hidden; }
	.p-img { position: relative; aspect-ratio: 1; background: #fafafa; }
	.p-img-inner { width: 100%; height: 100%; }
	.p-tag { position: absolute; left: 16rpx; top: 16rpx; background: #ff8a3d; color: #fff; font-size: 20rpx; padding: 4rpx 16rpx; border-radius: 16rpx; font-weight: 600; }
	.p-bd { padding: 18rpx 20rpx 22rpx; }
	.p-nm { font-size: 26rpx; font-weight: 500; color: #222; display: block; overflow: hidden; white-space: nowrap; text-overflow: ellipsis; }
	.p-sp { font-size: 22rpx; color: #bbb; margin-top: 4rpx; display: block; }
	.p-btm { display: flex; align-items: center; justify-content: space-between; margin-top: 14rpx; }
	.p-price { color: #f24e3e; font-size: 32rpx; font-weight: 700; }
	.p-unit { font-size: 20rpx; color: #bbb; }
	.p-add { position: relative; width: 56rpx; height: 56rpx; border-radius: 50%; background: #2e9e6b; color: #fff; display: flex; align-items: center; justify-content: center; }
	.add-badge { position: absolute; top: -10rpx; right: -10rpx; min-width: 30rpx; height: 30rpx; border-radius: 15rpx; background: #e54d42; color: #fff; font-size: 19rpx; line-height: 30rpx; text-align: center; padding: 0 5rpx; font-weight: 700; box-shadow: 0 2rpx 6rpx rgba(0,0,0,0.2); }
	.live-fab {
		position: fixed; right: 28rpx; bottom: 160rpx; width: 104rpx; height: 104rpx;
		border-radius: 50%; background: linear-gradient(135deg, #43c98a, #1f8a58);
		display: flex; flex-direction: column; align-items: center; justify-content: center;
		box-shadow: 0 8rpx 24rpx rgba(46, 158, 107, 0.4); border: 6rpx solid #fff; z-index: 99;
	}
	.live-fab-icon { width: 38rpx; height: 38rpx; }
	.live-fab-text { font-size: 17rpx; color: #fff; margin-top: 2rpx; font-weight: 600; }
</style>
