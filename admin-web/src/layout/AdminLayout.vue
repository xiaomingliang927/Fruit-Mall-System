<template>
  <el-container class="shell">
    <!-- 侧边栏 -->
    <aside class="side">
      <div class="brand">
        <div class="logo">🍊</div>
        <div>
          <div class="t1">鲜果集</div>
          <div class="t2">商家管理后台</div>
        </div>
      </div>
      <el-menu :default-active="$route.path" router class="menu"
               background-color="transparent" text-color="#a7c4b2" active-text-color="#ffffff">
        <el-menu-item index="/dashboard">
          <el-icon><Odometer /></el-icon><span>数据看板</span>
        </el-menu-item>
        <el-menu-item index="/products">
          <el-icon><Apple /></el-icon><span>商品管理</span>
        </el-menu-item>
        <el-menu-item index="/orders">
          <el-icon><List /></el-icon><span>订单管理</span>
        </el-menu-item>
      </el-menu>
      <div class="side-foot">M1 基础交易闭环 · v0.1</div>
    </aside>

    <el-container class="body">
      <!-- 顶栏（必须用 el-header，el-container 才会按纵向排布） -->
      <el-header class="head" height="60px">
        <div class="crumb"><span class="dot"></span>{{ $route.meta.title }}</div>
        <el-dropdown trigger="click" @command="onCommand">
          <div class="user">
            <div class="avatar">{{ (adminStore.name || '管')[0] }}</div>
            <span class="uname">{{ adminStore.name || '管理员' }}</span>
            <el-icon color="#909399"><ArrowDown /></el-icon>
          </div>
          <template #dropdown>
            <el-dropdown-menu>
              <el-dropdown-item command="logout">
                <el-icon><SwitchButton /></el-icon>退出登录
              </el-dropdown-item>
            </el-dropdown-menu>
          </template>
        </el-dropdown>
      </el-header>

      <el-main class="main"><router-view /></el-main>
    </el-container>
  </el-container>
</template>

<script setup>
import { useRouter } from 'vue-router'
import { adminStore } from '../store'

const router = useRouter()

function onCommand(cmd) {
  if (cmd === 'logout') {
    adminStore.token = ''
    adminStore.name = ''
    router.push('/login')
  }
}
</script>

<style scoped>
.shell { min-height: 100vh; }

/* ===== 侧边栏 ===== */
.side {
  width: 224px; flex: none; display: flex; flex-direction: column;
  background: linear-gradient(185deg, #0e3d1f 0%, #124624 55%, #0c331a 100%);
  position: sticky; top: 0; height: 100vh;
}
.brand { display: flex; align-items: center; gap: 12px; padding: 22px 20px 18px; }
.logo {
  width: 42px; height: 42px; border-radius: 12px; font-size: 24px;
  background: rgba(255, 255, 255, 0.12); display: flex; align-items: center; justify-content: center;
  box-shadow: inset 0 0 0 1px rgba(255, 255, 255, 0.15);
}
.t1 { color: #fff; font-size: 18px; font-weight: 800; letter-spacing: 2px; }
.t2 { color: #7fa98c; font-size: 11.5px; letter-spacing: 3px; margin-top: 2px; }

.menu { border: none; flex: 1; padding: 8px 12px; }
.menu :deep(.el-menu-item) {
  height: 46px; margin: 5px 0; border-radius: 10px; font-size: 14.5px;
  transition: all 0.18s;
}
.menu :deep(.el-menu-item:hover) { background: rgba(255, 255, 255, 0.08); }
.menu :deep(.el-menu-item.is-active) {
  background: linear-gradient(100deg, #2e9e5b, #238548);
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.25); font-weight: 600;
}
.side-foot { color: #5c8065; font-size: 11px; text-align: center; padding: 16px; letter-spacing: 1px; }

/* ===== 顶栏 ===== */
.body { background: #f3f6f2; }
.head {
  height: 60px; background: #fff; display: flex; align-items: center; justify-content: space-between;
  padding: 0 22px; box-shadow: 0 1px 6px rgba(30, 41, 59, 0.06); position: sticky; top: 0; z-index: 10;
}
.crumb { font-size: 16.5px; font-weight: 700; display: flex; align-items: center; gap: 9px; }
.dot { width: 8px; height: 8px; border-radius: 50%; background: #2e9e5b; }
.user { display: flex; align-items: center; gap: 10px; cursor: pointer; outline: none; }
.avatar {
  width: 34px; height: 34px; border-radius: 50%; color: #fff; font-size: 15px; font-weight: 700;
  background: linear-gradient(135deg, #2e9e5b, #14532d);
  display: flex; align-items: center; justify-content: center;
}
.uname { font-size: 14px; color: #374151; }

/* ===== 主区 ===== */
.main { padding: 18px 20px 30px; }
</style>
