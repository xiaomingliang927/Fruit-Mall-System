<template>
	<view class="page">
		<!-- 会员卡 -->
		<view class="card-wrap">
			<view class="mcard" :class="'s-' + st">
				<view class="mcard-glow"></view>
				<view class="mcard-top">
					<view>
						<view class="mcard-lv">
							<image src="/static/icon/png/crown.png" mode="aspectFit" class="crown"></image>
							<text class="lv-txt">{{ levelText }}</text>
						</view>
						<text class="mcard-name">{{ nickname || '未登录' }}</text>
					</view>
					<text class="mcard-badge">{{ badgeText }}</text>
				</view>
				<view class="mcard-bottom">
					<text class="mcard-exp">{{ expireText }}</text>
					<text v-if="m.remainDays !== null && m.remainDays !== undefined" class="mcard-days">
						剩 {{ m.remainDays }} 天
					</text>
				</view>
			</view>
		</view>

		<!-- 消费概览 -->
		<view class="stat">
			<view class="stat-it">
				<text class="stat-n">{{ yuan(m.totalGmv) }}</text>
				<text class="stat-l">累计消费</text>
			</view>
			<view class="stat-sp"></view>
			<view class="stat-it">
				<text class="stat-n">{{ m.orderCount || 0 }}</text>
				<text class="stat-l">完成订单</text>
			</view>
			<view class="stat-sp"></view>
			<view class="stat-it">
				<text class="stat-n">{{ m.joinedDays || 0 }}</text>
				<text class="stat-l">相识天数</text>
			</view>
		</view>

		<!-- 会员权益 -->
		<view class="card">
			<view class="card-hd">
				<text class="card-title">会员能省什么</text>
				<text class="card-tip">{{ st === 'ACTIVE' || st === 'EXPIRING' ? '已生效' : '开卡后生效' }}</text>
			</view>
			<view class="ben" v-for="b in benefits" :key="b.t">
				<view class="ben-ic"><image :src="b.ic" mode="aspectFit" class="ben-img"></image></view>
				<view class="ben-c">
					<text class="ben-t">{{ b.t }}</text>
					<text class="ben-d">{{ b.d }}</text>
				</view>
			</view>
		</view>

		<!-- 等级 -->
		<view class="card">
			<view class="card-hd">
				<text class="card-title">四档等级</text>
			</view>
			<view class="lv" v-for="r in levels" :key="r.n" :class="{ cur: r.n === (m.level || 1) }">
				<text class="lv-tag" :class="{ cur: r.n === (m.level || 1) }">{{ r.name }}</text>
				<text class="lv-d">{{ r.d }}</text>
				<text v-if="r.n === (m.level || 1)" class="lv-cur">当前</text>
			</view>
			<text class="lv-note">等级由客服开卡时设定，续费不会掉级。</text>
		</view>

		<!-- 开卡说明 -->
		<view class="card">
			<view class="card-hd">
				<text class="card-title">怎么开卡</text>
			</view>
			<text class="how">会员卡目前由客服人工开通，还没做成线上自助购买。想开卡或者续费，直接联系客服说一声就行，当场就能办好。</text>
			<view class="how-row" @click="goService">
				<text class="how-k">联系客服开卡</text>
				<text class="how-arrow">›</text>
			</view>
		</view>

		<view class="foot-sp"></view>

		<!-- 底部按钮 -->
		<view class="fixed-btn">
			<view class="btn" @click="goService">{{ btnText }}</view>
		</view>
	</view>
</template>

