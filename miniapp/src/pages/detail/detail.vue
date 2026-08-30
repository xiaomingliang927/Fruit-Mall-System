<template>
	<view class="page">
		<!-- 自定义导航 -->
		<view class="nav-bar" :style="{ paddingTop: statusBarHeight + 'px' }">
			<view class="nav-content">
				<view class="nav-back" @click="goBack"><text>‹</text></view>
				<text class="nav-title">商品详情</text>
				<view class="nav-share" @click="showToast('分享已复制')"><text>⤴</text></view>
			</view>
		</view>

		<scroll-view scroll-y class="detail-scroll">
			<!-- 商品大图 -->
			<view class="d-img">
				<image :src="product.img" mode="aspectFill" class="d-img-inner"></image>
				<view class="d-sale"><text>{{ product.tag ? product.tag + ' · ' : '' }}产地直采</text><text>已售 {{ product.sales }} 份</text></view>
			</view>

			<!-- 价格信息 -->
			<view class="d-main">
				<view class="d-price">
					<text class="d-price-num"><text class="d-price-symbol">¥</text>{{ product.price }}</text>
					<text class="d-orig">¥{{ origPrice }}</text>
					<text class="d-offtag">{{ product.id === 6 ? '新品 9 折' : '限时 9 折' }}</text>
				</view>
				<text class="d-name">{{ product.name }}</text>
				<text class="d-sub">{{ product.spec }}{{ product.unit }} · 现摘现发 · 全国包邮</text>
				<view class="d-meta">
					<text>已选：<text class="d-meta-b">{{ product.spec }}</text></text>
					<text>运费：<text class="d-meta-b">包邮</text></text>
					<text>评价：<text class="d-meta-b">4.9 分</text></text>
				</view>
			</view>

			<!-- 规格选择 -->
			<view class="d-sec">
				<view class="d-sec-hd"><text class="d-sec-icon">🏷️</text>选择规格</view>
				<view class="d-spec">
					<view class="spec-chip" v-for="(s, i) in specs" :key="i" :class="{ on: i === 1 }" @click="selectSpec(s)">{{ s }}</view>
				</view>
			</view>

			<!-- 服务保障 -->
			<view class="d-sec">
				<view class="d-sec-hd"><text class="d-sec-icon">🛡️</text>服务保障</view>
				<view class="d-serve">
					<view class="d-serve-it"><text class="d-serve-icon">✓</text><text>正品保障</text></view>
					<view class="d-serve-it"><text class="d-serve-icon">😊</text><text>坏果包赔</text></view>
					<view class="d-serve-it"><text class="d-serve-icon">🚚</text><text>同城速达</text></view>
				</view>
			</view>

			<!-- 商品详情 -->
			<view class="d-article">
				<view class="d-article-hd">商品详情</view>
				<image :src="product.img" mode="aspectFill" class="d-article-img"></image>
				<text class="d-article-text">{{ product.name }}，{{ product.spec }}{{ product.unit }}，现摘现发、坏果包赔。果肉饱满、口感清甜，是家庭日常与下午茶的不错选择。（此段为原型占位文案，正式上线将替换为商品详情图文。）</text>
			</view>
			<view style="height: 40rpx;"></view>
		</scroll-view>

		<!-- 底部操作栏 -->
		<view class="d-bar">
			<view class="d-bar-sv" @click="showToast('在线客服')">
				<text class="d-bar-icon">🎧</text>
				<text class="d-bar-lb">客服</text>
			</view>
			<view class="d-bar-sv" @click="goCart">
				<text class="d-bar-icon">🛒</text>
				<text class="d-bar-lb">购物车</text>
				<text class="d-bar-badge" v-if="cartCount > 0">{{ cartCount > 99 ? '99+' : cartCount }}</text>
			</view>
			<view class="d-bar-go">
				<view class="d-bar-btn add" @click="addCart">加入购物车</view>
				<view class="d-bar-btn buy" @click="buyNow">立即购买</view>
			</view>
		</view>
	</view>
</template>

<script>
	import { getProduct } from '@/utils/data.js'
	import { addToCart, getCartCount, updateCartBadge } from '@/utils/cart.js'

	export default {
		data() {
			return {
				statusBarHeight: 20,
				productId: 0,
				product: {},
				cartCount: 0
			}
		},
		computed: {
			origPrice() {
				return (this.product.price * 1.35).toFixed(1)
			},
			specs() {
				return ['标准装', this.product.spec, '礼盒装(2份)', '整箱(4份)']
			}
		},
		onLoad(options) {
			this.productId = Number(options.id) || 1
			this.product = getProduct(this.productId)
			const sysInfo = uni.getSystemInfoSync()
			this.statusBarHeight = sysInfo.statusBarHeight || 20
			this.refreshCart()
		},
		onShow() {
			this.refreshCart()
		},
		methods: {
			refreshCart() {
				this.cartCount = getCartCount()
			},
			goBack() {
				uni.navigateBack()
			},
			goCart() {
				uni.switchTab({ url: '/pages/cart/cart' })
			},
			selectSpec(s) {
				this.showToast('已选规格：' + s)
			},
			addCart() {
				addToCart(this.productId)
				updateCartBadge()
				this.refreshCart()
				uni.showToast({ title: '已加入购物车', icon: 'none' })
			},
			buyNow() {
				addToCart(this.productId)
				this.showToast('已加入购物车，去结算（原型）')
			},
			showToast(msg) {
				uni.showToast({ title: msg, icon: 'none' })
			}
		}
	}
