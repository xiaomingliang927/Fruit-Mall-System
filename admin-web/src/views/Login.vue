<template>
  <div class="login-page">
    <!-- 全屏背景照片 -->
    <img class="bg-photo" :src="'/images/products/fruit-strawberry.jpg'" alt="" />
    <div class="bg-mask"></div>

    <!-- 右半侧：半透明玻璃面板 -->
    <div class="glass-side">
      <div class="form-box">
        <div class="brand-row">
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
          <el-button type="primary" size="large" class="login-btn" :loading="loading" @click="doLogin">
            登 录
          </el-button>
        </el-form>
        <p class="tip">默认账号：admin / admin123</p>

        <div class="side-brand">
          <div class="sb-name">果上选</div>
          <div class="sb-slogan">产地直采 · 新鲜到家</div>
          <div class="sb-points"><span>产地直采</span><span>坏果包赔</span><span>极速退款</span></div>
        </div>
      </div>
      <div class="form-foot">© 2026 果上选 · 水果商城管理系统</div>
    </div>
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
    router.push('/dashboard')
  } catch (e) {
    ElMessage.error(e.message)
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.login-page {
  min-height: 100vh; display: flex; position: relative; overflow: hidden;
  background: #14351f;
}

/* 全屏背景照片 */
.bg-photo { position: fixed; inset: 0; width: 100%; height: 100%; object-fit: cover; }
.bg-mask {
  position: fixed; inset: 0;
  background: linear-gradient(180deg, rgba(8, 32, 18, 0.25) 0%, rgba(8, 32, 18, 0.6) 100%);
}

/* 右半侧玻璃面板 */
.glass-side {
  margin-left: auto; width: 46%; min-width: 400px; height: 100vh;
  background: rgba(255, 255, 255, 0.14);
  backdrop-filter: blur(18px) saturate(1.2);
  -webkit-backdrop-filter: blur(18px) saturate(1.2);
  border-left: 1px solid rgba(255, 255, 255, 0.28);
  display: flex; flex-direction: column; align-items: center; justify-content: center;
  position: relative; z-index: 2; padding: 40px 24px; color: #fff;
}

.form-box { width: 360px; max-width: 92vw; }
.brand-row { display: flex; align-items: center; gap: 12px; margin-bottom: 34px; }
.mark {
  width: 46px; height: 46px; border-radius: 11px; flex: none;
  background: rgba(255, 255, 255, 0.2); border: 1px solid rgba(255, 255, 255, 0.4);
  backdrop-filter: blur(6px);
  color: #fff; font-size: 22px; font-weight: 800;
  display: flex; align-items: center; justify-content: center;
}
.bt1 { font-size: 19px; font-weight: 700; color: #fff; text-shadow: 0 1px 6px rgba(0, 0, 0, 0.3); }
.bt2 { font-size: 12.5px; color: rgba(255, 255, 255, 0.8); margin-top: 2px; }

/* 输入框：玻璃质感 */
.form-box :deep(.el-input__wrapper) {
  background: rgba(255, 255, 255, 0.16);
  border: 1px solid rgba(255, 255, 255, 0.35);
  box-shadow: none; backdrop-filter: blur(6px);
}
.form-box :deep(.el-input__wrapper.is-focus) {
  background: rgba(255, 255, 255, 0.24);
  border-color: rgba(255, 255, 255, 0.65);
}
.form-box :deep(.el-input__inner) { color: #fff; }
.form-box :deep(.el-input__inner::placeholder) { color: rgba(255, 255, 255, 0.65); }
.form-box :deep(.el-input__prefix), .form-box :deep(.el-input__suffix) { color: rgba(255, 255, 255, 0.75); }

.login-btn {
  width: 100%; margin-top: 4px; letter-spacing: 6px;
  background: rgba(255, 255, 255, 0.92); color: #14532d;
  border: none; font-weight: 700;
}
.login-btn:hover { background: #fff; }

.tip { text-align: center; color: rgba(255, 255, 255, 0.65); font-size: 12px; margin-top: 16px; }

/* 面板底部品牌区 */
.side-brand { margin-top: 64px; border-top: 1px solid rgba(255, 255, 255, 0.22); padding-top: 26px; }
.sb-name { font-size: 24px; font-weight: 800; letter-spacing: 3px; }
.sb-slogan { font-size: 13px; color: rgba(255, 255, 255, 0.8); margin-top: 6px; letter-spacing: 1px; }
.sb-points { margin-top: 16px; display: flex; gap: 8px; flex-wrap: wrap; }
.sb-points span {
  font-size: 12px; padding: 4px 13px; border-radius: 999px;
  border: 1px solid rgba(255, 255, 255, 0.38); background: rgba(255, 255, 255, 0.12);
}

.form-foot {
  position: absolute; bottom: 18px; font-size: 12px;
  color: rgba(255, 255, 255, 0.55);
}

/* 小屏：面板全宽 */
@media (max-width: 900px) {
  .glass-side { width: 100%; min-width: 0; }
  .side-brand { margin-top: 40px; }
}
</style>
