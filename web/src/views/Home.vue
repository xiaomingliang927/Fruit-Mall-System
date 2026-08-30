<template>
  <div>
    <!-- 首屏：文案 + 真实产品图 -->
    <div class="hero2">
      <div class="hero-text">
        <h1>产地直采 · 新鲜到家</h1>
        <p>每颗水果都来自优选产区，坏果包赔，收货 24 小时内极速退款</p>
        <div class="hero-actions">
          <button class="btn" @click="pickCategory(1)">查看时令鲜果</button>
          <button class="btn hero-ghost" @click="scrollToAll">浏览全部商品</button>
        </div>
      </div>
      <img class="hero-img" :src="'/images/products/fruit-strawberry.jpg'" alt="当季草莓" />
    </div>

    <!-- 服务保障 -->
    <div class="serve">
      <div class="s-item" v-for="s in services" :key="s.title">
        <span class="s-icon" v-html="s.icon"></span>
        <div><b>{{ s.title }}</b><span>{{ s.desc }}</span></div>
      </div>
    </div>

    <!-- 热销榜 -->
    <div class="sec-head">
      <span class="t">热销榜</span><span class="s">大家都在买</span>
    </div>
    <div class="rank">
      <div v-for="(p, i) in top" :key="p.id" class="rank-card" @click="$router.push(`/product/${p.id}`)">
        <span class="rank-no" :class="`no-${i + 1}`">{{ i + 1 }}</span>
        <img :src="p.mainImage" :alt="p.name" />
        <div class="rank-body">
          <div class="rank-name">{{ p.name }}</div>
          <div class="rank-foot">
            <span class="price"><small>¥</small>{{ yuan(p.minPrice) }}</span>
            <span class="rank-sales">已售 {{ p.sales }}</span>
          </div>
        </div>
      </div>
    </div>

    <!-- 全部商品 -->
    <div class="sec-head" ref="allRef">
      <span class="t">全部商品</span><span class="s">共 {{ total }} 件</span>
    </div>
    <div class="cats">
      <span v-for="c in allCategories" :key="c.id ?? 'all'" class="cat-tab"
            :class="{ active: categoryId === c.id }" @click="pickCategory(c.id)">{{ c.name }}</span>
    </div>

    <div v-if="products.length" class="grid">
      <div v-for="p in products" :key="p.id" class="p-card" @click="$router.push(`/product/${p.id}`)">
        <div class="p-imgwrap">
          <img :src="p.mainImage" :alt="p.name" />
          <span v-if="badgeOf(p)" class="p-badge">{{ badgeOf(p) }}</span>
        </div>
        <div class="p-body">
          <div class="p-name">{{ p.name }}</div>
          <div class="p-sub">{{ p.subtitle }}</div>
          <div class="p-foot">
            <span class="price p-price"><small>¥</small>{{ yuan(p.minPrice) }}<small> 起</small></span>
            <span class="p-sales">已售 {{ p.sales }} 件</span>
          </div>
        </div>
      </div>
    </div>
    <div v-else-if="!loading" class="cart-empty">没有找到相关商品，换个关键词试试</div>

    <div class="pager">
      <button :disabled="page <= 1" @click="goPage(page - 1)">上一页</button>
      <span class="info">第 {{ page }} / {{ totalPages }} 页</span>
      <button :disabled="page >= totalPages" @click="goPage(page + 1)">下一页</button>
    </div>
  </div>
</template>

<script setup>
import { computed, onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { api, yuan } from '../api'

const route = useRoute()
const router = useRouter()
const categories = ref([])
const products = ref([])
const top = ref([])
const categoryId = ref(null)
const keyword = ref(route.query.keyword || '')
const page = ref(1)
const size = 8
const total = ref(0)
const totalPages = ref(1)
const loading = ref(true)
const allRef = ref(null)

const allCategories = computed(() => [{ id: null, name: '全部' }, ...categories.value])

const services = [
  { title: '产地直采', desc: '优选产区直发', icon: '<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8"><path d="M5 21c7 0 14-5 15-16H16C8 5 5 11 5 17v4z"/><path d="M5 21c2-6 6-9 11-11"/></svg>' },
  { title: '24h 极速发货', desc: '下单次日送达', icon: '<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8"><circle cx="12" cy="12" r="9"/><path d="M12 7v5l3 3"/></svg>' },
  { title: '坏果包赔', desc: '拍照极速退款', icon: '<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8"><path d="M12 3l8 3.5v5.5c0 5-3.4 8.2-8 9.5-4.6-1.3-8-4.5-8-9.5V6.5L12 3z"/><path d="M9 12l2 2 4-4"/></svg>' },
  { title: '全程冷链', desc: '锁鲜配送到家', icon: '<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8"><path d="M12 2v20M2 12h20M5 5l14 14M19 5L5 19"/></svg>' },
]

function badgeOf(p) {
  const t = (p.tags || '').split(',').filter(Boolean)[0]
  return t || ''
}

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

function scrollToAll() {
  allRef.value?.scrollIntoView({ behavior: 'smooth' })
}

watch(() => route.query.keyword, (v) => {
  keyword.value = v || ''
  page.value = 1
  load()
})

onMounted(async () => {
  categories.value = await api.get('/api/v1/categories')
  load()
  // 热销榜：默认按销量降序取前 4
  api.get('/api/v1/products?page=1&size=4').then((d) => (top.value = d.records))
})
</script>
