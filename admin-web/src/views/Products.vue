<template>
  <div>
    <el-card shadow="never" style="margin-bottom: 14px">
      <div style="display:flex;gap:12px;align-items:center;flex-wrap:wrap">
        <el-input v-model="keyword" placeholder="商品名称搜索" style="width:220px" clearable @keyup.enter="load" />
        <el-select v-model="categoryId" placeholder="全部分类" style="width:160px" clearable>
          <el-option v-for="c in categories" :key="c.id" :label="c.name" :value="c.id" />
        </el-select>
        <el-button type="primary" @click="load">查询</el-button>
        <el-button @click="() => { keyword = ''; categoryId = null; page = 1; load() }">重置</el-button>
        <div style="flex:1"></div>
        <el-button type="success" @click="openCreate">＋ 新增商品</el-button>
      </div>
    </el-card>

    <el-card shadow="never">
      <el-table :data="rows" v-loading="loading" stripe>
        <el-table-column prop="id" label="ID" width="60" />
        <el-table-column label="主图" width="86">
          <template #default="{ row }">
            <el-image :src="row.mainImage" style="width:64px;height:52px;border-radius:8px" fit="cover" />
          </template>
        </el-table-column>
        <el-table-column prop="name" label="商品名称" min-width="180" />
        <el-table-column prop="categoryName" label="分类" width="100" />
        <el-table-column label="规格 / 价格 / 库存" min-width="250">
          <template #default="{ row }">
            <div v-for="s in row.skus" :key="s.id" style="font-size:12.5px;line-height:1.9">
              {{ s.spec }}：<span style="color:#dc2626">¥{{ yuan(s.price).toFixed(2) }}</span>
              · 库存 {{ s.stock }}
              <el-tag v-if="s.status === 0" size="small" type="info">停售</el-tag>
            </div>
          </template>
        </el-table-column>
        <el-table-column prop="sales" label="销量" width="80" />
        <el-table-column label="状态" width="90">
          <template #default="{ row }">
            <el-switch :model-value="row.status === 1" @change="(v) => toggleStatus(row, v)"
                       inline-prompt active-text="上架" inactive-text="下架" />
          </template>
        </el-table-column>
        <el-table-column label="操作" width="90" fixed="right">
          <template #default="{ row }">
            <el-button size="small" @click="openEdit(row)">编辑</el-button>
          </template>
        </el-table-column>
      </el-table>
      <div style="display:flex;justify-content:flex-end;margin-top:14px">
        <el-pagination layout="total, prev, pager, next" :total="total"
                       :page-size="pageSize" :current-page="page"
                       @current-change="(p) => { page = p; load() }" />
      </div>
    </el-card>

    <!-- 新增 / 编辑对话框 -->
    <el-dialog v-model="dialog.visible" :title="dialog.isEdit ? '编辑商品' : '新增商品'" width="720px" top="6vh">
      <el-form label-width="82px">
        <el-row :gutter="12">
          <el-col :span="12"><el-form-item label="商品分类" required>
            <el-select v-model="form.categoryId" style="width:100%">
              <el-option v-for="c in categories" :key="c.id" :label="c.name" :value="c.id" />
            </el-select>
          </el-form-item></el-col>
          <el-col :span="12"><el-form-item label="商品名称" required>
            <el-input v-model="form.name" placeholder="如：阿克苏冰糖心苹果" />
          </el-form-item></el-col>
        </el-row>
        <el-form-item label="卖点副标题"><el-input v-model="form.subtitle" /></el-form-item>
        <el-row :gutter="12">
          <el-col :span="8"><el-form-item label="产地"><el-input v-model="form.origin" /></el-form-item></el-col>
          <el-col :span="8"><el-form-item label="计量单位"><el-input v-model="form.unit" placeholder="斤 / 箱 / 盒" /></el-form-item></el-col>
          <el-col :span="8"><el-form-item label="标签"><el-input v-model="form.tags" placeholder="逗号分隔" /></el-form-item></el-col>
        </el-row>
        <el-form-item label="主图地址">
          <el-select v-model="form.mainImage" style="width:100%" filterable allow-create>
            <el-option v-for="img in presetImages" :key="img.value" :label="img.label" :value="img.value" />
          </el-select>
          <el-image v-if="form.mainImage" :src="form.mainImage"
                    style="width:110px;height:84px;border-radius:8px;margin-top:8px" fit="cover" />
        </el-form-item>

        <el-form-item label="规格 SKU" required>
          <div style="width:100%">
            <div v-for="(s, i) in form.skus" :key="i"
                 style="display:flex;gap:10px;margin-bottom:10px;align-items:center">
              <el-input v-model="s.spec" placeholder="规格名，如 5斤装" style="width:180px" :disabled="!!s.id" />
              <el-input-number v-model="s.priceYuan" :min="0.01" :precision="2" :step="1" placeholder="元" />
              <el-input-number v-model="s.stock" :min="0" :step="10" placeholder="库存" />
              <el-button v-if="!s.id" circle type="danger" plain size="small"
                         @click="form.skus.splice(i, 1)">－</el-button>
              <el-tag v-else size="small" type="info">已有 SKU（规格不可改）</el-tag>
            </div>
            <el-button size="small" @click="addSkuRow">＋ 添加规格</el-button>
          </div>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialog.visible = false">取消</el-button>
        <el-button type="primary" :loading="dialog.saving" @click="save">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { api, toFen, yuan } from '../api'

