<template>
	<view class="page">
		<view class="card">
			<view class="h">售后订单</view>
			<view class="order-line">
				<text class="ono">{{ orderNo }}</text>
				<text class="oamt">¥{{ yuan(amount) }}</text>
			</view>
			<view class="tip">售后金额为整单实付金额；≤50 元自动秒审退款（坏果包赔），超额进入人工审核</view>
		</view>

		<view class="card">
			<view class="h">售后类型</view>
			<view class="types">
				<view class="tp" :class="{ on: type === 1 }" @tap="type = 1">仅退款<text class="tp-s">未收到货 / 坏果包赔</text></view>
				<view class="tp" :class="{ on: type === 2 }" @tap="type = 2">退货退款<text class="tp-s">已收到货，需寄回</text></view>
			</view>
		</view>

		<view class="card">
			<view class="h">申请原因</view>
			<textarea class="reason" v-model="reason" maxlength="200" placeholder="请填写申请原因（必填），如：坏果 / 腐烂 / 少件…" />
		</view>

		<view class="bar">
			<view class="submit" :class="{ off: submitting }" @tap="submit">{{ submitting ? '提交中…' : '提交申请' }}</view>
		</view>
	</view>
</template>

<script>
	import { api, yuan } from '../../api'

	export default {
		data() {
			return {
				orderNo: '',
				amount: 0,
				type: 1,
				reason: '',
				submitting: false
			}
		},
		onLoad(opt) {
			this.orderNo = opt.orderNo || ''
			this.amount = Number(opt.amount) || 0
		},
		methods: {
			async submit() {
				if (!this.reason.trim()) {
					return uni.showToast({ title: '请填写申请原因', icon: 'none' })
				}
				this.submitting = true
				try {
					const data = await api.post('/api/v1/refunds', {
						orderNo: this.orderNo,
						type: this.type,
						reason: this.reason.trim()
					})
					uni.showToast({
						title: data.statusText === '已退款' ? '审核通过，退款原路返回' : '已提交，等待审核',
						icon: 'success'
					})
					setTimeout(() => uni.navigateBack(), 900)
				} catch (e) {
					uni.showToast({ title: e.message, icon: 'none' })
				} finally {
					this.submitting = false
				}
			}
		}
	}
</script>

<style scoped>
	.page { min-height: 100vh; background: #f5f5f5; padding: 20rpx 24rpx 180rpx; }
	.card { background: #fff; border-radius: 20rpx; padding: 26rpx; margin-bottom: 20rpx; border: 1rpx solid #eceef0; }
	.h { font-size: 29rpx; font-weight: 700; margin-bottom: 16rpx; }
	.order-line { display: flex; justify-content: space-between; align-items: center; }
	.ono { color: #6b7280; font-size: 25rpx; }
	.oamt { color: #f24e3e; font-size: 32rpx; font-weight: 800; }
	.tip { margin-top: 14rpx; background: #fff7f0; border-radius: 10rpx; padding: 14rpx 18rpx; font-size: 22rpx; color: #a55a22; line-height: 1.7; }
	.types { display: flex; gap: 18rpx; }
	.tp {
		flex: 1; border: 2rpx solid #e5e7eb; border-radius: 14rpx; padding: 20rpx;
		font-size: 27rpx; font-weight: 600; text-align: center;
	}
	.tp-s { display: block; font-size: 21rpx; color: #a5aca1; font-weight: 400; margin-top: 8rpx; }
	.tp.on { border-color: #f24e3e; color: #f24e3e; background: #fef4f4; }
	.reason {
		width: 100%; min-height: 160rpx; border: 2rpx solid #e5e7eb; border-radius: 12rpx;
		padding: 16rpx 20rpx; font-size: 26rpx; box-sizing: border-box;
	}
	.bar {
		position: fixed; left: 0; right: 0; bottom: 0; background: #fff;
		padding: 18rpx 24rpx calc(18rpx + env(safe-area-inset-bottom));
		border-top: 1rpx solid #eceef0;
	}
	.submit {
		background: linear-gradient(90deg, #f37e5d, #f24e3e); color: #fff; text-align: center;
		border-radius: 44rpx; padding: 20rpx 0; font-size: 29rpx; font-weight: 600;
	}
	.submit.off { opacity: 0.6; }
</style>
