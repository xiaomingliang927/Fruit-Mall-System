<template>
  <div v-loading="loading">
    <!-- 指标卡 -->
    <div class="cards">
      <div v-for="c in cards" :key="c.label" class="card" :class="{ clickable: c.to }" @click="c.to && $router.push(c.to)">
        <div class="icon" :style="{ background: c.bg }">{{ c.icon }}</div>
        <div>
          <div class="value">{{ c.value }}</div>
          <div class="label">{{ c.label }}</div>
        </div>
      </div>
    </div>

    <!-- 趋势图 + 分类占比 -->
    <el-row :gutter="14" style="margin-top: 14px">
      <el-col :span="16">
        <el-card shadow="never" class="chart-card">
          <template #header>
            <div class="panel-title">📈 近 7 日销售趋势<span class="sub">销售额与支付订单数</span></div>
          </template>
          <div ref="trendRef" class="chart" style="height: 300px"></div>
        </el-card>
      </el-col>
      <el-col :span="8">
        <el-card shadow="never" class="chart-card">
          <template #header><div class="panel-title">🧩 销售分类占比<span class="sub">按商品累计销量</span></div></template>
          <div ref="pieRef" class="chart" style="height: 300px"></div>
        </el-card>
      </el-col>
    </el-row>

    <!-- 状态分布 + 热销榜 + 最近订单 -->
    <el-row :gutter="14" style="margin-top: 14px">
      <el-col :span="7">
        <el-card shadow="never" class="chart-card">
          <template #header><div class="panel-title">🍩 订单状态分布</div></template>
          <div ref="donutRef" class="chart" style="height: 260px"></div>
        </el-card>
      </el-col>
      <el-col :span="8">
        <el-card shadow="never" class="chart-card">
          <template #header><div class="panel-title">🔥 热销水果 TOP10</div></template>
          <div class="rank-list">
            <div v-for="(p, i) in stats.topProducts || []" :key="p.id" class="rank-item">
              <span class="rank-no" :class="`no-${i + 1}`">{{ i + 1 }}</span>
              <el-image :src="p.mainImage" class="rank-img" fit="cover" />
              <span class="rank-name">{{ p.name }}</span>
              <span class="rank-sales">{{ p.sales }}</span>
            </div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="9">
        <el-card shadow="never" class="chart-card">
          <template #header>
            <div class="panel-title">🧾 最近订单<el-link type="primary" style="margin-left:auto" @click="$router.push('/orders')">全部 →</el-link></div>
          </template>
          <div class="order-list">
            <div v-for="o in stats.recentOrders?.records || []" :key="o.id" class="order-item">
              <div>
                <div class="o-no">{{ o.orderNo }}</div>
                <div class="o-time">{{ fmtTime(o.createdAt) }}</div>
              </div>
              <div class="o-right">
                <div class="o-amount">¥{{ yuan(o.payAmount).toFixed(2) }}</div>
                <el-tag :type="statusType(o.status)" size="small" effect="light">{{ o.statusText }}</el-tag>
              </div>
            </div>
          </div>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup>
import { computed, nextTick, onBeforeUnmount, onMounted, ref } from 'vue'
import * as echarts from 'echarts'
import { api, fmtTime, yuan } from '../api'

const loading = ref(true)
const stats = ref({})
const trendRef = ref(null)
const pieRef = ref(null)
const donutRef = ref(null)
let charts = []

const STATUS_META = {
  10: { label: '待支付', color: '#f59e0b' },
  20: { label: '待发货', color: '#2e9e5b' },
  30: { label: '待收货', color: '#0e7490' },
  40: { label: '已完成', color: '#14532d' },
  50: { label: '已取消', color: '#94a3b8' },
  60: { label: '售后中', color: '#ff8c42' },
  70: { label: '已退款', color: '#7c3aed' },
}

