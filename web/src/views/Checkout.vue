<template>
  <div>
    <h3 style="margin-bottom:14px">确认订单</h3>

    <div class="panel">
      <h3>收货地址</h3>
      <div class="addr-grid">
        <div v-for="a in addresses" :key="a.id" class="addr-card" :class="{ active: addressId === a.id }"
             @click="addressId = a.id">
          <div class="receiver">{{ a.receiver }} {{ a.phone }}</div>
          <div>{{ a.province }}{{ a.city }}{{ a.district }} {{ a.detail }}</div>
          <span v-if="a.isDefault" class="tag" style="margin-top:4px">默认</span>
          <span class="addr-edit" @click.stop="editAddress(a)">编辑</span>
        </div>
        <div class="addr-card" style="text-align:center;color:var(--green-700)" @click="newAddress">
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
        <button class="btn btn-sm" style="justify-self:start" @click="saveAddress">{{ editingId ? '更新地址' : '保存地址' }}</button>
      </div>
    </div>

    <div class="panel">
      <h3>商品清单（{{ lines.length }} 件）</h3>
      <div v-for="l in lines" :key="l.skuId" class="check-line">
        <span><b>{{ l.name }}</b>（{{ l.spec }}）× {{ l.qty }}</span>
        <span class="price">¥{{ yuan(l.amount) }}</span>
      </div>
    </div>

    <div class="panel">
      <h3>优惠券</h3>
      <div v-if="!usableCoupons.length" class="check-line"><span class="lab">无可用优惠券（可在领券中心领取）</span></div>
      <label v-for="c in usableCoupons" :key="c.userCouponId" class="check-line coupon-line"
             :class="{ selected: userCouponId === c.userCouponId }"
             @click="userCouponId = userCouponId === c.userCouponId ? null : c.userCouponId">
        <span><b>{{ c.name }}</b>（{{ c.typeText }}）<span class="lab"> · 有效期至 {{ (c.expireAt || '').slice(0, 10) }}</span></span>
        <span class="price">-¥{{ ((c.type === 2 ? Math.floor(totalAmount * (100 - c.discountPercent) / 100) : Math.min(c.discountAmount || 0, totalAmount)) / 100).toFixed(2) }}</span>
      </label>
    </div>

    <div class="submit-row">
      <span class="amount">商品合计：<b>¥{{ yuan(totalAmount) }}</b>
        <template v-if="couponDiscount > 0"><span style="margin-left:10px">券已减</span><b class="price" style="font-size:15px">¥{{ yuan(couponDiscount) }}</b></template>
      </span>
      <span class="amount">应付总额：<b class="price">¥{{ yuan(payAmount) }}</b></span>
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
import { refreshMe, useStore } from '../store'

const route = useRoute()
const router = useRouter()
const addresses = ref([])
const addressId = ref(null)
const showForm = ref(false)
const lines = ref([])
const submitting = ref(false)
const form = reactive({ receiver: '', phone: '', province: '', city: '', district: '', detail: '' })
const editingId = ref(null)

/** 新增入口：清空表单与编辑态 */
function newAddress() {
  editingId.value = null
  Object.assign(form, { receiver: '', phone: '', province: '', city: '', district: '', detail: '' })
  showForm.value = true
}

/** 编辑入口：回填现有地址 */
function editAddress(a) {
  editingId.value = a.id
  Object.assign(form, { receiver: a.receiver, phone: a.phone, province: a.province, city: a.city, district: a.district, detail: a.detail })
  showForm.value = true
}

const totalAmount = computed(() => lines.value.reduce((s, l) => s + l.amount, 0))
const usableCoupons = ref([])
const userCouponId = ref(null)
const couponDiscount = computed(() => {
  if (!userCouponId.value) return 0
  const c = usableCoupons.value.find((x) => x.userCouponId === userCouponId.value)
  if (!c) return 0
  if (c.type === 2) return Math.floor(totalAmount.value * (100 - c.discountPercent) / 100)
  return Math.min(c.discountAmount || 0, totalAmount.value)
})
const payAmount = computed(() => Math.max(totalAmount.value - couponDiscount.value, 1))
async function loadUsable() {
  if (!useStore.token) return
  try { usableCoupons.value = await api.get(`/api/v1/coupons/usable?amount=${totalAmount.value}`) } catch (e) { /* 静默 */ }
}

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
  if (editingId.value) {
    // 修改：保持原默认态（要改默认走小程序地址页的「设为默认」）
    const target = addresses.value.find((a) => a.id === editingId.value)
    await api.put(`/api/v1/users/me/addresses/${editingId.value}`, { ...form, isDefault: !!target?.isDefault })
    addresses.value = await api.get('/api/v1/users/me/addresses')
    addressId.value = editingId.value
    showForm.value = false
    editingId.value = null
    toast('地址已更新')
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
      userCouponId: userCouponId.value || undefined,
    })
    if (fromCart) await api.delete('/api/v1/cart/items/checked')
    await refreshMe()
    toast(couponDiscount.value > 0 ? `下单成功，已优惠 ¥${yuan(couponDiscount.value)}` : '下单成功，去支付')
    router.push({ name: 'orders', query: { pay: orderNo } })
  } catch (e) {
    toast(e.message, 'err')
  } finally {
    submitting.value = false
  }
}
</script>

<style scoped>
.addr-edit {
  position: absolute;
  right: 12px;
  top: 10px;
  font-size: 12px;
  color: var(--green-700, #17704a);
  cursor: pointer;
}
.addr-card { position: relative; }
</style>
