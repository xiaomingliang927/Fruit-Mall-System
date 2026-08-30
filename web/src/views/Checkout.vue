<template>
  <div>
    <h3 style="margin-bottom:14px">📝 确认订单</h3>

    <div class="panel">
      <h3>📍 收货地址</h3>
      <div class="addr-grid">
        <div v-for="a in addresses" :key="a.id" class="addr-card" :class="{ active: addressId === a.id }"
             @click="addressId = a.id">
          <div class="receiver">{{ a.receiver }} {{ a.phone }}</div>
          <div>{{ a.province }}{{ a.city }}{{ a.district }} {{ a.detail }}</div>
          <span v-if="a.isDefault" class="tag" style="margin-top:4px">默认</span>
        </div>
        <div class="addr-card" style="text-align:center;color:var(--green-700)" @click="showForm = !showForm">
          <div style="font-size:22px">＋</div>新增地址
        </div>
      </div>
      <div v-if="showForm" class="addr-form">
        <input v-model.trim="form.receiver" placeholder="收货人姓名" />
        <input v-model.trim="form.phone" maxlength="11" placeholder="手机号" />
        <input v-model.trim="form.province" placeholder="省份" />
        <input v-model.trim="form.city" placeholder="城市" />
        <input v-model.trim="form.district" placeholder="区/县" />
        <input v-model.trim="form.detail" placeholder="详细地址（街道、门牌号）" />
        <button class="btn btn-sm" style="justify-self:start" @click="saveAddress">保存地址</button>
      </div>
    </div>

    <div class="panel">
      <h3>🍎 商品清单（{{ lines.length }} 件）</h3>
      <div v-for="l in lines" :key="l.skuId" class="check-line">
        <span><b>{{ l.name }}</b>（{{ l.spec }}）× {{ l.qty }}</span>
        <span class="price">¥{{ yuan(l.amount) }}</span>
      </div>
    </div>

    <div class="submit-row">
      <span class="amount">应付总额：<b class="price">¥{{ yuan(totalAmount) }}</b></span>
      <button class="btn" style="padding:12px 40px" :disabled="submitting || !addressId" @click="submitOrder">
        {{ submitting ? '提交中…' : '提交订单' }}
      </button>
    </div>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { api, toast, yuan } from '../api'
import { refreshMe } from '../store'

const route = useRoute()
const router = useRouter()
const addresses = ref([])
const addressId = ref(null)
const showForm = ref(false)
const lines = ref([])
const submitting = ref(false)
const form = reactive({ receiver: '', phone: '', province: '', city: '', district: '', detail: '' })

const totalAmount = computed(() => lines.value.reduce((s, l) => s + l.amount, 0))

onMounted(async () => {
  addresses.value = await api.get('/api/v1/users/me/addresses')
  const def = addresses.value.find((a) => a.isDefault)
  addressId.value = (def || addresses.value[0])?.id ?? null

  if (route.query.skuId) {
    // 立即购买：详情页带入 skuId + productId + qty
    lines.value = [await findSkuOwner(Number(route.query.skuId))]
  } else {
    // 购物车结算：取勾选商品
    const cart = await api.get('/api/v1/cart')
    lines.value = cart.items
      .filter((it) => it.checked && it.stock > 0)
      .map((it) => ({ skuId: it.skuId, name: it.productName, spec: it.spec, qty: it.quantity, amount: it.subtotal }))
  }
})

/** 商品接口按商品 id 查询，立即购买时由详情页同时携带 productId 定位 SKU */
async function findSkuOwner(skuId) {
  const productId = Number(route.query.productId)
  if (productId) {
    const p = await api.get(`/api/v1/products/${productId}`)
    const sku = p.skus.find((s) => s.id === skuId)
    if (sku) return { skuId, name: p.name, spec: sku.spec, qty: Number(route.query.qty) || 1, amount: sku.price * (Number(route.query.qty) || 1) }
  }
  toast('商品参数缺失', 'err')
  return { skuId, name: '未知商品', spec: '-', qty: 1, amount: 0 }
}

async function saveAddress() {
  if (!form.receiver || !form.phone || !form.province || !form.city || !form.detail) {
    toast('请完整填写收货人/手机号/省市/详细地址', 'err')
    return
  }
  const id = await api.post('/api/v1/users/me/addresses', { ...form, isDefault: addresses.value.length === 0 })
  addresses.value = await api.get('/api/v1/users/me/addresses')
  addressId.value = id
  showForm.value = false
  toast('地址已保存')
}

async function submitOrder() {
  submitting.value = true
  try {
    const fromCart = !route.query.skuId
    const orderNo = await api.post('/api/v1/orders', {
      addressId: addressId.value,
      items: lines.value.map((l) => ({ skuId: l.skuId, quantity: l.qty })),
    })
    if (fromCart) await api.delete('/api/v1/cart/items/checked')
    await refreshMe()
    toast('下单成功，去支付 →')
    router.push({ name: 'orders', query: { pay: orderNo } })
  } catch (e) {
    toast(e.message, 'err')
  } finally {
    submitting.value = false
  }
}
</script>
