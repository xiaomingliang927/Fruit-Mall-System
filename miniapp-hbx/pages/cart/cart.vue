<template>
	<view class="page">
		<!-- 满减提示 -->
		<view class="cart-hint" v-if="cartList.length > 0">
			<text>满 39 元免配送费，本单还需 {{ remainAmount }} 元</text>
		</view>

		<!-- 购物车列表 -->
		<view class="cart-list" v-if="cartList.length > 0">
			<view class="cart-item" v-for="item in cartList" :key="item.id">
				<view class="chk" :class="{ on: selected[item.id] }" @click="toggleSel(item.id)">
					<text v-if="selected[item.id]" class="chk-icon">✓</text>
				</view>
				<view class="cart-img" @click="goDetail(item.id)">
					<image :src="item.img" mode="aspectFill" class="cart-img-inner"></image>
				</view>
				<view class="cart-bd">
					<text class="cart-nm">{{ item.name }}</text>
					<text class="cart-sp">{{ item.spec }}</text>
					<view class="cart-btm">
						<view class="cart-pr">
							<text class="cart-price">¥{{ item.price }}</text>
						</view>
						<view class="step">
							<view class="step-btn" @click="dec(item.id)">
								<text class="step-icon">−</text>
							</view>
							<text class="step-num">{{ item.qty }}</text>
							<view class="step-btn step-add" @click="inc(item.id)">
								<text class="step-icon">+</text>
							</view>
						</view>
					</view>
				</view>
			</view>
		</view>

		<!-- 空状态 -->
		<view class="cart-empty" v-else>
			<text class="empty-icon">🛒</text>
			<text class="empty-text">购物车还是空的</text>
			<view class="empty-btn" @click="goCategory">去逛逛</view>
		</view>

		<!-- 底部结算栏 -->
		<view class="settle-bar" v-if="cartList.length > 0">
			<view class="settle-all" @click="toggleAll">
				<view class="chk" :class="{ on: allSelected }">
					<text v-if="allSelected" class="chk-icon">✓</text>
				</view>
				<text class="settle-text">全选</text>
			</view>
			<view class="settle-total">
				<text class="total-label">合计：</text>
				<text class="total-price">¥{{ totalPrice }}</text>
			</view>
			<view class="settle-btn" @click="checkout">
				<text>去结算({{ selectedCount }})</text>
			</view>
		</view>
	</view>
</template>

<script>
	import { getCart, getSelected, incQty, decQty, toggleSelect, toggleSelectAll, isAllSelected, getCartTotal, getCartCount } from '@/utils/cart.js'
	import { PRODUCTS } from '@/utils/data.js'

	export default {
		data() {
			return {
				cart: {},
				selected: {}
			}
		},
		computed: {
			cartList() {
				return PRODUCTS.filter(p => this.cart[p.id]).map(p => ({
					...p,
					qty: this.cart[p.id]
				}))
			},
			allSelected() {
				const ids = Object.keys(this.cart)
				return ids.length > 0 && ids.every(id => this.selected[id])
			},
			selectedCount() {
				return this.cartList.filter(p => this.selected[p.id]).reduce((s, p) => s + p.qty, 0)
			},
			totalPrice() {
				return this.cartList
					.filter(p => this.selected[p.id])
					.reduce((s, p) => s + p.price * p.qty, 0)
					.toFixed(2)
			},
			remainAmount() {
				const t = Number(this.totalPrice)
				return t >= 39 ? '0' : (39 - t).toFixed(2)
			}
		},
		onShow() {
			this.refresh()
			if (typeof this.getTabBar === 'function' && this.getTabBar()) {
				this.getTabBar().selected = 2
				this.getTabBar().refreshCartCount()
			}
		},
		methods: {
			refresh() {
				this.cart = getCart()
				this.selected = getSelected()
			},
			toggleSel(id) {
				this.selected = toggleSelect(id)
			},
			toggleAll() {
				this.selected = toggleSelectAll()
			},
			inc(id) {
				this.cart = incQty(id)
				this.refreshTabBar()
			},
			dec(id) {
				this.cart = decQty(id)
				this.refreshTabBar()
			},
			refreshTabBar() {
				if (typeof this.getTabBar === 'function' && this.getTabBar()) {
					this.getTabBar().refreshCartCount()
				}
			},
			goDetail(id) {
				uni.navigateTo({ url: '/pages/detail/detail?id=' + id })
			},
			goCategory() {
				uni.switchTab({ url: '/pages/category/category' })
			},
			checkout() {
				if (this.selectedCount === 0) {
					uni.showToast({ title: '请选择商品', icon: 'none' })
					return
				}
				uni.showToast({ title: '结算功能开发中', icon: 'none' })
			}
		}
	}
</script>

