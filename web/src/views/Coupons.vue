<template>
  <div>
    <h3 style="margin-bottom:14px">领券中心</h3>
    <div v-if="!list.length" class="cart-empty">暂无可领取的优惠券，常回来看看～</div>
    <div class="coupon-list">
      <div v-for="c in list" :key="c.id" class="coupon-card" :class="{ soldout: c.remaining <= 0 }">
        <div class="c-left">
          <template v-if="c.type === 2">
            <div class="c-big">{{ (c.discountPercent / 10).toFixed(1) }}<span class="c-unit">折</span></div>
          </template>
          <template v-else>
            <div class="c-big"><span class="c-rmb">¥</span>{{ yuan(c.discountAmount).toFixed(0) }}</div>
          </template>
          <div class="c-threshold">{{ c.thresholdAmount > 0 ? '满 ' + yuan(c.thresholdAmount).toFixed(0) + ' 元可用' : '无门槛' }}</div>
        </div>
        <div class="c-mid">
          <div class="c-name">{{ c.name }}</div>
          <div class="c-type">{{ c.typeText }} · 领取后 {{ 30 }} 天内有效</div>
          <div class="c-time">{{ fmtTime(c.startTime) }} ~ {{ fmtTime(c.endTime) }}</div>
        </div>
        <div class="c-right">
          <button v-if="c.remaining > 0" class="btn btn-sm receive-btn"
                  :disabled="receivedMap[c.id]" @click="receive(c)">
            {{ receivedMap[c.id] ? '已领取' : '立即领取' }}
          </button>
          <span v-else class="soldout-txt">已抢完</span>
          <div class="c-remaining">剩余 {{ c.remaining }} 张</div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { api, fmtTime, toast, yuan } from '../api'
import { useStore } from '../store'
import router from '../router'

const list = ref([])
const receivedMap = reactive({})

async function load() {
  list.value = await api.get('/api/v1/coupons/list')
}

async function receive(c) {
  if (!useStore.token) {
    return router.push({ name: 'login', query: { redirect: '/coupons' } })
  }
  try {
    await api.post('/api/v1/coupons/receive', { couponId: c.id })
    receivedMap[c.id] = true
    toast('领取成功，下单时可选用')
    load()
  } catch (e) {
    toast(e.message, 'err')
  }
}

onMounted(load)
</script>

<style scoped>
.coupon-list { display: grid; grid-template-columns: 1fr 1fr; gap: 14px; }
.coupon-card {
  display: flex; align-items: center; background: #fff; border: 1px solid var(--line);
  border-radius: 8px; overflow: hidden;
}
.coupon-card.soldout { opacity: .55; }
.c-left {
  width: 150px; align-self: stretch; flex: none; color: #fff;
  background: var(--green-700); display: flex; flex-direction: column;
  align-items: center; justify-content: center; padding: 16px 8px;
}
.c-big { font-size: 30px; font-weight: 800; line-height: 1.1; }
.c-rmb { font-size: 15px; }
.c-unit { font-size: 14px; }
.c-threshold { font-size: 11.5px; opacity: .85; margin-top: 5px; }
.c-mid { flex: 1; padding: 14px 14px; min-width: 0; }
.c-name { font-size: 15px; font-weight: 600; color: var(--ink); }
.c-type { font-size: 12px; color: var(--muted); margin-top: 5px; }
.c-time { font-size: 11.5px; color: #a8afb8; margin-top: 4px; }
.c-right { padding: 0 16px 0 0; text-align: center; }
.receive-btn { background: var(--danger); }
.receive-btn:hover { background: #c8232c; }
.receive-btn:disabled { background: #f3f4f6; color: #a8afb8; }
.soldout-txt { font-size: 12.5px; color: #a8afb8; }
.c-remaining { font-size: 11px; color: #a8afb8; margin-top: 5px; }
@media (max-width: 900px) { .coupon-list { grid-template-columns: 1fr; } }
</style>
