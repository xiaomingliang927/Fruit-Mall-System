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
			<view class="row" @click="openApiPanel">
				<view class="row-c">
					<text class="row-k">API 地址</text>
					<text class="row-d">真机预览时改为电脑局域网 IP（手机和电脑需同一 WiFi）</text>
				</view>
				<text class="row-v" style="max-width: 280rpx;">{{ apiCurrent }}</text>
				<text class="arrow">›</text>
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

		<!-- API 地址面板 -->
		<view v-if="showApiPanel" class="mask" @click="closeApiPanel">
			<view class="modal" @click.stop="">
				<view class="modal-t">API 地址</view>
				<view class="modal-d">
					开发工具里默认 <text style="color:#17704a">http://localhost:8080</text> 即可；真机预览请把 <text style="color:#17704a">localhost</text> 换成电脑的局域网 IP（如 <text style="color:#17704a">http://192.168.1.5:8080</text>），手机和电脑必须在同一 WiFi。
				</view>
				<input v-model="apiDraft" class="modal-input" placeholder="http://192.168.1.5:8080" :maxlength="64" />
				<view v-if="apiStatus" class="modal-status" :style="{ color: apiStatus.ok ? '#0a7c2f' : '#e54d42' }">{{ apiStatus.text }}</view>
				<view class="modal-btns">
					<view class="btn ghost" @click="resetApi">恢复默认</view>
					<view class="btn ghost" @click="testApi" :class="{ disabled: apiTesting }">{{ apiTesting ? '测试中…' : '测试连接' }}</view>
					<view class="btn primary" @click="saveApi">保存</view>
				</view>
			</view>
		</view>
	</view>
</template>

<script>
	import { api, setToken, getBaseUrl, setBaseUrl } from '../../api'
	import { clearLocalMirror, updateCartBadge } from '../../utils/cart.js'

	const NOTICE_KEY = 'fruit_mall_notice'
	const VERSION = '1.0.0'
	const API_KEY = 'fruit_mall_base_url'
	const DEFAULT_API = 'http://localhost:8080'

	export default {
		data() {
			return {
				notice: true,
				isLogin: false,
				nickname: '',
				cacheSize: '',
				version: VERSION,
				showApiPanel: false,
				apiDraft: '',
				apiCurrent: '',
				apiTesting: false,
				apiStatus: null
			}
		},
		async onShow() {
			this.isLogin = !!uni.getStorageSync('token')
			// 开关状态持久化：没存过的时候默认开
			const saved = uni.getStorageSync(NOTICE_KEY)
			this.notice = saved === '' || saved === null || saved === undefined ? true : !!saved
			this.refreshCacheSize()
			this.refreshApiDisplay()
			await this.loadMe()
		},
		methods: {
			refreshApiDisplay() {
				const v = getBaseUrl() || DEFAULT_API
				this.apiCurrent = v === DEFAULT_API ? '默认 (' + DEFAULT_API + ')' : v
			},
			openApiPanel() {
				const v = getBaseUrl() || DEFAULT_API
				this.apiDraft = v === DEFAULT_API ? '' : v
				this.apiStatus = null
				this.showApiPanel = true
			},
			closeApiPanel() {
				this.showApiPanel = false
				this.apiTesting = false
			},
			saveApi() {
				const v = this.apiDraft.trim()
				if (v && !/^https?:\/\//i.test(v)) {
					this.apiStatus = { ok: false, text: '地址必须以 http:// 或 https:// 开头' }
					return
				}
				setBaseUrl(v)
				this.apiStatus = { ok: true, text: '已保存，下次请求生效' }
				this.refreshApiDisplay()
				setTimeout(() => { this.showApiPanel = false }, 700)
			},
			resetApi() {
				setBaseUrl('')
				this.apiDraft = ''
				this.apiStatus = { ok: true, text: '已恢复默认' + (DEFAULT_API ? ' ' + DEFAULT_API : '') }
				this.refreshApiDisplay()
				setTimeout(() => { this.showApiPanel = false }, 700)
			},
			testApi() {
				if (this.apiTesting) return
				const url = (this.apiDraft.trim() || getBaseUrl() || DEFAULT_API)
				if (!/^https?:\/\//i.test(url)) {
					this.apiStatus = { ok: false, text: '地址必须以 http:// 或 https:// 开头' }
					return
				}
				this.apiTesting = true
				this.apiStatus = null
				// 不走全局基地址：临时发一次请求验证
				uni.request({
					url: url.replace(/\/$/, '') + '/api/v1/products/hot?size=1',
					method: 'GET',
					timeout: 4000,
					success: (r) => {
						const body = r.data || {}
						this.apiStatus = body.code === 0
							? { ok: true, text: '连接成功（HTTP ' + r.statusCode + '）' }
							: { ok: false, text: '后端响应异常：HTTP ' + r.statusCode + ' / code=' + body.code }
					},
					fail: (e) => {
						const m = (e && e.errMsg) || '网络不可达'
						this.apiStatus = { ok: false, text: '连接失败：' + m + '（检查 IP、WiFi 和后端进程）' }
					},
					complete: () => { this.apiTesting = false }
				})
			},
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

	/* API 地址面板 */
	.mask {
		position: fixed; inset: 0; background: rgba(0,0,0,0.45);
		display: flex; align-items: center; justify-content: center; z-index: 99;
	}
	.modal {
		width: 640rpx; background: #fff; border-radius: 24rpx; padding: 36rpx 40rpx 32rpx;
	}
	.modal-t { font-size: 32rpx; font-weight: 600; color: #222; }
	.modal-d { margin-top: 16rpx; font-size: 24rpx; color: #5e6760; line-height: 1.6; }
	.modal-input {
		margin-top: 22rpx; padding: 18rpx 22rpx; border: 1rpx solid #dde4dd; border-radius: 16rpx;
		font-size: 28rpx; color: #222; background: #f6f8f5;
	}
	.modal-status { margin-top: 16rpx; font-size: 25rpx; line-height: 1.5; }
	.modal-btns { display: flex; gap: 18rpx; margin-top: 30rpx; }
	.btn {
		flex: 1; text-align: center; line-height: 72rpx; height: 72rpx; border-radius: 20rpx;
		font-size: 28rpx; font-weight: 600;
	}
	.btn.ghost { background: #f1f4ef; color: #5e6760; }
	.btn.primary { background: #17704a; color: #fff; }
	.btn.disabled { opacity: 0.6; }
</style>
