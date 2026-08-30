<template>
  <view class="page">
    <view v-if="!items.length" class="empty">
      <view class="big">🛒</view>
      购物车还是空的
      <view class="gohome" @tap="goHome">去逛逛 →</view>
    </view>

    <view v-else>
      <view v-for="it in items" :key="it.itemId" class="row" :class="{ bad: it.stock <= 0 }">
        <view class="ck" :class="{ on: it.checked && it.stock > 0 }" @tap="toggle(it)">✓</view>
        <image class="img" :src="imgUrl(it.image)" mode="aspectFill" />
        <view class="mid">
          <view class="name">{{ it.productName }}</view>
          <view class="spec">{{ it.spec }}</view>
          <view class="price">¥{{ yuan(it.price) }}</view>
        </view>
        <view class="right">
          <view class="del" @tap="del(it)">删除</view>
          <view class="stepper">
            <view class="stbtn" @tap="changeQty(it, it.quantity - 1)">−</view>
            <text class="stnum">{{ it.quantity }}</text>
            <view class="stbtn" @tap="changeQty(it, it.quantity + 1)">＋</view>
          </view>
        </view>
      </view>

      <view class="foot">
        <view class="sum">
          合计 <text class="total">¥{{ yuan(summary.totalAmount) }}</text>
          <text class="cnt">（{{ summary.totalCount }} 件）</text>
        </view>
        <view class="pay" @tap="checkout">去结算</view>
      </view>
    </view>
  </view>
</template>

<script setup>
import { ref } from 'vue'
import { onShow } from '@dcloudio/uni-app'
import { api, imgUrl, yuan } from '../../api'

const summary = ref({ items: [], totalCount: 0, totalAmount: 0 })
const items = ref([])

async function load() {
  summary.value = await api.get('/api/v1/cart')
  items.value = summary.value.items
}

onShow(() => {
  if (uni.getStorageSync('token')) load()
})

async function toggle(it) {
  await api.put(`/api/v1/cart/items/${it.itemId}`, { checked: !(it.checked && it.stock > 0) })
  load()
}

async function changeQty(it, qty) {
  if (qty < 1 || qty > 99) return
  if (qty > it.stock) return uni.showToast({ title: '超出库存', icon: 'none' })
  await api.put(`/api/v1/cart/items/${it.itemId}`, { quantity: qty })
  load()
}

async function del(it) {
  const { confirm } = await uni.showModal({ title: '提示', content: `删除「${it.productName}」？` })
  if (!confirm) return
  await api.delete(`/api/v1/cart/items/${it.itemId}`)
  load()
}

function checkout() {
  if (!summary.value.totalCount) return uni.showToast({ title: '先勾选商品哦', icon: 'none' })
  uni.navigateTo({ url: '/pages/checkout/checkout' })
}

function goHome() { uni.switchTab({ url: '/pages/index/index' }) }
</script>

<style scoped>
.empty { text-align: center; color: #9ca3af; padding-top: 200rpx; font-size: 27rpx; }
.big { font-size: 90rpx; margin-bottom: 20rpx; }
.gohome { color: #1f7a3d; margin-top: 16rpx; font-weight: 600; }
.row {
  display: flex; align-items: center; background: #fff; border-radius: 18rpx;
  margin: 16rpx 24rpx 0; padding: 22rpx; gap: 18rpx;
}
.row.bad { opacity: 0.55; }
.ck {
  width: 40rpx; height: 40rpx; border-radius: 50%; border: 3rpx solid #cbd5e1;
  text-align: center; line-height: 36rpx; color: transparent; font-size: 24rpx; flex: none;
}
.ck.on { background: #2e9e5b; border-color: #2e9e5b; color: #fff; }
.img { width: 150rpx; height: 118rpx; border-radius: 12rpx; background: #f0f2ee; flex: none; }
.mid { flex: 1; min-width: 0; }
.name { font-size: 27rpx; font-weight: 600; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.spec { color: #9ca3af; font-size: 22rpx; margin-top: 4rpx; }
.price { color: #dc2626; font-weight: 700; margin-top: 8rpx; font-size: 27rpx; }
.right { display: flex; flex-direction: column; align-items: flex-end; gap: 14rpx; }
.del { color: #d1d5db; font-size: 23rpx; }
.stepper { display: flex; align-items: center; border: 3rpx solid #e5e7eb; border-radius: 10rpx; overflow: hidden; }
.stbtn { width: 52rpx; height: 50rpx; background: #f9fafb; text-align: center; line-height: 48rpx; font-size: 27rpx; }
.stnum { width: 70rpx; text-align: center; font-size: 26rpx; }
.foot {
  position: fixed; left: 0; right: 0; bottom: 0; background: #fff;
  display: flex; align-items: center; justify-content: space-between;
  padding: 20rpx 30rpx calc(20rpx + env(safe-area-inset-bottom));
  box-shadow: 0 -4rpx 16rpx rgba(30, 41, 59, 0.08);
}
.total { color: #dc2626; font-size: 36rpx; font-weight: 800; }
.cnt { color: #9ca3af; font-size: 23rpx; }
.pay { background: #2e9e5b; color: #fff; border-radius: 40rpx; padding: 18rpx 52rpx; font-size: 29rpx; font-weight: 600; }
</style>
