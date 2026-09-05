<template>
  <div>
    <el-card shadow="never" class="toolbar">
      <div class="bar">
        <el-select v-model="status" placeholder="状态" style="width: 120px" clearable>
          <el-option label="启用" :value="1" />
          <el-option label="禁用" :value="0" />
        </el-select>
        <el-button type="primary" @click="load">刷新</el-button>
        <div style="flex: 1"></div>
        <el-button type="success" @click="openCreate">＋ 新建轮播图</el-button>
      </div>
    </el-card>

    <el-card shadow="never" class="panel" style="margin-top: 12px">
      <el-table :data="rows" v-loading="loading" stripe>
        <el-table-column prop="id" label="ID" width="60" />
        <el-table-column label="预览" width="140" align="center">
          <template #default="{ row }">
            <el-image :src="row.image" class="thumb" fit="cover"
                      :preview-src-list="[row.image]" preview-teleported hide-on-click-modal />
          </template>
        </el-table-column>
        <el-table-column prop="title" label="标题" min-width="180" show-overflow-tooltip />
        <el-table-column label="跳转" width="170">
          <template #default="{ row }">
            <span v-if="row.linkType === 1">商品 #{{ row.linkValue }}</span>
            <span v-else-if="row.linkType === 2">{{ row.linkValue }}</span>
            <span v-else style="color:#c0c4cc">不跳转</span>
          </template>
        </el-table-column>
        <el-table-column prop="sort" label="排序" width="80" align="center" />
        <el-table-column label="状态" width="90" align="center">
          <template #default="{ row }">
            <el-switch v-model="row.status" :active-value="1" :inactive-value="0"
                       @change="(v) => toggleStatus(row, v)" />
          </template>
        </el-table-column>
        <el-table-column label="创建时间" width="150">
          <template #default="{ row }">{{ fmtTime(row.createdAt) }}</template>
        </el-table-column>
        <el-table-column label="操作" width="130" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" size="small" plain @click="openEdit(row)">编辑</el-button>
            <el-button type="danger" size="small" plain @click="remove(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
      <div style="display:flex;justify-content:flex-end;margin-top:14px">
        <el-pagination layout="total, prev, pager, next" :total="total"
                       :page-size="pageSize" :current-page="page"
                       @current-change="(p) => { page = p; load() }" />
      </div>
    </el-card>

    <el-dialog v-model="dlg.visible" :title="dlg.id ? '编辑轮播图' : '新建轮播图'" width="min(560px, 94vw)">
      <el-form label-width="90px">
        <el-form-item label="标题" required>
          <el-input v-model="dlg.title" maxlength="64" placeholder="后台标识用，如：当季西瓜大促" />
        </el-form-item>
        <el-form-item label="图片" required>
          <el-upload :show-file-list="false" :http-request="doUpload"
                     accept="image/jpeg,image/png,image/webp">
            <img v-if="dlg.image" :src="dlg.image" class="up-preview" alt="轮播图" />
            <div v-else class="up-box">＋ 点击上传</div>
          </el-upload>
          <div class="up-tip">建议 750×340 以上，jpg/png/webp，≤5MB；{{ dlg.uploading ? '上传中…' : '' }}</div>
        </el-form-item>
        <el-form-item label="跳转类型" required>
          <el-radio-group v-model="dlg.linkType">
            <el-radio-button :value="0">不跳转</el-radio-button>
            <el-radio-button :value="1">商品</el-radio-button>
            <el-radio-button :value="2">页面路径</el-radio-button>
          </el-radio-group>
        </el-form-item>
        <el-form-item v-if="dlg.linkType === 1" label="商品 ID" required>
          <el-input-number v-model="dlg.productId" :min="1" :precision="0" />
        </el-form-item>
        <el-form-item v-else-if="dlg.linkType === 2" label="页面路径" required>
          <el-input v-model="dlg.pagePath" placeholder="如：/pages/index/index" />
        </el-form-item>
        <el-form-item label="排序">
          <el-input-number v-model="dlg.sort" :min="0" :step="10" />
          <span style="margin-left:8px;color:#909399;font-size:12.5px">越小越靠前</span>
        </el-form-item>
        <el-form-item label="状态">
          <el-radio-group v-model="dlg.status">
            <el-radio-button :value="1">启用</el-radio-button>
            <el-radio-button :value="0">禁用</el-radio-button>
          </el-radio-group>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dlg.visible = false">取消</el-button>
        <el-button type="primary" :loading="dlg.saving" @click="save">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { api, fmtTime, uploadImage } from '../api'

const rows = ref([])
const total = ref(0)
const page = ref(1)
const pageSize = 10
const status = ref(null)
const loading = ref(false)