<script>
	import { api } from '../../api'

	export default {
		data() {
			return {
				m: {},
				nickname: '',
				// 权益写具体规则，不写"专属优惠、积分翻倍"这类空话
				benefits: [
					{ ic: '/static/icon/png/wallet.png', t: '会员价', d: '标价基础上再减，商品详情页直接显示会员价' },
					{ ic: '/static/icon/png/ticket.png', t: '每月券包', d: '每月 1 号发放，满减券 3 张，不用抢' },
					{ ic: '/static/icon/png/headset.png', t: '售后优先', d: '坏果理赔走优先队列，一般 2 小时内处理完' },
					{ ic: '/static/icon/png/truck.png', t: '包邮门槛降低', d: '普通用户满 88 包邮，会员满 49 就包' }
				],
				levels: [
					{ n: 1, name: '普通会员', d: '注册就是，不用开卡' },
					{ n: 2, name: '白银会员', d: '开卡起步档，日常买水果够用' },
					{ n: 3, name: '黄金会员', d: '券包翻倍，包邮无门槛' },
					{ n: 4, name: '钻石会员', d: '专属客服，新品优先尝鲜' }
				]
			}
		},
		computed: {
			st() {
				return this.m.memberStatus || 'NONE'
			},
			levelText() {
				return { 1: '普通会员', 2: '白银会员', 3: '黄金会员', 4: '钻石会员' }[this.m.level || 1] || '普通会员'
			},
			badgeText() {
				return { NONE: '未开卡', ACTIVE: '生效中', EXPIRING: '即将到期', EXPIRED: '已过期' }[this.st] || '未开卡'
			},
			expireText() {
				if (this.st === 'NONE') return '还没有开通会员卡'
				if (this.st === 'EXPIRED') return '会员卡已于 ' + (this.m.memberExpireAt || '') + ' 到期'
				return '有效期至 ' + (this.m.memberExpireAt || '')
			},
			btnText() {
				if (this.st === 'ACTIVE' || this.st === 'EXPIRING') return '联系客服续费'
				if (this.st === 'EXPIRED') return '联系客服重新开卡'
				return '联系客服开卡'
			}
		},
		async onShow() {
			if (!uni.getStorageSync('token')) {
				this.m = {}
				this.nickname = ''
				return
			}
			try {
				const [me, member] = await Promise.all([
					api.get('/api/v1/users/me'),
					api.get('/api/v1/users/me/member')
				])
				this.nickname = me.nickname || ''
				this.m = member || {}
			} catch (e) {
				this.m = {}
			}
		},
		methods: {
			/** 分转元，整数不显示小数 */
			yuan(fen) {
				const n = (fen || 0) / 100
				return '¥' + (Number.isInteger(n) ? n : n.toFixed(2))
			},
			goService() {
				uni.navigateTo({ url: '/pages/service/service' })
			}
		}
	}
</script>

