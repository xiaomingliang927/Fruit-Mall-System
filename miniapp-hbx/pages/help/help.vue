<template>
	<view class="page">
		<!-- 分类切换 -->
		<view class="tabs">
			<scroll-view scroll-x class="tabs-sc" :show-scrollbar="false">
				<view class="tabs-row">
					<view class="tab" v-for="c in cats" :key="c.k" :class="{ on: cat === c.k }" @click="switchCat(c.k)">
						{{ c.n }}
					</view>
				</view>
			</scroll-view>
		</view>

		<!-- 问答 -->
		<view class="card">
			<view class="qa" v-for="q in list" :key="q.q">
				<view class="q" @click="toggle(q.q)">
					<text class="q-t">{{ q.q }}</text>
					<text class="q-arrow" :class="{ open: open === q.q }">›</text>
				</view>
				<view v-if="open === q.q" class="a">
					<text class="a-t">{{ q.a }}</text>
					<view v-if="q.jump" class="a-jump" @click="onJump(q.jump)">
						<text class="a-jump-k">{{ q.jumpText }}</text>
						<text class="a-jump-arrow">›</text>
					</view>
				</view>
			</view>
		</view>

		<!-- 兜底 -->
		<view class="fallback">
			<text class="fb-t">没找到想问的？</text>
			<text class="fb-d">客服在线时间 09:00 - 22:00，说清订单号会快很多。</text>
			<view class="fb-btn" @click="goService">联系人工客服</view>
		</view>

		<view style="height: 40rpx;"></view>
	</view>
</template>

<script>
	export default {
		data() {
			return {
				cat: 'ship',
				open: '',
				cats: [
					{ k: 'ship', n: '配送' },
					{ k: 'refund', n: '售后' },
					{ k: 'pay', n: '支付优惠' },
					{ k: 'account', n: '账号' },
					{ k: 'member', n: '会员' }
				],
				// 答案写具体规则和操作路径，不写"请联系客服处理"这种废话
				faqs: {
					ship: [
						{
							q: '什么时候发货？',
							a: '每天 14:00 截单，之前付款的当天发，之后的第二天发。生鲜怕压怕热，我们尽量赶早上的车。遇到暴雨台风会延一天，会提前在订单里说明。'
						},
						{
							q: '几天能到？',
							a: '省内次日达，周边省份 2 天，偏远地区 3 到 4 天。发出后订单里能看到运单号，长按复制去快递官网查最准。',
							jump: 'orders', jumpText: '去看我的订单'
						},
						{
							q: '怎么保证不坏在路上？',
							a: '泡沫箱加冰袋，夏天多放两个。易压的桃子李子单个套网套。真坏了走坏果包赔，不用跟我们扯运输责任。'
						},
						{
							q: '能指定送达时间吗？',
							a: '暂时不行，快递上门时间我们控制不了。可以在下单备注里写「放快递柜」或者「送到门口」，快递员一般会看。'
						},
						{
							q: '地址写错了怎么办？',
							a: '没发货就自己改：进「收货地址」改好，然后联系客服说一下订单号。已经发货了改不了，只能等快递到了自己去取。',
							jump: 'address', jumpText: '去管理收货地址'
						}
					],
					refund: [
						{
							q: '水果坏了怎么赔？',
							a: '进「我的订单」找到那笔单，点申请售后，拍张能看清坏的样子的照片传上去。金额在 50 元以内系统直接通过，钱退回付款账户。超过 50 元人工看一眼，一般当天有结果。坏果不用寄回来。',
							jump: 'orders', jumpText: '去申请售后'
						},
						{
							q: '照片要怎么拍？',
							a: '把坏的部分拍清楚就行，别只拍箱子。如果是一整箱都不行，拍个全景更好说明问题。最多传 3 张。'
						},
						{
							q: '退款多久到账？',
							a: '审核通过后我们立刻发起退款，微信支付一般几分钟内到，慢的话最多等到第二天。银行卡渠道可能要 1 到 3 个工作日。'
						},
						{
							q: '申请错了能撤销吗？',
							a: '能。人工还没审的时候，在售后详情里点撤销就行，订单会恢复原来的状态。已经审完的撤不了。'
						},
						{
							q: '不想要了能退货吗？',
							a: '生鲜不支持无理由退货，这是行业惯例，二次销售确实做不到。但如果是品质问题、发错货、少发了，一律照赔。'
						}
					],
					pay: [
						{
							q: '支持什么支付方式？',
							a: '目前只有微信支付。当前版本是模拟支付流程，等营业执照和商户号办下来会切成真实的微信支付。'
						},
						{
							q: '优惠券怎么用？',
							a: '结算页面会自动列出这笔单能用的券，点一下选中就抵扣了。一笔订单只能用一张，不叠加，不找零。没到门槛的券会显示成灰的。',
							jump: 'coupon', jumpText: '去领券中心'
						},
						{
							q: '券过期了能补吗？',
							a: '补不了，系统到点自动失效。建议领了就在「我的券」里看一眼有效期，快到期的先用掉。'
						},
						{
							q: '取消订单券会退回来吗？',
							a: '会。订单取消或者退款成功后，用掉的券自动退回账户，有效期不变——所以如果券马上到期，退回来也可能就直接过期了。'
						},
						{
							q: '包邮门槛是多少？',
							a: '普通用户满 88 包邮，会员满 49。不够门槛按实际运费算，结算页会写清楚。'
						}
					],
					account: [
						{
							q: '怎么登录？',
							a: '手机号收验证码登录，或者微信一键授权。两种方式认的是同一个账号——同一个手机号在网站和小程序登进去，购物车和订单都是通的。'
						},
						{
							q: '收不到验证码？',
							a: '先看一下是不是被拦在垃圾短信里了。同一个号码 60 秒内只能发一次，一小时最多 10 次。还是收不到就打客服电话，人工核一下。'
						},
						{
							q: '能改昵称和头像吗？',
							a: '进「个人资料」改昵称。头像目前跟着微信授权来，不单独改。手机号绑定后不能自己换，要换联系客服。',
							jump: 'profile', jumpText: '去个人资料'
						},
						{
							q: '想注销账号',
							a: '联系客服说明要注销，我们会核对身份后处理。注销后订单记录、优惠券、收藏都会清掉，找不回来。'
						}
					],
					member: [
						{
							q: '会员卡怎么买？',
							a: '现在还没做线上自助购买，卡是客服人工开的。想开跟客服说一声，当场就能办。等自助购买上线了会在这里更新。',
							jump: 'vip', jumpText: '去看会员中心'
						},
						{
							q: '会员能省什么？',
							a: '商品有会员价，每月 1 号发 3 张满减券，包邮门槛从 88 降到 49，坏果理赔走优先队列。会员中心里列得更细。'
						},
						{
							q: '到期了会怎样？',
							a: '到期后会员价和券包停掉，账号和历史订单都还在。续费的话等级不会掉，还是原来那一档。'
						},
						{
							q: '没到期能提前续费吗？',
							a: '能，而且划算——续费是从原到期日往后加，不会浪费剩下的天数。'
						}
					]
				},
				jumpRoutes: {
					orders: { url: '/pages/orders/orders', needLogin: true },
					address: { url: '/pages/address/address', needLogin: true },
					profile: { url: '/pages/profile/profile', needLogin: true },
					coupon: { url: '/pages/coupon/coupon' },
					vip: { url: '/pages/vip/vip' }
				}
			}
		},
		computed: {
			list() {
				return this.faqs[this.cat] || []
			}
		},
		methods: {
			switchCat(k) {
				this.cat = k
				this.open = ''
			},
			toggle(q) {
				this.open = this.open === q ? '' : q
			},
			onJump(key) {
				const r = this.jumpRoutes[key]
				if (!r) return
				if (r.needLogin && !uni.getStorageSync('token')) {
					return uni.navigateTo({ url: '/pages/login/login' })
				}
				uni.navigateTo({ url: r.url })
			},
			goService() {
				uni.navigateTo({ url: '/pages/service/service' })
			}
		}
	}
