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
        <button class="btn btn-fav" :class="{ on: isFav }" @click="toggleFav">
          <svg viewBox="0 0 24 24" width="16" height="16" :fill="isFav ? 'currentColor' : 'none'"
               stroke="currentColor" stroke-width="2" style="vertical-align:-2px;margin-right:5px">
            <path d="M12 21C7 16.5 3 13.3 3 9.3 3 6.4 5.2 4 8 4c1.6 0 3.1.8 4 2 0.9-1.2 2.4-2 4-2 2.8 0 5 2.4 5 5.3 0 4-4 7.2-9 11.7z"/>
          </svg>{{ isFav ? '已收藏' : '收藏' }}
        </button>
        <button class="btn btn-warn" :disabled="!currentSku || currentSku.stock <= 0" @click="addToCart">加入购物车</button>
        <button class="btn" :disabled="!currentSku || currentSku.stock <= 0" @click="buyNow">立即购买</button>
      </div>
    </div>

    <!-- 评价区 -->
    <div class="detail reviews-card" style="display:block;margin-top:16px">
      <div style="display:flex;align-items:baseline;gap:12px;margin-bottom:12px">
        <span style="font-size:16px;font-weight:700">商品评价（{{ reviews.length }}）</span>
        <span style="font-size:13px;color:var(--muted)">评分 <b class="price" style="font-size:17px">{{ product.ratingAvg }}</b></span>
      </div>
      <div v-if="!reviews.length" style="color:var(--muted);font-size:13.5px;padding:8px 0 12px">暂无评价，买过的同学快来抢沙发～</div>
      <div v-for="r in reviews" :key="r.id" class="rv-item">
        <div class="rv-head">
          <span class="rv-user">{{ r.userNickname }}</span>
          <span class="rv-stars">
            <span v-for="n in 5" :key="n" class="rv-star" :class="{ on: n <= r.rating }">★</span>
          </span>
          <span class="rv-date">{{ r.createdAt }}</span>
        </div>
        <div class="rv-content">{{ r.content }}</div>
        <div v-if="r.adminReply" class="rv-reply">商家回复：{{ r.adminReply }}</div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { api, toast, yuan } from '../api'
import { refreshMe, useStore } from '../store'

const route = useRoute()
const router = useRouter()
const product = ref(null)
const currentSku = ref(null)
const qty = ref(1)

const isFav = ref(false)
const reviews = ref([])

onMounted(async () => {
  product.value = await api.get(`/api/v1/products/${route.params.id}`)
  currentSku.value = product.value.skus.find((s) => s.stock > 0) || product.value.skus[0]
  if (useStore.token) {
    isFav.value = await api.get(`/api/v1/favorites/${route.params.id}/exists`)
  }
  api.get(`/api/v1/products/${route.params.id}/reviews?page=1&size=5`)
    .then((d) => (reviews.value = d.records))
    .catch(() => {})
})

async function toggleFav() {
  if (!useStore.token) {
    return router.push({ name: 'login', query: { redirect: route.fullPath } })
  }
  if (isFav.value) {
    await api.delete(`/api/v1/favorites/${route.params.id}`)
    isFav.value = false
    toast('已取消收藏')
  } else {
    await api.post(`/api/v1/favorites/${route.params.id}`)
    isFav.value = true
    toast('已加入收藏')
  }
}

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
