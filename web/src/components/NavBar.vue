<template>
  <header class="nav">
    <div class="nav-inner">
      <div class="logo" @click="$router.push('/')">🍏 鲜果集</div>
      <div class="nav-search">
        <input v-model="keyword" placeholder="搜索水果：苹果 / 车厘子 / 草莓…" @keyup.enter="goSearch" />
        <button @click="goSearch">搜索</button>
      </div>
      <div class="nav-right">
        <a class="nav-link" @click="$router.push('/cart')">🛒 购物车<span v-if="globalState.cartCount" class="badge">{{ globalState.cartCount }}</span></a>
        <a class="nav-link" @click="$router.push('/orders')">📦 我的订单</a>
        <template v-if="globalState.nickname">
          <span style="color: var(--green-700)">👤 {{ globalState.nickname }}</span>
          <a class="nav-link" @click="logout">退出</a>
        </template>
        <a v-else class="nav-link" style="color: var(--green-700); font-weight: 600" @click="$router.push('/login')">登录 / 注册</a>
      </div>
    </div>
  </header>
</template>

<script setup>
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { globalState, useStore } from '../store'

const router = useRouter()
const keyword = ref('')

function goSearch() {
  router.push({ name: 'home', query: { keyword: keyword.value || undefined } })
}

function logout() {
  useStore.token = ''
  globalState.nickname = ''
  globalState.cartCount = 0
  router.push('/')
}
</script>
