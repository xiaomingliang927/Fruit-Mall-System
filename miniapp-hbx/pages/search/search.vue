<template>
	<view class="page">
		<!-- 自定义导航：搜索框 + 返回 -->
		<view class="nav" :style="{ paddingTop: statusBarHeight + 'px' }">
			<view class="nav-inner">
				<view class="back" @click="goBack"><text class="back-ico">‹</text></view>
				<view class="input-wrap">
					<text class="s-ico">🔍</text>
					<input
						class="input"
						v-model="keyword"
						:focus="autoFocus"
						:placeholder="placeholder"
						placeholder-class="ph"
						confirm-type="search"
						maxlength="30"
						@confirm="submit"
						@input="onInput"
					/>
					<text v-if="keyword" class="clear" @click="clearInput">×</text>
				</view>
				<text class="btn" @click="submit">搜索</text>
			</view>
		</view>

		<!-- 未搜索：历史 + 热词 -->
		<view v-if="!searched" class="panel">
			<view v-if="history.length" class="blk">
				<view class="blk-hd">
					<text class="blk-t">历史搜索</text>
					<text class="blk-op" @click="clearHistory">清空</text>
				</view>
				<view class="chips">
					<text class="chip" v-for="(h, i) in history" :key="i" @click="pick(h)">{{ h }}</text>
				</view>
			</view>

			<view class="blk">
				<view class="blk-hd"><text class="blk-t">热门搜索</text></view>
				<view class="chips">
					<text
						class="chip"
						:class="{ hot: i < 3 }"
						v-for="(h, i) in hotWords"
						:key="h"
						@click="pick(h)"
					>{{ h }}</text>
				</view>
			</view>
		</view>

		<!-- 搜索结果 -->
		<view v-else class="result">
			<view class="res-bar">
				<text class="res-cnt">找到 {{ total }} 件「{{ lastKeyword }}」相关商品</text>
			</view>

			<view v-if="loading && list.length === 0" class="tip">搜索中…</view>

			<view v-else-if="list.length === 0" class="empty">
				<text class="empty-ico">🍃</text>
				<text class="empty-t">没有找到相关商品</text>
				<text class="empty-d">换个关键词试试，比如「车厘子」「进口」「礼盒」</text>
			</view>

			<view v-else class="list">
				<view class="p-item" v-for="p in list" :key="p.id" @click="goDetail(p.id)">
					<view class="p-img">
						<image :src="p.img" mode="aspectFill" class="p-img-inner"></image>
						<text class="p-tag" v-if="p.tag">{{ p.tag }}</text>
					</view>
					<view class="p-bd">
						<view class="p-nm-row">
							<text class="p-nm">{{ p.name }}</text>
						</view>
						<text class="p-sp">{{ p.spec || '新鲜采摘 · 品质精选' }}</text>
						<text class="p-meta" v-if="p.sales">已售 {{ p.sales }}</text>
						<view class="p-btm">
							<view class="p-pr">
								<text class="p-price">¥{{ p.price }}</text>
								<text class="p-unit">{{ p.unit }}</text>
							</view>
							<view class="p-add" @click.stop="addCart(p)"><text class="add-ico">+</text></view>
						</view>
					</view>
				</view>

				<view v-if="loadingMore" class="tip">加载中…</view>
				<view v-else-if="!hasMore" class="tip tip-end">— 没有更多了 —</view>
			</view>
		</view>
	</view>
</template>