const cards = computed(() => [
  { icon: '💰', label: '今日销售额（元）', value: yuan(stats.value.todayGmv || 0).toFixed(2), bg: 'linear-gradient(135deg,#fff1e6,#ffe0cc)' },
  { icon: '🧾', label: '今日支付订单', value: stats.value.todayPaidOrders ?? '-', bg: 'linear-gradient(135deg,#e6f4ea,#d2ecd9)' },
  { icon: '👥', label: '会员总数', value: stats.value.userCount ?? '-', bg: 'linear-gradient(135deg,#e0f2f7,#cbe7f0)' },
  { icon: '🍎', label: '在售商品', value: stats.value.onSaleProductCount ?? '-', bg: 'linear-gradient(135deg,#f1e9fd,#e6dafb)' },
  {
    icon: '🚚', label: '待发货订单', value: stats.value.pendingShipCount ?? '-',
    bg: 'linear-gradient(135deg,#fdeaea,#fbd9d9)', to: { path: '/orders' },
  },
])

function statusType(s) {
  return { 10: 'warning', 20: 'primary', 30: 'info', 40: 'success', 50: 'info', 60: 'warning', 70: 'danger' }[s] || 'info'
}

function renderTrend(rows) {
  const chart = echarts.init(trendRef.value)
  chart.setOption({
    tooltip: { trigger: 'axis' },
    legend: { data: ['销售额（元）', '支付订单'], top: 0 },
    grid: { left: 12, right: 12, top: 40, bottom: 8, containLabel: true },
    xAxis: { type: 'category', data: rows.map((r) => r.day), boundaryGap: true },
    yAxis: [
      { type: 'value', name: '元', splitLine: { lineStyle: { type: 'dashed' } } },
      { type: 'value', name: '单', splitLine: { show: false } },
    ],
    series: [
      {
        name: '销售额（元）', type: 'line', smooth: true, yAxisIndex: 0,
        data: rows.map((r) => Number(r.gmv) / 100),
        lineStyle: { width: 3, color: '#2e9e5b' }, itemStyle: { color: '#2e9e5b' },
        areaStyle: {
          color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
            { offset: 0, color: 'rgba(46,158,91,0.28)' },
            { offset: 1, color: 'rgba(46,158,91,0.02)' },
          ]),
        },
      },
      {
        name: '支付订单', type: 'bar', yAxisIndex: 1, barWidth: 16,
        data: rows.map((r) => Number(r.orders)),
        itemStyle: { color: 'rgba(255,140,66,0.75)', borderRadius: [5, 5, 0, 0] },
      },
    ],
  })
  charts.push(chart)
}

function renderPie(rows) {
  const chart = echarts.init(pieRef.value)
  chart.setOption({
    tooltip: { trigger: 'item', formatter: '{b}<br/>销量占比：{d}%' },
    legend: { bottom: 0, icon: 'circle', itemWidth: 9, itemHeight: 9, textStyle: { fontSize: 12 } },
    series: [{
      type: 'pie', radius: ['0%', '62%'], center: ['50%', '44%'],
      data: rows.map((r) => ({ name: r.name, value: Number(r.value) })),
      label: { show: false },
      itemStyle: { borderRadius: 6, borderColor: '#fff', borderWidth: 2 },
      color: ['#2e9e5b', '#ff8c42', '#0e7490', '#7c3aed', '#f59e0b', '#dc2626', '#94a3b8'],
    }],
  })
  charts.push(chart)
}

function renderDonut(rows) {
  const chart = echarts.init(donutRef.value)
  const data = rows.map((r) => ({
    name: STATUS_META[Number(r.status)]?.label || '未知',
    value: Number(r.count),
    itemStyle: { color: STATUS_META[Number(r.status)]?.color },
  }))
  const total = data.reduce((s, d) => s + d.value, 0)
  chart.setOption({
    tooltip: { trigger: 'item', formatter: '{b}：{c} 单（{d}%）' },
    legend: { bottom: 0, icon: 'circle', itemWidth: 9, itemHeight: 9, textStyle: { fontSize: 12 } },
    title: {
      text: String(total), subtext: '总订单', left: 'center', top: '36%',
      textStyle: { fontSize: 26, fontWeight: 700 }, subtextStyle: { fontSize: 12, color: '#909399' },
    },
    series: [{
      type: 'pie', radius: ['52%', '70%'], center: ['50%', '44%'], data,
      label: { show: false }, itemStyle: { borderRadius: 5, borderColor: '#fff', borderWidth: 2 },
    }],
  })
  charts.push(chart)
}

