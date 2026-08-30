<template>
	<view class="page">
		<!-- 顶部用户区 -->
		<view class="mine-header">
			<view class="mine-user">
				<view class="mine-avatar"><text class="avatar-icon">👤</text></view>
				<view class="mine-info">
					<text class="mine-name">果友_小满</text>
					<view class="mine-level"><text class="level-tag">普通会员</text></view>
				</view>
			</view>
			<view class="mine-actions">
				<view class="action-btn" @click="showToast('消息')"><text>💬</text></view>
				<view class="action-btn" @click="showToast('设置')"><text>⚙️</text></view>
			</view>
		</view>

		<!-- 我的订单 -->
		<view class="mine-card">
			<view class="card-hd">
				<text class="card-title">我的订单</text>
				<text class="card-more" @click="showToast('全部订单')">查看全部 ›</text>
			</view>
			<view class="order-row">
				<view class="order-it" v-for="o in orders" :key="o.lb" @click="showToast(o.lb)">
					<view class="order-ic">
						<text class="order-icon">{{ o.icon }}</text>
						<text class="order-dot" v-if="o.dot">{{ o.dot }}</text>
					</view>
					<text class="order-lb">{{ o.lb }}</text>
				</view>
			</view>
		</view>

		<!-- 常用工具 -->
		<view class="mine-card">
			<view class="card-hd">
				<text class="card-title">常用工具</text>
			</view>
			<view class="tool-grid">
				<view class="tool-it" v-for="t in tools" :key="t.lb" @click="showToast(t.lb)">
					<view class="tool-ic"><text>{{ t.icon }}</text></view>
					<text class="tool-lb">{{ t.lb }}</text>
				</view>
			</view>
		</view>

		<!-- 会员入口 -->
		<view class="mine-vip-entry" @click="showToast('开通会员')">
			<view class="vip-entry-l">
				<text class="vip-entry-icon">👑</text>
				<text class="vip-entry-t">开通会员享专属优惠</text>
			</view>
			<text class="vip-entry-arrow">›</text>
		</view>

		<view style="height: 40rpx;"></view>
	</view>
</template>

<script>
	export default {
		data() {
			return {
				orders: [
					{ icon: '💰', lb: '待付款', dot: '1' },
					{ icon: '📦', lb: '待发货', dot: '2' },
					{ icon: '🚚', lb: '待收货', dot: '' },
					{ icon: '⭐', lb: '待评价', dot: '' },
					{ icon: '🎧', lb: '退款/售后', dot: '' }
				],
				tools: [
					{ icon: '❤️', lb: '我的收藏' },
					{ icon: '👣', lb: '浏览足迹' },
					{ icon: '📍', lb: '收货地址' },
					{ icon: '🎫', lb: '优惠券' },
					{ icon: '🎧', lb: '联系客服' },
					{ icon: '❓', lb: '帮助中心' },
					{ icon: '⚙️', lb: '设置' },
					{ icon: 'ℹ️', lb: '关于我们' }
				]
			}
		},
		onShow() {
			if (typeof this.getTabBar === 'function' && this.getTabBar()) {
				this.getTabBar().selected = 4
				this.getTabBar().refreshCartCount()
			}
		},
		methods: {
			showToast(msg) {
				uni.showToast({ title: msg + '（原型占位）', icon: 'none' })
			}
		}
	}
</script>

<style scoped>
	.page {
		min-height: 100vh;
		background: #f5f5f5;
		padding-bottom: 120rpx;
	}
	.mine-header {
		background: linear-gradient(180deg, #e8f0e8 0%, #f5f5f5 100%);
		padding: 40rpx 32rpx 32rpx;
		display: flex;
		align-items: center;
		justify-content: space-between;
	}
	.mine-user {
		display: flex;
		align-items: center;
		gap: 24rpx;
	}
	.mine-avatar {
		width: 112rpx;
		height: 112rpx;
		border-radius: 50%;
		background: #fff;
		display: flex;
		align-items: center;
		justify-content: center;
		box-shadow: 0 4rpx 16rpx rgba(0, 0, 0, 0.06);
		flex-shrink: 0;
	}
	.avatar-icon {
		font-size: 56rpx;
	}
	.mine-info {
		min-width: 0;
	}
	.mine-name {
		font-size: 34rpx;
		font-weight: 700;
		color: #222;
		display: block;
	}
	.mine-level {
		margin-top: 8rpx;
	}
	.level-tag {
		font-size: 20rpx;
		background: #f0f0f0;
		color: #888;
		padding: 4rpx 16rpx;
		border-radius: 16rpx;
	}
	.mine-actions {
		display: flex;
		gap: 16rpx;
	}
	.action-btn {
		width: 68rpx;
		height: 68rpx;
		background: rgba(255, 255, 255, 0.8);
		border-radius: 50%;
		display: flex;
		align-items: center;
		justify-content: center;
		font-size: 32rpx;
	}
	.mine-card {
		background: #fff;
		border-radius: 28rpx;
		margin: 20rpx 24rpx;
		padding: 28rpx;
	}
	.card-hd {
		display: flex;
		align-items: center;
		justify-content: space-between;
		margin-bottom: 28rpx;
	}
	.card-title {
		font-size: 30rpx;
		font-weight: 700;
		color: #222;
	}
	.card-more {
		font-size: 24rpx;
		color: #999;
	}
	.order-row {
		display: grid;
		grid-template-columns: repeat(5, 1fr);
		gap: 8rpx;
	}
	.order-it {
		display: flex;
		flex-direction: column;
		align-items: center;
		gap: 12rpx;
		padding: 8rpx 0;
	}
	.order-ic {
		position: relative;
		width: 72rpx;
		height: 72rpx;
		display: flex;
		align-items: center;
		justify-content: center;
		font-size: 40rpx;
	}
	.order-dot {
		position: absolute;
		top: 0;
		right: 4rpx;
		background: #f24e3e;
		color: #fff;
		font-size: 18rpx;
		min-width: 28rpx;
		height: 28rpx;
		border-radius: 14rpx;
		display: flex;
		align-items: center;
		justify-content: center;
		padding: 0 6rpx;
		font-weight: 600;
	}
	.order-lb {
		font-size: 22rpx;
		color: #555;
	}
	.tool-grid {
		display: grid;
		grid-template-columns: repeat(4, 1fr);
		gap: 16rpx;
	}
	.tool-it {
		display: flex;
		flex-direction: column;
		align-items: center;
		gap: 12rpx;
		padding: 16rpx 0;
	}
	.tool-ic {
		width: 64rpx;
		height: 64rpx;
		display: flex;
		align-items: center;
		justify-content: center;
		font-size: 36rpx;
	}
	.tool-lb {
		font-size: 22rpx;
		color: #555;
	}
	.mine-vip-entry {
		background: linear-gradient(135deg, #3d2e1f, #2a1f15);
		border-radius: 28rpx;
		margin: 20rpx 24rpx;
		padding: 28rpx 32rpx;
		display: flex;
		align-items: center;
		justify-content: space-between;
	}
	.vip-entry-l {
		display: flex;
		align-items: center;
		gap: 16rpx;
	}
	.vip-entry-icon {
		font-size: 36rpx;
	}
	.vip-entry-t {
		font-size: 26rpx;
		color: #f5e6c8;
		font-weight: 600;
	}
	.vip-entry-arrow {
		color: #f5e6c8;
		font-size: 36rpx;
	}
</style>
