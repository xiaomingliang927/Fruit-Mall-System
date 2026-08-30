<template>
  <div v-if="product" class="detail">
    <img class="main" :src="product.mainImage" :alt="product.name" />
    <div>
      <div class="d-title">{{ product.name }}</div>
      <div class="d-sub">{{ product.subtitle }}</div>
      <div class="d-price-box">
        <span class="price d-price"><small style="font-size:14px">¥ </small>{{ yuan(currentSku?.price || 0) }}</span>
        <span style="font-size:12.5px;color:var(--muted)">/ {{ product.unit }} · {{ currentSku?.spec }}</span>
      </div>
      <div class="d-meta">
        <div class="row"><span class="lab">产地</span><span>{{ product.origin || '甄选产区' }} ｜ 已售 {{ product.sales }} 件</span></div>
        <div class="row">
          <span v-for="t in (product.tags || '').split(',').filter(Boolean)" :key="t" class="tag">{{ t }}</span>
        </div>
        <div class="row"><span class="lab">保障</span><span>坏果包赔：收货 24 小时内拍照，极速退款</span></div>
      </div>

      <div class="sku-label">选择规格</div>
      <div class="sku-row">
        <span v-for="s in product.skus" :key="s.id" class="sku-chip"
              :class="{ active: currentSku?.id === s.id }" @click="currentSku = s">
          {{ s.spec }} · ¥{{ yuan(s.price) }}
          <small v-if="s.stock <= 0" style="color:var(--danger)">（缺货）</small>
        </span>
      </div>

      <div class="qty-row">
        <span>购买数量</span>
        <div class="stepper">
          <button @click="qty = Math.max(1, qty - 1)">−</button>
          <input v-model.number="qty" />
          <button @click="qty = Math.min(99, qty + 1)">＋</button>
        </div>
        <span style="color:var(--muted);font-size:12.5px">库存 {{ currentSku?.stock ?? 0 }} 件</span>
      </div>

      <div class="d-actions">
        <button class="btn btn-warn" :disabled="!currentSku || currentSku.stock <= 0" @click="addToCart">加入购物车</button>
        <button class="btn" :disabled="!currentSku || currentSku.stock <= 0" @click="buyNow">立即购买</button>
      </div>
    </div>
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { api, toast, yuan } from '../api'
import { refreshMe } from '../store'

const route = useRoute()
const router = useRouter()
const product = ref(null)
const currentSku = ref(null)
const qty = ref(1)

onMounted(async () => {
  product.value = await api.get(`/api/v1/products/${route.params.id}`)
  currentSku.value = product.value.skus.find((s) => s.stock > 0) || product.value.skus[0]
})

async function addToCart() {
  await api.post('/api/v1/cart/items', { skuId: currentSku.value.id, quantity: qty.value })
  await refreshMe()
  toast('已加入购物车')
}

function buyNow() {
  router.push({
    name: 'checkout',
    query: { skuId: currentSku.value.id, productId: product.value.id, qty: qty.value },
  })
}
</script>
