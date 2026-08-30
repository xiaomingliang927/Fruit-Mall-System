<template>
	<view class="page">
		<view class="tabs">
			<view class="tab" :class="{ on: tab === 'receive' }" @tap="tab = 'receive'">可领券</view>
			<view class="tab" :class="{ on: tab === 'mine' }" @tap="switchMine">我的券</view>
		</view>

		<!-- 可领券 -->
		<view v-if="tab === 'receive'">
			<view v-if="!receivable.length" class="empty">暂无可领取的优惠券</view>
			<view v-for="c in receivable" :key="c.id" class="cpn" :class="{ soldout: c.remaining <= 0 }">
				<view class="cpn-left">
					<view v-if="c.type === 2" class="cpn-big">{{ (c.discountPercent / 10).toFixed(1) }}<text class="cpn-unit">折</text></view>
					<view v-else class="cpn-big"><text class="cpn-rmb">¥</text>{{ yuan(c.discountAmount).toFixed(0) }}</view>
					<view class="cpn-th">{{ c.thresholdAmount > 0 ? '满 ' + yuan(c.thresholdAmount).toFixed(0) + ' 可用' : '无门槛' }}</view>
				</view>
				<view class="cpn-mid">
					<view class="cpn-name">{{ c.name }}</view>
					<view class="cpn-type">{{ c.typeText }} · 领取后 30 天内有效</view>
					<view class="cpn-time">{{ c.startTime.slice(0, 10) }} ~ {{ c.endTime.slice(0, 10) }}</view>
				</view>
				<view class="cpn-right">
					<view v-if="c.remaining > 0" class="recv-btn" :class="{ got: gotMap[c.id] }" @tap="receive(c)">
						{{ gotMap[c.id] ? '已领取' : '领取' }}
					</view>
					<view v-else class="soldout-txt">已抢完</view>
					<view class="cpn-remain">剩 {{ c.remaining }} 张</view>
				</view>
			</view>
		</view>

		<!-- 我的券 -->
		<view v-if="tab === 'mine'">
			<view v-if="!mine.length" class="empty">暂无优惠券，去领券中心看看</view>
			<view v-for="m in mine" :key="m.userCouponId" class="cpn">
				<view class="cpn-left">
					<view v-if="m.type === 2" class="cpn-big">{{ (m.discountPercent / 10).toFixed(1) }}<text class="cpn-unit">折</text></view>
					<view v-else class="cpn-big"><text class="cpn-rmb">¥</text>{{ yuan(m.discountAmount).toFixed(0) }}</view>
					<view class="cpn-th">{{ m.thresholdAmount > 0 ? '满 ' + yuan(m.thresholdAmount).toFixed(0) + ' 可用' : '无门槛' }}</view>
				</view>
				<view class="cpn-mid">
					<view class="cpn-name">{{ m.name }}</view>
					<view class="cpn-time">有效期至 {{ m.expireAt.slice(0, 10) }}</view>
				</view>
				<view class="cpn-right">
					<text class="cpn-status" :class="'st' + m.status">{{ m.statusText }}</text>
				</view>
			</view>
		</view>
	</view>
</template>

<script>
	import { api, imgUrl, yuan } from '../../api'

	export default {
		data() {
			return {
				tab: 'receive',
				receivable: [],
				mine: [],
				gotMap: {}
			}
		},
		onShow() {
			this.load()
		},
		methods: {
			imgUrl,
			yuan,
			async load() {
				try {
					this.receivable = await api.get('/api/v1/coupons/list')
					if (uni.getStorageSync('token')) {
						this.mine = await api.get('/api/v1/coupons/mine')
					}
				} catch (e) {
					uni.showToast({ title: e.message, icon: 'none' })
				}
			},
			switchMine() {
				this.tab = 'mine'
				if (!uni.getStorageSync('token')) {
					return uni.navigateTo({ url: '/pages/login/login' })
				}
				this.load()
			},
			async receive(c) {
				if (!uni.getStorageSync('token')) {
					return uni.navigateTo({ url: '/pages/login/login' })
				}
				try {
					await api.post('/api/v1/coupons/receive', { couponId: c.id })
					this.gotMap[c.id] = true
					uni.showToast({ title: '领取成功，下单时可选用', icon: 'none' })
					this.load()
				} catch (e) {
					uni.showToast({ title: e.message, icon: 'none' })
				}
			}
		}
	}
</script>

<style scoped>
	.page { min-height: 100vh; background: #f5f5f5; padding-bottom: 40rpx; }
	.tabs { display: flex; background: #fff; border-bottom: 1rpx solid #f0f2f4; }
	.tab { flex: 1; text-align: center; padding: 24rpx 0; font-size: 28rpx; color: #6b7280; border-bottom: 4rpx solid transparent; }
	.tab.on { color: #17704a; font-weight: 700; border-bottom-color: #17704a; }
	.empty { text-align: center; color: #a5aca1; padding-top: 180rpx; font-size: 26rpx; }
	.cpn {
		display: flex; align-items: center; background: #fff; border-radius: 20rpx;
		margin: 20rpx 24rpx 0; overflow: hidden;
	}
	.cpn.soldout { opacity: 0.55; }
	.cpn-left {
		width: 190rpx; align-self: stretch; flex: none; color: #fff;
		background: linear-gradient(135deg, #ff6b5e, #f24e3e);
		display: flex; flex-direction: column; align-items: center; justify-content: center;
		padding: 24rpx 8rpx;
	}
	.cpn-big { font-size: 46rpx; font-weight: 800; line-height: 1.1; }
	.cpn-rmb { font-size: 24rpx; }
	.cpn-unit { font-size: 22rpx; }
	.cpn-th { font-size: 19rpx; opacity: .85; margin-top: 8rpx; }
	.cpn-mid { flex: 1; padding: 20rpx; min-width: 0; }
	.cpn-name { font-size: 27rpx; font-weight: 600; color: #222; }
	.cpn-type { font-size: 21rpx; color: #6b7269; margin-top: 8rpx; }
	.cpn-time { font-size: 20rpx; color: #a5aca1; margin-top: 6rpx; }
	.cpn-right { padding-right: 22rpx; text-align: center; }
	.recv-btn {
		background: linear-gradient(90deg, #35b47e, #1f8a58); color: #fff;
		font-size: 24rpx; font-weight: 600; padding: 12rpx 28rpx; border-radius: 32rpx;
	}
	.recv-btn.got { background: #e6f4ec; color: #17704a; }
	.soldout-txt { font-size: 22rpx; color: #a5aca1; }
	.cpn-remain { font-size: 19rpx; color: #a5aca1; margin-top: 8rpx; }
	.cpn-status { font-size: 23rpx; font-weight: 600; }
	.st0 { color: #2e9e6b; } .st1 { color: #a5aca1; } .st2 { color: #c0c4cc; }
</style>
