<template>
  <div>
    <h3 style="margin-bottom:14px">我的账户</h3>
    <div class="mine-grid">
      <!-- 左：资料与入口 -->
      <div>
        <div class="panel profile">
          <div class="p-avatar">{{ (me.nickname || '会')[0] }}</div>
          <div class="p-info">
            <div class="p-name">{{ me.nickname || '-' }}</div>
            <div class="p-phone">{{ me.phone }} · {{ levelText(me.level) }}</div>
          </div>
          <button class="btn btn-ghost btn-sm" @click="logout">退出登录</button>
        </div>

        <div class="panel">
          <h3>快捷入口</h3>
          <div class="entry" @click="$router.push('/orders')">
            <span>我的订单</span><span class="arrow">→</span>
          </div>
          <div class="entry" @click="$router.push('/favorites')">
            <span>我的收藏</span><span class="arrow">→</span>
          </div>
          <div class="entry" @click="$router.push('/')">
            <span>继续购物</span><span class="arrow">→</span>
          </div>
        </div>
      </div>

      <!-- 右：地址管理 -->
      <div class="panel">
        <h3 style="display:flex;justify-content:space-between;align-items:center">
          收货地址管理
          <button class="btn btn-sm" @click="showForm = !showForm">新增地址</button>
        </h3>
        <div v-if="!addresses.length" class="addr-empty">还没有收货地址，点击右上角新增</div>
        <div v-for="a in addresses" :key="a.id" class="addr-item">
          <div class="addr-main">
            <b>{{ a.receiver }} {{ a.phone }}</b>
            <span>{{ a.province }}{{ a.city }}{{ a.district }} {{ a.detail }}</span>
          </div>
          <span v-if="a.isDefault" class="tag">默认</span>
          <span class="link-danger" @click="removeAddress(a)">删除</span>
        </div>
        <div v-if="showForm" class="addr-form">
          <input v-model.trim="form.receiver" placeholder="收货人姓名" />
          <input v-model.trim="form.phone" maxlength="11" placeholder="手机号" />
          <input v-model.trim="form.province" placeholder="省份" />
          <input v-model.trim="form.city" placeholder="城市" />
          <input v-model.trim="form.district" placeholder="区/县" />
          <input v-model.trim="form.detail" placeholder="详细地址（街道、门牌号）" />
          <button class="btn btn-sm" style="justify-self:start" @click="saveAddress">保存</button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { api, toast } from '../api'
import { globalState, useStore } from '../store'

const router = useRouter()
const me = ref({})
const addresses = ref([])
const showForm = ref(false)
const form = reactive({ receiver: '', phone: '', province: '', city: '', district: '', detail: '' })

function levelText(level) {
  return { 1: '普通会员', 2: '白银会员', 3: '黄金会员', 4: '钻石会员' }[level] || '普通会员'
}

async function load() {
  me.value = await api.get('/api/v1/users/me')
  addresses.value = await api.get('/api/v1/users/me/addresses')
}

async function saveAddress() {
  const f = form
  if (!f.receiver || !f.phone || !f.province || !f.city || !f.detail) {
    return toast('请完整填写收货信息', 'err')
  }
  await api.post('/api/v1/users/me/addresses', { ...f, isDefault: addresses.value.length === 0 })
  addresses.value = await api.get('/api/v1/users/me/addresses')
  showForm.value = false
  toast('地址已保存')
}

async function removeAddress(a) {
  await api.delete(`/api/v1/users/me/addresses/${a.id}`)
  addresses.value = await api.get('/api/v1/users/me/addresses')
  toast('已删除')
}

function logout() {
  useStore.token = ''
  globalState.nickname = ''
  globalState.cartCount = 0
  router.push('/')
}

onMounted(load)
</script>
