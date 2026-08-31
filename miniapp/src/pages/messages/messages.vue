<template>
	<view class="page">
		<view class="hd">消息中心</view>
		<view class="tabs">
			<view class="tab" :class="{ on: tab === 'order' }" @click="tab = 'order'">订单</view>
			<view class="tab" :class="{ on: tab === 'coupon' }" @click="tab = 'coupon'">优惠</view>
			<view class="tab" :class="{ on: tab === 'system' }" @click="tab = 'system'">系统</view>
		</view>

		<!-- 订单提醒：来自 /api/v1/orders 真实数据 -->
		<view v-if="tab === 'order'">
			<view v-if="!orderMsgs.length && !loading" class="empty">暂无需要处理的订单</view>
			<view v-for="m in orderMsgs" :key="m.orderNo" class="msg" @click="goOrders(m.status)">
				<view class="msg-top">
					<text class="msg-title">{{ statusTitle(m.status) }}</text>
					<text class="msg-time">{{ fmtTime(m.createdAt) }}</text>
				</view>
				<view class="msg-body">订单号 {{ m.orderNo }}</view>
				<view class="msg-foot">
					<text class="msg-amt">实付 ¥{{ yuan(m.payAmount) }}</text>
					<text class="msg-go">去处理 ›</text>
				</view>
			</view>
		</view>

		<!-- 优惠券提醒：来自 /api/v1/coupons/mine 真实数据 -->
		<view v-if="tab === 'coupon'">
			<view v-if="!couponMsgs.length && !loading" class="empty">暂无可用优惠券</view>
			<view v-for="c in couponMsgs" :key="c.userCouponId" class="msg" @click="goCoupons">
				<view class="msg-top">
					<text class="msg-title">优惠券待使用</text>
					<text class="msg-time">{{ (c.expireAt || '').slice(0, 10) }} 到期</text>
				</view>
				<view class="msg-body">{{ c.name }}</view>
				<view class="msg-foot">
					<text class="msg-amt">{{ c.thresholdAmount > 0 ? '满 ' + yuan(c.thresholdAmount).toFixed(0) + ' 可用' : '无门槛' }}</text>
					<text class="msg-go">去使用 ›</text>
				</view>
			</view>
		</view>

		<!-- 系统通知：后端暂无消息表 -->
		<view v-if="tab === 'system'">
			<view class="empty">暂无系统通知</view>
		</view>
	</view>
</template>

<script>
	import { api, yuan, fmtTime } from '../../api'

	export default {
		data() {
			return {
				tab: 'order',
				loading: true,
				orderMsgs: [],
				couponMsgs: []
			}
		},
		onShow() {
			if (!uni.getStorageSync('token')) {
				return uni.navigateTo({ url: '/pages/login/login' })
			}
			this.load()
		},
		methods: {
			yuan,
			fmtTime,
			async load() {
				this.loading = true
				try {
					const res = await api.get('/api/v1/orders?page=1&size=20')
					const list = (res && res.records) || []
					// 只展示需要用户下一步操作的订单
					this.orderMsgs = list.filter((o) => [10, 20, 30, 40].includes(o.status))
				} catch (e) {
					this.orderMsgs = []
				}
				try {
					const mine = await api.get('/api/v1/coupons/mine')
					this.couponMsgs = (mine || []).filter((c) => c.status === 0)
				} catch (e) {
					this.couponMsgs = []
				} finally {
					this.loading = false
				}
			},
			statusTitle(status) {
				return {
					10: '订单待付款',
					20: '商家已接单，待发货',
					30: '包裹已发出，待收货',
					40: '已签收，快去评价'
				}[status] || '订单状态更新'
			},
			goOrders(status) {
				uni.navigateTo({ url: '/pages/orders/orders' + (status ? '?status=' + status : '') })
			},
			goCoupons() {
				uni.navigateTo({ url: '/pages/coupon/coupon' })
			}
		}
	}
</script>

<style scoped>
	.page { min-height: 100vh; background: #f5f5f5; }
	.hd { padding: 28rpx 32rpx; background: #fff; font-size: 34rpx; font-weight: 700; color: #222; }
	.tabs { display: flex; background: #fff; border-bottom: 1rpx solid #f0f2f4; }
	.tab { flex: 1; text-align: center; padding: 22rpx 0; font-size: 28rpx; color: #6b7280; border-bottom: 4rpx solid transparent; }
	.tab.on { color: #17704a; font-weight: 700; border-bottom-color: #17704a; }
	.empty { text-align: center; padding-top: 180rpx; color: #a5aca1; font-size: 26rpx; }
	.msg { background: #fff; margin: 20rpx 24rpx 0; border-radius: 24rpx; padding: 26rpx 28rpx; }
	.msg-top { display: flex; align-items: baseline; justify-content: space-between; gap: 16rpx; }
	.msg-title { font-size: 29rpx; font-weight: 600; color: #222; }
	.msg-time { font-size: 21rpx; color: #a5aca1; flex: none; }
	.msg-body { margin-top: 10rpx; font-size: 25rpx; color: #666; }
	.msg-foot { display: flex; align-items: baseline; justify-content: space-between; margin-top: 16rpx; }
	.msg-amt { font-size: 25rpx; color: #e54d42; font-weight: 600; }
	.msg-go { font-size: 24rpx; color: #17704a; }
</style>
