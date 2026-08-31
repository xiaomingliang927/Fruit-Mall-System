<template>
  <div class="login-wrap">
    <div class="login-card">
      <div class="login-brand">
        <span class="mark">鲜</span><span class="name">登录果上选</span>
      </div>
      <div class="tip">登录后可下单购买，新手机号自动注册</div>
      <input v-model.trim="phone" maxlength="11" placeholder="手机号" />
      <input v-model.trim="code" maxlength="6" placeholder="短信验证码" @keyup.enter="doLogin" />
      <button class="btn" :disabled="submitting" @click="doLogin">{{ submitting ? '登录中…' : '登录' }}</button>
      <div class="login-hint">
        演示环境：任意 11 位手机号 + 验证码 <b>123456</b> 即可登录<br />
        接入真实短信服务后，此处为「获取验证码」按钮
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { api, toast } from '../api'
import { globalState, useStore, refreshMe } from '../store'

const route = useRoute()
const router = useRouter()
const phone = ref('')
const code = ref('123456')
const submitting = ref(false)

async function doLogin() {
  if (!/^1\d{10}$/.test(phone.value)) {
    toast('请输入 11 位手机号', 'err')
    return
  }
  submitting.value = true
  try {
    const data = await api.post('/api/v1/auth/sms-login', { phone: phone.value, code: code.value })
    useStore.token = data.token
    await refreshMe()
    toast(`欢迎回来，${data.nickname}！`)
    router.push(route.query.redirect || '/')
  } catch (e) {
    toast(e.message, 'err')
  } finally {
    submitting.value = false
  }
}
</script>
