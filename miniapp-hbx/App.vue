<script>
	import { updateCartBadge } from './utils/cart.js'

export default {
		onLaunch: function() {
			updateCartBadge()
			console.log('App Launch')
		},
		onShow: function() {
			console.log('App Show')
		},
		onHide: function() {
			console.log('App Hide')
		}
	}
</script>

<style>
	/*每个页面公共css */
	page {
		background-color: #f7f6f1;
		font-family: -apple-system, BlinkMacSystemFont, "PingFang SC", "Helvetica Neue", "Microsoft YaHei", sans-serif;
		font-size: 28rpx;
		color: #23261f;
	}
	view, text, image {
		box-sizing: border-box;
	}
</style>
