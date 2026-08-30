<template>
  <div>
    <h3 style="margin-bottom:14px">我的订单</h3>

    <div class="order-tabs">
      <span v-for="t in tabs" :key="t.value ?? 'all'" class="cat-tab"
            :class="{ active: status === t.value }" @click="pickStatus(t.value)">{{ t.label }}</span>
    </div>

    <div v-if="!orders.length" class="cart-empty">
      暂无相关订单，<router-link to="/" style="color:var(--green-700)">去下一单 →</router-link>
    </div>

    <div v-for="o in orders" :key="o.orderNo" class="order-card">
      <div class="oc-head">
        <span class="oc-no">单号 {{ o.orderNo }} · {{ fmtTime(o.createdAt) }}</span>
        <span class="st" :class="`st-${o.status}`">{{ o.statusText }}</span>
      </div>
      <div class="oc-body">
        <span style="color:var(--muted);font-size:13px">共 {{ o.count }} 件</span>
        <span class="oc-sum">
          实付 <b class="price" style="font-size:18px">¥{{ yuan(o.payAmount) }}</b>
        </span>
      </div>
      <div class="oc-actions">
        <button v-if="o.status === 10" class="btn btn-sm" @click="pay(o)">立即支付</button>
        <button v-if="o.status === 10" class="btn btn-sm btn-danger" @click="cancel(o)">取消订单</button>
        <button v-if="o.status === 20" class="btn btn-sm btn-ghost" disabled>等待商家发货</button>
        <button v-if="o.status === 30" class="btn btn-sm" @click="confirm(o)">确认收货</button>
        <button v-if="[20, 30, 40].includes(o.status)" class="btn btn-sm btn-outline" @click="openRefund(o)">申请售后</button>
        <button class="btn btn-sm btn-outline" @click="openDetail(o)">查看详情</button>
      </div>
    </div>

    <div class="pager" v-if="totalPages > 1">
      <button :disabled="page <= 1" @click="goPage(page - 1)">上一页</button>
      <span style="align-self:center;color:var(--muted)">{{ page }} / {{ totalPages }}</span>
      <button :disabled="page >= totalPages" @click="goPage(page + 1)">下一页</button>
    </div>

    <!-- 售后申请弹层 -->
    <div v-if="refundOrder" class="modal-mask" @click.self="refundOrder = null">
      <div class="modal">
        <h3>申请售后 · {{ refundOrder.orderNo }}</h3>
        <div class="addr-text">售后金额：整单实付 <b class="price">¥{{ yuan(refundOrder.payAmount) }}</b>；≤50 元自动秒审退款，超额进入人工审核</div>
        <div class="refund-type">
          <label class="rf-item" :class="{ on: refundForm.type === 1 }">
            <input type="radio" value="1" v-model="refundForm.type" /> 仅退款（未收到货/坏果包赔）
          </label>
          <label class="rf-item" :class="{ on: refundForm.type === 2 }">
            <input type="radio" value="2" v-model="refundForm.type" /> 退货退款（已收到货）
          </label>
        </div>
        <textarea class="refund-reason" v-model="refundForm.reason" maxlength="200"
                  placeholder="请填写申请原因（必填），如：坏果/腐烂/少件…"></textarea>
        <div style="text-align:right;margin-top:14px">
          <button class="btn btn-sm btn-ghost" @click="refundOrder = null">取消</button>
          <button class="btn btn-sm" style="margin-left:10px" :disabled="refundSubmitting" @click="submitRefund">提交申请</button>
        </div>
      </div>
    </div>

    <!-- 订单详情弹层 -->
    <div v-if="detail" class="modal-mask" @click.self="detail = null">
      <div class="modal">
        <h3>订单详情 · {{ detail.orderNo }}</h3>
        <div class="addr-text">
          收货：{{ addrText }}<br />
          配送：快递{{ detail.trackingNo ? `（运单号 ${detail.trackingNo}）` : '' }}
          <template v-if="detail.remark"><br />留言：{{ detail.remark }}</template>
        </div>
        <div v-for="(it, i) in detail.items" :key="i" class="check-line">
          <span><b>{{ it.productName }}</b>（{{ it.skuSpec }}）× {{ it.quantity }}</span>
          <span class="price">¥{{ yuan(it.subtotal) }}</span>
        </div>
        <div class="check-line" style="border-top:1px solid var(--line);margin-top:8px;padding-top:12px">
          <span class="lab">实付总额</span>
          <span class="price" style="font-size:19px">¥{{ yuan(detail.payAmount) }}</span>
        </div>
        <div style="text-align:right;margin-top:14px">
          <button class="btn btn-sm btn-ghost" @click="detail = null">关闭</button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { useRoute } from 'vue-router'
