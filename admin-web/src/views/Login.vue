<template>
  <div class="login-bg">
    <el-card class="login-card">
      <div class="brand">
        <div class="mark">鲜</div>
        <div>
          <div class="bt1">果上选 · 管理后台</div>
          <div class="bt2">商家运营一体化工作台</div>
        </div>
      </div>
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
        <el-button type="primary" size="large" style="width:100%" :loading="loading" @click="doLogin">
          登 录
        </el-button>
      </el-form>
      <p class="tip">默认账号：admin / admin123</p>
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
    adminStore.permissions = data.permissions || []
    adminStore.superAdmin = !!data.superAdmin
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
  background: #1d2733;
}
.login-card { width: 380px; border-radius: 8px; }
.brand { display: flex; align-items: center; gap: 12px; margin: 4px 0 26px; }
.mark {
  width: 40px; height: 40px; border-radius: 8px; flex: none;
  background: #4080ff; color: #fff; font-size: 19px; font-weight: 700;
  display: flex; align-items: center; justify-content: center;
}
.bt1 { font-size: 17px; font-weight: 700; color: #1f2937; }
.bt2 { font-size: 12px; color: #909399; margin-top: 2px; }
.tip { text-align: center; color: #c0c4cc; font-size: 12px; margin-top: 18px; }
</style>
