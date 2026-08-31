<template>
	<view class="page">
		<view class="hd">邀请有礼</view>
		<view class="banner">
			<view class="title">邀请好友，各得 ¥20 券</view>
			<view class="sub">好友下单并完成首单，双方自动到账优惠券</view>
		</view>

		<view class="card">
			<text class="label">我的邀请码</text>
			<view class="code-wrap">
				<text class="code">{{ code }}</text>
				<text class="copy" @click="copyCode">复制</text>
			</view>
		</view>

		<view class="card">
			<view class="title2">邀请方式</view>
			<view class="step" v-for="(s,i) in steps" :key="i">
				<text class="idx">{{ i+1 }}</text>
				<text class="txt">{{ s }}</text>
			</view>
		</view>

		<button class="share" open-type="share">分享给好友</button>
	</view>
</template>

<script>
	export default {
		data() {
			return {
				code: 'FRUIT2026',
				steps: ['复制邀请码或点击分享给好友', '好友注册时填写你的邀请码', '好友完成首单，双方各得优惠券']
			}
		},
		onShareAppMessage() {
			return {
				title: '果上选新人礼，一起买新鲜水果',
				path: '/pages/index/index?inviteCode=' + this.code
			}
		},
		methods: {
			copyCode() {
				uni.setClipboardData({
					data: this.code,
					success: () => uni.showToast({ title: '邀请码已复制', icon: 'none' })
				})
			}
		}
	}
</script>

<style scoped>
	.page { min-height: 100vh; background: #f5f5f5; padding-bottom: 60rpx; }
	.hd { padding: 28rpx 32rpx; background: #fff; font-size: 34rpx; font-weight: 700; color: #222; }
	.banner { padding: 52rpx 32rpx; background: linear-gradient(135deg, #35b47e, #1f8a58); }
	.title { font-size: 40rpx; font-weight: 800; color: #fff; }
	.sub { margin-top: 10rpx; font-size: 24rpx; color: #e6f4ec; }
	.card { background: #fff; margin: 20rpx 24rpx; border-radius: 24rpx; padding: 28rpx; }
	.label { font-size: 26rpx; color: #888; }
	.code-wrap { display: flex; align-items: center; justify-content: space-between; margin-top: 20rpx; }
	.code { font-size: 44rpx; font-weight: 800; color: #222; letter-spacing: 4rpx; }
	.copy { background: #17704a; color: #fff; font-size: 26rpx; padding: 12rpx 30rpx; border-radius: 30rpx; }
	.title2 { font-size: 30rpx; font-weight: 700; color: #222; margin-bottom: 20rpx; }
	.step { display: flex; align-items: center; gap: 16rpx; padding: 14rpx 0; font-size: 26rpx; color: #555; }
	.idx { width: 36rpx; height: 36rpx; line-height: 36rpx; text-align: center; border-radius: 50%; background: #e6f4ec; color: #17704a; font-size: 22rpx; flex: none; }
	.share {
		margin: 40rpx 24rpx 0; background: linear-gradient(90deg, #ff6b5e, #f24e3e); color: #fff;
		font-size: 30rpx; font-weight: 600; border-radius: 44rpx; line-height: 84rpx;
	}
</style>