function onResize() {
  charts.forEach((c) => c.resize())
}

onMounted(async () => {
  stats.value = await api.get('/api/admin/stats/dashboard')
  loading.value = false
  await nextTick()
  renderTrend(stats.value.salesTrend || [])
  renderPie(stats.value.categorySales || [])
  renderDonut(stats.value.statusDistribution || [])
  window.addEventListener('resize', onResize)
})

onBeforeUnmount(() => {
  window.removeEventListener('resize', onResize)
  charts.forEach((c) => c.dispose())
})
</script>

<style scoped>
.cards { display: grid; grid-template-columns: repeat(5, 1fr); gap: 14px; }
.card {
  background: #fff; border-radius: 14px; padding: 18px; display: flex; align-items: center; gap: 14px;
  box-shadow: 0 2px 10px rgba(30, 41, 59, 0.05); position: relative;
}
.card.clickable { cursor: pointer; transition: transform 0.15s, box-shadow 0.15s; }
.card.clickable:hover { transform: translateY(-2px); box-shadow: 0 8px 18px rgba(220, 38, 38, 0.12); }
.icon {
  width: 48px; height: 48px; border-radius: 13px; font-size: 24px; flex: none;
  display: flex; align-items: center; justify-content: center;
}
.value { font-size: 23px; font-weight: 800; line-height: 1.15; white-space: nowrap; }
.label { font-size: 12.5px; color: #909399; margin-top: 3px; white-space: nowrap; }

.chart-card { border-radius: 14px; box-shadow: 0 2px 10px rgba(30, 41, 59, 0.05); }
.chart-card :deep(.el-card__header) { padding: 14px 18px; border-bottom: 1px solid #f1f5f0; }
.chart-card :deep(.el-card__body) { padding: 8px 14px 14px; }
.panel-title { font-size: 14.5px; font-weight: 700; display: flex; align-items: center; }
.panel-title .sub { font-size: 12px; color: #909399; font-weight: 400; margin-left: 10px; }

.rank-list { max-height: 260px; overflow: auto; }
.rank-item { display: flex; align-items: center; gap: 10px; padding: 7px 4px; }
.rank-no {
  width: 20px; height: 20px; border-radius: 6px; font-size: 12px; font-weight: 700; flex: none;
  background: #f1f5f9; color: #64748b; display: flex; align-items: center; justify-content: center;
}
.rank-no.no-1 { background: #fde68a; color: #92400e; }
.rank-no.no-2 { background: #e5e7eb; color: #374151; }
.rank-no.no-3 { background: #fed7aa; color: #9a3412; }
.rank-img { width: 42px; height: 36px; border-radius: 7px; flex: none; }
.rank-name { font-size: 13.5px; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.rank-sales { margin-left: auto; color: #dc2626; font-weight: 700; font-size: 13.5px; }

.order-list { max-height: 260px; overflow: auto; }
.order-item {
  display: flex; justify-content: space-between; align-items: center;
  padding: 8px 4px; border-bottom: 1px dashed #f1f5f0;
}
.order-item:last-child { border-bottom: none; }
.o-no { font-size: 13px; font-weight: 600; }
.o-time { font-size: 11.5px; color: #a8abb2; margin-top: 2px; }
.o-right { text-align: right; }
.o-amount { color: #dc2626; font-weight: 700; font-size: 13.5px; margin-bottom: 3px; }
</style>
