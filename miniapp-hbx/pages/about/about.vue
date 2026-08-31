<template>
	<view class="page">
		<!-- 品牌头 -->
		<view class="hero">
			<view class="logo"><text class="logo-t">果</text></view>
			<text class="brand">鲜果集</text>
			<text class="slogan">水果这行没什么秘密，好吃就是好吃</text>
		</view>

		<!-- 数字 -->
		<view class="nums">
			<view class="num-it">
				<text class="num-n">12</text>
				<text class="num-l">合作产区</text>
			</view>
			<view class="num-sp"></view>
			<view class="num-it">
				<text class="num-n">2 年</text>
				<text class="num-l">做到现在</text>
			</view>
			<view class="num-sp"></view>
			<view class="num-it">
				<text class="num-n">24h</text>
				<text class="num-l">产地直发</text>
			</view>
		</view>

		<!-- 我们是谁 -->
		<view class="card">
			<view class="card-title">我们是谁</view>
			<text class="p">2024 年开始做水果，最早就在小区门口摆个摊，卖点当季的橘子橙子。后来老客户越来越多，干脆把摊子搬到线上。</text>
			<text class="p">现在合作的产区有 12 个，卖得最好的是赣南脐橙和阳山水蜜桃。每年产季前会去园子里转一圈，看树看果看老板靠不靠谱。</text>
			<text class="p">水果没多少门道可讲，能做的就是把中间环节砍掉，从产地直接发，坏了痛快赔。</text>
		</view>

		<!-- 承诺 -->
		<view class="card">
			<view class="card-title">几条承诺</view>
			<view class="pm" v-for="p in promises" :key="p.t">
				<view class="pm-ic"><image :src="p.ic" mode="aspectFit" class="pm-img"></image></view>
				<view class="pm-c">
					<text class="pm-t">{{ p.t }}</text>
					<text class="pm-d">{{ p.d }}</text>
				</view>
			</view>
		</view>

		<!-- 联系 -->
		<view class="card">
			<view class="card-title">找我们</view>
			<view class="ct" @click="call">
				<text class="ct-k">客服电话</text>
				<text class="ct-v link">{{ phone }}</text>
			</view>
			<view class="ct" @click="copyWx">
				<text class="ct-k">客服微信</text>
				<text class="ct-v link">{{ wechat }}</text>
			</view>
			<view class="ct">
				<text class="ct-k">服务时间</text>
				<text class="ct-v">每天 09:00 - 22:00</text>
			</view>
		</view>

		<!-- 版本 -->
		<view class="card">
			<view class="card-title">版本信息</view>
			<view class="ct">
				<text class="ct-k">小程序版本</text>
				<text class="ct-v">{{ version }}</text>
			</view>
			<view class="ct">
				<text class="ct-k">支付方式</text>
				<text class="ct-v">微信支付（当前为模拟流程）</text>
			</view>
			<text class="note">正式接入微信支付需要营业执照办下来才能申请商户号，办好后会在这里更新。</text>
		</view>

		<text class="copy">© 2026 鲜果集</text>
		<view style="height: 40rpx;"></view>
	</view>
</template>

<script>
	const PHONE = '400-888-1234'
	const WECHAT = 'guoxiaoman_kefu'

	export default {
		data() {
			return {
				phone: PHONE,
				wechat: WECHAT,
				version: '1.0.0',
				promises: [
					{ ic: '/static/icon/png/headset.png', t: '坏果就赔', d: '拍张照片就行，50 元以内不用人工审，坏果不用寄回来' },
					{ ic: '/static/icon/png/truck.png', t: '当天发货', d: '14:00 前付款当天走，泡沫箱加冰袋，夏天多放两个' },
					{ ic: '/static/icon/png/star.png', t: '不刷评价', d: '评价里的差评我们不删，回复了但不删' },
					{ ic: '/static/icon/png/wallet.png', t: '不玩先涨后降', d: '标价就是卖价，会员价单独标，不搞两套价格' }
				]
			}
		},
		methods: {
			call() {
				uni.makePhoneCall({
					phoneNumber: PHONE,
					fail: () => uni.showToast({ title: '拨号取消了', icon: 'none' })
				})
			},
			copyWx() {
				uni.setClipboardData({
					data: WECHAT,
					success: () => uni.showToast({ title: '微信号已复制', icon: 'none' })
				})
			}
		}
	}
