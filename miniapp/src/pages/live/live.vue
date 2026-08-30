<template>
	<view class="page">
		<!-- 顶部大直播卡 -->
		<view class="live-hero">
			<image :src="IMG.banner" mode="aspectFill" class="live-hero-bg"></image>
			<view class="live-hero-mask"></view>
			<view class="live-hero-top">
				<view class="live-hero-badge"><text class="live-dot"></text><text>LIVE · 直播中</text></view>
				<view class="live-hero-viewers">👁 1.8万人在看</view>
			</view>
			<view class="live-hero-mid">
				<text class="live-hero-en">ORCHARD DIRECT</text>
				<text class="live-hero-title">阿橙的果园挑选间</text>
			</view>
			<view class="live-hero-btm">
				<text class="live-hero-cutting">正在开切：新疆哈密瓜</text>
				<view class="live-hero-enter" @click="enterLive">进入直播 →</view>
			</view>
		</view>

		<!-- 直播间正在讲 -->
		<view class="live-picks">
			<view class="live-picks-hd">
				<text class="live-picks-title">直播间正在讲</text>
				<text class="live-picks-more">限时好价</text>
			</view>
			<view class="live-picks-grid">
				<view class="pick-card" v-for="p in pickList" :key="p.id" @click="goDetail(p.id)">
					<view class="pick-img">
						<image :src="p.img" mode="aspectFill" class="pick-img-inner"></image>
						<text class="pick-tag">{{ p.tag }}</text>
					</view>
					<view class="pick-info">
						<text class="pick-nm">{{ p.label }}</text>
						<text class="pick-sp">{{ p.spec }}</text>
						<view class="pick-btm">
							<text class="pick-pr">¥{{ p.price }}</text>
							<view class="pick-add" @click.stop="addCart(p.id)"><text class="add-icon">+</text></view>
						</view>
					</view>
				</view>
			</view>
		</view>
	</view>
</template>

<script>
	import { IMG, getProduct } from '@/utils/data.js'
	import { addToCart } from '@/utils/cart.js'

	export default {
		data() {
			return {
				IMG,
				picks: [
					{ id: 3, tag: '新鲜', label: '新疆哈密瓜', spec: '约4kg/个', price: '29.9', productId: 3 },
					{ id: 7, tag: '甜蜜', label: '芒果', spec: '2个', price: '16.5', productId: 7 },
					{ id: 4, tag: '特价', label: '脐橙', spec: '500g', price: '9.9', productId: 4 },
					{ id: 2, tag: '热卖', label: '红富士苹果', spec: '500g', price: '8.8', productId: 2 }
				]
			}
		},
		computed: {
			pickList() {
				return this.picks.map(p => ({
					...p,
					img: getProduct(p.productId).img
				}))
			}
		},
		onShow() {
			if (typeof this.getTabBar === 'function' && this.getTabBar()) {
				this.getTabBar().selected = 2
				this.getTabBar().refreshCartCount()
			}
		},
		methods: {
			enterLive() {
				uni.showToast({ title: '进入直播间（原型占位）', icon: 'none' })
			},
			goDetail(id) {
				uni.navigateTo({ url: '/pages/detail/detail?id=' + id })
			},
			addCart(id) {
				addToCart(id)
				if (typeof this.getTabBar === 'function' && this.getTabBar()) {
					this.getTabBar().refreshCartCount()
				}
				uni.showToast({ title: '已加入购物车', icon: 'none' })
			}
		}
	}
</script>

<style scoped>
	.page { min-height: 100vh; background: #f5f5f5; padding-bottom: 120rpx; }
	.live-hero { position: relative; height: 440rpx; overflow: hidden; margin: 24rpx; border-radius: 28rpx; }
	.live-hero-bg { width: 100%; height: 100%; }
	.live-hero-mask { position: absolute; inset: 0; background: linear-gradient(180deg, rgba(0,0,0,.1) 0%, rgba(0,0,0,.55) 100%); }
	.live-hero-top { position: absolute; top: 24rpx; left: 24rpx; right: 24rpx; display: flex; justify-content: space-between; align-items: center; }
	.live-hero-badge { background: rgba(242,78,62,.92); color: #fff; font-size: 22rpx; padding: 8rpx 20rpx; border-radius: 16rpx; display: flex; align-items: center; gap: 8rpx; font-weight: 600; }
	.live-dot { width: 12rpx; height: 12rpx; border-radius: 50%; background: #fff; }
	.live-hero-viewers { background: rgba(0,0,0,.5); color: #fff; font-size: 22rpx; padding: 8rpx 20rpx; border-radius: 16rpx; }
	.live-hero-mid { position: absolute; left: 28rpx; bottom: 104rpx; }
	.live-hero-en { font-size: 20rpx; color: rgba(255,255,255,.7); letter-spacing: 4rpx; font-weight: 600; display: block; }
	.live-hero-title { font-size: 40rpx; font-weight: 800; color: #fff; margin-top: 6rpx; display: block; text-shadow: 0 2rpx 8rpx rgba(0,0,0,.3); }
	.live-hero-btm { position: absolute; left: 28rpx; right: 28rpx; bottom: 24rpx; display: flex; align-items: center; justify-content: space-between; }
	.live-hero-cutting { font-size: 24rpx; color: rgba(255,255,255,.9); }
	.live-hero-enter { background: #fff; color: #222; font-size: 24rpx; font-weight: 700; padding: 14rpx 32rpx; border-radius: 32rpx; }
	.live-picks { padding: 0 24rpx; }
	.live-picks-hd { display: flex; align-items: center; justify-content: space-between; margin: 28rpx 0 20rpx; }
	.live-picks-title { font-size: 30rpx; font-weight: 800; color: #222; }
	.live-picks-more { font-size: 24rpx; color: #f24e3e; font-weight: 600; }
	.live-picks-grid { display: grid; grid-template-columns: 1fr 1fr; gap: 20rpx; }
	.pick-card { background: #fff; border-radius: 24rpx; overflow: hidden; }
	.pick-img { position: relative; aspect-ratio: 1; background: #fafafa; }
	.pick-img-inner { width: 100%; height: 100%; }
	.pick-tag { position: absolute; left: 0; top: 0; background: linear-gradient(135deg, #43c98a, #1f8a58); color: #fff; font-size: 20rpx; padding: 6rpx 20rpx; border-radius: 0 0 16rpx 0; font-weight: 600; }
	.pick-info { padding: 16rpx 20rpx 20rpx; }
	.pick-nm { font-size: 26rpx; font-weight: 600; color: #222; white-space: nowrap; overflow: hidden; text-overflow: ellipsis; display: block; }
	.pick-sp { font-size: 22rpx; color: #999; margin-top: 4rpx; display: block; }
	.pick-btm { display: flex; align-items: center; justify-content: space-between; margin-top: 12rpx; }
	.pick-pr { font-size: 30rpx; font-weight: 800; color: #f24e3e; }
	.pick-add { width: 52rpx; height: 52rpx; border-radius: 50%; background: #2e9e6b; color: #fff; display: flex; align-items: center; justify-content: center; }
	.add-icon { font-size: 32rpx; line-height: 1; font-weight: 300; }
</style>
