<script>
	import { updateCartBadge, syncFromServer } from './utils/cart.js'
	import { api, setToken, getToken } from './api.js'

	/**
	 * 微信开发者工具自动注入的 runtime（WAServiceMainContext.js）会在热更新/
	 * vConsole 通道关闭时偶发 closeSocket(code:1006)，1006 不在 RFC 6455 允许范围
	 * （要求 1000 或 3000-4999）。该错误只出现在 dev 工具与模拟器通信链路上，
	 * 真机生产模式不触发，业务代码无 WebSocket 调用，可安全忽略。
	 *
	 * 这里覆盖一下 wx 的 onError，让它在控制台只输出一行说明就静默，
	 * 避免刷屏吓到用户。
	 */
	const origOnError = uni.onError
	uni.onError = function (callback) {
		return origOnError.call(this, (err) => {
			const msg = String(err && err.message || err || '')
			if (msg.includes('closeSocket') && msg.includes('1006')) {
				console.info('[果上选] 微信开发者工具内部 devtools 通道异常，已静默（不影响数据）')
				return
			}
			callback(err)
		})
	}

	export default {
		onLaunch: async function() {
			updateCartBadge()
			if (getToken()) {
				// 已有 token：静默拉取服务端购物车（跨端同步）
				await syncFromServer()
				updateCartBadge()
				return
			}
			// 自动登录：wx.login 静默获取 code 换 token（用户无感知）
			uni.login({
				provider: 'weixin',
				success: async ({ code }) => {
					try {
						const data = await api.post('/api/v1/auth/wx-login', { code })
						setToken(data.token)
						await syncFromServer()
						updateCartBadge()
					} catch (e) { /* 后端未启动时静默 */ }
				}
			})
		}
	}
</script>

<style>
	page {
		background-color: #f5f5f5;
		font-family: -apple-system, BlinkMacSystemFont, 'PingFang SC', 'Helvetica Neue', 'Microsoft YaHei', sans-serif;
		font-size: 28rpx;
		color: #23261f;
	}
	view, text, image {
		box-sizing: border-box;
	}
</style>
