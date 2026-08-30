<template>
  <view class="page">
    <view class="card">
      <view class="h">📍 收货地址</view>
      <view v-for="a in addresses" :key="a.id" class="addr" :class="{ on: addressId === a.id }" @tap="addressId = a.id">
        <view class="r1">{{ a.receiver }} {{ a.phone }}</view>
        <view class="r2">{{ a.province }}{{ a.city }}{{ a.district }} {{ a.detail }}</view>
      </view>
      <view class="addr add" :class="{ on: showForm }" @tap="showForm = !showForm">＋ 新增收货地址</view>
      <view v-if="showForm" class="form">
        <input class="in" v-model="form.receiver" placeholder="收货人姓名" />
        <input class="in" v-model="form.phone" type="number" maxlength="11" placeholder="手机号" />
        <view class="row3">
          <input class="in" v-model="form.province" placeholder="省份" />
          <input class="in" v-model="form.city" placeholder="城市" />
          <input class="in" v-model="form.district" placeholder="区/县" />
        </view>
        <input class="in" v-model="form.detail" placeholder="详细地址（街道、门牌号）" />
        <button class="savebtn" size="mini" @tap="saveAddress">保存地址</button>
      </view>
    </view>

    <view class="card">
      <view class="h">🍎 商品清单（{{ lines.length }} 种）</view>
      <view v-for="l in lines" :key="l.skuId" class="line">
        <text class="lname"><text class="bold">{{ l.name }}</text>（{{ l.spec }}）× {{ l.qty }}</text>
        <text class="lprice">¥{{ yuan(l.amount) }}</text>
      </view>
    </view>

    <view class="foot">
      <view class="sum">应付 <text class="total">¥{{ yuan(totalAmount) }}</text></view>
      <view class="submit" @tap="submit">提交订单</view>
    </view>
  </view>
</template>

<script setup>
import { computed, ref } from 'vue'
import { onLoad } from '@dcloudio/uni-app'
import { api, yuan } from '../../api'

const addresses = ref([])
const addressId = ref(null)
const showForm = ref(false)
const lines = ref([])
const buyNowMode = ref(false)
const form = ref({ receiver: '', phone: '', province: '', city: '', district: '', detail: '' })

const totalAmount = computed(() => lines.value.reduce((s, l) => s + l.amount, 0))

onLoad(async (opt) => {
  addresses.value = await api.get('/api/v1/users/me/addresses')
  addressId.value = (addresses.value.find((a) => a.isDefault) || addresses.value[0])?.id ?? null

  if (opt.skuId) {
    buyNowMode.value = true
    const p = await api.get(`/api/v1/products/${opt.productId}`)
    const sku = p.skus.find((s) => s.id === Number(opt.skuId))
    const qty = Number(opt.qty) || 1
    lines.value = [{ skuId: sku.id, name: p.name, spec: sku.spec, qty, amount: sku.price * qty }]
  } else {
    const cart = await api.get('/api/v1/cart')
    lines.value = cart.items
      .filter((it) => it.checked && it.stock > 0)
      .map((it) => ({ skuId: it.skuId, name: it.productName, spec: it.spec, qty: it.quantity, amount: it.subtotal }))
  }
})

async function saveAddress() {
  const f = form.value
  if (!f.receiver || !f.phone || !f.province || !f.city || !f.detail) {
    return uni.showToast({ title: '请完整填写地址信息', icon: 'none' })
  }
  const id = await api.post('/api/v1/users/me/addresses', { ...f, isDefault: addresses.value.length === 0 })
  addresses.value = await api.get('/api/v1/users/me/addresses')
  addressId.value = id
  showForm.value = false
  uni.showToast({ title: '已保存', icon: 'success' })
}

async function submit() {
  if (!addressId.value) return uni.showToast({ title: '请先选择收货地址', icon: 'none' })
  try {
    const orderNo = await api.post('/api/v1/orders', {
      addressId: addressId.value,
      items: lines.value.map((l) => ({ skuId: l.skuId, quantity: l.qty })),
    })
    if (!buyNowMode.value) await api.delete('/api/v1/cart/items/checked')
    const { confirm } = await uni.showModal({
      title: '下单成功',
      content: '演示环境：确认后模拟微信支付完成付款',
      confirmText: '模拟支付',
    })
    if (confirm) {
      await api.post(`/api/v1/payments/${orderNo}/mock-pay`)
      uni.showToast({ title: '支付成功', icon: 'success' })
    }
    setTimeout(() => uni.switchTab({ url: '/pages/orders/orders' }), 800)
  } catch (e) {
    uni.showToast({ title: e.message, icon: 'none' })
  }
}
</script>

<style scoped>
.card { background: #fff; border-radius: 20rpx; margin: 20rpx 24rpx 0; padding: 26rpx; }
.h { font-size: 29rpx; font-weight: 700; margin-bottom: 18rpx; }
.addr { border: 3rpx solid #e5e7eb; border-radius: 14rpx; padding: 18rpx 22rpx; margin-bottom: 14rpx; font-size: 25rpx; }
.addr.on { border-color: #2e9e5b; background: #e6f4ea; }
.addr .r1 { font-weight: 700; margin-bottom: 6rpx; }
.addr .r2 { color: #6b7280; }
.addr.add { text-align: center; color: #1f7a3d; font-weight: 600; }
.form { margin-top: 8rpx; }
.in { border: 3rpx solid #e5e7eb; border-radius: 12rpx; padding: 14rpx 20rpx; font-size: 25rpx; margin-bottom: 14rpx; }
.row3 { display: flex; gap: 12rpx; }
.row3 .in { flex: 1; min-width: 0; }
.savebtn { background: #2e9e5b; color: #fff; margin-top: 4rpx; }
.line { display: flex; justify-content: space-between; padding: 10rpx 0; font-size: 25rpx; }
.lname { color: #6b7280; }
.bold { color: #1e293b; font-weight: 600; }
.lprice { color: #dc2626; font-weight: 700; }
.foot {
  position: fixed; left: 0; right: 0; bottom: 0; background: #fff;
  display: flex; align-items: center; justify-content: space-between;
  padding: 20rpx 30rpx calc(20rpx + env(safe-area-inset-bottom));
  box-shadow: 0 -4rpx 16rpx rgba(30, 41, 59, 0.08);
}
.total { color: #dc2626; font-size: 36rpx; font-weight: 800; }
.submit { background: #2e9e5b; color: #fff; border-radius: 40rpx; padding: 18rpx 56rpx; font-size: 29rpx; font-weight: 600; }
</style>
