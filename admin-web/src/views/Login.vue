<template>
  <div class="login-page">
    <!-- 左侧品牌照片区 -->
    <div class="photo-side">
      <img class="photo" :src="'/images/products/fruit-strawberry.jpg'" alt="" />
      <div class="photo-mask"></div>
      <div class="photo-brand">
        <div class="pb-mark">鲜</div>
        <div class="pb-name">果上选</div>
        <div class="pb-slogan">产地直采 · 新鲜到家</div>
        <div class="pb-points">
          <span>产地直采</span><span>坏果包赔</span><span>极速退款</span>
        </div>
      </div>
    </div>

    <!-- 右侧表单区 -->
    <div class="form-side">
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
.login-page { min-height: 100vh; display: flex; }

/* 左侧照片 */
.photo-side { flex: 1.15; position: relative; overflow: hidden; background: #14351f; }
.photo { position: absolute; inset: 0; width: 100%; height: 100%; object-fit: cover; }
.photo-mask {
  position: absolute; inset: 0;
  background: linear-gradient(180deg, rgba(10, 40, 22, 0.18) 0%, rgba(10, 40, 22, 0.78) 100%);
}
.photo-brand { position: absolute; left: 48px; bottom: 52px; color: #fff; }
.pb-mark {
  width: 52px; height: 52px; border-radius: 12px; background: rgba(255, 255, 255, 0.16);
  border: 1px solid rgba(255, 255, 255, 0.35); backdrop-filter: blur(4px);
  font-size: 26px; font-weight: 800; display: flex; align-items: center; justify-content: center;
}
.pb-name { font-size: 30px; font-weight: 800; letter-spacing: 3px; margin-top: 16px; }
.pb-slogan { font-size: 15px; opacity: 0.88; margin-top: 8px; letter-spacing: 1px; }
.pb-points { margin-top: 22px; display: flex; gap: 10px; flex-wrap: wrap; }
.pb-points span {
  font-size: 12.5px; padding: 5px 14px; border-radius: 999px;
  border: 1px solid rgba(255, 255, 255, 0.4); background: rgba(255, 255, 255, 0.1);
}

/* 右侧表单 */
.form-side {
  flex: 1; display: flex; flex-direction: column; align-items: center; justify-content: center;
  background: #f5f7fa; padding: 40px 24px; position: relative;
}
.form-box { width: 380px; max-width: 92vw; }
.brand-row { display: flex; align-items: center; gap: 12px; margin-bottom: 34px; }
.mark {
  width: 44px; height: 44px; border-radius: 10px; flex: none;
  background: #4080ff; color: #fff; font-size: 21px; font-weight: 800;
  display: flex; align-items: center; justify-content: center;
}
.bt1 { font-size: 19px; font-weight: 700; color: #1f2937; }
.bt2 { font-size: 12.5px; color: #909399; margin-top: 2px; }
.login-btn { width: 100%; margin-top: 4px; letter-spacing: 6px; }
.tip { text-align: center; color: #c0c4cc; font-size: 12px; margin-top: 18px; }
.form-foot { position: absolute; bottom: 20px; font-size: 12px; color: #b9c0c9; }

/* 小屏：隐藏照片 */
@media (max-width: 900px) {
  .photo-side { display: none; }
}
</style>
