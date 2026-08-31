<template>
	<view class="page">
		<view class="hd">联系客服</view>
		<view class="card">
			<view class="row">
				<text class="label">客服电话</text>
				<text class="value" @click="call">400-888-1234</text>
				<text class="btn" @click="call">拨打</text>
			</view>
			<view class="row">
				<text class="label">客服微信</text>
				<text class="value">guoxiaoman_kefu</text>
				<text class="btn" @click="copy">复制</text>
			</view>
			<view class="row">
				<text class="label">服务时间</text>
				<text class="value">09:00 - 22:00</text>
			</view>
		</view>

		<view class="card">
			<view class="title">自助服务</view>
			<view class="link" @click="goHelp">常见问题 ›</view>
			<view class="link" @click="goOrders">我的订单 ›</view>
		</view>
	</view>
</template>

<script>
	export default {
		methods: {
			call() {
				uni.makePhoneCall({ phoneNumber: '400-888-1234' })
			},
			copy() {
				uni.setClipboardData({
					data: 'guoxiaoman_kefu',
					success: () => uni.showToast({ title: '已复制客服微信', icon: 'none' })
				})
			},
			goHelp() {
				uni.navigateTo({ url: '/pages/help/help' })
			},
			goOrders() {
				if (!uni.getStorageSync('token')) {
					return uni.navigateTo({ url: '/pages/login/login' })
				}
				uni.navigateTo({ url: '/pages/orders/orders' })
			}
		}
	}
</script>

<style scoped>
	.page { min-height: 100vh; background: #f5f5f5; }
	.hd { padding: 28rpx 32rpx; background: #fff; font-size: 34rpx; font-weight: 700; color: #222; }
	.card { background: #fff; margin: 20rpx 24rpx; border-radius: 24rpx; padding: 12rpx 28rpx; }
	.row { display: flex; align-items: center; padding: 24rpx 0; border-bottom: 1rpx solid #f0f2f4; }
	.row:last-child { border-bottom: none; }
	.label { width: 180rpx; flex: none; font-size: 28rpx; color: #555; }
	.value { flex: 1; text-align: right; font-size: 28rpx; color: #17704a; }
	.btn { margin-left: 16rpx; background: #17704a; color: #fff; font-size: 22rpx; padding: 8rpx 20rpx; border-radius: 20rpx; }
	.title { font-size: 30rpx; font-weight: 700; color: #222; margin-bottom: 8rpx; }
	.link { padding: 22rpx 0; font-size: 28rpx; color: #333; border-bottom: 1rpx solid #f0f2f4; }
	.link:last-child { border-bottom: none; }
</style>
