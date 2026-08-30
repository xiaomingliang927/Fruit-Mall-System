<template>
  <div>
    <div class="hero">
      <div>
        <h1>产地直采 · 新鲜到家</h1>
        <p>时令鲜果 24 小时直达 🚚 坏果包赔 · 极速退款</p>
      </div>
      <div class="emoji">🍎🍊🍇🍓</div>
    </div>

    <div class="cats">
      <span class="cat-chip" :class="{ active: !categoryId }" @click="pickCategory(null)">全部</span>
      <span v-for="c in categories" :key="c.id" class="cat-chip"
            :class="{ active: categoryId === c.id }" @click="pickCategory(c.id)">{{ c.name }}</span>
    </div>

    <div v-if="products.length" class="grid">
      <div v-for="p in products" :key="p.id" class="p-card" @click="$router.push(`/product/${p.id}`)">
        <img :src="p.mainImage" :alt="p.name" />
        <div class="p-body">
          <div class="p-name">{{ p.name }}</div>
          <div class="p-sub">{{ p.subtitle }}</div>
          <div class="p-foot">
            <span class="price p-price"><small>¥</small>{{ yuan(p.minPrice) }}<small> 起</small></span>
            <span class="p-sales">已售 {{ p.sales }}</span>
          </div>
        </div>
      </div>
    </div>
    <div v-else-if="!loading" class="cart-empty">
      <div class="big">🔍</div>没有找到相关商品，换个关键词试试
    </div>

    <div class="pager">
      <button :disabled="page <= 1" @click="goPage(page - 1)">上一页</button>
      <button v-for="n in totalPages" :key="n" :class="{ cur: n === page }" @click="goPage(n)">{{ n }}</button>
      <button :disabled="page >= totalPages" @click="goPage(page + 1)">下一页</button>
    </div>
  </div>
</template>

<script setup>
import { onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { api, yuan } from '../api'

const route = useRoute()
const router = useRouter()
const categories = ref([])
const products = ref([])
const categoryId = ref(null)
const keyword = ref(route.query.keyword || '')
const page = ref(1)
const size = 8
const total = ref(0)
const totalPages = ref(1)
const loading = ref(true)

async function load() {
  loading.value = true
  const data = await api.get(`/api/v1/products?${new URLSearchParams({
    page: page.value, size, keyword: keyword.value || '',
    ...(categoryId.value ? { categoryId: categoryId.value } : {}),
  })}`)
  products.value = data.records
  total.value = data.total
  totalPages.value = Math.max(1, Math.ceil(data.total / size))
  loading.value = false
}

function pickCategory(id) {
  categoryId.value = id
  page.value = 1
  load()
}

function goPage(n) {
  page.value = n
  load()
  window.scrollTo({ top: 0 })
}

watch(() => route.query.keyword, (v) => {
  keyword.value = v || ''
  page.value = 1
  load()
})

onMounted(async () => {
  categories.value = await api.get('/api/v1/categories')
  load()
})
</script>
