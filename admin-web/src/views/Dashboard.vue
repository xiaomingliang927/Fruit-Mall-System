<template>
  <div v-loading="loading">
    <!-- 指标卡 -->
    <div class="cards">
      <div v-for="c in cards" :key="c.label" class="card" :class="{ clickable: c.to }" @click="c.to && $router.push(c.to)">
        <div>
          <div class="label">{{ c.label }}</div>
          <div class="value">{{ c.value }}</div>
        </div>
        <el-icon v-if="c.icon" class="cicon" :size="30" :style="{ color: c.iconColor }">
          <component :is="c.icon" />
        </el-icon>
      </div>
    </div>

    <!-- 趋势图 + 分类营业额占比 -->
    <el-row :gutter="12" style="margin-top: 12px">
      <el-col :span="16">
        <el-card shadow="never" class="panel">
          <template #header>
            <div class="ptitle">近 7 日销售趋势<span class="psub">销售额（元）与支付订单数</span></div>
          </template>
          <div ref="trendRef" class="chart" style="height: 290px"></div>
        </el-card>
      </el-col>
      <el-col :span="8">
        <el-card shadow="never" class="panel">
          <template #header><div class="ptitle">分类营业额占比<span class="psub">已支付订单</span></div></template>
          <div ref="pieRef" class="chart" style="height: 190px"></div>
          <!-- 具体数字图例：名称 / 金额 / 占比 -->
          <div class="pie-legend">
            <div v-for="(item, i) in pieData" :key="item.name" class="pl-row">
              <span class="dot" :style="{ background: PIE_COLORS[i % PIE_COLORS.length] }"></span>
              <span class="pl-name">{{ item.name }}</span>
              <span class="pl-amount">¥{{ item.yuanText }}</span>
              <span class="pl-pct">{{ item.pct }}%</span>
            </div>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <!-- 状态分布 + 热销榜 + 最近订单 -->
    <el-row :gutter="12" style="margin-top: 12px">
      <el-col :span="7">
        <el-card shadow="never" class="panel">
          <template #header><div class="ptitle">订单状态分布</div></template>
          <div ref="donutRef" class="chart" style="height: 250px"></div>
        </el-card>
      </el-col>
      <el-col :span="8">
        <el-card shadow="never" class="panel">
          <template #header><div class="ptitle">热销商品 TOP10<span class="psub">按累计销量</span></div></template>
          <div class="rank-list">
            <div v-for="(p, i) in stats.topProducts || []" :key="p.id" class="rank-item">
              <span class="rank-no">{{ i + 1 }}</span>
              <el-image :src="p.mainImage" class="rank-img" fit="cover" />
              <span class="rank-name">{{ p.name }}</span>
              <span class="rank-sales">{{ p.sales }}</span>
            </div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="9">
        <el-card shadow="never" class="panel">
          <template #header>
            <div class="ptitle">最近订单<el-link type="primary" style="margin-left:auto" @click="$router.push('/orders')">全部 →</el-link></div>
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
const pieData = ref([])
let charts = []

const PIE_COLORS = ['#4080ff', '#36b37e', '#f5a623', '#8e9cb5', '#e5615c', '#9b7fe6', '#5fb3c9']

const cards = computed(() => [
  { label: '今日营业额（元）', value: yuan(stats.value.todayGmv || 0).toFixed(2), icon: 'Wallet', iconColor: '#4080ff' },
  { label: '今日支付订单', value: stats.value.todayPaidOrders ?? '-', icon: 'Tickets', iconColor: '#36b37e' },
  { label: '会员总数', value: stats.value.userCount ?? '-', icon: 'User', iconColor: '#8e9cb5' },
  { label: '在售商品', value: stats.value.onSaleProductCount ?? '-', icon: 'Goods', iconColor: '#f5a623' },
  {
    label: '待发货订单', value: stats.value.pendingShipCount ?? '-',
    icon: 'Van', iconColor: '#e5615c', to: '/orders',
  },
])

function statusType(s) {
  return { 10: 'warning', 20: 'primary', 30: 'info', 40: 'success', 50: 'info', 60: 'warning', 70: 'danger' }[s] || 'info'
}

function renderTrend(rows) {
  const chart = echarts.init(trendRef.value)
  chart.setOption({
    tooltip: { trigger: 'axis' },
    legend: { data: ['营业额（元）', '支付订单'], top: 0, itemWidth: 14, itemHeight: 8 },
    grid: { left: 10, right: 10, top: 38, bottom: 6, containLabel: true },
    xAxis: { type: 'category', data: rows.map((r) => r.day), boundaryGap: true },
    yAxis: [
      { type: 'value', name: '元', splitLine: { lineStyle: { type: 'dashed', color: '#e8ebee' } } },
      { type: 'value', name: '单', splitLine: { show: false } },
    ],
    series: [
      {
        name: '营业额（元）', type: 'line', smooth: true, yAxisIndex: 0,
        data: rows.map((r) => Number(r.gmv) / 100),
        lineStyle: { width: 2.5, color: '#4080ff' }, itemStyle: { color: '#4080ff' },
        symbolSize: 6,
        areaStyle: {
          color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
            { offset: 0, color: 'rgba(64,128,255,0.14)' },
            { offset: 1, color: 'rgba(64,128,255,0.01)' },
          ]),
        },
      },
      {
        name: '支付订单', type: 'bar', yAxisIndex: 1, barWidth: 14,
        data: rows.map((r) => Number(r.orders)),
        itemStyle: { color: '#c3cbd5', borderRadius: [3, 3, 0, 0] },
      },
    ],
  })
  charts.push(chart)
}

