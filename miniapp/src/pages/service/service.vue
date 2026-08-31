<template>
	<view class="page">
		<!-- 客服状态 -->
		<view class="hero">
			<view class="hero-ic"><image src="/static/icon/png/headset.png" mode="aspectFit" class="hero-img"></image></view>
			<view class="hero-c">
				<view class="hero-t">
					<text class="hero-name">鲜果集客服</text>
					<text class="dot" :class="{ off: !online }"></text>
					<text class="hero-st" :class="{ off: !online }">{{ online ? '在线' : '已下班' }}</text>
				</view>
				<text class="hero-d">{{ statusTip }}</text>
			</view>
		</view>

		<!-- 联系方式 -->
		<view class="card">
			<view class="way" @click="call">
				<view class="way-ic"><image src="/static/icon/png/chat.png" mode="aspectFit" class="way-img"></image></view>
				<view class="way-c">
					<text class="way-t">打电话</text>
					<text class="way-d">{{ phone }}</text>
				</view>
				<text class="way-btn">拨打</text>
			</view>
			<view class="way" @click="copyWx">
				<view class="way-ic"><image src="/static/icon/png/person.png" mode="aspectFit" class="way-img"></image></view>
				<view class="way-c">
					<text class="way-t">加微信</text>
					<text class="way-d">{{ wechat }}</text>
				</view>
				<text class="way-btn ghost">复制</text>
			</view>
		</view>

		<!-- 自助 -->
		<view class="card">
			<view class="card-hd">
				<text class="card-title">自己也能办</text>
				<text class="card-tip">比等客服快</text>
			</view>
			<view class="self-grid">
				<view class="self-it" v-for="s in selfs" :key="s.action" @click="onSelfTap(s)">
					<view class="self-ic"><image :src="s.ic" mode="aspectFit" class="self-img"></image></view>
					<text class="self-t">{{ s.t }}</text>
					<text class="self-d">{{ s.d }}</text>
				</view>
			</view>
		</view>

		<!-- 找客服前先看看 -->
		<view class="card">
			<view class="card-hd">
				<text class="card-title">这几件事不用找客服</text>
			</view>
			<view class="tip" v-for="(t, i) in tips" :key="i">
				<text class="tip-n">{{ i + 1 }}</text>
				<view class="tip-c">
					<text class="tip-t">{{ t.t }}</text>
					<text class="tip-d">{{ t.d }}</text>
				</view>
			</view>
			<view class="more" @click="goHelp">
				<text class="more-k">看完整的常见问题</text>
				<text class="more-arrow">›</text>
			</view>
		</view>

		<view style="height: 40rpx;"></view>
	</view>
</template>

<script>
	const PHONE = '400-888-1234'
	const WECHAT = 'guoxiaoman_kefu'
	const OPEN_HOUR = 9
	const CLOSE_HOUR = 22

	export default {
		data() {
			return {
				phone: PHONE,
				wechat: WECHAT,
				online: false,
				statusTip: '',
				selfs: [
					{ ic: '/static/icon/png/box.png', t: '查订单', d: '物流进度', action: 'orders', needLogin: true },
					{ ic: '/static/icon/png/headset.png', t: '报坏果', d: '拍照就赔', action: 'orders', needLogin: true },
					{ ic: '/static/icon/png/map-pin.png', t: '改地址', d: '发货前可改', action: 'address', needLogin: true },
					{ ic: '/static/icon/png/ticket.png', t: '我的券', d: '看有效期', action: 'coupon' }
				],
				// 注意：data() 里不能引用裸方法名，跳转统一用 action 字符串由 onSelfTap 分发
				selfRoutes: {
					orders: '/pages/orders/orders',
					address: '/pages/address/address',
					coupon: '/pages/coupon/coupon'
				},
				tips: [
					{ t: '水果坏了要赔', d: '进「我的订单」点申请售后，拍张照片传上去。50 元以内系统直接过，钱退回原路，坏果不用寄回来。' },
					{ t: '想知道什么时候到', d: '订单列表里点开就有运单号，长按可以复制去快递官网查。' },
					{ t: '下错了想取消', d: '没发货之前订单页自己就能取消，库存和优惠券会退回来。已经发货了就得联系客服。' }
				]
			}
		},
		onShow() {
			this.refreshStatus()
		},
		methods: {
			/** 按当前时间判断客服是否在服务时段内，顺便给一句人话提示 */
			refreshStatus() {
				const now = new Date()
				const h = now.getHours()
				this.online = h >= OPEN_HOUR && h < CLOSE_HOUR
				if (this.online) {
					const left = CLOSE_HOUR - h
					this.statusTip = left <= 1
						? '今天快下班了，有急事赶紧说'
						: '服务时间 09:00 - 22:00，现在有人'
				} else {
					this.statusTip = h < OPEN_HOUR
						? '早上 9 点上班，留言的话开工先回你'
						: '已经过 22 点了，明早 9 点回复'
				}
			},
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
			},
			onSelfTap(s) {
				const url = this.selfRoutes[s.action]
				if (!url) return
				if (s.needLogin && !uni.getStorageSync('token')) {
					return uni.navigateTo({ url: '/pages/login/login' })
				}
				uni.navigateTo({ url })
			},
			goHelp() {
				uni.navigateTo({ url: '/pages/help/help' })
			}
		}
	}
