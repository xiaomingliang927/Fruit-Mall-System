<template>
	<view class="page">
		<view class="hd">消息中心</view>
		<view class="tabs">
			<view class="tab" :class="{ on: tab === 'order' }" @click="tab = 'order'">订单</view>
			<view class="tab" :class="{ on: tab === 'coupon' }" @click="tab = 'coupon'">优惠</view>
			<view class="tab" :class="{ on: tab === 'system' }" @click="tab = 'system'">系统</view>
		</view>
		<view class="empty">
			<view class="empty-icon">✉</view>
			<view>暂无{{ tabMap[tab] }}</view>
			<view class="empty-sub">重要提醒会第一时间推送到这里</view>
		</view>
	</view>
</template>

<script>
	export default {
		data() {
			return {
				tab: 'order',
				tabMap: { order: '订单消息', coupon: '优惠消息', system: '系统通知' }
			}
		},
		onShow() {
			if (!uni.getStorageSync('token')) {
				uni.navigateTo({ url: '/pages/login/login' })
			}
		}
	}
</script>

<style scoped>
	.page { min-height: 100vh; background: #f5f5f5; }
	.hd { padding: 28rpx 32rpx; background: #fff; font-size: 34rpx; font-weight: 700; color: #222; }
	.tabs { display: flex; background: #fff; border-bottom: 1rpx solid #f0f2f4; }
	.tab { flex: 1; text-align: center; padding: 22rpx 0; font-size: 28rpx; color: #6b7280; border-bottom: 4rpx solid transparent; }
	.tab.on { color: #17704a; font-weight: 700; border-bottom-color: #17704a; }
	.empty { text-align: center; padding-top: 180rpx; color: #a5aca1; }
	.empty-icon { font-size: 80rpx; color: #d8ddd4; margin-bottom: 16rpx; }
	.empty-sub { font-size: 24rpx; margin-top: 12rpx; }
</style>