function renderPie(rows) {
  const total = rows.reduce((s, r) => s + Number(r.gmv), 0) || 1
  pieData.value = rows.map((r) => {
    const gmv = Number(r.gmv)
    return {
      name: r.name,
      value: gmv / 100,
      yuanText: (gmv / 100).toLocaleString('zh-CN', { minimumFractionDigits: 2, maximumFractionDigits: 2 }),
      pct: ((gmv / total) * 100).toFixed(1),
    }
  })
  const chart = echarts.init(pieRef.value)
  chart.setOption({
    tooltip: {
      trigger: 'item',
      formatter: (p) => `${p.name}<br/>营业额 ¥${p.value.toLocaleString('zh-CN', { minimumFractionDigits: 2 })}（${p.percent}%）`,
    },
    series: [{
      type: 'pie', radius: ['0%', '78%'], center: ['50%', '50%'],
      data: pieData.value.map((d, i) => ({
        name: d.name, value: d.value, itemStyle: { color: PIE_COLORS[i % PIE_COLORS.length] },
      })),
      label: { show: false },
      itemStyle: { borderColor: '#fff', borderWidth: 2 },
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
    legend: { bottom: 0, icon: 'circle', itemWidth: 8, itemHeight: 8, textStyle: { fontSize: 11.5 } },
    title: {
      text: String(total), subtext: '总订单', left: 'center', top: '34%',
      textStyle: { fontSize: 24, fontWeight: 600, color: '#1f2937' },
      subtextStyle: { fontSize: 11.5, color: '#8a939f' },
    },
    series: [{
      type: 'pie', radius: ['54%', '72%'], center: ['50%', '44%'], data,
      label: { show: false }, itemStyle: { borderColor: '#fff', borderWidth: 2 },
    }],
  })
  charts.push(chart)
}

const STATUS_META = {
  10: { label: '待支付', color: '#f5a623' },
  20: { label: '待发货', color: '#4080ff' },
  30: { label: '待收货', color: '#5fb3c9' },
  40: { label: '已完成', color: '#36b37e' },
  50: { label: '已取消', color: '#c3cbd5' },
  60: { label: '售后中', color: '#e5615c' },
  70: { label: '已退款', color: '#9b7fe6' },
}

function onResize() {
  charts.forEach((c) => c.resize())
}

onMounted(async () => {
  stats.value = await api.get('/api/admin/stats/dashboard')
  loading.value = false
  await nextTick()
  renderTrend(stats.value.salesTrend || [])
  renderPie(stats.value.categoryGmv || [])
  renderDonut(stats.value.statusDistribution || [])
  window.addEventListener('resize', onResize)
})

onBeforeUnmount(() => {
  window.removeEventListener('resize', onResize)
  charts.forEach((c) => c.dispose())
})
</script>

<style scoped>
/* 指标卡：平面白卡 + 细边框，去装饰 */
.cards { display: grid; grid-template-columns: repeat(5, 1fr); gap: 12px; }
.card {
  background: #fff; border: 1px solid #e8ebee; border-radius: 8px; padding: 16px 18px;
  display: flex; align-items: center; justify-content: space-between;
}
.card.clickable { cursor: pointer; transition: border-color 0.15s, box-shadow 0.15s; }
.card.clickable:hover { border-color: #4080ff; box-shadow: 0 2px 10px rgba(64, 128, 255, 0.12); }
.label { font-size: 12.5px; color: #8a939f; white-space: nowrap; }
.value { font-size: 22px; font-weight: 600; color: #1f2937; margin-top: 4px; white-space: nowrap; }
.cicon { opacity: 0.85; }

.panel { border-radius: 8px; border: 1px solid #e8ebee; box-shadow: none; }
.panel :deep(.el-card__header) { padding: 13px 16px; border-bottom: 1px solid #f0f2f4; }
.panel :deep(.el-card__body) { padding: 8px 12px 12px; }
.ptitle { font-size: 14px; font-weight: 600; color: #1f2937; display: flex; align-items: center; }
.psub { font-size: 12px; color: #a8afb8; font-weight: 400; margin-left: 8px; }

/* 分类占比数字图例 */
.pie-legend { margin-top: 4px; }
.pl-row { display: flex; align-items: center; gap: 8px; padding: 5px 2px; font-size: 12.5px; }
.dot { width: 9px; height: 9px; border-radius: 2px; flex: none; }
.pl-name { color: #4b5563; flex: 1; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.pl-amount { color: #1f2937; font-weight: 600; font-variant-numeric: tabular-nums; }
.pl-pct { color: #8a939f; width: 52px; text-align: right; }

.rank-list { max-height: 250px; overflow: auto; }
.rank-item { display: flex; align-items: center; gap: 10px; padding: 6px 2px; }
.rank-no {
  width: 18px; height: 18px; border-radius: 4px; font-size: 11.5px; flex: none;
  background: #f0f2f4; color: #6b7280; display: flex; align-items: center; justify-content: center;
}
.rank-img { width: 40px; height: 34px; border-radius: 5px; flex: none; }
.rank-name { font-size: 13px; color: #374151; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.rank-sales { margin-left: auto; color: #1f2937; font-weight: 600; font-size: 13px; }

.order-list { max-height: 250px; overflow: auto; }
.order-item {
  display: flex; justify-content: space-between; align-items: center;
  padding: 7px 2px; border-bottom: 1px solid #f4f5f7;
}
.order-item:last-child { border-bottom: none; }
.o-no { font-size: 12.5px; font-weight: 600; color: #374151; }
.o-time { font-size: 11px; color: #a8afb8; margin-top: 2px; }
.o-right { text-align: right; }
.o-amount { color: #1f2937; font-weight: 600; font-size: 13px; margin-bottom: 3px; }
</style>