</script>

<style scoped>
	.page { min-height: 100vh; background: #f5f5f5; }

	/* 品牌头 */
	.hero {
		display: flex; flex-direction: column; align-items: center;
		padding: 56rpx 32rpx 48rpx;
		background: linear-gradient(180deg, #e8f0e8 0%, #f5f5f5 100%);
	}
	.logo {
		width: 128rpx; height: 128rpx; border-radius: 36rpx;
		background: linear-gradient(135deg, #35b47e, #17704a);
		display: flex; align-items: center; justify-content: center;
		box-shadow: 0 6rpx 20rpx rgba(23, 112, 74, 0.22);
	}
	.logo-t { font-size: 62rpx; font-weight: 800; color: #fff; }
	.brand { margin-top: 22rpx; font-size: 40rpx; font-weight: 800; color: #1a2b20; letter-spacing: 2rpx; }
	.slogan { margin-top: 10rpx; font-size: 24rpx; color: #6b7269; }

	/* 数字 */
	.nums {
		display: flex; align-items: center;
		background: #fff; border-radius: 28rpx; margin: 0 24rpx; padding: 30rpx 0;
	}
	.num-it { flex: 1; display: flex; flex-direction: column; align-items: center; gap: 8rpx; }
	.num-n { font-size: 34rpx; font-weight: 700; color: #17704a; }
	.num-l { font-size: 22rpx; color: #a5aca1; }
	.num-sp { width: 1rpx; height: 46rpx; background: #f0f2f4; }

	/* 卡片 */
	.card { background: #fff; border-radius: 28rpx; margin: 20rpx 24rpx; padding: 28rpx; }
	.card-title { font-size: 30rpx; font-weight: 700; color: #222; margin-bottom: 20rpx; }
	/* 每段独立一个 text，不靠 \n 换行——模板静态文本里的 \n 是字面反斜杠加 n，不会渲染成换行 */
	.p { display: block; font-size: 26rpx; color: #555; line-height: 1.8; margin-bottom: 16rpx; }
	.p:last-child { margin-bottom: 0; }

	/* 承诺 */
	.pm { display: flex; align-items: flex-start; gap: 20rpx; padding: 18rpx 0; border-bottom: 1rpx solid #f7f9f5; }
	.pm:last-child { border-bottom: none; padding-bottom: 0; }
	.pm-ic {
		flex: none; width: 68rpx; height: 68rpx; border-radius: 20rpx;
		background: #f7f9f5; display: flex; align-items: center; justify-content: center;
	}
	.pm-img { width: 36rpx; height: 36rpx; }
	.pm-c { flex: 1; min-width: 0; }
	.pm-t { display: block; font-size: 28rpx; font-weight: 600; color: #222; }
	.pm-d { display: block; margin-top: 6rpx; font-size: 24rpx; color: #6b7280; line-height: 1.6; }

	/* 联系 / 版本 */
	.ct { display: flex; align-items: center; padding: 22rpx 0; border-bottom: 1rpx solid #f7f9f5; }
	.ct:last-of-type { border-bottom: none; }
	.ct-k { flex: none; width: 200rpx; font-size: 27rpx; color: #6b7280; }
	.ct-v { flex: 1; text-align: right; font-size: 27rpx; color: #333; }
	.ct-v.link { color: #17704a; font-weight: 600; }
	.note { display: block; margin-top: 16rpx; font-size: 22rpx; color: #a5aca1; line-height: 1.6; }

	.copy { display: block; text-align: center; font-size: 22rpx; color: #b9beb5; margin-top: 32rpx; }
</style>
