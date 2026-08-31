<template>
	<view class="page">
		<view class="banner">
			<view class="level">{{ levelText(level) }}</view>
			<view class="sub">专属优惠 · 积分加倍 · 极速客服</view>
		</view>

		<view class="card">
			<view class="card-title">会员权益</view>
			<view class="grid">
				<view class="it" v-for="b in benefits" :key="b">
					<text class="dot">✓</text>
					<text>{{ b }}</text>
				</view>
			</view>
		</view>

		<view class="card">
			<view class="card-title">等级说明</view>
			<view class="rule" v-for="r in rules" :key="r.level">
				<text class="level-tag">{{ r.level }}</text>
				<text class="txt">{{ r.txt }}</text>
			</view>
		</view>

		<view class="fixed-btn">
			<button class="open-btn" @click="showToast('会员卡购买功能上线后开放')">立即开通</button>
		</view>
	</view>
</template>

<script>
	export default {
		data() {
			return {
				level: 1,
				benefits: ['会员专享价', '消费积分翻倍', '每月优惠券礼包', '售后优先处理', '生日双倍积分'],
				rules: [
					{ level: '普通会员', txt: '注册即享' },
					{ level: '白银会员', txt: '累计消费满 ¥500 自动升级' },
					{ level: '黄金会员', txt: '累计消费满 ¥2,000 自动升级' },
					{ level: '钻石会员', txt: '累计消费满 ¥5,000 自动升级' }
				]
			}
		},
		onShow() {
			const u = uni.getStorageSync('userLevel')
			if (u) this.level = Number(u) || 1
		},
		methods: {
			levelText(level) {
				return { 1: '普通会员', 2: '白银会员', 3: '黄金会员', 4: '钻石会员' }[level] || '普通会员'
			},
			showToast(msg) {
				uni.showToast({ title: msg, icon: 'none' })
			}
		}
	}
</script>

<style scoped>
	.page { min-height: 100vh; background: #f5f5f5; padding-bottom: 160rpx; }
	.banner {
		text-align: center; padding: 60rpx 32rpx 80rpx;
		background: linear-gradient(135deg, #3d2e1f, #2a1f15);
		border-radius: 0 0 48rpx 48rpx;
	}
	.level { font-size: 48rpx; font-weight: 800; color: #f5e6c8; }
	.sub { margin-top: 12rpx; font-size: 24rpx; color: #b8a993; }
	.card { background: #fff; margin: 20rpx 24rpx; border-radius: 24rpx; padding: 28rpx; }
	.card-title { font-size: 30rpx; font-weight: 700; color: #222; margin-bottom: 24rpx; }
	.grid { display: grid; grid-template-columns: 1fr 1fr; gap: 20rpx; }
	.it { display: flex; align-items: center; gap: 12rpx; font-size: 26rpx; color: #555; }
	.dot { color: #17704a; font-weight: 700; }
	.rule { display: flex; align-items: center; gap: 16rpx; padding: 16rpx 0; border-bottom: 1rpx solid #f0f2f4; }
	.rule:last-child { border-bottom: none; }
	.level-tag { font-size: 20rpx; color: #17704a; background: #e6f4ec; padding: 6rpx 16rpx; border-radius: 16rpx; }
	.txt { font-size: 26rpx; color: #555; }
	.fixed-btn { position: fixed; left: 0; right: 0; bottom: 0; padding: 24rpx; background: #fff; border-top: 1rpx solid #f0f2f4; }
	.open-btn {
		background: linear-gradient(90deg, #35b47e, #1f8a58); color: #fff;
		font-size: 30rpx; font-weight: 600; border-radius: 44rpx; line-height: 84rpx;
	}
</style>
