<template>
  <div>
    <h3 style="margin-bottom:14px">我的收藏</h3>
    <div v-if="!items.length" class="cart-empty">
      还没有收藏商品，<router-link to="/" style="color:var(--green-700)">去逛逛 →</router-link>
    </div>
    <div v-else class="grid">
      <div v-for="it in items" :key="it.favoriteId" class="p-card">
        <div class="p-imgwrap" @click="$router.push(`/product/${it.productId}`)">
          <img :src="it.mainImage" :alt="it.name" />
          <span v-if="it.status === 0" class="p-badge off">已下架</span>
        </div>
        <div class="p-body">
          <div class="p-name" @click="$router.push(`/product/${it.productId}`)">{{ it.name }}</div>
          <div class="p-sub">{{ it.subtitle }}</div>
          <div class="p-foot">
            <span class="price p-price"><small>¥</small>{{ yuan(it.minPrice) }}</span>
            <span class="unfav" @click="remove(it)">取消收藏</span>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import { api, toast, yuan } from '../api'
import { refreshMe } from '../store'

const items = ref([])

async function load() {
  const data = await api.get('/api/v1/favorites?page=1&size=50')
  items.value = data.records
}

async function remove(it) {
  await api.delete(`/api/v1/favorites/${it.productId}`)
  toast('已取消收藏')
  load()
}

onMounted(load)
</script>