const rows = ref([])
const total = ref(0)
const page = ref(1)
const pageSize = 10
const keyword = ref('')
const categoryId = ref(null)
const categories = ref([])
const loading = ref(false)

const presetImages = [
  'aksu-apple', 'gannan-orange', 'dandong-strawberry', 'yunnan-blueberry',
  'chile-cherry', 'thai-durian', 'gift-box',
].map((n) => ({ label: n, value: `/images/products/${n}.svg` }))

const dialog = reactive({ visible: false, isEdit: false, saving: false, editId: null })
const form = reactive({
  categoryId: null, name: '', subtitle: '', origin: '', unit: '', tags: '', mainImage: '', skus: [],
})

async function load() {
  loading.value = true
  try {
    const data = await api.get(`/api/admin/products?${new URLSearchParams({
      page: page.value, size: pageSize,
      ...(keyword.value ? { keyword: keyword.value } : {}),
      ...(categoryId.value ? { categoryId: categoryId.value } : {}),
    })}`)
    rows.value = data.records
    total.value = data.total
  } catch (e) {
    ElMessage.error(e.message)
  } finally {
    loading.value = false
  }
}

function addSkuRow() {
  form.skus.push({ spec: '', priceYuan: 1, stock: 100, status: 1 })
}

function openCreate() {
  dialog.isEdit = false
  dialog.editId = null
  Object.assign(form, { categoryId: categories.value[0]?.id ?? null, name: '', subtitle: '', origin: '', unit: '斤', tags: '', mainImage: presetImages[0].value, skus: [] })
  addSkuRow()
  dialog.visible = true
}

function openEdit(row) {
  dialog.isEdit = true
  dialog.editId = row.id
  Object.assign(form, {
    categoryId: row.categoryId, name: row.name, subtitle: row.subtitle || '',
    origin: row.origin || '', unit: row.unit || '', tags: row.tags || '',
    mainImage: row.mainImage || '',
    skus: row.skus.map((s) => ({ id: s.id, spec: s.spec, priceYuan: yuan(s.price), stock: s.stock, status: s.status })),
  })
  dialog.visible = true
}

async function save() {
  if (!form.name) { ElMessage.warning('请填写商品名称'); return }
  if (form.skus.some((s) => !s.spec)) { ElMessage.warning('SKU 规格名不能为空'); return }
  dialog.saving = true
  const payload = {
    categoryId: form.categoryId, name: form.name, subtitle: form.subtitle,
    mainImage: form.mainImage, origin: form.origin, unit: form.unit, tags: form.tags,
    skus: form.skus.map((s) => ({
      id: s.id ?? undefined, spec: s.spec, price: toFen(s.priceYuan), stock: s.stock, status: s.status,
    })),
  }
  try {
    if (dialog.isEdit) await api.put(`/api/admin/products/${dialog.editId}`, payload)
    else await api.post('/api/admin/products', payload)
    ElMessage.success(dialog.isEdit ? '商品已更新' : '商品已创建并上架')
    dialog.visible = false
    load()
  } catch (e) {
    ElMessage.error(e.message)
  } finally {
    dialog.saving = false
  }
}

async function toggleStatus(row, v) {
  try {
    await ElMessageBox.confirm(`确认${v ? '上架' : '下架'}「${row.name}」？`, '提示', { type: 'warning' })
    await api.put(`/api/admin/products/${row.id}/status`, { status: v ? 1 : 0 })
    ElMessage.success('操作成功')
    load()
  } catch (e) {
    if (e !== 'cancel') ElMessage.error(e.message || '操作失败')
  }
}

onMounted(async () => {
  categories.value = await api.get('/api/v1/categories')
  load()
})
</script>
