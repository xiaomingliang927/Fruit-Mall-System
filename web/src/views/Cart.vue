<template>
  <div>
    <h3 style="margin-bottom:14px">我的购物车</h3>
    <div v-if="!items.length" class="cart-empty">
      购物车还是空的，<router-link to="/" style="color:var(--green-700)">去逛逛 →</router-link>
    </div>
    <template v-else>
      <div class="cart-list">
        <div class="cart-head">
          <span></span><span></span><span>商品</span><span>单价</span><span>数量</span><span>小计</span><span></span>
        </div>
        <div v-for="it in items" :key="it.itemId" class="cart-row" :class="{ invalid: it.stock <= 0 }">
          <input type="checkbox" :checked="it.checked" :disabled="it.stock <= 0"
                 @change="toggle(it, $event.target.checked)" />
          <img :src="it.image" />
          <div>
            <div class="cname">{{ it.productName }}</div>
            <div class="cspec">{{ it.spec }}</div>
          </div>
          <span class="price">¥{{ yuan(it.price) }}</span>
          <div class="stepper">
            <button @click="changeQty(it, it.quantity - 1)">−</button>
            <input :value="it.quantity" readonly />
            <button @click="changeQty(it, it.quantity + 1)">＋</button>
          </div>
          <span class="price">¥{{ yuan(it.subtotal) }}</span>
          <span class="link-danger" @click="remove(it)">删除</span>
        </div>
      </div>
      <div class="cart-footer">
        <span style="color:var(--muted)">共 {{ summary.totalCount }} 件商品</span>
        <span>合计：<b class="price" style="font-size:22px">¥{{ yuan(summary.totalAmount) }}</b></span>
        <button class="btn" style="padding:11px 34px" :disabled="!summary.totalCount" @click="$router.push('/checkout')">
          去结算
        </button>
      </div>
    </template>
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { api, toast, yuan } from '../api'
import { refreshMe } from '../store'

const summary = ref({ items: [], totalCount: 0, totalAmount: 0 })
const items = computed(() => summary.value.items)

async function load() {
  summary.value = await api.get('/api/v1/cart')
}

async function toggle(it, checked) {
  await api.put(`/api/v1/cart/items/${it.itemId}`, { checked })
  load()
}

async function changeQty(it, qty) {
  if (qty < 1 || qty > 99) return
  if (qty > it.stock) { toast('超出库存数量', 'err'); return }
  await api.put(`/api/v1/cart/items/${it.itemId}`, { quantity: qty })
  load()
  refreshMe()
}

async function remove(it) {
  await api.delete(`/api/v1/cart/items/${it.itemId}`)
  load()
  refreshMe()
}

onMounted(load)
</script>
