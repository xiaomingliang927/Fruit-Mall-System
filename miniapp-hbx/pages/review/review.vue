<template>
	<view class="page">
		<view class="card" v-for="(it, i) in items" :key="it.orderItemId">
			<view class="it-name">{{ it.productName }}<text class="it-spec">（{{ it.skuSpec }}）</text></view>
			<view v-if="it.reviewed" class="done">已评价 ✓</view>
			<template v-else>
				<view class="stars">
					<text v-for="n in 5" :key="n" class="star" :class="{ on: n <= (it.rating || 5) }"
						@tap="it.rating = n">★</text>
					<text class="star-tip">{{ (it.rating || 5) >= 4 ? '好评' : (it.rating || 5) >= 3 ? '中评' : '差评' }}</text>
				</view>
				<textarea class="content" v-model="it.content" maxlength="200"
					placeholder="宝贝新鲜吗？说说你的体验吧…" />
				<view class="anon" @tap="it.anonymous = !it.anonymous">
					<text class="anon-box" :class="{ on: it.anonymous }">✓</text>
					<text class="anon-lb">匿名评价</text>
				</view>
			</template>
		</view>

		<view class="bar">
			<view class="submit" :class="{ off: submitting || !pendingCount }" @tap="submit">
				{{ submitting ? '提交中…' : '提交评价（' + pendingCount + '）' }}
			</view>
		</view>
	</view>
</template>

<script>
	import { api } from '../../api'

	export default {
		data() {
			return {
				orderNo: '',
				items: [],
				submitting: false
			}
		},
		computed: {
			pendingCount() {
				return this.items.filter((it) => !it.reviewed).length
			}
		},
		onLoad(opt) {
			this.orderNo = opt.orderNo || ''
			this.load()
		},
		methods: {
			async load() {
				try {
					const items = await api.get('/api/v1/reviews/summary?orderNo=' + this.orderNo)
					this.items = items.map((it) => ({ ...it, rating: 5, content: '', anonymous: false }))
				} catch (e) {
					uni.showToast({ title: e.message, icon: 'none' })
				}
			},
			async submit() {
				const pending = this.items.filter((it) => !it.reviewed)
				for (const it of pending) {
					if (!it.content.trim()) {
						return uni.showToast({ title: '请填写「' + it.productName + '」的评价内容', icon: 'none' })
					}
				}
				this.submitting = true
				try {
					for (const it of pending) {
						await api.post('/api/v1/reviews', {
							orderNo: this.orderNo,
							orderItemId: it.orderItemId,
							rating: it.rating,
							content: it.content.trim(),
							isAnonymous: it.anonymous
						})
					}
					uni.showToast({ title: '评价成功，感谢反馈', icon: 'success' })
					setTimeout(() => uni.navigateBack(), 800)
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
	.it-name { font-size: 28rpx; font-weight: 600; margin-bottom: 14rpx; }
	.it-spec { color: #a5aca1; font-weight: 400; font-size: 23rpx; }
	.stars { display: flex; align-items: center; gap: 8rpx; margin-bottom: 14rpx; }
	.star { font-size: 44rpx; color: #e2e5e9; line-height: 1; }
	.star.on { color: #f7ba2a; }
	.star-tip { font-size: 22rpx; color: #a5aca1; margin-left: 10rpx; }
	.content {
		width: 100%; min-height: 140rpx; border: 2rpx solid #e5e7eb; border-radius: 12rpx;
		padding: 16rpx 20rpx; font-size: 26rpx; box-sizing: border-box;
	}
	.anon { display: flex; align-items: center; gap: 10rpx; margin-top: 12rpx; }
	.anon-box {
		width: 34rpx; height: 34rpx; border-radius: 6rpx; border: 2rpx solid #cbd5e1;
		text-align: center; line-height: 30rpx; font-size: 22rpx; color: transparent;
	}
	.anon-box.on { background: #2e9e6b; border-color: #2e9e6b; color: #fff; }
	.anon-lb { font-size: 23rpx; color: #6b7280; }
	.done { color: #2e9e6b; font-size: 25rpx; }
	.bar {
		position: fixed; left: 0; right: 0; bottom: 0; background: #fff;
		padding: 18rpx 24rpx calc(18rpx + env(safe-area-inset-bottom));
		border-top: 1rpx solid #eceef0;
	}
	.submit {
		background: linear-gradient(90deg, #35b47e, #1f8a58); color: #fff; text-align: center;
		border-radius: 44rpx; padding: 20rpx 0; font-size: 29rpx; font-weight: 600;
	}
	.submit.off { opacity: 0.5; }
</style>
