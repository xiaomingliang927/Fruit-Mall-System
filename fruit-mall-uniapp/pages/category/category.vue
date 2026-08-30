<template>
	<view class="page">
		<view class="cat-wrap">
			<!-- 左侧分类 -->
			<scroll-view scroll-y class="cat-side">
				<view
					class="cat-item"
					:class="{ on: currentCat === index }"
					v-for="(cat, index) in CATS"
					:key="index"
					@click="switchCat(index)"
				>
					{{ cat }}
				</view>
			</scroll-view>
			<!-- 右侧商品 -->
			<scroll-view scroll-y class="cat-main">
				<view class="cat-banner">
					<image :src="IMG.banner" mode="aspectFill" class="cat-banner-img"></image>
				</view>
				<text class="cat-sub">{{ CATS[currentCat] }} · {{ productList.length }} 款</text>
				<view class="cat-list">
					<view class="product-item" v-for="p in productList" :key="p.id" @click="goDetail(p.id)">
						<view class="product-img">
							<image :src="p.img" mode="aspectFill" class="product-img-inner"></image>
							<text class="product-img-tag" v-if="getTag(p)">{{ getTag(p) }}</text>
						</view>
						<view class="product-bd">
							<view class="product-nm-row">
								<text class="product-nm">{{ p.name }}</text>
							</view>
							<text class="product-sp">{{ p.spec }} · 新鲜采摘 · 品质精选</text>
							<text class="product-meta product-meta-red">{{ getGoodRate(p) }} · 48小时内发货</text>
							<view class="product-btm">
								<view class="product-pr">
									<text class="product-price">¥{{ p.price }}</text>
									<text class="product-unit">{{ p.unit }}</text>
									<text class="product-orig">¥{{ getOrigPrice(p) }}</text>
								</view>
								<view class="product-add" @click.stop="addCart(p.id)">
									<text class="add-icon">+</text>
								</view>
							</view>
						</view>
					</view>
				</view>
				<view style="height: 40rpx;"></view>
			</scroll-view>
		</view>
	</view>
</template>

<script>
	import { IMG, CATS, getProductsByCat } from '@/utils/data.js'
	import { addToCart } from '@/utils/cart.js'

	export default {
		data() {
			return {
				IMG,
				CATS,
				currentCat: 0
			}
		},
		computed: {
			productList() {
				return getProductsByCat(this.CATS[this.currentCat])
			}
		},
		onShow() {
			if (typeof this.getTabBar === 'function' && this.getTabBar()) {
				this.getTabBar().selected = 1
			}
		},
		methods: {
			switchCat(index) {
				this.currentCat = index
			},
			getTag(p) {
				if (p.tag) return p.tag
				if (p.cat === '热带水果') return '进口'
				if (p.sales > 2000) return '热销'
				return ''
			},
			getTagClass(p) {
				const tag = this.getTag(p)
				if (['热卖', '热销'].includes(tag)) return 'hot'
				if (tag === '特价') return 'sale'
				if (tag === '新品') return 'new'
				if (tag === '进口') return 'import'
				return 'hot'
			},
			getOrigPrice(p) {
				return (p.price * 1.3).toFixed(1)
			},
			getGoodRate(p) {
				const n = Math.floor(p.sales / 100)
				return n > 0 ? '超' + n + '人好评' : '品质精选'
			},
			goDetail(id) {
				uni.navigateTo({ url: '/pages/detail/detail?id=' + id })
			},
			addCart(id) {
				addToCart(id)
				if (typeof this.getTabBar === 'function' && this.getTabBar()) {
					this.getTabBar().refreshCartCount()
				}
				uni.showToast({ title: '已加入购物车', icon: 'none' })
			}
		}
	}
</script>

