<script>
	import { updateCartBadge, syncFromServer } from './utils/cart.js'
	import { api, setToken, getToken } from './api.js'

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
