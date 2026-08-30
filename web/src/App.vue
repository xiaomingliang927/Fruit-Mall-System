<template>
  <div class="app-shell">
    <NavBar />
    <main class="page">
      <router-view />
    </main>
    <footer class="foot">鲜果集 · 水果商城 — 产地直采 · 坏果包赔 · 极速退款（演示项目）</footer>  </div>
</template>

<script setup>
import NavBar from './components/NavBar.vue'
</script>
