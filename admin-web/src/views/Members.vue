<template>
  <div>
    <el-card shadow="never" class="toolbar">
      <div class="bar">
        <el-input v-model="keyword" placeholder="昵称 / 手机号搜索" style="width: 200px" clearable @keyup.enter="load" />
        <el-select v-model="hasOrdered" placeholder="消费情况" style="width: 130px" clearable>
          <el-option label="有消费" :value="true" />
          <el-option label="无消费" :value="false" />
        </el-select>
        <el-select v-model="memberStatus" placeholder="会员卡状态" style="width: 130px" clearable>
          <el-option label="正常" value="ACTIVE" />
          <el-option label="临期（7天内到期）" value="EXPIRING" />
          <el-option label="已过期" value="EXPIRED" />
          <el-option label="未开通" value="NONE" />
        </el-select>
        <el-button type="primary" @click="load">查询</el-button>
        <el-button @click="reset">重置</el-button>
        <div style="flex: 1"></div>
        <el-button type="primary" plain @click="openAdd">新增 / 续费会员</el-button>
      </div>
    </el-card>

    <el-card shadow="never" class="panel" style="margin-top: 12px">
      <el-table :data="rows" v-loading="loading" stripe>
        <el-table-column prop="id" label="ID" width="64" />
        <el-table-column label="会员" min-width="170">
          <template #default="{ row }">
            <div class="member">
              <div class="mavatar">{{ (row.nickname || '会')[0] }}</div>
              <span>{{ row.nickname || '-' }}</span>
            </div>
          </template>
        </el-table-column>
        <el-table-column prop="phone" label="手机号" width="125" />
        <el-table-column label="等级" width="105">
          <template #default="{ row }">{{ levelText(row.level) }}</template>
        </el-table-column>
        <el-table-column label="会员卡" width="190">
          <template #default="{ row }">
            <div class="card-cell">
              <el-tag :type="cardTagType(row.memberStatus)" size="small" effect="light">
                {{ cardStatusText(row.memberStatus) }}
              </el-tag>
              <span v-if="row.memberExpireAt" class="expire">{{ row.memberExpireAt }} 到期</span>
              <span v-else class="expire muted">未开通</span>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="累计订单" width="95" align="right">
          <template #default="{ row }">{{ row.orderCount }}</template>
        </el-table-column>
        <el-table-column label="累计消费（元）" width="135" align="right">
          <template #default="{ row }">{{ yuan(row.totalGmv).toLocaleString('zh-CN', { minimumFractionDigits: 2 }) }}</template>
        </el-table-column>
        <el-table-column label="账号" width="85">
          <template #default="{ row }">
            <el-tag :type="row.status === 1 ? 'success' : 'info'" size="small" effect="light">
              {{ row.status === 1 ? '正常' : '已禁用' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createdAt" label="注册日期" width="110" />
      </el-table>
      <div style="display:flex;justify-content:flex-end;margin-top:14px">
        <el-pagination layout="total, prev, pager, next" :total="total"
                       :page-size="pageSize" :current-page="page"
                       @current-change="(p) => { page = p; load() }" />
      </div>
    </el-card>

    <!-- 新增 / 续费会员 -->
    <el-dialog v-model="dlg.visible" title="新增 / 续费会员" width="440px">
      <el-form label-width="82px">
        <el-form-item label="手机号" required>
          <el-input v-model="dlg.phone" maxlength="11" placeholder="11 位手机号；已存在则自动续费" />
        </el-form-item>
        <el-form-item label="昵称">
          <el-input v-model="dlg.nickname" placeholder="选填，线下顾客备注名" />
        </el-form-item>
        <el-form-item label="会员等级">
          <el-select v-model="dlg.level" style="width: 100%">
            <el-option v-for="(t, v) in levels" :key="v" :label="t" :value="Number(v)" />
          </el-select>
        </el-form-item>
        <el-form-item label="有效期">
          <el-radio-group v-model="dlg.preset">
            <el-radio-button :value="365">年卡</el-radio-button>
            <el-radio-button :value="180">半年卡</el-radio-button>
            <el-radio-button :value="90">季卡</el-radio-button>
            <el-radio-button :value="0">自定义</el-radio-button>
          </el-radio-group>
        </el-form-item>
        <el-form-item v-if="dlg.preset === 0" label="到期日期" required>
          <el-date-picker v-model="dlg.expireDate" type="date" value-format="YYYY-MM-DD"
                          :disabled-date="(d) => d.getTime() < Date.now() - 86400000" />
        </el-form-item>
        <div class="renew-tip">
          手机号已存在时为续费：未到期自动在原到期日上顺延，已过期从今天起算。
        </div>
      </el-form>
      <template #footer>
        <el-button @click="dlg.visible = false">取消</el-button>
        <el-button type="primary" :loading="dlg.saving" @click="save">确认开卡</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { api, yuan } from '../api'

const rows = ref([])
const total = ref(0)
const page = ref(1)
const pageSize = 10
const keyword = ref('')
const hasOrdered = ref(null)
const memberStatus = ref(null)
const loading = ref(false)

const levels = { 1: '普通会员', 2: '白银会员', 3: '黄金会员', 4: '钻石会员' }

const dlg = reactive({
  visible: false, saving: false,
  phone: '', nickname: '', level: 1, preset: 365, expireDate: '',
})

function levelText(level) {
  return levels[level] || '普通会员'
}

function cardStatusText(s) {
  return { ACTIVE: '正常', EXPIRING: '临期', EXPIRED: '已过期', NONE: '未开通' }[s] || '未开通'
}

function cardTagType(s) {
  return { ACTIVE: 'success', EXPIRING: 'warning', EXPIRED: 'danger', NONE: 'info' }[s] || 'info'
}

async function load() {
  loading.value = true
  try {
    const data = await api.get(`/api/admin/members?${new URLSearchParams({
      page: page.value, size: pageSize,
      ...(keyword.value ? { keyword: keyword.value } : {}),
      ...(hasOrdered.value !== null && hasOrdered.value !== undefined && hasOrdered.value !== ''
        ? { hasOrdered: hasOrdered.value } : {}),
      ...(memberStatus.value ? { memberStatus: memberStatus.value } : {}),
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
  memberStatus.value = null
  page.value = 1
  load()
}

function openAdd() {
  Object.assign(dlg, { visible: true, saving: false, phone: '', nickname: '', level: 1, preset: 365, expireDate: '' })
}

async function save() {
  if (!/^1\d{10}$/.test(dlg.phone)) {
    return ElMessage.warning('请输入 11 位手机号')
  }
  let durationDays = dlg.preset
  if (dlg.preset === 0) {
    if (!dlg.expireDate) return ElMessage.warning('请选择到期日期')
    durationDays = Math.max(1, Math.ceil((new Date(dlg.expireDate) - new Date()) / 86400000))
  }
  dlg.saving = true
  try {
    const vo = await api.post('/api/admin/members', {
      phone: dlg.phone, nickname: dlg.nickname || undefined,
      level: dlg.level, durationDays,
    })
    ElMessage.success(`已开通，${vo.memberExpireAt} 到期`)
    dlg.visible = false
    page.value = 1
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

.member { display: flex; align-items: center; gap: 9px; }
.mavatar {
  width: 30px; height: 30px; border-radius: 50%; flex: none; font-size: 13px; font-weight: 600;
  color: #fff; background: #8e9cb5; display: flex; align-items: center; justify-content: center;
}
.card-cell { display: flex; align-items: center; gap: 7px; }
.expire { font-size: 12.5px; color: #4b5563; }
.expire.muted { color: #a8afb8; }
.renew-tip {
  background: #f4f6fa; border-radius: 6px; padding: 9px 12px; margin-left: 82px;
  font-size: 12px; color: #8a939f; line-height: 1.7;
}
</style>