<script>
	import { api, imgUrl } from '@/api'
	import { mergeProducts } from '@/utils/data.js'
	import { addToCart, getCartCounts, updateCartBadge } from '@/utils/cart.js'

	const HISTORY_KEY = 'fruit_mall_search_history'
	const HISTORY_MAX = 10
	const PAGE_SIZE = 20

	export default {
		data() {
			return {
				statusBarHeight: 20,
				keyword: '',
				lastKeyword: '',
				placeholder: '搜索水果名称 / 产地',
				autoFocus: true,
				searched: false,
				loading: false,
				loadingMore: false,
				list: [],
				total: 0,
				page: 1,
				hasMore: false,
				history: [],
				hotWords: ['车厘子', '草莓', '芒果', '蓝莓', '进口', '礼盒', '西瓜', '橙子'],
				timer: null,
				reqSeq: 0
			}
		},
		onLoad(options) {
			const sys = uni.getSystemInfoSync()
			this.statusBarHeight = sys.statusBarHeight || 20
			this.history = uni.getStorageSync(HISTORY_KEY) || []
			// 首页/分类页带 keyword 跳转过来时直接搜
			if (options && options.keyword) {
				this.keyword = decodeURIComponent(options.keyword)
				this.submit()
			} else if (options && options.placeholder) {
				this.placeholder = decodeURIComponent(options.placeholder)
			}
		},
		onUnload() {
			if (this.timer) clearTimeout(this.timer)
		},
		onReachBottom() {
			if (this.searched && this.hasMore && !this.loadingMore) this.loadMore()
		},
		methods: {
			/** 输入时防抖自动搜索，避免每敲一个字就打一次接口 */
			onInput() {
				if (this.timer) clearTimeout(this.timer)
				const kw = this.keyword.trim()
				if (!kw) {
					this.resetResult()
					return
				}
				this.timer = setTimeout(() => this.search(1), 300)
			},
			submit() {
				const kw = this.keyword.trim()
				if (!kw) {
					uni.showToast({ title: '请输入搜索关键词', icon: 'none' })
					return
				}
				this.saveHistory(kw)
				this.search(1)
			},
			pick(word) {
				this.keyword = word
				this.saveHistory(word)
				this.search(1)
			},
			clearInput() {
				this.keyword = ''
				this.resetResult()
				this.autoFocus = true
			},
			resetResult() {
				this.searched = false
				this.list = []
				this.total = 0
				this.page = 1
				this.hasMore = false
			},
			async search(page) {
				const kw = this.keyword.trim()
				if (!kw) return
				const seq = ++this.reqSeq
				if (page === 1) {
					this.loading = true
					this.searched = true
				}
				this.lastKeyword = kw
				try {
					const data = await api.get(
						`/api/v1/products?page=${page}&size=${PAGE_SIZE}&keyword=${encodeURIComponent(kw)}`
					)
					// 丢弃过期请求：快速改词时旧响应晚到会覆盖新结果
					if (seq !== this.reqSeq) return
					const records = (data && data.records) || []
					const items = mergeProducts(records)
					this.total = (data && data.total) || 0
					this.page = page
					this.list = page === 1 ? items : this.list.concat(items)
					this.hasMore = this.list.length < this.total
				} catch (e) {
					if (seq !== this.reqSeq) return
					if (page === 1) {
						this.list = []
						this.total = 0
					}
					uni.showToast({ title: e.message || '搜索失败', icon: 'none' })
				} finally {
					if (seq === this.reqSeq) this.loading = false
				}
			},
			async loadMore() {
				this.loadingMore = true
				await this.search(this.page + 1)
				this.loadingMore = false
			},
			saveHistory(word) {
				const w = String(word).trim()
				if (!w) return
				const next = [w, ...this.history.filter((x) => x !== w)].slice(0, HISTORY_MAX)
				this.history = next
				uni.setStorageSync(HISTORY_KEY, next)
			},
			clearHistory() {
				uni.showModal({
					title: '清空历史搜索',
					content: '确认删除全部历史搜索记录？',
					success: (r) => {
						if (!r.confirm) return
						this.history = []
						uni.removeStorageSync(HISTORY_KEY)
					}
				})
			},
			addCart(p) {
				if (!p) return
				// 搜索结果可能不在首页缓存里，显式带上 skuId/价格，保证购物车快照完整
				addToCart(p.id, 1, { skuId: p.skuId, spec: p.spec, price: p.price })
				this.cartCounts = getCartCounts()
				updateCartBadge()
				uni.showToast({ title: '已加入购物车', icon: 'none' })
			},
			goDetail(id) {
				uni.navigateTo({ url: '/pages/detail/detail?id=' + id })
			},
			goBack() {
				const pages = getCurrentPages()
				if (pages.length > 1) uni.navigateBack()
				else uni.switchTab({ url: '/pages/index/index' })
			}
		}
	}
</script>

