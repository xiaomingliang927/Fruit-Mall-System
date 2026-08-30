<template>
  <div>
    <el-card shadow="never" class="toolbar">
      <div class="bar">
        <el-input v-model="keyword" placeholder="昵称 / 手机号搜索" style="width: 220px" clearable @keyup.enter="load" />
        <el-select v-model="hasOrdered" placeholder="消费情况" style="width: 140px" clearable>
          <el-option label="有消费" :value="true" />
          <el-option label="无消费" :value="false" />
        </el-select>
        <el-button type="primary" @click="load">查询</el-button>
        <el-button @click="reset">重置</el-button>
      </div>
    </el-card>

    <el-card shadow="never" class="panel" style="margin-top: 12px">
      <el-table :data="rows" v-loading="loading" stripe>
        <el-table-column prop="id" label="ID" width="64" />
        <el-table-column label="会员" min-width="180">
          <template #default="{ row }">
            <div class="member">
              <div class="mavatar">{{ (row.nickname || '会')[0] }}</div>
              <span>{{ row.nickname || '-' }}</span>
            </div>
          </template>
        </el-table-column>
        <el-table-column prop="phone" label="手机号" width="130" />
        <el-table-column label="等级" width="110">
          <template #default="{ row }">{{ levelText(row.level) }}</template>
        </el-table-column>
        <el-table-column label="累计订单" width="100" align="right">
          <template #default="{ row }">{{ row.orderCount }}</template>
        </el-table-column>
        <el-table-column label="累计消费（元）" width="140" align="right">
          <template #default="{ row }">{{ yuan(row.totalGmv).toLocaleString('zh-CN', { minimumFractionDigits: 2 }) }}</template>
        </el-table-column>
        <el-table-column label="状态" width="90">
          <template #default="{ row }">
            <el-tag :type="row.status === 1 ? 'success' : 'info'" size="small" effect="light">
              {{ row.status === 1 ? '正常' : '已禁用' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createdAt" label="注册日期" width="120" />
      </el-table>
      <div style="display:flex;justify-content:flex-end;margin-top:14px">
        <el-pagination layout="total, prev, pager, next" :total="total"
                       :page-size="pageSize" :current-page="page"
                       @current-change="(p) => { page = p; load() }" />
      </div>
    </el-card>
  </div>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { api, yuan } from '../api'

const rows = ref([])
const total = ref(0)
const page = ref(1)
const pageSize = 10
const keyword = ref('')
const hasOrdered = ref(null)
const loading = ref(false)

function levelText(level) {
  return { 1: '普通会员', 2: '白银会员', 3: '黄金会员', 4: '钻石会员' }[level] || '普通会员'
}

async function load() {
  loading.value = true
  try {
    const data = await api.get(`/api/admin/members?${new URLSearchParams({
      page: page.value, size: pageSize,
      ...(keyword.value ? { keyword: keyword.value } : {}),
      ...(hasOrdered.value !== null && hasOrdered.value !== undefined && hasOrdered.value !== ''
        ? { hasOrdered: hasOrdered.value } : {}),
    })}`)
    rows.value = data.records
    total.value = data.total
  } catch (e) {
    ElMessage.error(e.message)
  } finally {
    loading.value = false
  }
}

function reset() {
  keyword.value = ''
  hasOrdered.value = null
  page.value = 1
  load()
}

onMounted(load)
</script>

<style scoped>
.toolbar { border-radius: 8px; border: 1px solid #e8ebee; box-shadow: none; }
.toolbar :deep(.el-card__body) { padding: 14px 16px; }
.bar { display: flex; gap: 12px; align-items: center; flex-wrap: wrap; }

.panel { border-radius: 8px; border: 1px solid #e8ebee; box-shadow: none; }

.member { display: flex; align-items: center; gap: 9px; }
.mavatar {
  width: 30px; height: 30px; border-radius: 50%; flex: none; font-size: 13px; font-weight: 600;
  color: #fff; background: #8e9cb5; display: flex; align-items: center; justify-content: center;
}
</style>
