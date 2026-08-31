<template>
  <div>
    <el-card shadow="never" class="toolbar">
      <div class="bar">
        <el-select v-model="status" placeholder="售后状态" style="width: 150px" clearable>
          <el-option label="待审核" :value="0" />
          <el-option label="已同意待退款" :value="1" />
          <el-option label="已拒绝" :value="2" />
          <el-option label="已退款" :value="4" />
          <el-option label="已撤销" :value="5" />
        </el-select>
        <el-input v-model="keyword" placeholder="售后单号 / 订单号" style="width: 220px" clearable @keyup.enter="load" />
        <el-button type="primary" @click="load">查询</el-button>
        <el-button @click="reset">重置</el-button>
      </div>
    </el-card>

    <el-card shadow="never" class="panel" style="margin-top: 12px">
      <el-table :data="rows" v-loading="loading" stripe>
        <el-table-column prop="refundNo" label="售后单号" width="185" />
        <el-table-column prop="orderNo" label="订单号" width="185" />
        <el-table-column label="买家" width="110">
          <template #default="{ row }">{{ row.userNickname || '-' }}</template>
        </el-table-column>
        <el-table-column label="类型" width="95">
          <template #default="{ row }">{{ row.typeText }}</template>
        </el-table-column>
        <el-table-column label="退款金额" width="110" align="right">
          <template #default="{ row }">
            <b style="color: #f24e3e">¥{{ yuan(row.amount).toFixed(2) }}</b>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="105">
          <template #default="{ row }">
            <el-tag :type="statusType(row.status)" size="small" effect="light">{{ row.statusText }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="申请原因" min-width="170" show-overflow-tooltip>
          <template #default="{ row }">{{ row.reason }}</template>
        </el-table-column>
        <el-table-column label="凭证" width="132" align="center">
          <template #default="{ row }">
            <div v-if="imgList(row).length" class="proofs">
              <el-image v-for="(src, i) in imgList(row)" :key="src" class="proof"
                        :src="src" :preview-src-list="imgList(row)" :initial-index="i"
                        fit="cover" preview-teleported hide-on-click-modal />
            </div>
            <span v-else class="proof-none">无凭证</span>
          </template>
        </el-table-column>
        <el-table-column label="申请时间" width="150">
          <template #default="{ row }">{{ fmtTime(row.createdAt) }}</template>
        </el-table-column>
        <el-table-column label="操作" width="150" fixed="right">
          <template #default="{ row }">
            <template v-if="row.status === 0">
              <el-button type="success" size="small" @click="audit(row, true)">通过</el-button>
              <el-button type="danger" plain size="small" @click="audit(row, false)">驳回</el-button>
            </template>
            <span v-else style="color: #c0c4cc; font-size: 12px">已处理</span>
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
import { api, fmtTime, yuan } from '../api'

const rows = ref([])
const total = ref(0)
const page = ref(1)
const pageSize = 10
const status = ref(null)
const keyword = ref('')
const loading = ref(false)

function statusType(s) {
  return { 0: 'warning', 1: 'primary', 2: 'danger', 3: 'info', 4: 'success', 5: 'info' }[s] || 'info'
}

/** 凭证图：数据库存 JSON 字符串（兼容已是数组的场景），解析为可访问的路径数组 */
function imgList(row) {
  const raw = row.images
  if (!raw) return []
  if (Array.isArray(raw)) return raw.filter(Boolean)
  try {
    const arr = JSON.parse(raw)
    return Array.isArray(arr) ? arr.filter(Boolean) : []
  } catch (e) {
    return []
  }
}

async function load() {
  loading.value = true
  try {
    const data = await api.get(`/api/admin/refunds?${new URLSearchParams({
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

async function audit(row, approve) {
  try {
    let remark = ''
    if (approve) {
      const proofTip = imgList(row).length ? `（用户上传了 ${imgList(row).length} 张凭证图，请核对后再处理）` : '（该申请未上传凭证图）'
      await ElMessageBox.confirm(
        `通过「${row.refundNo}」的售后申请？退款 ¥${yuan(row.amount).toFixed(2)} 将原路退回，订单转已退款并回补库存。${proofTip}`,
        '审核通过', { confirmButtonText: '通过并退款', cancelButtonText: '取消', type: 'warning' })
      remark = '审核通过'
    } else {
      const r = await ElMessageBox.prompt('请填写驳回原因（用户可见）：', '驳回申请', {
        confirmButtonText: '驳回', cancelButtonText: '取消',
        inputValidator: (v) => (v && v.trim() ? true : '驳回原因不能为空'),
      })
      remark = r.value
    }
    await api.post(`/api/admin/refunds/${row.id}/${approve ? 'approve' : 'reject'}`, { remark })
    ElMessage.success(approve ? '已通过并退款' : '已驳回')
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
.proofs { display: flex; gap: 6px; justify-content: center; flex-wrap: wrap; }
.proof {
  width: 46px; height: 46px; border-radius: 6px; border: 1px solid #e8ebee;
  background: #fafbfc; cursor: zoom-in;
}
.proof-none { color: #c0c4cc; font-size: 12px; }
</style>