<style scoped>
	.page {
		min-height: 100vh;
		background: #f7f6f1;
		padding-bottom: 180rpx;
	}

	.cart-hint {
		display: flex;
		align-items: center;
		margin: 20rpx 24rpx 0;
		background: #fff2e6;
		border-radius: 20rpx;
		padding: 16rpx 24rpx;
		font-size: 23rpx;
		color: #a55a22;
	}

	.cart-list {
		display: flex;
		flex-direction: column;
		gap: 20rpx;
		padding: 24rpx;
	}

	.cart-item {
		display: flex;
		align-items: center;
		gap: 20rpx;
		background: #fff;
		border-radius: 28rpx;
		padding: 24rpx;
		box-shadow: 0 4rpx 20rpx rgba(0, 0, 0, 0.06);
	}

	.chk {
		width: 40rpx;
		height: 40rpx;
		border-radius: 50%;
		border: 3rpx solid #cfd3c9;
		display: flex;
		align-items: center;
		justify-content: center;
		flex-shrink: 0;
		background: #fff;
	}

	.chk.on {
		border-color: #2e9e6b;
		background: #2e9e6b;
	}

	.chk-icon {
		color: #fff;
		font-size: 24rpx;
		font-weight: 700;
	}

	.cart-img {
		width: 160rpx;
		height: 160rpx;
		border-radius: 20rpx;
		overflow: hidden;
		flex-shrink: 0;
	}

	.cart-img-inner {
		width: 100%;
		height: 100%;
	}

	.cart-bd {
		flex: 1;
		min-width: 0;
	}

	.cart-nm {
		font-size: 27rpx;
		font-weight: 500;
		display: block;
		overflow: hidden;
		text-overflow: ellipsis;
		white-space: nowrap;
	}

	.cart-sp {
		font-size: 22rpx;
		color: #a5aca1;
		margin-top: 4rpx;
		display: block;
	}

	.cart-btm {
		display: flex;
		align-items: center;
		justify-content: space-between;
		margin-top: 18rpx;
	}

	.cart-price {
		color: #f24e3e;
		font-size: 30rpx;
		font-weight: 700;
	}

	.step {
		display: flex;
		align-items: center;
	}

	.step-btn {
		width: 52rpx;
		height: 52rpx;
		border: 2rpx solid #d8dcd3;
		background: #fff;
		color: #6b7269;
		display: flex;
		align-items: center;
		justify-content: center;
		border-radius: 16rpx 0 0 16rpx;
	}

	.step-btn:last-child {
		border-radius: 0 16rpx 16rpx 0;
		border-color: #2e9e6b;
		color: #2e9e6b;
		background: #e6f4ec;
	}

	.step-icon {
		font-size: 28rpx;
		line-height: 1;
		font-weight: 300;
	}

	.step-num {
		min-width: 64rpx;
		text-align: center;
		font-size: 26rpx;
		border-top: 2rpx solid #d8dcd3;
		border-bottom: 2rpx solid #d8dcd3;
		padding: 6rpx 0;
		font-variant-numeric: tabular-nums;
	}

	.cart-empty {
		display: flex;
		flex-direction: column;
		align-items: center;
		justify-content: center;
		padding: 180rpx 0;
	}

	.empty-icon {
		font-size: 120rpx;
		opacity: 0.4;
		margin-bottom: 28rpx;
	}

	.empty-text {
		font-size: 27rpx;
		color: #a5aca1;
	}

	.empty-btn {
		margin-top: 36rpx;
		padding: 20rpx 68rpx;
		background: #2e9e6b;
		color: #fff;
		border-radius: 44rpx;
		font-size: 28rpx;
	}

	.settle-bar {
		position: fixed;
		bottom: 0;
		left: 0;
		right: 0;
		background: #fff;
		display: flex;
		align-items: center;
		padding: 16rpx 24rpx;
		padding-bottom: calc(16rpx + env(safe-area-inset-bottom));
		box-shadow: 0 -4rpx 20rpx rgba(0, 0, 0, 0.06);
		z-index: 99;
	}

	.settle-all {
		display: flex;
		align-items: center;
		gap: 12rpx;
	}

	.settle-text {
		font-size: 26rpx;
		color: #23261f;
	}

	.settle-total {
		flex: 1;
		text-align: right;
		margin-right: 20rpx;
	}

	.total-label {
		font-size: 24rpx;
		color: #6b7269;
	}

	.total-price {
		color: #f24e3e;
		font-size: 36rpx;
		font-weight: 700;
	}

	.settle-btn {
		background: linear-gradient(90deg, #35b47e, #1f8a58);
		color: #fff;
		padding: 20rpx 40rpx;
		border-radius: 44rpx;
		font-size: 28rpx;
		font-weight: 600;
	}
</style>
