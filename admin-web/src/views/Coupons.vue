<template>
  <div>
    <el-card shadow="never" class="toolbar">
      <div class="bar">
        <el-input v-model="keyword" placeholder="券名搜索" style="width: 200px" clearable @keyup.enter="load" />
        <el-button type="primary" @click="load">查询</el-button>
        <el-button @click="reset">重置</el-button>
        <div style="flex: 1"></div>
        <el-button type="success" @click="openCreate">＋ 新建优惠券</el-button>
      </div>
    </el-card>

    <el-card shadow="never" class="panel" style="margin-top: 12px">
      <el-table :data="rows" v-loading="loading" stripe>
        <el-table-column prop="id" label="ID" width="60" />
        <el-table-column prop="name" label="券名" min-width="150" />
        <el-table-column label="类型" width="90">
          <template #default="{ row }">{{ row.typeText }}</template>
        </el-table-column>
        <el-table-column label="面额 / 折扣" width="120">
          <template #default="{ row }">
            <span v-if="row.type === 2">{{ (row.discountPercent / 10).toFixed(1) }} 折</span>
            <span v-else>减 {{ yuan(row.discountAmount).toFixed(2) }} 元</span>
          </template>
        </el-table-column>
        <el-table-column label="门槛" width="110">
          <template #default="{ row }">{{ row.thresholdAmount > 0 ? '满 ' + yuan(row.thresholdAmount).toFixed(0) + ' 元' : '无门槛' }}</template>
        </el-table-column>
        <el-table-column label="已领 / 总量" width="100" align="center">
          <template #default="{ row }">{{ row.totalCount - row.remaining }} / {{ row.totalCount + (row.totalCount - row.remaining) }}</template>
        </el-table-column>
        <el-table-column label="剩余" width="80" align="center">
          <template #default="{ row }">
            <span :style="{ color: row.remaining === 0 ? '#f24e3e' : '#1f2937' }">{{ row.remaining }}</span>
          </template>
        </el-table-column>
        <el-table-column label="领取时间" min-width="240">
          <template #default="{ row }">{{ fmtTime(row.startTime) }} ~ {{ fmtTime(row.endTime) }}</template>
        </el-table-column>
      </el-table>
      <div style="display:flex;justify-content:flex-end;margin-top:14px">
        <el-pagination layout="total, prev, pager, next" :total="total"
                       :page-size="pageSize" :current-page="page"
                       @current-change="(p) => { page = p; load() }" />
      </div>
    </el-card>

    <!-- 新建优惠券 -->
    <el-dialog v-model="dlg.visible" title="新建优惠券" width="560px">
      <el-form label-width="90px">
        <el-form-item label="券名" required>
          <el-input v-model="dlg.name" placeholder="如：新人满99减20" />
        </el-form-item>
        <el-form-item label="类型" required>
          <el-radio-group v-model="dlg.type">
            <el-radio-button :value="1">满减</el-radio-button>
            <el-radio-button :value="2">折扣</el-radio-button>
            <el-radio-button :value="3">无门槛</el-radio-button>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="使用门槛">
          <el-input-number v-model="dlg.thresholdYuan" :min="0" :precision="0" :step="10" />
          <span style="margin-left:8px;color:#909399;font-size:12.5px">满 X 元可用，0 = 无门槛</span>
        </el-form-item>
        <el-form-item v-if="dlg.type === 2" label="折扣率" required>
          <el-input-number v-model="dlg.discountPercent" :min="1" :max="99" />
          <span style="margin-left:8px;color:#909399;font-size:12.5px">{{ (dlg.discountPercent / 10).toFixed(1) }} 折</span>
        </el-form-item>
        <el-form-item v-else label="优惠金额" required>
          <el-input-number v-model="dlg.discountYuan" :min="0.01" :precision="2" :step="5" />
          <span style="margin-left:8px;color:#909399;font-size:12.5px">元</span>
        </el-form-item>
        <el-form-item label="发放总量" required>
          <el-input-number v-model="dlg.totalCount" :min="1" :step="10" />
        </el-form-item>
        <el-form-item label="每人限领">
          <el-input-number v-model="dlg.perUserLimit" :min="1" :max="10" />
        </el-form-item>
        <el-form-item label="领取时间" required>
          <el-date-picker v-model="dlg.timeRange" type="datetimerange"
                          value-format="YYYY-MM-DDTHH:mm:ss" start-placeholder="开始" end-placeholder="结束" />
        </el-form-item>
        <el-form-item label="领取后有效">
          <el-input-number v-model="dlg.validDays" :min="1" :max="365" />
          <span style="margin-left:8px;color:#909399;font-size:12.5px">天</span>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dlg.visible = false">取消</el-button>
        <el-button type="primary" :loading="dlg.saving" @click="save">创建</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { api, fmtTime, toFen, yuan } from '../api'

const rows = ref([])
const total = ref(0)
const page = ref(1)
const pageSize = 10
const keyword = ref('')
const loading = ref(false)
const statusMap = reactive({})

const dlg = reactive({
  visible: false, saving: false,
  name: '', type: 1, thresholdYuan: 99, discountYuan: 20, discountPercent: 90,
  totalCount: 100, perUserLimit: 1,
  timeRange: [new Date().toISOString().slice(0, 16), new Date(Date.now() + 30 * 86400000).toISOString().slice(0, 16)],
  validDays: 30,
})

async function load() {
  loading.value = true
  try {
    const data = await api.get(`/api/admin/coupons?${new URLSearchParams({
      page: page.value, size: pageSize, ...(keyword.value ? { keyword: keyword.value } : {}),
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
  page.value = 1
  load()
}

function openCreate() {
  Object.assign(dlg, {
    visible: true, saving: false, name: '', type: 1,
    thresholdYuan: 99, discountYuan: 20, discountPercent: 90,
    totalCount: 100, perUserLimit: 1, validDays: 30,
    timeRange: [new Date().toISOString().slice(0, 16), new Date(Date.now() + 30 * 86400000).toISOString().slice(0, 16)],
  })
}

async function save() {
  if (!dlg.name.trim()) return ElMessage.warning('请填写券名')
  if (!dlg.timeRange || dlg.timeRange.length !== 2) return ElMessage.warning('请选择领取时间')
  dlg.saving = true
  try {
    await api.post('/api/admin/coupons', {
      name: dlg.name.trim(),
      type: dlg.type,
      thresholdAmount: toFen(dlg.thresholdYuan),
      discountAmount: dlg.type === 2 ? 0 : toFen(dlg.discountYuan),
      discountPercent: dlg.type === 2 ? dlg.discountPercent : null,
      totalCount: dlg.totalCount,
      perUserLimit: dlg.perUserLimit,
      startTime: dlg.timeRange[0],
      endTime: dlg.timeRange[1],
      validDays: dlg.validDays,
    })
    ElMessage.success('优惠券已创建')
    dlg.visible = false
    load()
  } catch (e) {
    ElMessage.error(e.message)
  } finally {
    dlg.saving = false
  }
}

onMounted(load)
</script>

<style scoped>
.toolbar { border-radius: 8px; border: 1px solid #e8ebee; box-shadow: none; }
.toolbar :deep(.el-card__body) { padding: 14px 16px; }
.bar { display: flex; gap: 12px; align-items: center; flex-wrap: wrap; }
.panel { border-radius: 8px; border: 1px solid #e8ebee; box-shadow: none; }
</style>
