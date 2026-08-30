<template>
  <view v-if="product" class="page">
    <image class="main" :src="imgUrl(product.mainImage)" mode="aspectFill" />
    <view class="info card">
      <view class="title">{{ product.name }}</view>
      <view class="sub">{{ product.subtitle }}</view>
      <view class="pricebox">
        <text class="price"><text class="rmb">¥</text>{{ yuan(currentSku?.price || 0) }}</text>
        <text class="unit">/ {{ product.unit }} · {{ currentSku?.spec }}</text>
      </view>
      <view class="meta">
        <view class="mrow"><text class="mlab">产地</text><text>{{ product.origin || '甄选产区' }} ｜ 已售 {{ product.sales }} 件</text></view>
        <view class="mrow tags">
          <text v-for="t in tagList" :key="t" class="tag">{{ t }}</text>
        </view>
        <view class="mrow"><text class="mlab">保障</text><text>坏果包赔：收货 24 小时内拍照，极速退款</text></view>
      </view>
    </view>

    <view class="card sec">
      <view class="lab">选择规格</view>
      <view class="skus">
        <view v-for="s in product.skus" :key="s.id" class="sku" :class="{ on: currentSku?.id === s.id }" @tap="currentSku = s">
          {{ s.spec }} · ¥{{ yuan(s.price) }}
        </view>
      </view>
      <view class="lab" style="margin-top: 28rpx">购买数量</view>
      <view class="qtyrow">
        <view class="stepper">
          <view class="stbtn" @tap="qty = Math.max(1, qty - 1)">−</view>
          <input class="stin" type="number" :value="qty" @input="(e) => (qty = Number(e.detail.value) || 1)" />
          <view class="stbtn" @tap="qty = Math.min(99, qty + 1)">＋</view>
        </view>
        <text class="stock">库存 {{ currentSku?.stock ?? 0 }} 件</text>
      </view>
    </view>

    <view class="bar">
      <view class="btn warn" @tap="addCart">加入购物车</view>
      <view class="btn main" @tap="buyNow">立即购买</view>
    </view>
  </view>
</template>

<script setup>
import { computed, ref } from 'vue'
import { onLoad } from '@dcloudio/uni-app'
import { api, imgUrl, yuan } from '../../api'

const product = ref(null)
const currentSku = ref(null)
const qty = ref(1)
const tagList = computed(() => (product.value?.tags || '').split(',').filter(Boolean))

onLoad(async (opt) => {
  product.value = await api.get(`/api/v1/products/${opt.id}`)
  currentSku.value = product.value.skus.find((s) => s.stock > 0) || product.value.skus[0]
})

async function addCart() {
  if (!getToken()) return goLogin()
  try {
    await api.post('/api/v1/cart/items', { skuId: currentSku.value.id, quantity: qty.value })
    uni.showToast({ title: '已加入购物车', icon: 'success' })
  } catch (e) { uni.showToast({ title: e.message, icon: 'none' }) }
}

function buyNow() {
  if (!getToken()) return goLogin()
  uni.navigateTo({
    url: `/pages/checkout/checkout?skuId=${currentSku.value.id}&productId=${product.value.id}&qty=${qty.value}`,
  })
}

function goLogin() {
  uni.showToast({ title: '请先登录', icon: 'none' })
  uni.navigateTo({ url: '/pages/login/login' })
}

import { getToken } from '../../api'
</script>

<style scoped>
.main { width: 100%; height: 560rpx; background: #f0f2ee; display: block; }
.card { background: #fff; border-radius: 10rpx; margin: 16rpx 20rpx 0; padding: 26rpx; border: 1rpx solid #eceef0; }
.title { font-size: 35rpx; font-weight: 700; }
.sub { color: #9ca3af; font-size: 25rpx; margin-top: 8rpx; }
.pricebox { margin-top: 20rpx; padding: 16rpx 22rpx; background: #faf7f5; border-radius: 6rpx; border: 1rpx solid #f0e5e0; }
.price { color: #e4393c; font-size: 42rpx; font-weight: 800; }
.rmb { font-size: 25rpx; }
.unit { color: #9ca3af; font-size: 23rpx; margin-left: 12rpx; }
.meta { margin-top: 16rpx; color: #6b7280; font-size: 24rpx; }
.mrow { display: flex; gap: 12rpx; padding: 4rpx 0; align-items: baseline; }
.mlab { color: #b6bcc6; flex: none; }
.tag { display: inline-block; background: #eaf4ee; color: #1f7a3d; font-size: 21rpx; border-radius: 4rpx; padding: 2rpx 12rpx; margin-right: 10rpx; border: 1rpx solid #d3e8db; }
.lab { font-size: 27rpx; font-weight: 600; }
.skus { display: flex; flex-wrap: wrap; gap: 16rpx; margin-top: 16rpx; }
.sku { border: 2rpx solid #e5e7eb; border-radius: 6rpx; padding: 11rpx 22rpx; font-size: 25rpx; }
.sku.on { border-color: #e4393c; color: #e4393c; background: #fef4f4; font-weight: 600; }
.qtyrow { display: flex; align-items: center; gap: 20rpx; margin-top: 16rpx; }
.stepper { display: flex; align-items: center; border: 2rpx solid #e5e7eb; border-radius: 6rpx; overflow: hidden; }
.stbtn { width: 62rpx; height: 58rpx; background: #f9fafb; text-align: center; line-height: 56rpx; font-size: 30rpx; }
.stin { width: 88rpx; height: 58rpx; text-align: center; font-size: 27rpx; }
.stock { color: #9ca3af; font-size: 23rpx; }
.bar {
  position: fixed; left: 0; right: 0; bottom: 0; display: flex; gap: 16rpx;
  padding: 16rpx 24rpx calc(16rpx + env(safe-area-inset-bottom)); background: #fff;
  border-top: 1rpx solid #eceef0;
}
.btn { flex: 1; text-align: center; padding: 21rpx 0; border-radius: 8rpx; font-size: 29rpx; font-weight: 600; }
.btn.warn { background: #ff6e26; color: #fff; }
.btn.main { background: #1f7a3d; color: #fff; }
</style>
