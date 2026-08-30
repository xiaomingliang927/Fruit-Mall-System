<template>
	<view class="tab-bar">
		<view class="tab-bar-border"></view>
		<view
			class="tab-bar-item"
			:class="{ mid: index === 2 }"
			v-for="(item, index) in list"
			:key="index"
			@click="switchTab(item, index)"
		>
			<view class="tab-icon-wrap" v-if="index !== 2">
				<image class="tab-icon" :src="selected === index ? item.selectedIconPath : item.iconPath" mode="aspectFit"></image>
				<view class="tab-badge" v-if="index === 3 && cartCount > 0">{{ cartCount > 99 ? '99+' : cartCount }}</view>
			</view>
			<view class="tab-mid-icon" v-else>
				<text class="mid-icon-text">📺</text>
			</view>
			<text class="tab-text" :class="{ active: selected === index, midText: index === 2 }">{{ item.text }}</text>
		</view>
	</view>
</template>

<script>
	import { getCartCount } from '@/utils/cart.js'

	export default {
		data() {
			return {
				selected: 0,
				cartCount: 0,
				list: [
					{ pagePath: '/pages/index/index', text: '首页', iconPath: '/static/tab/home.png', selectedIconPath: '/static/tab/home-on.png' },
					{ pagePath: '/pages/category/category', text: '分类', iconPath: '/static/tab/category.png', selectedIconPath: '/static/tab/category-on.png' },
					{ pagePath: '/pages/live/live', text: '直播', iconPath: '', selectedIconPath: '' },
					{ pagePath: '/pages/cart/cart', text: '购物车', iconPath: '/static/tab/cart.png', selectedIconPath: '/static/tab/cart-on.png' },
					{ pagePath: '/pages/mine/mine', text: '我的', iconPath: '/static/tab/mine.png', selectedIconPath: '/static/tab/mine-on.png' }
				]
			}
		},
		created() {
			this.refreshCartCount()
		},
		methods: {
			switchTab(item, index) {
				uni.switchTab({ url: item.pagePath })
				this.selected = index
			},
			refreshCartCount() {
				this.cartCount = getCartCount()
			}
		}
	}
</script>

<style>
	.tab-bar {
		position: fixed;
		bottom: 0;
		left: 0;
		right: 0;
		height: 100rpx;
		background: #ffffff;
		display: flex;
		padding-bottom: env(safe-area-inset-bottom);
		box-shadow: 0 -2rpx 10rpx rgba(0, 0, 0, 0.04);
		z-index: 999;
	}

	.tab-bar-border {
		background-color: #f0f0f0;
		position: absolute;
		left: 0;
		top: 0;
		width: 100%;
		height: 1rpx;
	}

	.tab-bar-item {
		flex: 1;
		display: flex;
		flex-direction: column;
		align-items: center;
		justify-content: center;
		height: 100%;
		position: relative;
	}

	.tab-bar-item.mid {
		justify-content: flex-start;
		padding-top: 10rpx;
	}

	.tab-icon-wrap {
		position: relative;
		width: 48rpx;
		height: 48rpx;
		display: flex;
		align-items: center;
		justify-content: center;
	}

	.tab-icon {
		width: 44rpx;
		height: 44rpx;
	}

	.tab-mid-icon {
		width: 96rpx;
		height: 96rpx;
		border-radius: 50%;
		background: linear-gradient(135deg, #43c98a, #1f8a58);
		display: flex;
		align-items: center;
		justify-content: center;
		box-shadow: 0 8rpx 24rpx rgba(46, 158, 107, 0.4);
		margin-top: -36rpx;
		border: 6rpx solid #fff;
	}

	.mid-icon-text {
		font-size: 44rpx;
	}

	.tab-text {
		font-size: 20rpx;
		color: #aaa;
		margin-top: 4rpx;
	}

	.tab-text.active {
		color: #17704a;
		font-weight: 600;
	}

	.tab-text.midText {
		color: #17704a;
		font-weight: 600;
		margin-top: 2rpx;
	}

	.tab-badge {
		position: absolute;
		top: -8rpx;
		right: -12rpx;
		background: #f24e3e;
		color: #fff;
		font-size: 18rpx;
		min-width: 30rpx;
		height: 30rpx;
		border-radius: 15rpx;
		display: flex;
		align-items: center;
		justify-content: center;
		padding: 0 6rpx;
		border: 2rpx solid #fff;
		line-height: 1;
	}
</style>
