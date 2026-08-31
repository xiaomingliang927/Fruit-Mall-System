<template>
	<view class="page">
		<!-- 顶部用户区 -->
		<view class="mine-header">
			<view class="mine-user" @click="me.nickname ? showToast('个人资料') : goLogin()">
				<view class="mine-avatar">
					<text v-if="me.nickname" class="avatar-text">{{ me.nickname[0] }}</text>
					<image v-else src="/static/icon/svg/person.svg" mode="aspectFit" class="avatar-icon"></image>
				</view>
				<view class="mine-info">
					<text class="mine-name">{{ me.nickname || '点击登录' }}</text>
					<view class="mine-level"><text class="level-tag">{{ levelText(me.level) }}</text></view>
				</view>
			</view>
			<view class="mine-actions">
				<view class="action-btn" @click="showToast('消息')">
					<image src="/static/icon/svg/chat.svg" mode="aspectFit" class="action-img"></image>
				</view>
				<view class="action-btn" @click="showToast('设置')">
					<image src="/static/icon/svg/settings.svg" mode="aspectFit" class="action-img"></image>
				</view>
			</view>
		</view>

		<!-- 会员入口 -->
		<view class="mine-vip-entry" @click="showToast('开通会员')">
			<view class="vip-entry-l">
				<image src="/static/icon/svg/crown.svg" mode="aspectFit" class="vip-icon"></image>
				<text class="vip-entry-t">开通会员享专属优惠</text>
			</view>
			<text class="vip-entry-arrow">›</text>
		</view>

		<!-- 我的订单 -->
		<view class="mine-card">
			<view class="card-hd">
				<text class="card-title">我的订单</text>
				<text class="card-more" @click="goOrders(null)">查看全部 ›</text>
			</view>
			<view class="order-row">
				<view class="order-it" v-for="o in orders" :key="o.lb" @click="goOrders(o.status)">
					<view class="order-ic"><image :src="o.img" mode="aspectFit" class="order-img"></image></view>
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
				<view class="tool-it" v-for="t in tools" :key="t.lb" @click="t.tap ? t.tap() : showToast(t.lb)">
					<view class="tool-ic"><image :src="t.img" mode="aspectFit" class="tool-img"></image></view>
					<text class="tool-lb">{{ t.lb }}</text>
				</view>
			</view>
		</view>

		<!-- 退出登录 -->
		<view v-if="me.nickname" class="mine-card logout-card">
			<view class="logout-btn" @click="confirmLogout">退出登录</view>
		</view>

		<view style="height: 40rpx;"></view>
	</view>
</template>

<script>
	import { api, setToken } from '../../api'
	import { clearLocalMirror, updateCartBadge } from '../../utils/cart.js'
	export default {
		data() {
			return {
				me: {},
				orders: [
					{ img: '/static/icon/svg/wallet.svg', lb: '待付款', status: 10 },
					{ img: '/static/icon/svg/box.svg', lb: '待发货', status: 20 },
					{ img: '/static/icon/svg/truck.svg', lb: '待收货', status: 30 },
					{ img: '/static/icon/svg/star.svg', lb: '待评价', status: 40 },
					{ img: '/static/icon/svg/headset.svg', lb: '退款/售后', status: null }
				],
			tools: [
				{ img: '/static/icon/svg/heart.svg', lb: '我的收藏', tap: goFavorite },
				{ img: '/static/icon/svg/map-pin.svg', lb: '收货地址', tap: goAddress },
				{ img: '/static/icon/svg/ticket.svg', lb: '优惠券', tap: goCoupons },
				{ img: '/static/icon/svg/gift.svg', lb: '邀请有礼' },
				{ img: '/static/icon/svg/headset.svg', lb: '联系客服' },
				{ img: '/static/icon/svg/help.svg', lb: '帮助中心' },
				{ img: '/static/icon/svg/settings.svg', lb: '设置' },
				{ img: '/static/icon/svg/person.svg', lb: '关于我们' }
			]
			}
		},
		onShow() {
			this.load()
		},
		methods: {
			async load() {
				if (!uni.getStorageSync('token')) {
					this.me = {}
					return
				}
				try {
					this.me = await api.get('/api/v1/users/me')
				} catch (e) { /* 未登录 */ }
			},
			levelText(level) {
				return { 1: '普通会员', 2: '白银会员', 3: '黄金会员', 4: '钻石会员' }[level] || '普通会员'
			},
			goCoupons() {
				uni.navigateTo({ url: '/pages/coupon/coupon' })
			},
			goFavorite() {
				if (!this.me.nickname) return this.goLogin()
				uni.navigateTo({ url: '/pages/favorite/favorite' })
			},
			goAddress() {
				if (!this.me.nickname) return this.goLogin()
				uni.navigateTo({ url: '/pages/address/address' })
			},
			goLogin() {
				uni.navigateTo({ url: '/pages/login/login' })
			},
			toast(msg) {
				uni.showToast({ title: msg + '（原型占位）', icon: 'none' })
			},
			showToast(msg) {
				uni.showToast({ title: msg, icon: 'none' })
			},
			goOrders(status) {
				if (!this.me.nickname) return this.goLogin()
				uni.navigateTo({ url: '/pages/orders/orders' + (status ? '?status=' + status : '') })
			},
			logout() {
				setToken('')
				clearLocalMirror()
				updateCartBadge()
				this.me = {}
				uni.showToast({ title: '已退出登录', icon: 'none' })
			},
			confirmLogout() {
				uni.showModal({
					title: '退出登录',
					content: '确定要退出当前账号吗？',
					confirmText: '退出',
					confirmColor: '#e54d42',
					success: (r) => { if (r.confirm) this.logout() }
				})
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
		background: #17704a;
		display: flex;
		align-items: center;
		justify-content: center;
		box-shadow: 0 4rpx 16rpx rgba(0, 0, 0, 0.06);
		flex-shrink: 0;
	}
	.avatar-text {
		font-size: 48rpx;
		color: #fff;
		font-weight: 700;
	}
	.avatar-icon {
		width: 56rpx;
		height: 56rpx;
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
		background: #e6f4ec;
		color: #17704a;
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
		background: #fff;
		border-radius: 50%;
		display: flex;
		align-items: center;
		justify-content: center;
	}
	.action-img {
		width: 34rpx;
		height: 34rpx;
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
		width: 72rpx;
		height: 72rpx;
		display: flex;
		align-items: center;
		justify-content: center;
		background: #f7f9f5;
		border-radius: 20rpx;
	}
	.order-img {
		width: 44rpx;
		height: 44rpx;
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
		width: 72rpx;
		height: 72rpx;
		display: flex;
		align-items: center;
		justify-content: center;
		background: #f7f9f5;
		border-radius: 20rpx;
	}
	.tool-img {
		width: 40rpx;
		height: 40rpx;
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
	.vip-icon {
		width: 48rpx;
		height: 48rpx;
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
	.logout-card {
		padding: 24rpx;
	}
	.logout-btn {
		text-align: center;
		line-height: 84rpx;
		height: 84rpx;
		border-radius: 20rpx;
		background: #fff;
		color: #e54d42;
		font-size: 30rpx;
		font-weight: 600;
		border: 2rpx solid #f3d6d2;
	}
	.logout-btn:active {
		background: #fff5f4;
	}
</style>
