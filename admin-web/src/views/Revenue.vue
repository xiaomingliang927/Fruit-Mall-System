<template>
  <div>
    <!-- 筛选工具条 -->
    <el-card shadow="never" class="toolbar">
      <div class="bar">
        <el-date-picker v-model="range" type="daterange" value-format="YYYY-MM-DD"
                        range-separator="至" start-placeholder="开始日期" end-placeholder="结束日期"
                        :shortcuts="shortcuts" style="width: 280px" :clearable="false" @change="load" />
        <el-button type="primary" :loading="loading" @click="load">查询</el-button>
        <div style="flex: 1"></div>
        <el-button :disabled="!data" @click="exportExcel">
          <el-icon style="margin-right: 5px"><Download /></el-icon>导出 Excel
        </el-button>
      </div>
    </el-card>

    <!-- 汇总指标 -->
    <div class="cards" v-if="data">
      <div class="card">
        <div>
          <div class="label">营业额合计（元）</div>
          <div class="value">{{ yuan(data.totalGmv).toLocaleString('zh-CN', { minimumFractionDigits: 2 }) }}</div>
        </div>
      </div>
      <div class="card">
        <div>
          <div class="label">支付订单数</div>
          <div class="value">{{ data.totalOrders }}</div>
        </div>
      </div>
      <div class="card">
        <div>
          <div class="label">客单价（元）</div>
          <div class="value">{{ yuan(data.avgOrderAmount).toFixed(2) }}</div>
        </div>
      </div>
      <div class="card">
        <div>
          <div class="label">有营业额天数</div>
          <div class="value">{{ activeDays }} / {{ data.days.length }}</div>
        </div>
      </div>
    </div>

    <!-- 每日营业额 -->
    <el-card shadow="never" class="panel" style="margin-top: 12px">
      <template #header><div class="ptitle">每日营业额</div></template>
      <div ref="chartRef" style="height: 260px" v-show="data"></div>
    </el-card>

    <el-card shadow="never" class="panel" style="margin-top: 12px" v-if="data">
      <template #header><div class="ptitle">每日明细</div></template>
      <el-table :data="data.days" stripe max-height="420" size="default">
        <el-table-column prop="day" label="日期" width="140" />
        <el-table-column prop="orders" label="支付订单数" width="140" align="right" />
        <el-table-column label="营业额（元）" align="right">
          <template #default="{ row }">{{ yuan(row.gmv).toLocaleString('zh-CN', { minimumFractionDigits: 2 }) }}</template>
        </el-table-column>
      </el-table>
    </el-card>
  </div>
</template>

<script setup>
import { computed, nextTick, onBeforeUnmount, onMounted, ref } from 'vue'
import * as echarts from 'echarts'
import { ElMessage } from 'element-plus'
import { api, exportFile, yuan } from '../api'

const range = ref(defaultRange())
const data = ref(null)
const loading = ref(false)
const chartRef = ref(null)
let chart = null

const activeDays = computed(() =>
  data.value ? data.value.days.filter((d) => Number(d.gmv) > 0).length : 0)

function defaultRange() {
  const fmt = (d) => d.toISOString().slice(0, 10)
  const to = new Date()
  const from = new Date(to.getTime() - 29 * 86400000)
  return [fmt(from), fmt(to)]
}

const shortcuts = [
  { text: '近 7 天', value: () => sub(6) },
  { text: '近 30 天', value: () => sub(29) },
  { text: '近 90 天', value: () => sub(89) },
]

function sub(days) {
  const to = new Date()
  const from = new Date(to.getTime() - days * 86400000)
  return [from, to]
}

async function load() {
  loading.value = true
  try {
    data.value = await api.get(`/api/admin/stats/revenue?from=${range.value[0]}&to=${range.value[1]}`)
    await nextTick()
    renderChart()
  } catch (e) {
    ElMessage.error(e.message)
  } finally {
    loading.value = false
  }
}

function renderChart() {
  if (!chartRef.value) return
  if (!chart) chart = echarts.init(chartRef.value)
  chart.setOption({
    tooltip: {
      trigger: 'axis',
      valueFormatter: (v) => '¥' + Number(v).toLocaleString('zh-CN', { minimumFractionDigits: 2 }),
    },
    grid: { left: 10, right: 10, top: 30, bottom: 6, containLabel: true },
    xAxis: { type: 'category', data: data.value.days.map((d) => d.day.slice(5)) },
    yAxis: {
      type: 'value', name: '元',
      splitLine: { lineStyle: { type: 'dashed', color: '#e8ebee' } },
    },
    series: [{
      name: '营业额（元）', type: 'bar', barMaxWidth: 22,
      data: data.value.days.map((d) => Number(d.gmv) / 100),
      itemStyle: { color: '#4080ff', borderRadius: [3, 3, 0, 0] },
    }],
  })
}

async function exportExcel() {
  try {
    await exportFile(
      `/api/admin/stats/revenue/export?from=${range.value[0]}&to=${range.value[1]}`,
      `营业统计_${range.value[0]}_${range.value[1]}.xlsx`)
    ElMessage.success('已导出')
  } catch (e) {
    ElMessage.error(e.message || '导出失败')
  }
}

function onResize() {
  chart && chart.resize()
}

onMounted(() => {
  load()
  window.addEventListener('resize', onResize)
})
onBeforeUnmount(() => {
  window.removeEventListener('resize', onResize)
  chart && chart.dispose()
})
</script>

<style scoped>
.toolbar { border-radius: 8px; border: 1px solid #e8ebee; box-shadow: none; }
.toolbar :deep(.el-card__body) { padding: 14px 16px; }
.bar { display: flex; gap: 12px; align-items: center; flex-wrap: wrap; }

.cards { display: grid; grid-template-columns: repeat(4, 1fr); gap: 12px; margin-top: 12px; }
.card {
  background: #fff; border: 1px solid #e8ebee; border-radius: 8px; padding: 16px 18px;
}
.label { font-size: 12.5px; color: #8a939f; }
.value { font-size: 22px; font-weight: 600; color: #1f2937; margin-top: 4px; white-space: nowrap; }

.panel { border-radius: 8px; border: 1px solid #e8ebee; box-shadow: none; }
.panel :deep(.el-card__header) { padding: 13px 16px; border-bottom: 1px solid #f0f2f4; }
.panel :deep(.el-card__body) { padding: 8px 12px 12px; }
.ptitle { font-size: 14px; font-weight: 600; color: #1f2937; }
</style>
