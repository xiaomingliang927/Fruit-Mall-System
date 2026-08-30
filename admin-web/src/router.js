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
      { path: 'revenue', name: 'revenue', component: () => import('./views/Revenue.vue'), meta: { title: '营业统计' } },
      { path: 'coupons', name: 'coupons', component: () => import('./views/Coupons.vue'), meta: { title: '优惠券' } },
      { path: 'products', name: 'products', component: () => import('./views/Products.vue'), meta: { title: '商品列表' } },
      { path: 'orders', name: 'orders', component: () => import('./views/Orders.vue'), meta: { title: '订单列表' } },
      { path: 'refunds', name: 'refunds', component: () => import('./views/Refunds.vue'), meta: { title: '售后管理' } },
      { path: 'members', name: 'members', component: () => import('./views/Members.vue'), meta: { title: '会员列表' } },
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
