<script setup>
import { ref, onMounted, onBeforeUnmount, watch } from 'vue'
import * as echarts from 'echarts/core'
import { BarChart, LineChart, PieChart } from 'echarts/charts'
import { GridComponent, TooltipComponent, LegendComponent } from 'echarts/components'
import { CanvasRenderer } from 'echarts/renderers'
echarts.use([
  BarChart,
  LineChart,
  PieChart,
  GridComponent,
  TooltipComponent,
  LegendComponent,
  CanvasRenderer,
])
const props = defineProps({ option: Object })
const container = ref(null)
let chart, observer
onMounted(() => {
  chart = echarts.init(container.value)
  chart.setOption(props.option || {})
  observer = new ResizeObserver(() => chart?.resize())
  observer.observe(container.value)
})
watch(
  () => props.option,
  (value) => chart?.setOption(value || {}, true),
  { deep: true },
)
onBeforeUnmount(() => {
  observer?.disconnect()
  chart?.dispose()
})
</script>
<template>
  <div ref="container" class="chart" role="img" aria-label="空气质量统计图"></div>
</template>
