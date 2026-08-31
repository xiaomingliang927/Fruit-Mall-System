<template>
	<view class="page">
		<view class="hd">关于我们</view>
		<view class="banner">
			<view class="name">果小满</view>
			<view class="slogan">新鲜水果，从枝头到舌尖</view>
		</view>

		<view class="card">
			<view class="section">
				<view class="title">品牌介绍</view>
				<text class="p">果小满是一家专注于高品质时令水果的线上零售品牌，直连优质产区，全程冷链保鲜，让每一份水果都能保留枝头的新鲜滋味。</text>
			</view>
			<view class="section">
				<view class="title">联系我们</view>
				<text class="p">客服电话：400-888-1234\n客服微信：guoxiaoman_kefu\n服务时间：09:00 - 22:00</text>
			</view>
			<view class="section">
				<view class="title">版本信息</view>
				<text class="p">当前版本：1.0.0\n小程序 AppID：wx1234567890abcdef（示例）</text>
			</view>
		</view>

		<view class="copy">© 2026 果小满 fruit-mall. All rights reserved.</view>
	</view>
</template>

<script>
	export default {}
</script>

<style scoped>
	.page { min-height: 100vh; background: #f5f5f5; padding-bottom: 60rpx; }
	.hd { padding: 28rpx 32rpx; background: #fff; font-size: 34rpx; font-weight: 700; color: #222; }
	.banner { text-align: center; padding: 60rpx 32rpx; background: linear-gradient(135deg, #35b47e, #1f8a58); }
	.name { font-size: 52rpx; font-weight: 800; color: #fff; }
	.slogan { margin-top: 10rpx; font-size: 26rpx; color: #e6f4ec; }
	.card { background: #fff; margin: 20rpx 24rpx; border-radius: 24rpx; padding: 28rpx; }
	.section { margin-bottom: 36rpx; }
	.section:last-child { margin-bottom: 0; }
	.title { font-size: 30rpx; font-weight: 700; color: #222; margin-bottom: 12rpx; }
	.p { font-size: 26rpx; color: #555; line-height: 1.7; white-space: pre-line; }
	.copy { text-align: center; font-size: 22rpx; color: #a5aca1; margin-top: 32rpx; }
</style>
