<script setup>
import { ref, computed, onMounted, onBeforeUnmount } from 'vue'
import { api, dateTime } from '../api'
import Chart from './Chart.vue'
const props = defineProps({ screen: Boolean })
const data = ref(null),
  loading = ref(false),
  error = ref(''),
  clock = ref(new Date())
let refreshTimer, clockTimer
const number = (value) => Number(value || 0).toLocaleString()
const percent = (value, total) =>
  total ? ((Number(value) / Number(total)) * 100).toFixed(1) : '0.0'
const text = computed(() => (props.screen ? '#a5bad0' : '#708177'))
const line = computed(() => (props.screen ? '#1a354d' : '#e9eeeb'))
const common = computed(() => ({
  tooltip: { trigger: 'axis' },
  textStyle: { color: text.value, fontFamily: 'sans-serif' },
  grid: { left: 40, right: 22, top: 30, bottom: 35 },
}))
const pie = computed(() => ({
  tooltip: { trigger: 'item', formatter: '{b}: {c} 条 ({d}%)' },
  legend: {
    bottom: 0,
    textStyle: { color: text.value },
    itemWidth: 9,
    itemHeight: 9,
  },
  series: [
    {
      type: 'pie',
      radius: ['45%', '70%'],
      center: ['50%', '44%'],
      avoidLabelOverlap: true,
      label: { show: false },
      data:
        data.value?.distribution.map((row) => ({
          name: row.name,
          value: row.count,
          itemStyle: { color: row.color },
        })) || [],
    },
  ],
}))
const trend = computed(() => ({
  ...common.value,
  legend: { top: 0, textStyle: { color: text.value } },
  xAxis: {
    type: 'category',
    boundaryGap: false,
    data: data.value?.trend.map((r) => r.month.slice(2)),
    axisLine: { lineStyle: { color: line.value } },
    axisLabel: { color: text.value, fontSize: 10 },
  },
  yAxis: {
    type: 'value',
    minInterval: 1,
    splitLine: { lineStyle: { color: line.value } },
  },
  series: [
    {
      name: '实测记录',
      type: 'line',
      smooth: true,
      symbolSize: 5,
      data: data.value?.trend.map((r) => r.total),
      itemStyle: { color: props.screen ? '#38bdf8' : '#218b69' },
      areaStyle: { opacity: 0.09 },
    },
    {
      name: '污染记录',
      type: 'line',
      smooth: true,
      symbolSize: 5,
      data: data.value?.trend.map((r) => r.polluted),
      itemStyle: { color: '#f59e0b' },
    },
  ],
}))
const regions = computed(() => ({
  ...common.value,
  xAxis: {
    type: 'category',
    data: data.value?.regions.map((r) => r.abbr),
    axisLine: { lineStyle: { color: line.value } },
    axisLabel: { color: text.value, interval: 0 },
  },
  yAxis: {
    type: 'value',
    minInterval: 1,
    splitLine: { lineStyle: { color: line.value } },
  },
  series: [
    {
      name: '污染记录',
      type: 'bar',
      barMaxWidth: 22,
      data: data.value?.regions.map((r) => r.polluted),
      itemStyle: {
        color: props.screen ? '#22d3ee' : '#238b68',
        borderRadius: [4, 4, 0, 0],
      },
    },
  ],
}))
function pollutant(key, name, color) {
  return {
    ...common.value,
    grid: { left: 32, right: 12, top: 18, bottom: 25 },
    xAxis: {
      type: 'category',
      data: data.value?.regions.map((r) => r.abbr),
      axisLine: { lineStyle: { color: line.value } },
      axisLabel: { color: text.value, interval: 0, fontSize: 10 },
    },
    yAxis: {
      type: 'value',
      minInterval: 1,
      splitLine: { lineStyle: { color: line.value } },
    },
    series: [
      {
        name,
        type: 'bar',
        barMaxWidth: 15,
        data: data.value?.regions.map((r) => r[key]),
        itemStyle: { color, borderRadius: [3, 3, 0, 0] },
      },
    ],
  }
}
async function load() {
  if (loading.value) return
  loading.value = true
  try {
    data.value = await api.get(props.screen ? '/screen/statistics' : '/admin/statistics', null, {
      silent: true,
    })
    error.value = ''
  } catch (e) {
    error.value = e.message
  } finally {
    loading.value = false
  }
}
onMounted(() => {
  load()
  refreshTimer = setInterval(load, 10000)
  clockTimer = setInterval(() => {
    clock.value = new Date()
  }, 1000)
})
onBeforeUnmount(() => {
  clearInterval(refreshTimer)
  clearInterval(clockTimer)
})
</script>
<template>
  <div class="stats-dashboard" :class="{ 'dark-stats': screen }">
    <div v-if="screen" class="screen-title">
      <router-link to="/" class="screen-back">← 首页</router-link>
      <div>
        <span>QINGJING ENVIRONMENTAL MONITORING</span>
        <h1>青境 · 空气质量监测大屏</h1>
      </div>
      <time>{{ clock.toLocaleString('zh-CN', { hour12: false }) }}</time>
    </div>
    <div class="stats-toolbar">
      <span>
        <i class="status-dot" :class="{ offline: !!error }"></i>
        {{ error ? '刷新失败 · 保留上次数据' : '数据每10秒自动更新' }}
      </span>
      <span>
        更新于 {{ dateTime(data?.updatedAt) }}
        <button @click="load" :disabled="loading">刷新</button>
      </span>
    </div>
    <el-alert v-if="error" :title="error" type="warning" :closable="false" />
    <el-skeleton v-if="!data && loading" :rows="8" animated />
    <template v-if="data">
      <div class="metric-grid">
        <div class="metric-card">
          <span>累计公众反馈</span>
          <strong>{{ number(data.summary.feedbackTotal) }}</strong>
          <small>
            待指派 {{ number(data.summary.pending) }} · 待实测 {{ number(data.summary.assigned) }}
          </small>
        </div>
        <div class="metric-card">
          <span>已完成实测</span>
          <strong>{{ number(data.summary.total) }}</strong>
          <small>一条反馈对应一份实测结果</small>
        </div>
        <div class="metric-card metric-good">
          <span>空气优良记录</span>
          <strong>{{ number(data.summary.good) }}</strong>
          <small>一级与二级 · 占比 {{ percent(data.summary.good, data.summary.total) }}%</small>
        </div>
        <div class="metric-card metric-warn">
          <span>空气污染记录</span>
          <strong>{{ number(data.summary.polluted) }}</strong>
          <small>三级及以上 · 占比 {{ percent(data.summary.polluted, data.summary.total) }}%</small>
        </div>
      </div>
      <div class="chart-grid">
        <section class="chart-panel">
          <h3>
            空气质量等级分布
            <small>全部实测记录</small>
          </h3>
          <Chart :option="pie" />
        </section>
        <section class="chart-panel chart-wide">
          <h3>
            近12个月监测趋势
            <small>按确认月份统计</small>
          </h3>
          <Chart :option="trend" />
        </section>
        <section class="chart-panel chart-wide">
          <h3>
            各省污染记录数量
            <small>三级及以上</small>
          </h3>
          <Chart :option="regions" />
        </section>
        <section class="chart-panel coverage-panel">
          <h3>演示地区监测覆盖率</h3>
          <div class="coverage-item">
            <span>省级地区</span>
            <strong>
              {{ percent(data.summary.coveredProvinces, data.summary.provinceTotal) }}
              <small>%</small>
            </strong>
            <div class="coverage-track">
              <i
                :style="{
                  width: percent(data.summary.coveredProvinces, data.summary.provinceTotal) + '%',
                }"
              ></i>
            </div>
            <p>
              {{ data.summary.coveredProvinces }} / {{ data.summary.provinceTotal }} 个演示省级地区
            </p>
          </div>
          <div class="coverage-item">
            <span>市级地区</span>
            <strong>
              {{ percent(data.summary.coveredCities, data.summary.cityTotal) }}
              <small>%</small>
            </strong>
            <div class="coverage-track">
              <i
                :style="{
                  width: percent(data.summary.coveredCities, data.summary.cityTotal) + '%',
                }"
              ></i>
            </div>
            <p>{{ data.summary.coveredCities }} / {{ data.summary.cityTotal }} 个演示城市</p>
          </div>
        </section>
      </div>
      <div class="pollutant-grid">
        <section class="chart-panel">
          <h3>
            二氧化硫 SO₂
            <small>分项三级及以上</small>
          </h3>
          <Chart :option="pollutant('so2', 'SO₂污染记录', '#38bdf8')" />
        </section>
        <section class="chart-panel">
          <h3>
            一氧化碳 CO
            <small>分项三级及以上</small>
          </h3>
          <Chart :option="pollutant('co', 'CO污染记录', '#f59e0b')" />
        </section>
        <section class="chart-panel">
          <h3>
            细颗粒物 PM2.5
            <small>分项三级及以上</small>
          </h3>
          <Chart :option="pollutant('pm25', 'PM2.5污染记录', '#a78bfa')" />
        </section>
      </div>
      <p class="stats-note">
        统计范围：系统演示地区；等级采用课程三指标简化规则。公众预估等级不计入实测统计。
      </p>
    </template>
  </div>
</template>