const dlg = reactive({
  visible: false, saving: false, uploading: false,
  id: null, title: '', image: '', linkType: 0,
  productId: null, pagePath: '', sort: 10, status: 1,
})

async function load() {
  loading.value = true
  try {
    const data = await api.get(`/api/admin/banners?${new URLSearchParams({
      page: page.value, size: pageSize, position: 'home',
      ...(status.value !== null && status.value !== undefined && status.value !== ''
        ? { status: status.value } : {}),
    })}`)
    rows.value = data.records
    total.value = data.total
  } catch (e) {
    ElMessage.error(e.message)
  } finally {
    loading.value = false
  }
}

function openCreate() {
  Object.assign(dlg, {
    visible: true, saving: false, uploading: false,
    id: null, title: '', image: '', linkType: 0,
    productId: null, pagePath: '', sort: 10, status: 1,
  })
}

function openEdit(row) {
  Object.assign(dlg, {
    visible: true, saving: false, uploading: false,
    id: row.id, title: row.title, image: row.image, linkType: row.linkType ?? 0,
    productId: row.linkType === 1 ? Number(row.linkValue) || null : null,
    pagePath: row.linkType === 2 ? row.linkValue : '',
    sort: row.sort ?? 0, status: row.status ?? 1,
  })
}

async function doUpload(opt) {
  if (dlg.uploading) return
  dlg.uploading = true
  try {
    dlg.image = await uploadImage(opt.file)
    ElMessage.success('图片已上传')
  } catch (e) {
    ElMessage.error(e.message)
  } finally {
    dlg.uploading = false
  }
}

function payload() {
  return {
    title: dlg.title.trim(),
    image: dlg.image,
    linkType: dlg.linkType,
    linkValue: dlg.linkType === 1 ? String(dlg.productId ?? '') : (dlg.linkType === 2 ? dlg.pagePath.trim() : null),
    position: 'home',
    sort: dlg.sort ?? 0,
    status: dlg.status,
  }
}

async function save() {
  if (!dlg.title.trim()) return ElMessage.warning('请填写标题')
  if (!dlg.image) return ElMessage.warning('请上传轮播图片')
  if (dlg.linkType === 1 && !dlg.productId) return ElMessage.warning('请填写商品 ID')
  if (dlg.linkType === 2 && !dlg.pagePath.trim()) return ElMessage.warning('请填写页面路径')
  if (dlg.uploading) return ElMessage.warning('图片上传中，请稍候')

  dlg.saving = true
  try {
    if (dlg.id) await api.put(`/api/admin/banners/${dlg.id}`, payload())
    else await api.post('/api/admin/banners', payload())
    ElMessage.success(dlg.id ? '已保存' : '轮播图已创建')
    dlg.visible = false
    load()
  } catch (e) {
    ElMessage.error(e.message)
  } finally {
    dlg.saving = false
  }
}

async function toggleStatus(row, v) {
  const old = v === 1 ? 0 : 1
  try {
    await api.put(`/api/admin/banners/${row.id}/status`, { status: v })
    ElMessage.success(v === 1 ? '已启用' : '已禁用')
  } catch (e) {
    row.status = old
    ElMessage.error(e.message)
  }
}

async function remove(row) {
  try {
    await ElMessageBox.confirm(`删除「${row.title}」？删除后首页立即不再展示。`, '删除轮播图', {
      confirmButtonText: '删除', cancelButtonText: '取消', type: 'warning',
    })
  } catch {
    return
  }
  try {
    await api.delete(`/api/admin/banners/${row.id}`)
    ElMessage.success('已删除')
    load()
  } catch (e) {
    ElMessage.error(e.message)
  }
}

onMounted(load)
</script>

<style scoped>
.toolbar { border-radius: 8px; border: 1px solid #e8ebee; box-shadow: none; }
.toolbar :deep(.el-card__body) { padding: 14px 16px; }
.bar { display: flex; gap: 12px; align-items: center; flex-wrap: wrap; }
.panel { border-radius: 8px; border: 1px solid #e8ebee; box-shadow: none; }
.thumb { width: 110px; height: 48px; border-radius: 6px; border: 1px solid #e8ebee; background: #fafbfc; }
.up-preview { width: 220px; height: 100px; object-fit: cover; border-radius: 8px; border: 1px solid #e8ebee; display: block; }
.up-box {
  width: 220px; height: 100px; border: 1px dashed #d9dde2; border-radius: 8px;
  background: #fafbfc; color: #a8abb2; font-size: 13px;
  display: flex; align-items: center; justify-content: center;
}
.up-tip { margin-top: 6px; font-size: 12px; color: #909399; }
</style>
