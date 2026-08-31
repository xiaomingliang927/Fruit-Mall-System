<template>
	<view class="page">
		<view class="hd">帮助中心</view>
		<view class="card">
			<view class="qa" v-for="(q,i) in faq" :key="i">
				<view class="q" @click="toggle(i)">
					<text class="tag">Q</text>
					<text class="txt">{{ q.q }}</text>
					<text class="arrow" :class="{ open: open === i }">›</text>
				</view>
				<view v-if="open === i" class="a">{{ q.a }}</view>
			</view>
		</view>
	</view>
</template>

<script>
	export default {
		data() {
			return {
				open: null,
				faq: [
					{ q: '下单后多久发货？', a: '订单支付成功后，一般 24 小时内发货，生鲜水果每天 14:00 前截单当日发，节假日顺延。' },
					{ q: '如何申请售后/坏果包赔？', a: '进入「我的 → 我的订单 → 申请售后」，上传坏果照片，50 元以下系统自动审核秒赔，超过 50 元人工复核。' },
					{ q: '优惠券怎么用？', a: '结算页会展示当前可用的优惠券，点击选择即可自动抵扣；每张券只能使用一次，不可找零。' },
					{ q: '支持哪些支付方式？', a: '目前支持微信支付（M1 阶段为模拟支付，正式上线后对接真实微信 JSAPI 支付）。' },
					{ q: '会员等级怎么提升？', a: '累计消费金额达到对应门槛自动升级：白银 ¥500、黄金 ¥2,000、钻石 ¥5,000，无需手动开通。' },
					{ q: '如何联系客服？', a: '可拨打客服电话 400-888-1234，或加客服微信 guoxiaoman_kefu，服务时间 09:00-22:00。' }
				]
			}
		},
		methods: {
			toggle(i) {
				this.open = this.open === i ? null : i
			}
		}
	}
</script>

<style scoped>
	.page { min-height: 100vh; background: #f5f5f5; }
	.hd { padding: 28rpx 32rpx; background: #fff; font-size: 34rpx; font-weight: 700; color: #222; }
	.card { background: #fff; margin: 20rpx 24rpx; border-radius: 24rpx; padding: 12rpx 28rpx; }
	.qa { border-bottom: 1rpx solid #f0f2f4; }
	.qa:last-child { border-bottom: none; }
	.q { display: flex; align-items: center; padding: 26rpx 0; }
	.tag { flex: none; width: 40rpx; height: 40rpx; line-height: 40rpx; text-align: center; border-radius: 50%; background: #17704a; color: #fff; font-size: 22rpx; font-weight: 700; margin-right: 16rpx; }
	.txt { flex: 1; font-size: 28rpx; color: #222; }
	.arrow { font-size: 30rpx; color: #999; transform: rotate(90deg); transition: transform .2s; }
	.arrow.open { transform: rotate(-90deg); }
	.a { padding: 0 0 24rpx 56rpx; font-size: 26rpx; color: #666; line-height: 1.6; }
</style>