<style scoped>
	.page { min-height: 100vh; background: #f5f5f5; padding-bottom: 140rpx; }

	/* 会员卡 */
	.card-wrap { padding: 28rpx 24rpx 8rpx; background: linear-gradient(180deg, #eceef0 0%, #f5f5f5 100%); }
	.mcard {
		position: relative; overflow: hidden;
		border-radius: 28rpx; padding: 34rpx;
		background: linear-gradient(135deg, #3d2e1f 0%, #2a1f15 100%);
		box-shadow: 0 8rpx 28rpx rgba(42, 31, 21, 0.22);
	}
	.mcard.s-NONE, .mcard.s-EXPIRED {
		background: linear-gradient(135deg, #4a4f55 0%, #2f3438 100%);
		box-shadow: 0 8rpx 28rpx rgba(47, 52, 56, 0.2);
	}
	/* 右上角柔光，让卡片有质感而不是一块死色 */
	.mcard-glow {
		position: absolute; top: -80rpx; right: -60rpx;
		width: 260rpx; height: 260rpx; border-radius: 50%;
		background: rgba(245, 230, 200, 0.1);
	}
	.mcard-top { position: relative; display: flex; align-items: flex-start; justify-content: space-between; }
	.mcard-lv { display: flex; align-items: center; gap: 10rpx; }
	.crown { width: 36rpx; height: 36rpx; }
	.lv-txt { font-size: 34rpx; font-weight: 800; color: #f5e6c8; letter-spacing: 1rpx; }
	.mcard-name { display: block; margin-top: 10rpx; font-size: 24rpx; color: rgba(245, 230, 200, 0.62); }
	.mcard-badge {
		flex: none; font-size: 20rpx; color: #f5e6c8;
		background: rgba(245, 230, 200, 0.16);
		padding: 8rpx 18rpx; border-radius: 20rpx;
	}
	.mcard-bottom {
		position: relative; margin-top: 48rpx;
		display: flex; align-items: baseline; justify-content: space-between;
	}
	.mcard-exp { font-size: 24rpx; color: rgba(245, 230, 200, 0.72); }
	.mcard-days { font-size: 22rpx; color: #f5e6c8; font-weight: 600; }

	/* 消费概览 */
	.stat {
		display: flex; align-items: center;
		background: #fff; border-radius: 28rpx;
		margin: 20rpx 24rpx; padding: 32rpx 0;
	}
	.stat-it { flex: 1; display: flex; flex-direction: column; align-items: center; gap: 8rpx; }
	.stat-n { font-size: 34rpx; font-weight: 700; color: #222; }
	.stat-l { font-size: 22rpx; color: #a5aca1; }
	.stat-sp { width: 1rpx; height: 48rpx; background: #f0f2f4; }

	/* 通用卡片 */
	.card { background: #fff; border-radius: 28rpx; margin: 20rpx 24rpx; padding: 28rpx; }
	.card-hd { display: flex; align-items: center; justify-content: space-between; margin-bottom: 24rpx; }
	.card-title { font-size: 30rpx; font-weight: 700; color: #222; }
	.card-tip { font-size: 22rpx; color: #a5aca1; }

	/* 权益 */
	.ben { display: flex; align-items: flex-start; gap: 20rpx; padding: 20rpx 0; border-bottom: 1rpx solid #f7f9f5; }
	.ben:last-child { border-bottom: none; padding-bottom: 0; }
	.ben-ic {
		flex: none; width: 72rpx; height: 72rpx; border-radius: 20rpx;
		background: #f7f9f5; display: flex; align-items: center; justify-content: center;
	}
	.ben-img { width: 38rpx; height: 38rpx; }
	.ben-c { flex: 1; min-width: 0; }
	.ben-t { display: block; font-size: 28rpx; font-weight: 600; color: #222; }
	.ben-d { display: block; margin-top: 6rpx; font-size: 24rpx; color: #6b7280; line-height: 1.55; }

	/* 等级 */
	.lv { display: flex; align-items: center; gap: 16rpx; padding: 20rpx 0; border-bottom: 1rpx solid #f7f9f5; }
	.lv:last-of-type { border-bottom: none; }
	.lv-tag {
		flex: none; font-size: 22rpx; color: #6b7280; background: #f0f2f4;
		padding: 8rpx 18rpx; border-radius: 18rpx;
	}
	.lv-tag.cur { color: #17704a; background: #e6f4ec; font-weight: 600; }
	.lv-d { flex: 1; font-size: 25rpx; color: #6b7280; }
	.lv-cur { flex: none; font-size: 20rpx; color: #17704a; font-weight: 600; }
	.lv-note { display: block; margin-top: 18rpx; font-size: 22rpx; color: #a5aca1; }

	/* 开卡说明 */
	.how { display: block; font-size: 25rpx; color: #6b7280; line-height: 1.7; }
	.how-row {
		margin-top: 22rpx; padding-top: 22rpx; border-top: 1rpx solid #f7f9f5;
		display: flex; align-items: center; justify-content: space-between;
	}
	.how-k { font-size: 28rpx; color: #17704a; font-weight: 600; }
	.how-arrow { font-size: 32rpx; color: #17704a; }

	.foot-sp { height: 20rpx; }

	/* 底部按钮 */
	.fixed-btn {
		position: fixed; left: 0; right: 0; bottom: 0;
		padding: 20rpx 24rpx calc(20rpx + env(safe-area-inset-bottom));
		background: rgba(255, 255, 255, 0.96); border-top: 1rpx solid #f0f2f4;
	}
	.btn {
		text-align: center; line-height: 88rpx; height: 88rpx; border-radius: 44rpx;
		background: linear-gradient(90deg, #35b47e, #1f8a58); color: #fff;
		font-size: 30rpx; font-weight: 600;
	}
	.btn:active { opacity: 0.88; }
</style>