import { api, toast, fmtTime, yuan } from '../api'
import { refreshMe } from '../store'

const route = useRoute()
const tabs = [
  { label: '全部', value: null },
  { label: '待支付', value: 10 },
  { label: '待发货', value: 20 },
  { label: '待收货', value: 30 },
  { label: '已完成', value: 40 },
]
const status = ref(null)
const page = ref(1)
const totalPages = ref(1)
const orders = ref([])
const detail = ref(null)
const refundOrder = ref(null)
const refundForm = reactive({ type: 1, reason: '' })
const refundSubmitting = ref(false)

function openRefund(o) {
  refundForm.type = 1
  refundForm.reason = ''
  refundOrder.value = o
}

async function submitRefund() {
  if (!refundForm.reason.trim()) return toast('请填写申请原因', 'err')
  refundSubmitting.value = true
  try {
    const data = await api.post('/api/v1/refunds', {
      orderNo: refundOrder.value.orderNo,
      type: refundForm.type,
      reason: refundForm.reason.trim(),
    })
    refundOrder.value = null
    toast(data.statusText === '已退款' ? '坏果包赔审核通过，退款原路返回' : '已提交，等待审核')
    load()
  } catch (e) {
    toast(e.message, 'err')
  } finally {
    refundSubmitting.value = false
  }
}

async function load() {
  const data = await api.get(`/api/v1/orders?${new URLSearchParams({
    page: page.value, size: 6, ...(status.value ? { status: status.value } : {}),
  })}`)
  const enriched = await Promise.all(data.records.map(async (o) => {
    const d = await api.get(`/api/v1/orders/${o.orderNo}`)
    return { ...o, count: d.items.reduce((s, it) => s + it.quantity, 0) }
  }))
  orders.value = enriched
  totalPages.value = Math.max(1, Math.ceil(data.total / 6))

  // 从结算页跳转过来：自动弹出支付
  if (route.query.pay && page.value === 1) {
    const target = orders.value.find((o) => o.orderNo === route.query.pay)
    if (target && target.status === 10) await pay(target)
    router_safe_clean()
  }
}

function router_safe_clean() {
  // 清掉 pay 参数避免刷新重复弹支付
  window.history.replaceState({}, '', '/orders')
}

function pickStatus(v) {
  status.value = v
  page.value = 1
  load()
}

function goPage(n) {
  page.value = n
  load()
}

    async function pay(o) {
      try {
        await api.post(`/api/v1/payments/${o.orderNo}/mock-pay`)
        toast('支付成功（演示环境模拟微信支付）')
        load()
      } catch (e) {
        toast(e.message, 'err')
      }
    }

async function cancel(o) {
  await api.post(`/api/v1/orders/${o.orderNo}/cancel`)
  toast('订单已取消，库存已释放')
  load()
}

async function confirm(o) {
  await api.post(`/api/v1/orders/${o.orderNo}/confirm`)
  toast('已确认收货，感谢购买')
  load()
}

async function openDetail(o) {
  detail.value = await api.get(`/api/v1/orders/${o.orderNo}`)
}

onMounted(load)
</script>
