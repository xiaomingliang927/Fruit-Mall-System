<template>
  <div>
    <el-card shadow="never" class="toolbar">
      <div class="bar">
        <el-input v-model="keyword" placeholder="活动名搜索" style="width: 200px" clearable @keyup.enter="load" />
        <el-button type="primary" @click="load">查询</el-button>
        <el-button @click="reset">重置</el-button>
        <div style="flex: 1"></div>
        <el-button type="success" @click="openCreate">＋ 新建秒杀</el-button>
      </div>
    </el-card>

    <el-card shadow="never" class="panel" style="margin-top: 12px">
      <el-table :data="rows" v-loading="loading" stripe>
        <el-table-column prop="id" label="ID" width="60" />
        <el-table-column prop="name" label="活动名" min-width="180" />
        <el-table-column label="商品" min-width="180">
          <template #default="{ row }">{{ row.productName }} / {{ row.skuSpec || '-' }}</template>
        </el-table-column>
        <el-table-column label="秒杀价" width="120">
          <template #default="{ row }">¥ {{ yuan(row.seckillPrice).toFixed(2) }}</template>
        </el-table-column>
        <el-table-column label="原售价" width="120">
          <template #default="{ row }">¥ {{ yuan(row.skuOriginalPrice).toFixed(2) }}</template>
        </el-table-column>
        <el-table-column label="限量 / 已抢" width="120" align="center">
          <template #default="{ row }">
            <span :style="{ color: row.totalStock - row.soldCount === 0 ? '#f24e3e' : '#1f2937' }">
              {{ row.totalStock - row.soldCount }} / {{ row.totalStock }}
            </span>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="100" align="center">
          <template #default="{ row }">
            <el-tag :type="statusTag(row.status).type" size="small">{{ statusTag(row.status).text }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="活动时间" min-width="260">
          <template #default="{ row }">{{ fmtTime(row.startTime) }} ~ {{ fmtTime(row.endTime) }}</template>
        </el-table-column>
        <el-table-column label="操作" width="170" fixed="right">
          <template #default="{ row }">
            <el-button size="small" link @click="openEdit(row)">编辑</el-button>
            <el-button size="small" link @click="toggleStatus(row)">
              {{ row.status === 1 ? '停用' : '启用' }}
            </el-button>
            <el-button size="small" link type="danger" @click="remove(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
      <div style="display:flex;justify-content:flex-end;margin-top:14px">
        <el-pagination layout="total, prev, pager, next" :total="total"
                       :page-size="pageSize" :current-page="page"
                       @current-change="(p) => { page = p; load() }" />
      </div>
    </el-card>

    <!-- 新建 / 编辑秒杀 -->
    <el-dialog v-model="dlg.visible" :title="dlg.id ? '编辑秒杀' : '新建秒杀'" width="560px">
      <el-form label-width="90px">
        <el-form-item label="活动名" required>
          <el-input v-model="dlg.name" placeholder="如：智利车厘子 JJ 级 限时秒杀" />
        </el-form-item>
        <el-form-item label="商品 SKU" required>
          <el-select v-model="dlg.skuId" filterable style="width:100%" placeholder="先选商品再选规格" @change="onSkuPick">
            <template v-if="products.length">
              <el-option-group v-for="p in products" :key="p.id" :label="p.name">
                <el-option v-for="s in p.skus" :key="s.id" :value="s.id"
                           :label="`${s.spec || '默认规格'} ¥${yuan(s.price).toFixed(2)}  库存 ${s.stock}`" />
              </el-option-group>
            </template>
          </el-select>
        </el-form-item>
        <el-form-item label="原价" required>
          <el-input-number v-model="dlg.originalPriceYuan" :precision="2" :step="1" :disabled="dlg.id != null" />
          <span style="margin-left:8px;color:#909399;font-size:12.5px">¥（自动取 SKU 当前价）</span>
        </el-form-item>
        <el-form-item label="秒杀价" required>
          <el-input-number v-model="dlg.seckillPriceYuan" :precision="2" :step="1" :min="0.01" />
          <span style="margin-left:8px;color:#909399;font-size:12.5px">¥</span>
        </el-form-item>
        <el-form-item label="每人限抢" required>
          <el-input-number v-model="dlg.perUserLimit" :min="1" :max="10" />
          <span style="margin-left:8px;color:#909399;font-size:12.5px">单</span>
        </el-form-item>
        <el-form-item label="限量" required>
          <el-input-number v-model="dlg.totalStock" :min="1" :step="10" />
          <span style="margin-left:8px;color:#909399;font-size:12.5px">份（售完即止）</span>
        </el-form-item>
        <el-form-item label="活动时间" required>
          <el-date-picker v-model="dlg.timeRange" type="datetimerange"
                          value-format="YYYY-MM-DDTHH:mm:ss" start-placeholder="开始" end-placeholder="结束" />
        </el-form-item>
        <el-form-item label="状态">
          <el-radio-group v-model="dlg.status">
            <el-radio-button :value="1">启用</el-radio-button>
            <el-radio-button :value="0">停用</el-radio-button>
          </el-radio-group>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dlg.visible = false">取消</el-button>
        <el-button type="primary" :loading="dlg.saving" @click="save">{{ dlg.id ? '保存' : '创建' }}</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { api, fmtTime, toFen, yuan } from '../api'

const rows = ref([])
const total = ref(0)
const page = ref(1)
const pageSize = 10
const keyword = ref('')
const loading = ref(false)
const products = ref([])

const dlg = reactive({
  visible: false, saving: false, id: null,
  name: '', productId: null, skuId: null, skuSpec: '',
  originalPriceYuan: 0, seckillPriceYuan: 0,
  perUserLimit: 1, totalStock: 100,
  timeRange: [
    new Date().toISOString().slice(0, 16),
    new Date(Date.now() + 7 * 86400000).toISOString().slice(0, 16),
  ],
  status: 1,
})

function statusTag(s) {
  if (s === 1) return { type: 'success', text: '启用' }
  return { type: 'info', text: '停用' }
}

async function load() {
  loading.value = true
  try {
    const data = await api.get(`/api/admin/seckill/activities?${new URLSearchParams({
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

async function loadProducts() {
  // 仅拉一页（管理后台多数 SKU 集中于热门商品）
  const data = await api.get('/api/admin/products?page=1&size=50')
  products.value = (data.records || []).map((p) => ({
    id: p.id, name: p.name,
    skus: (p.skus || []).map((s) => ({ id: s.id, spec: s.spec, price: s.price, stock: s.stock })),
  }))
}

function reset() {
  keyword.value = ''
  page.value = 1
  load()
}

function blankForm() {
  return Object.assign(dlg, {
    visible: true, saving: false, id: null,
    name: '', productId: null, skuId: null, skuSpec: '',
    originalPriceYuan: 0, seckillPriceYuan: 0,
    perUserLimit: 1, totalStock: 100,
    timeRange: [
      new Date().toISOString().slice(0, 16),
      new Date(Date.now() + 7 * 86400000).toISOString().slice(0, 16),
    ],
    status: 1,
  })
}

function openCreate() { blankForm() }

function openEdit(row) {
  dlg.id = row.id
  dlg.name = row.name
  dlg.skuId = row.skuId
  dlg.skuSpec = row.skuSpec || ''
  dlg.productId = row.productId
  dlg.originalPriceYuan = yuan(row.skuOriginalPrice)
  dlg.seckillPriceYuan = yuan(row.seckillPrice)
  dlg.perUserLimit = row.perUserLimit
  dlg.totalStock = row.totalStock
  dlg.timeRange = [row.startTime, row.endTime]
  dlg.status = row.status
  dlg.visible = true
}

function onSkuPick(skuId) {
  for (const p of products.value) {
    const s = (p.skus || []).find((x) => x.id === skuId)
    if (s) {
      dlg.productId = p.id
      dlg.skuSpec = s.spec || ''
      dlg.originalPriceYuan = Number(yuan(s.price).toFixed(2))
      if (!dlg.seckillPriceYuan || dlg.seckillPriceYuan > dlg.originalPriceYuan) {
        dlg.seckillPriceYuan = Math.max(0.01, Number((dlg.originalPriceYuan * 0.7).toFixed(2)))
      }
      return
    }
  }
}

async function save() {
  if (!dlg.name.trim()) return ElMessage.warning('请填写活动名')
  if (!dlg.skuId) return ElMessage.warning('请选择商品 SKU')
  if (!dlg.timeRange || dlg.timeRange.length !== 2) return ElMessage.warning('请选择活动时间')
  if (dlg.seckillPriceYuan >= dlg.originalPriceYuan) return ElMessage.warning('秒杀价必须低于原价')
  dlg.saving = true
  try {
    const payload = {
      name: dlg.name.trim(),
      productId: dlg.productId,
      skuId: dlg.skuId,
      seckillPrice: toFen(dlg.seckillPriceYuan),
      perUserLimit: dlg.perUserLimit,
      totalStock: dlg.totalStock,
      startTime: dlg.timeRange[0],
      endTime: dlg.timeRange[1],
      status: dlg.status,
    }
    if (dlg.id) {
      await api.put(`/api/admin/seckill/activities/${dlg.id}`, payload)
      ElMessage.success('秒杀已更新')
    } else {
      await api.post('/api/admin/seckill/activities', payload)
      ElMessage.success('秒杀已创建')
    }
    dlg.visible = false
    load()
  } catch (e) {
    ElMessage.error(e.message)
  } finally {
    dlg.saving = false
  }
}

async function toggleStatus(row) {
  const next = row.status === 1 ? 0 : 1
  try {
    await api.put(`/api/admin/seckill/activities/${row.id}/status`, { status: next })
    ElMessage.success(next === 1 ? '已启用' : '已停用')
    load()
  } catch (e) {
    ElMessage.error(e.message)
  }
}

async function remove(row) {
  try {
    await ElMessageBox.confirm(`确认删除「${row.name}」？该操作不可恢复。`, '提示', { type: 'warning' })
  } catch { return }
  try {
    await api.delete(`/api/admin/seckill/activities/${row.id}`)
    ElMessage.success('已删除')
    load()
  } catch (e) {
    ElMessage.error(e.message)
  }
}

onMounted(async () => {
  await loadProducts()
  await load()
})
</script>

<style scoped>
.toolbar { border-radius: 8px; border: 1px solid #e8ebee; box-shadow: none; }
.toolbar :deep(.el-card__body) { padding: 14px 16px; }
.bar { display: flex; gap: 12px; align-items: center; flex-wrap: wrap; }
.panel { border-radius: 8px; border: 1px solid #e8ebee; box-shadow: none; }
</style>
