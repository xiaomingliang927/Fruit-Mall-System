<template>
  <el-container class="shell">
    <!-- 侧边栏 -->
    <aside class="side">
      <div class="brand">
        <div class="mark">鲜</div>
        <div class="bt">
          <div class="t1">果上选</div>
          <div class="t2">商家管理后台</div>
        </div>
      </div>
      <el-menu :default-active="$route.path" router class="menu"
               background-color="transparent" text-color="#9aa8b8" active-text-color="#ffffff">
        <template v-for="group in groups" :key="group.title">
          <el-menu-item-group :title="group.title">
            <el-menu-item v-for="item in group.items" :key="item.path" :index="item.path">
              <el-icon><component :is="item.icon" /></el-icon><span>{{ item.title }}</span>
            </el-menu-item>
          </el-menu-item-group>
        </template>
      </el-menu>
      <div class="side-foot">v0.1.0</div>
    </aside>

    <el-container class="body">
      <!-- 顶栏（必须用 el-header，el-container 才会按纵向排布） -->
      <el-header class="head" height="56px">
        <div class="crumb">{{ $route.meta.title }}</div>
        <el-dropdown trigger="click" @command="onCommand">
          <div class="user">
            <div class="avatar">{{ (adminStore.name || '管')[0] }}</div>
            <span class="uname">{{ adminStore.name || '管理员' }}</span>
            <el-icon color="#8a939f"><ArrowDown /></el-icon>
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
import { computed } from 'vue'
import { useRouter } from 'vue-router'
import { adminStore } from '../store'
import { visibleMenuGroups } from '../menu'

const router = useRouter()
const groups = computed(() => visibleMenuGroups(adminStore.permissions))

function onCommand(cmd) {
  if (cmd === 'logout') {
    adminStore.token = ''
    adminStore.name = ''
    adminStore.permissions = []
    adminStore.superAdmin = false
    router.push('/login')
  }
}
</script>

<style scoped>
.shell { min-height: 100vh; }

/* ===== 侧边栏：深蓝灰平面风格 ===== */
.side {
  width: 216px; flex: none; display: flex; flex-direction: column;
  background: #1d2733;
  position: sticky; top: 0; height: 100vh;
}
.brand { display: flex; align-items: center; gap: 11px; padding: 18px 18px 14px; }
.mark {
  width: 36px; height: 36px; border-radius: 8px; flex: none;
  background: #4080ff; color: #fff; font-size: 17px; font-weight: 700;
  display: flex; align-items: center; justify-content: center;
}
.bt .t1 { color: #fff; font-size: 16px; font-weight: 600; letter-spacing: 1px; }
.bt .t2 { color: #7d8b9c; font-size: 11px; letter-spacing: 1px; margin-top: 1px; }

.menu { border: none; flex: 1; padding: 2px 10px; overflow-y: auto; }
.menu :deep(.el-menu-item-group__title) {
  color: #66768a; font-size: 11.5px; padding: 14px 8px 4px; letter-spacing: 1px;
}
.menu :deep(.el-menu-item) {
  height: 40px; margin: 2px 0; border-radius: 6px; font-size: 14px;
  transition: background 0.15s;
}
.menu :deep(.el-menu-item:hover) { background: rgba(255, 255, 255, 0.06); }
.menu :deep(.el-menu-item.is-active) { background: #4080ff; font-weight: 500; }
.side-foot { color: #55636f; font-size: 11px; text-align: center; padding: 14px; }

/* ===== 顶栏 ===== */
.body { background: #f0f2f5; }
.head {
  background: #fff; display: flex; align-items: center; justify-content: space-between;
  padding: 0 20px; border-bottom: 1px solid #e8ebee; position: sticky; top: 0; z-index: 10;
}
.crumb { font-size: 15px; font-weight: 600; color: #1f2937; }
.user { display: flex; align-items: center; gap: 9px; cursor: pointer; outline: none; }
.avatar {
  width: 30px; height: 30px; border-radius: 50%; color: #fff; font-size: 13px; font-weight: 600;
  background: #4080ff; display: flex; align-items: center; justify-content: center;
}
.uname { font-size: 13.5px; color: #374151; }

/* ===== 主区 ===== */
.main { padding: 16px 18px 28px; }
</style>
