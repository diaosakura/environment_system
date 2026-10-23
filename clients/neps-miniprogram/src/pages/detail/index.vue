<script setup>
import { ref } from 'vue'
import { onLoad } from '@dcloudio/uni-app'
import { request, stateName, gradeName, dateTime } from '../../api'
const detail = ref(null),
  id = ref(''),
  busy = ref(false),
  error = ref('')
async function load() {
  busy.value = true
  error.value = ''
  try {
    detail.value = await request('/supervisor/feedbacks/' + id.value)
  } catch (e) {
    error.value = e.message
  } finally {
    busy.value = false
  }
}
onLoad((query) => {
  id.value = query.id
  load()
})
</script>
<template>
  <view class="page">
    <view v-if="error" class="error" @tap="load">{{ error }} · 点击重试</view>
    <view v-if="busy" class="empty">正在加载…</view>
    <template v-if="detail">
      <view class="hero">
        <text class="tag">{{ stateName(detail.state) }}</text>
        <view class="title">反馈 #{{ detail.id }}</view>
        <view class="muted">{{ detail.provinceName }} · {{ detail.cityName }}</view>
      </view>
      <view class="card">
        <view class="address">{{ detail.address }}</view>
        <view class="body">{{ detail.information }}</view>
        <view class="sep" />
        <view class="muted">公众预估：{{ gradeName(detail.estimatedGrade) }}</view>
        <view class="muted">负责网格员：{{ detail.gridMemberName || '等待指派' }}</view>
      </view>
      <view class="card">
        <view class="heading">处理进展</view>
        <view class="body">已上报：{{ dateTime(detail.submittedAt) }}</view>
        <view class="body">已指派：{{ dateTime(detail.assignedAt) }}</view>
        <view class="body">已确认：{{ dateTime(detail.confirmedAt) }}</view>
      </view>
      <view v-if="detail.state === 2" class="card">
        <view class="heading">实测确认结果</view>
        <view class="value">{{ gradeName(detail.actualGrade) }}</view>
        <view class="sep" />
        <view class="body">SO₂：{{ detail.so2 }} μg/m³</view>
        <view class="body">CO：{{ detail.co }} mg/m³</view>
        <view class="body">PM2.5：{{ detail.pm25 }} μg/m³</view>
        <view class="muted">等级按课程约定的三污染物阈值确认。</view>
      </view>
      <button class="secondary" :loading="busy" @tap="load">刷新进展</button>
    </template>
  </view>
</template>
