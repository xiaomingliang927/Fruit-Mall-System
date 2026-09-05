<template>
  <div>
    <el-card shadow="never" style="margin-bottom: 14px">
      <div style="display:flex;gap:12px;align-items:center;flex-wrap:wrap">
        <el-radio-group v-model="status" @change="() => { page = 1; load() }">
          <el-radio-button :value="null">全部</el-radio-button>
          <el-radio-button v-for="t in statusTabs" :key="t.value" :value="t.value">{{ t.label }}</el-radio-button>
        </el-radio-group>
        <el-input v-model="keyword" placeholder="订单号搜索" style="width:210px" clearable @keyup.enter="load" />
        <el-button type="primary" @click="load">查询</el-button>
        <div style="flex:1"></div>
        <el-button :loading="exporting" @click="exportExcel">
          <el-icon style="margin-right:5px"><Download /></el-icon>导出 Excel
        </el-button>
      </div>
    </el-card>

    <el-card shadow="never">
      <el-table :data="rows" v-loading="loading" stripe>
        <el-table-column prop="orderNo" label="订单号" width="190" />
        <el-table-column prop="userNickname" label="买家" width="110" />
        <el-table-column prop="userPhone" label="手机号" width="125" />
        <el-table-column label="实付金额" width="100">
          <template #default="{ row }"><b style="color:#dc2626">¥{{ yuan(row.payAmount).toFixed(2) }}</b></template>
        </el-table-column>
        <el-table-column label="状态" width="95">
          <template #default="{ row }">
            <el-tag :type="statusType(row.status)" size="small">{{ row.statusText }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="trackingNo" label="运单号" width="140">
          <template #default="{ row }">{{ row.trackingNo || '-' }}</template>
        </el-table-column>
        <el-table-column label="下单时间" width="150">
          <template #default="{ row }">{{ fmtTime(row.createdAt) }}</template>
        </el-table-column>
        <el-table-column label="操作" width="200" fixed="right">
          <template #default="{ row }">
            <el-button size="small" @click="openDetail(row)">详情</el-button>
            <el-button v-if="row.status === 20" type="success" size="small" @click="openShip(row)">发货</el-button>
            <el-button v-if="row.status === 10" type="danger" plain size="small" @click="cancel(row)">取消</el-button>
          </template>
        </el-table-column>
      </el-table>
      <div style="display:flex;justify-content:flex-end;margin-top:14px">
        <el-pagination layout="total, prev, pager, next" :total="total"
                       :page-size="pageSize" :current-page="page"
                       @current-change="(p) => { page = p; load() }" />
      </div>
    </el-card>

    <!-- 详情抽屉 -->
    <el-drawer v-model="drawer.visible" title="订单详情" size="460px">
      <template v-if="drawer.data">
        <el-descriptions :column="1" border size="small">
          <el-descriptions-item label="订单号">{{ drawer.data.orderNo }}</el-descriptions-item>
          <el-descriptions-item label="状态">{{ drawer.data.statusText }}</el-descriptions-item>
          <el-descriptions-item label="收货信息">{{ addrText }}</el-descriptions-item>
          <el-descriptions-item label="买家留言">{{ drawer.data.remark || '无' }}</el-descriptions-item>
          <el-descriptions-item label="运单号">{{ drawer.data.trackingNo || '-' }}</el-descriptions-item>
          <el-descriptions-item label="支付时间">{{ fmtTime(drawer.data.paidAt) }}</el-descriptions-item>
        </el-descriptions>
        <el-table :data="drawer.data.items" size="small" style="margin-top:14px">
          <el-table-column label="商品" min-width="180">
            <template #default="{ row }">{{ row.productName }}（{{ row.skuSpec }}）</template>
          </el-table-column>
          <el-table-column label="单价" width="90">
            <template #default="{ row }">¥{{ yuan(row.price).toFixed(2) }}</template>
          </el-table-column>
          <el-table-column prop="quantity" label="数量" width="60" />
          <el-table-column label="小计" width="95">
            <template #default="{ row }">¥{{ yuan(row.subtotal).toFixed(2) }}</template>
          </el-table-column>
        </el-table>
        <div style="text-align:right;margin-top:12px;font-size:15px">
          实付：<b style="color:#dc2626;font-size:19px">¥{{ yuan(drawer.data.payAmount).toFixed(2) }}</b>
        </div>
      </template>
    </el-drawer>

    <!-- 发货对话框 -->
    <el-dialog v-model="ship.visible" title="订单发货" width="min(420px, 94vw)">
      <p style="margin-bottom:12px;color:#606266">订单号：{{ ship.orderNo }}</p>
      <el-input v-model="ship.trackingNo" placeholder="请输入快递运单号" />
      <template #footer>
        <el-button @click="ship.visible = false">取消</el-button>
        <el-button type="primary" :loading="ship.saving" @click="doShip">确认发货</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { api, exportFile, fmtTime, yuan } from '../api'

const statusTabs = [
  { label: '待支付', value: 10 },
  { label: '待发货', value: 20 },
  { label: '待收货', value: 30 },
  { label: '已完成', value: 40 },
  { label: '已取消', value: 50 },
]
const rows = ref([])
const total = ref(0)
const page = ref(1)
const pageSize = 10
const status = ref(null)
const keyword = ref('')
const loading = ref(false)

const drawer = reactive({ visible: false, data: null })
const ship = reactive({ visible: false, orderId: null, orderNo: '', trackingNo: '', saving: false })
const exporting = ref(false)

async function exportExcel() {
  exporting.value = true
  try {
    const qs = new URLSearchParams()
    if (status.value) qs.append('status', status.value)
    if (keyword.value) qs.append('keyword', keyword.value)
    await exportFile(`/api/admin/orders/export?${qs}`, `订单列表_${new Date().toISOString().slice(0, 10)}.xlsx`)
    ElMessage.success('已导出')
  } catch (e) {
    ElMessage.error(e.message || '导出失败')
  } finally {
    exporting.value = false
  }
}

const addrText = computed(() => {
  try {
    const a = JSON.parse(drawer.data?.addressSnapshot || '{}')
    return `${a.receiver} ${a.phone} ｜ ${a.province}${a.city}${a.district || ''} ${a.detail}`
  } catch {
    return '-'
  }
})

function statusType(s) {
  return { 10: 'warning', 20: 'primary', 30: 'info', 40: 'success', 50: 'info', 60: 'warning', 70: 'danger' }[s] || 'info'
}

async function load() {
  loading.value = true
  try {
    const data = await api.get(`/api/admin/orders?${new URLSearchParams({
      page: page.value, size: pageSize,
      ...(status.value ? { status: status.value } : {}),
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

async function openDetail(row) {
  drawer.data = await api.get(`/api/admin/orders/${row.id}`)
  drawer.visible = true
}

function openShip(row) {
  Object.assign(ship, { visible: true, orderId: row.id, orderNo: row.orderNo, trackingNo: '', saving: false })
}

async function doShip() {
  if (!ship.trackingNo.trim()) {
    ElMessage.warning('请填写运单号')
    return
  }
  ship.saving = true
  try {
    await api.post(`/api/admin/orders/${ship.orderId}/ship`, { trackingNo: ship.trackingNo.trim() })
    ElMessage.success('发货成功，订单转「待收货」')
    ship.visible = false
    load()
  } catch (e) {
    ElMessage.error(e.message)
  } finally {
    ship.saving = false
  }
}

async function cancel(row) {
  try {
    await ElMessageBox.confirm(
      `确认取消未支付订单 ${row.orderNo}？取消后库存自动回补。`, '取消订单', { type: 'warning' })
    await api.post(`/api/admin/orders/${row.id}/cancel`)
    ElMessage.success('订单已取消')
    load()
  } catch (e) {
    if (e !== 'cancel') ElMessage.error(e.message || '操作失败')
  }
}

onMounted(load)
</script>