<style scoped>
	.page { min-height: 100vh; background: #f5f5f5; }

	/* 自定义导航 */
	.nav { background: #fff; padding-bottom: 16rpx; border-bottom: 1rpx solid #f0f0f0; position: sticky; top: 0; z-index: 20; }
	.nav-inner { display: flex; align-items: center; gap: 12rpx; padding: 0 20rpx; height: 88rpx; }
	.back { width: 56rpx; height: 56rpx; display: flex; align-items: center; justify-content: center; flex: none; }
	.back-ico { font-size: 46rpx; color: #333; line-height: 1; margin-top: -6rpx; }
	.input-wrap {
		flex: 1; display: flex; align-items: center; gap: 10rpx;
		background: #f4f5f3; border-radius: 36rpx; padding: 0 22rpx; height: 68rpx;
	}
	.s-ico { font-size: 24rpx; flex: none; }
	.input { flex: 1; font-size: 27rpx; color: #222; height: 68rpx; }
	.ph { color: #b3b8b3; }
	.clear { flex: none; font-size: 34rpx; color: #bbb; padding: 0 6rpx; line-height: 1; }
	.btn { flex: none; font-size: 27rpx; color: #17704a; font-weight: 600; padding: 0 6rpx; }

	/* 历史 / 热词 */
	.panel { padding: 8rpx 0; }
	.blk { background: #fff; margin: 20rpx 24rpx; border-radius: 24rpx; padding: 26rpx 28rpx 28rpx; }
	.blk-hd { display: flex; align-items: center; justify-content: space-between; margin-bottom: 6rpx; }
	.blk-t { font-size: 27rpx; font-weight: 700; color: #222; }
	.blk-op { font-size: 24rpx; color: #a5aca1; }
	.chips { display: flex; flex-wrap: wrap; gap: 16rpx; margin-top: 16rpx; }
	.chip {
		background: #f4f5f3; color: #55594f; font-size: 25rpx;
		padding: 12rpx 26rpx; border-radius: 30rpx;
	}
	.chip.hot { background: #fff2ee; color: #f24e3e; font-weight: 600; }
	.chip:active { opacity: .7; }

	/* 结果 */
	.result { padding: 0 24rpx; }
	.res-bar { padding: 20rpx 4rpx 12rpx; }
	.res-cnt { font-size: 24rpx; color: #8b9186; }
	.tip { text-align: center; font-size: 25rpx; color: #a5aca1; padding: 40rpx 0; }
	.tip-end { font-size: 23rpx; }
	.empty { display: flex; flex-direction: column; align-items: center; padding: 120rpx 40rpx; }
	.empty-ico { font-size: 90rpx; margin-bottom: 24rpx; }
	.empty-t { font-size: 29rpx; color: #55594f; font-weight: 600; }
	.empty-d { font-size: 24rpx; color: #a5aca1; margin-top: 12rpx; text-align: center; line-height: 1.6; }

	.list { display: flex; flex-direction: column; gap: 20rpx; padding-bottom: 40rpx; }
	.p-item { display: flex; background: #fff; border-radius: 24rpx; overflow: hidden; }
	.p-img { width: 180rpx; height: 180rpx; flex: none; position: relative; background: #fafafa; }
	.p-img-inner { width: 100%; height: 100%; }
	.p-tag {
		position: absolute; left: 0; top: 0;
		background: linear-gradient(135deg, #43c98a, #1f8a58); color: #fff;
		font-size: 18rpx; padding: 4rpx 14rpx; border-radius: 0 0 12rpx 0; font-weight: 600;
	}
	.p-bd { flex: 1; padding: 20rpx 24rpx; display: flex; flex-direction: column; justify-content: center; }
	.p-nm-row { display: flex; align-items: center; }
	.p-nm { font-size: 28rpx; font-weight: 600; color: #222; flex: 1; overflow: hidden; white-space: nowrap; text-overflow: ellipsis; }
	.p-sp { font-size: 22rpx; color: #a5aca1; margin-top: 8rpx; display: block; overflow: hidden; white-space: nowrap; text-overflow: ellipsis; }
	.p-meta { font-size: 20rpx; color: #f24e3e; margin-top: 8rpx; display: block; }
	.p-btm { display: flex; align-items: center; justify-content: space-between; margin-top: 14rpx; }
	.p-pr { display: flex; align-items: baseline; gap: 4rpx; }
	.p-price { color: #f24e3e; font-size: 32rpx; font-weight: 700; }
	.p-unit { font-size: 20rpx; color: #a5aca1; }
	.p-add {
		width: 56rpx; height: 56rpx; border-radius: 50%;
		background: #2e9e6b; color: #fff;
		display: flex; align-items: center; justify-content: center;
	}
	.add-ico { font-size: 32rpx; line-height: 1; font-weight: 300; }
</style>
