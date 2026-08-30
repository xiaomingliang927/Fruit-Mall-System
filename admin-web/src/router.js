import { createRouter, createWebHistory } from 'vue-router'
import { adminStore } from './store'

const routes = [
  {
    path: '/login',
    name: 'login',
    component: () => import('./views/Login.vue'),
  },
  {
    path: '/',
    component: () => import('./layout/AdminLayout.vue'),
    redirect: '/dashboard',
    children: [
      { path: 'dashboard', name: 'dashboard', component: () => import('./views/Dashboard.vue'), meta: { title: '数据看板' } },
      { path: 'products', name: 'products', component: () => import('./views/Products.vue'), meta: { title: '商品管理' } },
      { path: 'orders', name: 'orders', component: () => import('./views/Orders.vue'), meta: { title: '订单管理' } },
    ],
  },
]

const router = createRouter({ history: createWebHistory(), routes })

router.beforeEach((to) => {
  if (to.name !== 'login' && !adminStore.token) {
    return { name: 'login' }
  }
})

export default router
