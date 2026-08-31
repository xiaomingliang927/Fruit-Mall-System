<template>
	<view class="page">
		<!-- 账号 -->
		<view class="card">
			<view class="grp-t">账号</view>
			<view class="row" @click="goProfile">
				<text class="row-k">个人资料</text>
				<text class="row-v">{{ isLogin ? (nickname || '未设置昵称') : '未登录' }}</text>
				<text class="arrow">›</text>
			</view>
			<view class="row" @click="goAddress">
				<text class="row-k">收货地址</text>
				<text class="arrow">›</text>
			</view>
		</view>

		<!-- 通用 -->
		<view class="card">
			<view class="grp-t">通用</view>
			<view class="row">
				<view class="row-c">
					<text class="row-k">订单消息提醒</text>
					<text class="row-d">发货和售后进度变化时提醒你</text>
				</view>
				<switch :checked="notice" color="#17704a" @change="onNoticeChange" />
			</view>
			<view class="row" @click="clearCache">
				<view class="row-c">
					<text class="row-k">清除缓存</text>
					<text class="row-d">只清购物车等本地暂存，不会退出登录</text>
				</view>
				<text class="row-v">{{ cacheSize }}</text>
				<text class="arrow">›</text>
			</view>
		</view>

		<!-- 关于 -->
		<view class="card">
			<view class="grp-t">关于</view>
			<view class="row" @click="goHelp">
				<text class="row-k">帮助中心</text>
				<text class="arrow">›</text>
			</view>
			<view class="row" @click="goService">
				<text class="row-k">联系客服</text>
				<text class="row-v">09:00 - 22:00</text>
				<text class="arrow">›</text>
			</view>
			<view class="row" @click="goAbout">
				<text class="row-k">关于果上选</text>
				<text class="arrow">›</text>
			</view>
			<view class="row" @click="copyVersion">
				<view class="row-c">
					<text class="row-k">当前版本</text>
					<text class="row-d">点一下复制，反馈问题时发给客服</text>
				</view>
				<text class="row-v">{{ version }}</text>
			</view>
		</view>

		<!-- 退出 -->
		<view v-if="isLogin" class="logout" @click="confirmLogout">退出登录</view>
		<view v-else class="login-tip" @click="goLogin">还没登录，点这里登录 ›</view>

		<view style="height: 60rpx;"></view>
	</view>
</template>

