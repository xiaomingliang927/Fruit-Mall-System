<template>
  <view class="page">
    <scroll-view scroll-x class="tabs">
      <view v-for="t in tabs" :key="t.v ?? 'a'" class="tab" :class="{ on: status === t.v }" @tap="pick(t.v)">{{ t.l }}</view>
    </scroll-view>

    <view v-if="!orders.length" class="empty">暂无相关订单</view>

    <view v-for="o in orders" :key="o.orderNo" class="ocard">
      <view class="ohead">
        <text class="ono">{{ o.orderNo }}</text>
        <text class="st" :class="`s${o.status}`">{{ o.statusText }}</text>
      </view>
      <view class="obody">
        <text class="ocnt">共 {{ o.itemCount ?? 0 }} 件</text>
        <text class="oamt">实付 <text class="money">¥{{ yuan(o.payAmount) }}</text></text>
      </view>
      <view class="oacts">
        <view v-if="o.status === 10" class="act main" @tap="pay(o)">立即支付</view>
        <view v-if="o.status === 10" class="act danger" @tap="cancel(o)">取消</view>
        <view v-if="o.status === 30" class="act main" @tap="confirmOrder(o)">确认收货</view>
        <view v-if="o.status === 40" class="act main" @tap="goReview(o)">评价</view>
        <view v-if="[20, 30, 40].includes(o.status)" class="act ghost" @tap="goRefund(o)">申请售后</view>
        <view class="act ghost" @tap="toggleDetail(o)">{{ o._open ? '收起' : '详情' }}</view>
      </view>
      <view v-if="o._open" class="odetail">
        <view v-for="(it, i) in o._detail?.items || []" :key="i" class="dline">
          <text class="dname">{{ it.productName }}（{{ it.skuSpec }}）× {{ it.quantity }}</text>
          <text class="dprice">¥{{ yuan(it.subtotal) }}</text>
        </view>
        <view class="daddr">收货：{{ o._addrText }}</view>
        <view v-if="o._detail?.trackingNo" class="daddr">运单号：{{ o._detail.trackingNo }}</view>
      </view>
    </view>
  </view>
</template>

<script setup>
import { ref } from 'vue'
import { onShow, onLoad } from '@dcloudio/uni-app'
import { api, fmtTime, yuan } from '../../api'

const tabs = [
  { l: '全部', v: null }, { l: '待支付', v: 10 }, { l: '待发货', v: 20 },
  { l: '待收货', v: 30 }, { l: '已完成', v: 40 },
]
const status = ref(null)
const orders = ref([])

onLoad((opt) => {
  if (opt && opt.status) {
    status.value = Number(opt.status)
  }
})

async function load() {
  // 小程序 JSCore 没有 URLSearchParams，手动拼查询串
  const params = ['page=1', 'size=20']
  if (status.value) params.push(`status=${status.value}`)
  const data = await api.get(`/api/v1/orders?${params.join('&')}`)
  // 保留已展开状态（不用 Object.fromEntries，避免 ES2019 依赖）
  const old = {}
  orders.value.forEach((o) => { old[o.orderNo] = o })
  orders.value = data.records.map((o) => ({ ...o, _open: old[o.orderNo]?._open, _detail: old[o.orderNo]?._detail }))
}

onShow(() => { if (uni.getStorageSync('token')) load() })

function pick(v) { status.value = v; load() }

async function toggleDetail(o) {
  if (!o._open) {
    const d = await api.get(`/api/v1/orders/${o.orderNo}`)
    let addr = {}
    try { addr = JSON.parse(d.addressSnapshot || '{}') } catch {}
    o._detail = d
    o._addrText = `${addr.receiver || ''} ${addr.phone || ''} ｜ ${addr.province || ''}${addr.city || ''}${addr.district || ''} ${addr.detail || ''}`
  }
  o._open = !o._open
}

function goRefund(o) {
  uni.navigateTo({ url: '/pages/refund/refund?orderNo=' + o.orderNo + '&amount=' + o.payAmount })
}

function goReview(o) {
  uni.navigateTo({ url: '/pages/review/review?orderNo=' + o.orderNo })
}

async function pay(o) {
  try {
    await api.post(`/api/v1/payments/${o.orderNo}/mock-pay`)
    uni.showToast({ title: '支付成功（演示）', icon: 'success' })
    load()
  } catch (e) { uni.showToast({ title: e.message, icon: 'none' }) }
}

async function cancel(o) {
  const { confirm } = await uni.showModal({ title: '提示', content: '确认取消该订单？库存将自动释放' })
  if (!confirm) return
  await api.post(`/api/v1/orders/${o.orderNo}/cancel`)
  uni.showToast({ title: '已取消', icon: 'success' })
  load()
}

async function confirmOrder(o) {
  const { confirm } = await uni.showModal({ title: '提示', content: '确认已收到货？' })
  if (!confirm) return
  await api.post(`/api/v1/orders/${o.orderNo}/confirm`)
  uni.showToast({ title: '交易完成', icon: 'success' })
  load()
}
</script>

<style scoped>
.tabs { white-space: nowrap; padding: 0 24rpx; background: #fff; border-bottom: 1rpx solid #f0f2f4; }
.tab {
  display: inline-block; padding: 22rpx 4rpx 18rpx; margin-right: 40rpx;
  font-size: 27rpx; color: #4b5563; border-bottom: 4rpx solid transparent;
}
.tab.on { color: #1f7a3d; font-weight: 600; border-bottom-color: #1f7a3d; }
.empty { text-align: center; color: #9ca3af; padding-top: 180rpx; font-size: 27rpx; }
.ocard { background: #fff; border-radius: 10rpx; margin: 14rpx 20rpx 0; padding: 22rpx; border: 1rpx solid #eceef0; }
.ohead { display: flex; justify-content: space-between; align-items: center; }
.ono { color: #6b7280; font-size: 23rpx; }
.st { font-weight: 600; font-size: 26rpx; }
.s10 { color: #d97706; } .s20 { color: #1f7a3d; } .s30 { color: #0e7490; }
.s40 { color: #14532d; } .s50 { color: #9ca3af; } .s60 { color: #ff6e26; } .s70 { color: #7c3aed; }
.obody { display: flex; justify-content: space-between; align-items: baseline; margin-top: 14rpx; }
.ocnt { color: #9ca3af; font-size: 24rpx; }
.oamt { color: #6b7280; font-size: 24rpx; }
.money { color: #e4393c; font-size: 31rpx; font-weight: 800; }
.oacts { display: flex; justify-content: flex-end; gap: 14rpx; margin-top: 16rpx; }
.act { border-radius: 6rpx; padding: 9rpx 28rpx; font-size: 24rpx; }
.act.main { background: #1f7a3d; color: #fff; font-weight: 600; }
.act.danger { border: 2rpx solid #f5b5b6; color: #e4393c; }
.act.ghost { border: 2rpx solid #e5e7eb; color: #6b7280; }
.odetail { margin-top: 16rpx; border-top: 2rpx dashed #f1f5f0; padding-top: 13rpx; }
.dline { display: flex; justify-content: space-between; font-size: 24rpx; padding: 6rpx 0; }
.dname { color: #374151; }
.dprice { color: #e4393c; font-weight: 600; }
.daddr { background: #f9fafb; border-radius: 6rpx; padding: 11rpx 16rpx; font-size: 22rpx; color: #6b7280; margin-top: 10rpx; }
</style>
