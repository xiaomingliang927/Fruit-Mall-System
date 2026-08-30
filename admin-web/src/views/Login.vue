<template>
  <div class="login-bg">
    <el-card class="login-card">
      <h2 style="text-align:center;margin: 4px 0 4px">🍊 鲜果集 · 管理后台</h2>
      <p style="text-align:center;color:#909399;font-size:13px;margin-bottom:22px">商家运营一体化工作台</p>
      <el-form @submit.prevent>
        <el-form-item>
          <el-input v-model="username" placeholder="用户名" size="large">
            <template #prefix><el-icon><User /></el-icon></template>
          </el-input>
        </el-form-item>
        <el-form-item>
          <el-input v-model="password" type="password" placeholder="密码" size="large" show-password
                    @keyup.enter="doLogin">
            <template #prefix><el-icon><Lock /></el-icon></template>
          </el-input>
        </el-form-item>
        <el-button type="success" size="large" style="width:100%" :loading="loading" @click="doLogin">
          登 录
        </el-button>
      </el-form>
      <p style="text-align:center;color:#c0c4cc;font-size:12px;margin-top:18px">默认账号：admin / admin123</p>
    </el-card>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { api } from '../api'
import { adminStore } from '../store'

const router = useRouter()
const username = ref('admin')
const password = ref('')
const loading = ref(false)

async function doLogin() {
  if (!username.value || !password.value) {
    ElMessage.warning('请输入用户名和密码')
    return
  }
  loading.value = true
  try {
    const data = await api.post('/api/admin/auth/login', {
      username: username.value, password: password.value,
    })
    adminStore.token = data.token
    adminStore.name = data.realName || data.username
    ElMessage.success(`欢迎，${adminStore.name}`)
    router.push('/dashboard')
  } catch (e) {
    ElMessage.error(e.message)
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.login-bg {
  min-height: 100vh; display: flex; align-items: center; justify-content: center;
  background: linear-gradient(120deg, #14532d, #1f7a3d 60%, #2e9e5b);
}
.login-card { width: 380px; border-radius: 14px; }
</style>