<script>
	import { api, setToken } from '../../api'
	import { clearLocalMirror, updateCartBadge } from '../../utils/cart.js'

	const NOTICE_KEY = 'fruit_mall_notice'
	const VERSION = '1.0.0'

	export default {
		data() {
			return {
				notice: true,
				isLogin: false,
				nickname: '',
				cacheSize: '',
				version: VERSION
			}
		},
		async onShow() {
			this.isLogin = !!uni.getStorageSync('token')
			// 开关状态持久化：没存过的时候默认开
			const saved = uni.getStorageSync(NOTICE_KEY)
			this.notice = saved === '' || saved === null || saved === undefined ? true : !!saved
			this.refreshCacheSize()
			await this.loadMe()
		},
		methods: {
			/** 昵称从接口拿（不要读缓存 key，项目里没人写入过，读了永远是空） */
			async loadMe() {
				if (!this.isLogin) {
					this.nickname = ''
					return
				}
				try {
					const me = await api.get('/api/v1/users/me')
					this.nickname = me.nickname || ''
				} catch (e) {
					this.nickname = ''
				}
			},
			/** 读真实占用，getStorageInfoSync 的 currentSize 单位是 KB */
			refreshCacheSize() {
				try {
					const info = uni.getStorageInfoSync()
					const kb = info.currentSize || 0
					this.cacheSize = kb < 1024 ? kb + ' KB' : (kb / 1024).toFixed(1) + ' MB'
				} catch (e) {
					this.cacheSize = ''
				}
			},
			onNoticeChange(e) {
				this.notice = e.detail.value
				uni.setStorageSync(NOTICE_KEY, this.notice)
				uni.showToast({ title: this.notice ? '已开启提醒' : '已关闭提醒', icon: 'none' })
			},
			/**
			 * 只清业务缓存，保留 token。
			 * 原来用 uni.clearStorageSync() 全清，会把登录态一起清掉，
			 * 用户清个缓存就被迫重新登录，体验很差。
			 */
			clearCache() {
				uni.showModal({
					title: '清除缓存',
					content: '会清掉购物车等本地暂存，登录状态保留。已同步到服务器的商品不受影响；只在本地暂存、还没同步的商品会被清掉。',
					confirmText: '清除',
					success: (r) => {
						if (!r.confirm) return
						clearLocalMirror()
						updateCartBadge()
						uni.showToast({ title: '缓存已清除', icon: 'none' })
						this.refreshCacheSize()
					}
				})
			},
			copyVersion() {
				const sys = uni.getSystemInfoSync()
				const txt = '果上选 v' + VERSION + ' / ' + (sys.platform || '') + ' / 微信 ' + (sys.version || '')
				uni.setClipboardData({
					data: txt,
					success: () => uni.showToast({ title: '版本信息已复制', icon: 'none' })
				})
			},
			goProfile() {
				if (!this.isLogin) return this.goLogin()
				uni.navigateTo({ url: '/pages/profile/profile' })
			},
			goAddress() {
				if (!this.isLogin) return this.goLogin()
				uni.navigateTo({ url: '/pages/address/address' })
			},
			goHelp() {
				uni.navigateTo({ url: '/pages/help/help' })
			},
			goService() {
				uni.navigateTo({ url: '/pages/service/service' })
			},
			goAbout() {
				uni.navigateTo({ url: '/pages/about/about' })
			},
			goLogin() {
				uni.navigateTo({ url: '/pages/login/login' })
			},
			confirmLogout() {
				uni.showModal({
					title: '退出登录',
					content: '退出后会清空本地购物车暂存。已同步到服务器的商品，重新登录后还在；只在本地暂存、还没同步的商品会被清掉。',
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
				this.nickname = ''
				this.refreshCacheSize()
				uni.showToast({ title: '已退出登录', icon: 'none' })
			}
		}
	}
</script>

<style scoped>
	.page { min-height: 100vh; background: #f5f5f5; padding-top: 8rpx; }

	.card { background: #fff; border-radius: 28rpx; margin: 20rpx 24rpx; padding: 8rpx 28rpx 4rpx; }
	.grp-t {
		font-size: 23rpx; color: #a5aca1; font-weight: 600;
		padding: 22rpx 0 6rpx; letter-spacing: 1rpx;
	}
	.row {
		display: flex; align-items: center; gap: 16rpx;
		padding: 26rpx 0; border-bottom: 1rpx solid #f7f9f5;
	}
	.row:last-child { border-bottom: none; }
	.row:active { background: #fcfdfc; }
	.row-c { flex: 1; min-width: 0; }
	.row-k { flex: 1; font-size: 28rpx; color: #222; }
	.row-c .row-k { display: block; flex: none; }
	.row-d { display: block; margin-top: 6rpx; font-size: 22rpx; color: #a5aca1; line-height: 1.5; }
	.row-v { flex: none; font-size: 25rpx; color: #a5aca1; max-width: 300rpx; overflow: hidden; }
	.arrow { flex: none; font-size: 30rpx; color: #c2c8bf; }

	.logout {
		margin: 36rpx 24rpx 0; text-align: center; line-height: 88rpx; height: 88rpx;
		background: #fff; color: #e54d42; font-size: 30rpx; font-weight: 600; border-radius: 24rpx;
	}
	.logout:active { background: #fff5f4; }
	.login-tip {
		margin: 36rpx 24rpx 0; text-align: center; line-height: 88rpx; height: 88rpx;
		background: #fff; color: #17704a; font-size: 28rpx; font-weight: 600; border-radius: 24rpx;
	}
</style>
