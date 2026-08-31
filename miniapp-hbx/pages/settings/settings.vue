<template>
	<view class="page">
		<view class="hd">设置</view>
		<view class="card">
			<view class="row" @click="clearCache">
				<text class="label">清除缓存</text>
				<text class="arrow">›</text>
			</view>
			<view class="row">
				<text class="label">消息提醒</text>
				<switch :checked="notice" color="#17704a" @change="notice = $event.detail.value" />
			</view>
			<view class="row" @click="goHelp">
				<text class="label">帮助中心</text>
				<text class="arrow">›</text>
			</view>
			<view class="row" @click="goAbout">
				<text class="label">关于我们</text>
				<text class="arrow">›</text>
			</view>
			<view class="row">
				<text class="label">当前版本</text>
				<text class="value">1.0.0</text>
			</view>
		</view>

		<view v-if="isLogin" class="logout" @click="confirmLogout">退出登录</view>
	</view>
</template>

<script>
	import { setToken } from '../../api'
	import { clearLocalMirror, updateCartBadge } from '../../utils/cart.js'

	export default {
		data() {
			return { notice: true, isLogin: false }
		},
		onShow() {
			this.isLogin = !!uni.getStorageSync('token')
		},
		methods: {
			clearCache() {
				uni.showModal({
					title: '清除缓存',
					content: '清除本地缓存后需要重新登录，确定吗？',
					success: (r) => {
						if (!r.confirm) return
						uni.clearStorageSync()
						uni.showToast({ title: '已清除', icon: 'none' })
						this.isLogin = false
					}
				})
			},
			goHelp() {
				uni.navigateTo({ url: '/pages/help/help' })
			},
			goAbout() {
				uni.navigateTo({ url: '/pages/about/about' })
			},
			confirmLogout() {
				uni.showModal({
					title: '退出登录',
					content: '确定要退出当前账号吗？',
					confirmText: '退出',
					confirmColor: '#e54d42',
					success: (r) => { if (r.confirm) this.logout() }
				})
			},
			logout() {
				setToken('')
				clearLocalMirror()
				updateCartBadge()
				this.isLogin = false
				uni.showToast({ title: '已退出登录', icon: 'none' })
			}
		}
	}
</script>

<style scoped>
	.page { min-height: 100vh; background: #f5f5f5; padding-bottom: 60rpx; }
	.hd { padding: 28rpx 32rpx; background: #fff; font-size: 34rpx; font-weight: 700; color: #222; }
	.card { background: #fff; margin: 20rpx 24rpx; border-radius: 24rpx; padding: 12rpx 28rpx; }
	.row { display: flex; align-items: center; padding: 26rpx 0; border-bottom: 1rpx solid #f0f2f4; }
	.row:last-child { border-bottom: none; }
	.label { flex: 1; font-size: 28rpx; color: #333; }
	.value { font-size: 28rpx; color: #888; }
	.arrow { font-size: 30rpx; color: #999; margin-left: 12rpx; }
	.logout {
		margin: 40rpx 24rpx 0; text-align: center; line-height: 84rpx;
		background: #fff; color: #e54d42; font-size: 30rpx; font-weight: 600;
		border-radius: 20rpx; border: 2rpx solid #f3d6d2;
	}
</style>