</script>

<style scoped>
	.page { min-height: 100vh; background: #f5f5f5; }

	/* 客服状态 */
	.hero {
		display: flex; align-items: center; gap: 24rpx;
		padding: 36rpx 32rpx 40rpx;
		background: linear-gradient(180deg, #e8f0e8 0%, #f5f5f5 100%);
	}
	.hero-ic {
		flex: none; width: 96rpx; height: 96rpx; border-radius: 50%;
		background: #17704a; display: flex; align-items: center; justify-content: center;
		box-shadow: 0 4rpx 16rpx rgba(23, 112, 74, 0.18);
	}
	.hero-img { width: 48rpx; height: 48rpx; }
	.hero-c { flex: 1; min-width: 0; }
	.hero-t { display: flex; align-items: center; gap: 10rpx; }
	.hero-name { font-size: 32rpx; font-weight: 700; color: #222; }
	.dot { width: 12rpx; height: 12rpx; border-radius: 50%; background: #2e9e6b; }
	.dot.off { background: #c2c8bf; }
	.hero-st { font-size: 22rpx; color: #2e9e6b; font-weight: 600; }
	.hero-st.off { color: #a5aca1; }
	.hero-d { display: block; margin-top: 8rpx; font-size: 24rpx; color: #6b7280; }

	/* 通用卡片 */
	.card { background: #fff; border-radius: 28rpx; margin: 20rpx 24rpx; padding: 28rpx; }
	.card-hd { display: flex; align-items: center; justify-content: space-between; margin-bottom: 24rpx; }
	.card-title { font-size: 30rpx; font-weight: 700; color: #222; }
	.card-tip { font-size: 22rpx; color: #a5aca1; }

	/* 联系方式 */
	.way { display: flex; align-items: center; gap: 20rpx; padding: 22rpx 0; border-bottom: 1rpx solid #f7f9f5; }
	.way:last-child { border-bottom: none; }
	.way-ic {
		flex: none; width: 76rpx; height: 76rpx; border-radius: 22rpx;
		background: #f7f9f5; display: flex; align-items: center; justify-content: center;
	}
	.way-img { width: 38rpx; height: 38rpx; }
	.way-c { flex: 1; min-width: 0; }
	.way-t { display: block; font-size: 28rpx; font-weight: 600; color: #222; }
	.way-d { display: block; margin-top: 6rpx; font-size: 24rpx; color: #6b7280; }
	.way-btn {
		flex: none; font-size: 24rpx; font-weight: 600; color: #fff;
		background: #17704a; padding: 12rpx 30rpx; border-radius: 26rpx;
	}
	.way-btn.ghost { color: #17704a; background: #e6f4ec; }
	.way-btn:active { opacity: 0.85; }

	/* 自助 */
	.self-grid { display: grid; grid-template-columns: 1fr 1fr; gap: 16rpx; }
	.self-it {
		background: #fafbf9; border-radius: 22rpx; padding: 24rpx 20rpx;
		display: flex; flex-direction: column; gap: 8rpx;
	}
	.self-it:active { background: #f2f5f0; }
	.self-ic {
		width: 60rpx; height: 60rpx; border-radius: 18rpx; background: #fff;
		display: flex; align-items: center; justify-content: center; margin-bottom: 6rpx;
	}
	.self-img { width: 34rpx; height: 34rpx; }
	.self-t { font-size: 27rpx; font-weight: 600; color: #222; }
	.self-d { font-size: 22rpx; color: #a5aca1; }

	/* 提示 */
	.tip { display: flex; gap: 18rpx; padding: 18rpx 0; border-bottom: 1rpx solid #f7f9f5; }
	.tip:last-of-type { border-bottom: none; }
	.tip-n {
		flex: none; width: 36rpx; height: 36rpx; line-height: 36rpx; text-align: center;
		border-radius: 50%; background: #e6f4ec; color: #17704a;
		font-size: 21rpx; font-weight: 700; margin-top: 4rpx;
	}
	.tip-c { flex: 1; min-width: 0; }
	.tip-t { display: block; font-size: 27rpx; font-weight: 600; color: #222; }
	.tip-d { display: block; margin-top: 6rpx; font-size: 24rpx; color: #6b7280; line-height: 1.6; }
	.more {
		margin-top: 20rpx; padding-top: 20rpx; border-top: 1rpx solid #f7f9f5;
		display: flex; align-items: center; justify-content: space-between;
	}
	.more-k { font-size: 27rpx; color: #17704a; font-weight: 600; }
	.more-arrow { font-size: 32rpx; color: #17704a; }
</style>
