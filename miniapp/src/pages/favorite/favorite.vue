<template>
	<view class="page">
		<view class="hd">
			<text class="hd-title">我的收藏</text>
			<text class="hd-count" v-if="list.length">{{ list.length }} 件</text>
		</view>

		<view v-if="!list.length && !loading" class="empty">
			<view class="empty-icon">♡</view>
			<view>还没有收藏的商品</view>
			<view class="empty-sub" @click="goShop">去逛逛 ›</view>
		</view>

		<view v-for="f in list" :key="f.favoriteId" class="fav-card" @click="goDetail(f)">
			<image class="fav-img" :src="imgUrl(f.mainImage)" mode="aspectFill"></image>
			<view class="fav-info">
				<text class="fav-name">{{ f.name }}</text>
				<text class="fav-sub" v-if="f.subtitle">{{ f.subtitle }}</text>
				<view class="fav-bottom">
					<text class="fav-price">¥{{ yuan(f.minPrice) }}</text>
					<text class="fav-sales">已售 {{ f.sales || 0 }}</text>
				</view>
				<text v-if="f.status === 0" class="fav-off">已下架</text>
			</view>
			<view class="fav-cancel" @click.stop="cancel(f)">取消收藏</view>
		</view>
	</view>
</template>

<script>
	import { api, imgUrl, yuan } from '../../api'

	export default {
		data() {
			return {
				list: [],
				loading: true
			}
		},
		onShow() {
			if (!uni.getStorageSync('token')) {
				return uni.navigateTo({ url: '/pages/login/login' })
			}
			this.load()
		},
		methods: {
			imgUrl,
			yuan,
			async load() {
				try {
					const res = await api.get('/api/v1/favorites?page=1&size=50')
					this.list = (res && res.records) || []
				} catch (e) {
					uni.showToast({ title: e.message, icon: 'none' })
				} finally {
					this.loading = false
				}
			},
			goDetail(f) {
				if (f.status === 0) return uni.showToast({ title: '商品已下架', icon: 'none' })
				uni.navigateTo({ url: '/pages/detail/detail?id=' + f.productId })
			},
			async cancel(f) {
				try {
					await api.delete(`/api/v1/favorites/${f.productId}`)
					uni.showToast({ title: '已取消收藏', icon: 'none' })
					this.list = this.list.filter((x) => x.favoriteId !== f.favoriteId)
				} catch (e) {
					uni.showToast({ title: e.message, icon: 'none' })
				}
			},
			goShop() {
				uni.switchTab({ url: '/pages/index/index' })
			}
		}
	}
</script>

<style scoped>
	.page { min-height: 100vh; background: #f5f5f5; padding-bottom: 40rpx; }
	.hd {
		display: flex; align-items: baseline; gap: 16rpx;
		padding: 28rpx 32rpx; background: #fff;
	}
	.hd-title { font-size: 34rpx; font-weight: 700; color: #222; }
	.hd-count { font-size: 24rpx; color: #a5aca1; }
	.empty { text-align: center; color: #a5aca1; padding-top: 160rpx; font-size: 26rpx; }
	.empty-icon { font-size: 80rpx; color: #d8ddd4; margin-bottom: 16rpx; }
	.empty-sub { margin-top: 16rpx; color: #17704a; font-weight: 600; }
	.fav-card {
		display: flex; align-items: center; background: #fff;
		margin: 20rpx 24rpx 0; border-radius: 24rpx; padding: 24rpx; gap: 24rpx;
	}
	.fav-img {
		width: 140rpx; height: 140rpx; border-radius: 16rpx; flex: none;
		background: #f3f5f2;
	}
	.fav-info { flex: 1; min-width: 0; }
	.fav-name {
		font-size: 28rpx; font-weight: 600; color: #222; display: block;
		overflow: hidden; text-overflow: ellipsis; white-space: nowrap;
	}
	.fav-sub {
		font-size: 22rpx; color: #999; display: block; margin-top: 6rpx;
		overflow: hidden; text-overflow: ellipsis; white-space: nowrap;
	}
	.fav-bottom { display: flex; align-items: baseline; gap: 16rpx; margin-top: 14rpx; }
	.fav-price { font-size: 30rpx; font-weight: 700; color: #e54d42; }
	.fav-sales { font-size: 20rpx; color: #a5aca1; }
	.fav-off {
		display: inline-block; margin-top: 10rpx; font-size: 20rpx;
		color: #a5aca1; background: #f0f2f4; padding: 4rpx 12rpx; border-radius: 12rpx;
	}
	.fav-cancel {
		flex: none; align-self: center; font-size: 24rpx; color: #e54d42;
		background: #fff5f4; border: 2rpx solid #f3d6d2;
		padding: 12rpx 22rpx; border-radius: 30rpx;
	}
</style>
