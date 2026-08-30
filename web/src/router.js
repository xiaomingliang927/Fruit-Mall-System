import { createRouter, createWebHistory } from 'vue-router'
import { useStore } from './store'

const routes = [
  { path: '/', name: 'home', component: () => import('./views/Home.vue') },
  { path: '/login', name: 'login', component: () => import('./views/Login.vue') },
  { path: '/product/:id', name: 'product', component: () => import('./views/ProductDetail.vue') },
  { path: '/cart', name: 'cart', component: () => import('./views/Cart.vue'), meta: { requiresAuth: true } },
  { path: '/checkout', name: 'checkout', component: () => import('./views/Checkout.vue'), meta: { requiresAuth: true } },
  { path: '/orders', name: 'orders', component: () => import('./views/Orders.vue'), meta: { requiresAuth: true } },
]

const router = createRouter({ history: createWebHistory(), routes })

router.beforeEach((to) => {
  if (to.meta.requiresAuth && !useStore.token) {
    return { name: 'login', query: { redirect: to.fullPath } }
  }
})

export default router
