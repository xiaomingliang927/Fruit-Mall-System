<template>
  <div>
    <el-card shadow="never" class="toolbar">
      <div class="bar">
        <el-select v-model="status" placeholder="评价状态" style="width: 140px" clearable>
          <el-option label="显示中" :value="1" />
          <el-option label="已隐藏" :value="0" />
        </el-select>
        <el-input v-model="keyword" placeholder="评价内容 / 商品名" style="width: 220px" clearable @keyup.enter="load" />
        <el-button type="primary" @click="load">查询</el-button>
        <el-button @click="reset">重置</el-button>
      </div>
    </el-card>

    <el-card shadow="never" class="panel" style="margin-top: 12px">
      <el-table :data="rows" v-loading="loading" stripe>
        <el-table-column label="评分" width="130">
          <template #default="{ row }">
            <span class="stars">
              <span v-for="n in 5" :key="n" :class="{ on: n <= row.rating }">★</span>
            </span>
          </template>
        </el-table-column>
        <el-table-column label="评价内容" min-width="220">
          <template #default="{ row }">
            <div>{{ row.content }}</div>
            <div v-if="row.adminReply" class="reply">商家回复：{{ row.adminReply }}</div>
          </template>
        </el-table-column>
        <el-table-column prop="userNickname" label="评价人" width="110" />
        <el-table-column prop="productName" label="商品" min-width="150" show-overflow-tooltip />
        <el-table-column label="状态" width="90">
          <template #default="{ row }">
            <el-tag :type="row.status === 1 ? 'success' : 'info'" size="small" effect="light">
              {{ row.status === 1 ? '显示中' : '已隐藏' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="时间" width="150">
          <template #default="{ row }">{{ fmtTime(row.createdAt) }}</template>
        </el-table-column>
        <el-table-column label="操作" width="170" fixed="right">
          <template #default="{ row }">
            <el-button size="small" @click="reply(row)">回复</el-button>
            <el-button v-if="row.status === 1" size="small" type="warning" plain @click="setStatus(row, 0)">隐藏</el-button>
            <el-button v-else size="small" type="success" plain @click="setStatus(row, 1)">显示</el-button>
          </template>
        </el-table-column>
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
import { ElMessage, ElMessageBox } from 'element-plus'
import { api, fmtTime } from '../api'

const rows = ref([])
const total = ref(0)
const page = ref(1)
const pageSize = 10
const status = ref(null)
const keyword = ref('')
const loading = ref(false)

async function load() {
  loading.value = true
  try {
    const data = await api.get(`/api/admin/reviews?${new URLSearchParams({
      page: page.value, size: pageSize,
      ...(status.value !== null && status.value !== undefined && status.value !== ''
        ? { status: status.value } : {}),
      ...(keyword.value ? { keyword: keyword.value } : {}),
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
  status.value = null
  keyword.value = ''
  page.value = 1
  load()
}

async function reply(row) {
  const r = await ElMessageBox.prompt(`回复「${row.userNickname}」的评价：`, '商家回复', {
    confirmButtonText: '回复', cancelButtonText: '取消',
    inputValidator: (v) => (v && v.trim() ? true : '回复内容不能为空'),
  })
  try {
    await api.post(`/api/admin/reviews/${row.id}/reply`, { reply: r.value.trim() })
    ElMessage.success('已回复')
    load()
  } catch (e) {
    ElMessage.error(e.message)
  }
}

async function setStatus(row, v) {
  const action = v === 1 ? '显示' : '隐藏'
  try {
    await ElMessageBox.confirm(`确认${action}该条评价？`, action, { type: 'warning' })
    await api.put(`/api/admin/reviews/${row.id}/status?status=${v}`)
    ElMessage.success(`已${action}`)
    load()
  } catch (e) {
    if (e !== 'cancel' && e?.message) ElMessage.error(e.message)
  }
}

onMounted(load)
</script>

<style scoped>
.toolbar { border-radius: 8px; border: 1px solid #e8ebee; box-shadow: none; }
.toolbar :deep(.el-card__body) { padding: 14px 16px; }
.bar { display: flex; gap: 12px; align-items: center; flex-wrap: wrap; }
.panel { border-radius: 8px; border: 1px solid #e8ebee; box-shadow: none; }
.stars { color: #f7ba2a; font-size: 14px; letter-spacing: 2px; }
.stars span:not(.on) { color: #e2e5e9; }
.reply { margin-top: 6px; font-size: 12.5px; color: #2e9e6b; background: #f0f9f4; border-radius: 4px; padding: 4px 8px; }
</style>