<style scoped>
	.page {
		height: 100vh;
		background: #f7f6f1;
	}
	.cat-wrap {
		display: flex;
		height: 100%;
		padding-bottom: 120rpx;
	}
	.cat-side {
		width: 184rpx;
		flex-shrink: 0;
		background: #efeee7;
		height: 100%;
	}
	.cat-item {
		padding: 30rpx 16rpx;
		font-size: 26rpx;
		color: #6b7269;
		text-align: center;
		position: relative;
	}
	.cat-item.on {
		background: #fff;
		color: #17704a;
		font-weight: 700;
	}
	.cat-item.on::before {
		content: '';
		position: absolute;
		left: 0;
		top: 50%;
		transform: translateY(-50%);
		width: 6rpx;
		height: 36rpx;
		background: #2e9e6b;
		border-radius: 0 6rpx 6rpx 0;
	}
	.cat-main {
		flex: 1;
		padding: 24rpx;
		height: 100%;
	}
	.cat-banner {
		border-radius: 24rpx;
		overflow: hidden;
		height: 184rpx;
		margin-bottom: 24rpx;
	}
	.cat-banner-img {
		width: 100%;
		height: 100%;
	}
	.cat-sub {
		font-size: 25rpx;
		color: #6b7269;
		font-weight: 600;
		margin: 4rpx 0 20rpx;
		display: block;
	}
	.cat-list {
		display: flex;
		flex-direction: column;
		gap: 24rpx;
	}
	.product-item {
		display: flex;
		background: #fff;
		border-radius: 24rpx;
		overflow: hidden;
		box-shadow: 0 4rpx 20rpx rgba(0, 0, 0, 0.06);
	}
	.product-img {
		width: 168rpx;
		height: 168rpx;
		flex-shrink: 0;
		position: relative;
	}
	.product-img-inner {
		width: 100%;
		height: 100%;
	}
	.product-img-tag {
		position: absolute;
		left: 0;
		top: 0;
		background: linear-gradient(135deg, #43c98a, #1f8a58);
		color: #fff;
		font-size: 18rpx;
		padding: 4rpx 14rpx;
		border-radius: 0 0 12rpx 0;
		font-weight: 600;
	}
	.product-bd {
		flex: 1;
		padding: 20rpx 24rpx;
		display: flex;
		flex-direction: column;
		justify-content: center;
	}
	.product-nm-row {
		display: flex;
		align-items: center;
		gap: 10rpx;
	}
	.product-nm {
		font-size: 27rpx;
		font-weight: 600;
		color: #222;
		flex: 1;
		overflow: hidden;
		white-space: nowrap;
		text-overflow: ellipsis;
	}
	.product-tag {
		font-size: 18rpx;
		padding: 4rpx 12rpx;
		border-radius: 8rpx;
		font-weight: 600;
		flex-shrink: 0;
	}
	.tag-hot { background: #f24e3e; color: #fff; }
	.tag-sale { background: #f26b21; color: #fff; }
	.tag-new { background: #17704a; color: #fff; }
	.tag-import { background: #7c3aed; color: #fff; }
	.product-sp {
		font-size: 22rpx;
		color: #a5aca1;
		margin-top: 6rpx;
		display: block;
	}
	.product-meta {
		font-size: 20rpx;
		color: #bbb;
		margin-top: 6rpx;
		display: block;
	}
	.product-meta-red {
		color: #f24e3e;
		font-weight: 600;
	}
	.product-btm {
		display: flex;
		align-items: center;
		justify-content: space-between;
		margin-top: 12rpx;
	}
	.product-pr {
		display: flex;
		align-items: baseline;
		gap: 6rpx;
	}
	.product-pr-label {
		font-size: 18rpx;
		background: #f24e3e;
		color: #fff;
		padding: 2rpx 10rpx;
		border-radius: 6rpx;
		margin-right: 4rpx;
		font-weight: 600;
	}
	.product-price {
		color: #f24e3e;
		font-size: 30rpx;
		font-weight: 700;
	}
	.product-unit {
		font-size: 20rpx;
		color: #a5aca1;
	}
	.product-orig {
		font-size: 20rpx;
		color: #ccc;
		text-decoration: line-through;
	}
	.product-add {
		width: 56rpx;
		height: 56rpx;
		border-radius: 50%;
		background: #2e9e6b;
		color: #fff;
		display: flex;
		align-items: center;
		justify-content: center;
	}
	.add-icon {
		font-size: 32rpx;
		line-height: 1;
		font-weight: 300;
	}
</style>