</script>

<style scoped>
	.page { height: 100vh; display: flex; flex-direction: column; background: #f5f5f5; }
	.nav-bar { background: #fff; flex-shrink: 0; }
	.nav-content { display: flex; align-items: center; justify-content: center; position: relative; height: 88rpx; }
	.nav-back { position: absolute; left: 16rpx; top: 50%; transform: translateY(-50%); width: 72rpx; height: 72rpx; display: flex; align-items: center; justify-content: center; font-size: 48rpx; color: #333; }
	.nav-title { font-size: 32rpx; font-weight: 600; color: #222; }
	.nav-share { position: absolute; right: 16rpx; top: 50%; transform: translateY(-50%); width: 72rpx; height: 72rpx; display: flex; align-items: center; justify-content: center; font-size: 36rpx; color: #333; }
	.detail-scroll { flex: 1; }
	.d-img { position: relative; background: #fff; }
	.d-img-inner { width: 100%; aspect-ratio: 1; }
	.d-sale { position: absolute; left: 0; right: 0; bottom: 0; background: linear-gradient(180deg, rgba(0,0,0,0), rgba(0,0,0,.4)); color: #fff; font-size: 24rpx; padding: 52rpx 28rpx 20rpx; display: flex; justify-content: space-between; }
	.d-main { background: #fff; padding: 28rpx 28rpx 32rpx; margin-top: -28rpx; border-radius: 32rpx 32rpx 0 0; position: relative; }
	.d-price { display: flex; align-items: flex-end; gap: 16rpx; }
	.d-price-num { color: #f24e3e; font-size: 52rpx; font-weight: 800; }
	.d-price-symbol { font-size: 30rpx; }
	.d-orig { font-size: 24rpx; color: #bbb; text-decoration: line-through; }
	.d-offtag { margin-left: auto; background: #f24e3e; color: #fff; font-size: 22rpx; padding: 6rpx 18rpx; border-radius: 18rpx; font-weight: 600; }
	.d-name { font-size: 32rpx; font-weight: 700; line-height: 1.5; margin-top: 18rpx; color: #222; display: block; }
	.d-sub { font-size: 25rpx; color: #888; margin-top: 10rpx; display: block; }
	.d-meta { display: flex; gap: 36rpx; font-size: 24rpx; color: #aaa; margin-top: 22rpx; }
	.d-meta-b { color: #666; font-weight: 600; }
	.d-sec { background: #fff; border-radius: 32rpx; margin-top: 20rpx; padding: 28rpx; }
	.d-sec-hd { font-size: 30rpx; font-weight: 700; display: flex; align-items: center; gap: 12rpx; margin-bottom: 22rpx; color: #222; }
	.d-sec-icon { font-size: 32rpx; }
	.d-spec { display: flex; flex-wrap: wrap; gap: 16rpx; }
	.spec-chip { font-size: 25rpx; padding: 14rpx 28rpx; border-radius: 18rpx; border: 2rpx solid #eee; color: #666; background: #fafafa; }
	.spec-chip.on { border-color: #2e9e6b; color: #17704a; background: #e6f4ec; font-weight: 600; }
	.d-serve { display: grid; grid-template-columns: repeat(3, 1fr); gap: 16rpx; }
	.d-serve-it { display: flex; flex-direction: column; align-items: center; gap: 12rpx; font-size: 23rpx; color: #666; }
	.d-serve-icon { width: 48rpx; height: 48rpx; border-radius: 50%; background: #e6f4ec; color: #2e9e6b; display: flex; align-items: center; justify-content: center; font-size: 24rpx; }
	.d-article { background: #fff; border-radius: 32rpx; margin-top: 20rpx; overflow: hidden; }
	.d-article-hd { font-size: 30rpx; font-weight: 700; padding: 28rpx 28rpx 8rpx; color: #222; }
	.d-article-img { width: 100%; }
	.d-article-text { font-size: 26rpx; line-height: 1.9; color: #888; padding: 12rpx 28rpx 28rpx; display: block; }
	.d-bar { background: #fff; display: flex; align-items: center; gap: 12rpx; padding: 16rpx 24rpx; padding-bottom: calc(16rpx + env(safe-area-inset-bottom)); border-top: 1rpx solid #f0f0f0; flex-shrink: 0; }
	.d-bar-sv { display: flex; flex-direction: column; align-items: center; justify-content: center; gap: 4rpx; font-size: 21rpx; color: #888; width: 92rpx; position: relative; }
	.d-bar-icon { font-size: 40rpx; }
	.d-bar-badge { position: absolute; top: -8rpx; right: 8rpx; background: #f24e3e; color: #fff; font-size: 18rpx; min-width: 30rpx; height: 30rpx; border-radius: 15rpx; display: flex; align-items: center; justify-content: center; padding: 0 8rpx; font-weight: 600; }
	.d-bar-go { flex: 1; display: flex; gap: 16rpx; }
	.d-bar-btn { flex: 1; height: 80rpx; border-radius: 44rpx; font-size: 28rpx; color: #fff; display: flex; align-items: center; justify-content: center; font-weight: 600; }
	.d-bar-btn.add { background: linear-gradient(90deg, #ffa84d, #ff8a3d); }
	.d-bar-btn.buy { background: linear-gradient(90deg, #35b47e, #1f8a58); }
</style>