</script>

<style scoped>
	.page { min-height: 100vh; background: #f5f5f5; }

	/* 分类 */
	.tabs { background: #fff; padding: 20rpx 0 4rpx; }
	.tabs-sc { white-space: nowrap; }
	.tabs-row { display: inline-flex; gap: 16rpx; padding: 0 24rpx 16rpx; }
	.tab {
		flex: none; font-size: 27rpx; color: #6b7280;
		background: #f5f7f4; padding: 14rpx 32rpx; border-radius: 30rpx;
	}
	.tab.on { color: #fff; background: #17704a; font-weight: 600; }

	/* 问答 */
	.card { background: #fff; border-radius: 28rpx; margin: 20rpx 24rpx; padding: 8rpx 28rpx; }
	.qa { border-bottom: 1rpx solid #f7f9f5; }
	.qa:last-child { border-bottom: none; }
	.q { display: flex; align-items: center; gap: 16rpx; padding: 28rpx 0; }
	.q-t { flex: 1; font-size: 28rpx; color: #222; font-weight: 500; line-height: 1.5; }
	.q-arrow {
		flex: none; font-size: 30rpx; color: #c2c8bf;
		transform: rotate(90deg); transition: transform 0.2s;
	}
	.q-arrow.open { transform: rotate(-90deg); color: #17704a; }
	.a { padding: 0 0 26rpx; }
	.a-t { display: block; font-size: 25rpx; color: #6b7280; line-height: 1.75; }
	.a-jump {
		margin-top: 18rpx; display: inline-flex; align-items: center; gap: 6rpx;
		background: #e6f4ec; padding: 12rpx 24rpx; border-radius: 24rpx;
	}
	.a-jump-k { font-size: 24rpx; color: #17704a; font-weight: 600; }
	.a-jump-arrow { font-size: 26rpx; color: #17704a; }

	/* 兜底 */
	.fallback {
		background: #fff; border-radius: 28rpx; margin: 20rpx 24rpx;
		padding: 36rpx 28rpx; text-align: center;
	}
	.fb-t { display: block; font-size: 29rpx; font-weight: 700; color: #222; }
	.fb-d { display: block; margin-top: 10rpx; font-size: 23rpx; color: #a5aca1; line-height: 1.6; }
	.fb-btn {
		margin: 26rpx auto 0; width: 320rpx; line-height: 78rpx; height: 78rpx;
		border-radius: 40rpx; background: #e6f4ec; color: #17704a;
		font-size: 28rpx; font-weight: 600;
	}
	.fb-btn:active { background: #d8ecdf; }
</style>
