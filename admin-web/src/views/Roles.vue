<template>
  <div>
    <el-card shadow="never" class="toolbar">
      <div class="bar">
        <div style="flex: 1">
          <div style="font-size: 14px; font-weight: 600; color: #1f2937">角色与权限</div>
          <div class="sub">内置「超级管理员」跳过逐点校验；其余账号按勾选的权限点控制菜单与接口</div>
        </div>
        <el-button type="success" @click="openCreate">＋ 新建角色</el-button>
      </div>
    </el-card>

    <el-card shadow="never" class="panel" style="margin-top: 12px">
      <el-table :data="roles" v-loading="loading" stripe>
        <el-table-column prop="name" label="角色名" min-width="130" />
        <el-table-column prop="code" label="编码" width="130" />
        <el-table-column prop="remark" label="说明" min-width="200" show-overflow-tooltip />
        <el-table-column label="状态" width="90" align="center">
          <template #default="{ row }">
            <el-tag :type="row.status === 1 ? 'success' : 'info'" size="small" effect="light">
              {{ row.status === 1 ? '启用' : '禁用' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="管理员" width="90" align="center">
          <template #default="{ row }">{{ row.adminCount }}</template>
        </el-table-column>
        <el-table-column label="权限点" width="90" align="center">
          <template #default="{ row }">{{ row.permissions.length }}</template>
        </el-table-column>
        <el-table-column label="操作" width="150" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" size="small" plain @click="openEdit(row)">配置权限</el-button>
            <el-button type="danger" size="small" plain @click="remove(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <el-card shadow="never" class="panel" style="margin-top: 12px">
      <template #header>
        <div class="ph">账号授权</div>
      </template>
      <el-table :data="admins" v-loading="loading" stripe>
        <el-table-column prop="username" label="账号" width="150" />
        <el-table-column prop="realName" label="姓名" width="150" />
        <el-table-column label="角色" min-width="200">
          <template #default="{ row }">
            <el-tag v-for="n in row.roleNames" :key="n" size="small" effect="light" style="margin-right: 6px">
              {{ n }}
            </el-tag>
            <span v-if="!row.roleNames.length" class="muted">未分配（无任何权限）</span>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="90" align="center">
          <template #default="{ row }">
            <el-tag :type="row.status === 1 ? 'success' : 'danger'" size="small" effect="light">
              {{ row.status === 1 ? '正常' : '禁用' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="120" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" size="small" plain @click="openAssign(row)">分配角色</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <!-- 角色编辑 -->
    <el-dialog v-model="dlg.visible" :title="dlg.id ? '配置权限 · ' + dlg.name : '新建角色'" width="min(620px, 94vw)">
      <el-form label-width="80px">
        <el-form-item label="角色名" required>
          <el-input v-model="dlg.name" maxlength="32" placeholder="如：商品运营" />
        </el-form-item>
        <el-form-item label="编码" required>
          <el-input v-model="dlg.code" maxlength="32" :disabled="!!dlg.id"
                    placeholder="英文小写，如 product_operator" />
        </el-form-item>
        <el-form-item label="说明">
          <el-input v-model="dlg.remark" maxlength="128" placeholder="这个角色负责什么" />
        </el-form-item>
        <el-form-item label="状态">
          <el-radio-group v-model="dlg.status">
            <el-radio-button :value="1">启用</el-radio-button>
            <el-radio-button :value="0">禁用</el-radio-button>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="权限点" required>
          <div class="perm-box">
            <div class="perm-head">
              <el-checkbox v-model="checkAll" :indeterminate="indeterminate" @change="toggleAll">
                全选
              </el-checkbox>
              <span class="ph-tip">已选 {{ dlg.permissions.length }} / {{ permissions.length }}</span>
            </div>
            <div class="perm-group">
              <div class="pg-t">菜单权限</div>
              <el-checkbox-group v-model="dlg.permissions">
                <el-checkbox v-for="p in menuPerms" :key="p.code" :value="p.code">
                  {{ p.name }}
                </el-checkbox>
              </el-checkbox-group>
            </div>
            <div class="perm-group">
              <div class="pg-t">操作权限</div>
              <el-checkbox-group v-model="dlg.permissions">
                <el-checkbox v-for="p in actionPerms" :key="p.code" :value="p.code">
                  {{ p.name }}
                </el-checkbox>
              </el-checkbox-group>
            </div>
          </div>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dlg.visible = false">取消</el-button>
        <el-button type="primary" :loading="dlg.saving" @click="save">保存</el-button>
      </template>
    </el-dialog>

    <!-- 账号分配角色 -->
    <el-dialog v-model="assignDlg.visible" :title="'分配角色 · ' + assignDlg.username" width="min(480px, 94vw)">
      <el-select v-model="assignDlg.roleIds" multiple style="width: 100%" placeholder="选择角色（可多选）">
        <el-option v-for="r in roles" :key="r.id" :label="r.name" :value="r.id" />
      </el-select>
      <div class="sub" style="margin-top: 10px">
        留空表示不分配任何角色：该账号登录后看不到任何菜单，也无法调用受控接口。
      </div>
      <template #footer>
        <el-button @click="assignDlg.visible = false">取消</el-button>
        <el-button type="primary" :loading="assignDlg.saving" @click="saveAssign">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { api } from '../api'

const roles = ref([])
const admins = ref([])
const permissions = ref([])
const loading = ref(false)

const menuPerms = computed(() => permissions.value.filter((p) => p.type === 1))
const actionPerms = computed(() => permissions.value.filter((p) => p.type === 2))

const dlg = reactive({
  visible: false, saving: false,
  id: null, name: '', code: '', remark: '', status: 1, permissions: [],
})

const checkAll = computed({
  get: () => permissions.value.length > 0 && dlg.permissions.length === permissions.value.length,
  set: () => {},
})
const indeterminate = computed(() =>
  dlg.permissions.length > 0 && dlg.permissions.length < permissions.value.length)

function toggleAll(checked) {
  dlg.permissions = checked ? permissions.value.map((p) => p.code) : []
}

const assignDlg = reactive({ visible: false, saving: false, adminId: null, username: '', roleIds: [] })

async function load() {
  loading.value = true
  try {
    const [r, p, a] = await Promise.all([
      api.get('/api/admin/roles'),
      api.get('/api/admin/permissions'),
      api.get('/api/admin/admins'),
    ])
    roles.value = r
    permissions.value = p
    admins.value = a
  } catch (e) {
    ElMessage.error(e.message)
  } finally {
    loading.value = false
  }
}

function openCreate() {
  Object.assign(dlg, {
    visible: true, saving: false, id: null,
    name: '', code: '', remark: '', status: 1, permissions: [],
  })
}

function openEdit(row) {
  Object.assign(dlg, {
    visible: true, saving: false, id: row.id,
    name: row.name, code: row.code, remark: row.remark || '',
    status: row.status ?? 1, permissions: [...row.permissions],
  })
}

async function save() {
  if (!dlg.name.trim()) return ElMessage.warning('请填写角色名')
  if (!dlg.code.trim()) return ElMessage.warning('请填写角色编码')
  if (!dlg.permissions.length) return ElMessage.warning('请至少选择一个权限点')
  dlg.saving = true
  try {
    const payload = {
      name: dlg.name.trim(), code: dlg.code.trim(), remark: dlg.remark.trim(),
      status: dlg.status, permissions: dlg.permissions,
    }
    if (dlg.id) await api.put(`/api/admin/roles/${dlg.id}`, payload)
    else await api.post('/api/admin/roles', payload)
    ElMessage.success(dlg.id ? '权限已更新' : '角色已创建')
    dlg.visible = false
    load()
  } catch (e) {
    ElMessage.error(e.message)
  } finally {
    dlg.saving = false
  }
}

async function remove(row) {
  try {
    await ElMessageBox.confirm(`删除角色「${row.name}」？`, '删除角色', {
      confirmButtonText: '删除', cancelButtonText: '取消', type: 'warning',
    })
  } catch {
    return
  }
  try {
    await api.delete(`/api/admin/roles/${row.id}`)
    ElMessage.success('已删除')
    load()
  } catch (e) {
    ElMessage.error(e.message)
  }
}

function openAssign(row) {
  Object.assign(assignDlg, {
    visible: true, saving: false,
    adminId: row.id, username: row.username, roleIds: [...(row.roleIds || [])],
  })
}

async function saveAssign() {
  assignDlg.saving = true
  try {
    await api.put(`/api/admin/roles/admins/${assignDlg.adminId}`, { roleIds: assignDlg.roleIds })
    ElMessage.success('已保存，该账号重新登录后生效')
    assignDlg.visible = false
    load()
  } catch (e) {
    ElMessage.error(e.message)
  } finally {
    assignDlg.saving = false
  }
}

onMounted(load)
</script>

<style scoped>
.toolbar { border-radius: 8px; border: 1px solid #e8ebee; box-shadow: none; }
.toolbar :deep(.el-card__body) { padding: 14px 16px; }
.bar { display: flex; gap: 12px; align-items: center; flex-wrap: wrap; }
.panel { border-radius: 8px; border: 1px solid #e8ebee; box-shadow: none; }
.panel :deep(.el-card__header) { padding: 12px 16px; border-bottom: 1px solid #f0f2f4; }
.ph { font-size: 14px; font-weight: 600; color: #1f2937; }
.sub { font-size: 12.5px; color: #909399; margin-top: 4px; line-height: 1.6; }
.muted { color: #c0c4cc; font-size: 12px; }
.perm-box { width: 100%; border: 1px solid #e8ebee; border-radius: 8px; padding: 10px 12px; }
.perm-head { display: flex; align-items: center; justify-content: space-between; padding-bottom: 8px; border-bottom: 1px dashed #eef0f2; }
.ph-tip { font-size: 12.5px; color: #909399; }
.perm-group { margin-top: 10px; }
.pg-t { font-size: 12px; color: #909399; margin-bottom: 6px; }
.perm-group :deep(.el-checkbox) { margin-right: 14px; }
</style>
