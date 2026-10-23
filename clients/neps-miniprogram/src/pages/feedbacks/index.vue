<script setup>
import { ref } from 'vue'
import { onShow, onPullDownRefresh, onReachBottom } from '@dcloudio/uni-app'
import { request, stateName, dateTime, clearSession } from '../../api'
const rows = ref([]),
  total = ref(0),
  page = ref(1),
  state = ref(''),
  busy = ref(false),
  error = ref('')
async function load(more = false) {
  if (busy.value) return
  busy.value = true
  error.value = ''
  const next = more ? page.value + 1 : 1
  try {
    const data = await request('/supervisor/feedbacks', 'GET', {
      page: next,
      size: 10,
      ...(state.value !== '' ? { state: state.value } : {}),
    })
    rows.value = more ? [...rows.value, ...data.records] : data.records
    page.value = next
    total.value = data.total
  } catch (e) {
    error.value = e.message
  } finally {
    busy.value = false
    uni.stopPullDownRefresh()
  }
}
function filter(value) {
  if (busy.value) return
  state.value = value
  load()
}
async function logout() {
  try {
    await request('/auth/logout', 'POST')
  } catch {}
  clearSession()
  uni.reLaunch({ url: '/pages/login/index' })
}
onShow(() => load())
onPullDownRefresh(() => load())
onReachBottom(() => {
  if (rows.value.length < total.value) load(true)
})
</script>
<template>
  <view class="page">
    <view class="hero">
      <view class="row">
        <view class="eyebrow">青境 / 我的监督记录</view>
        <text class="link" @tap="logout">退出</text>
      </view>
      <view class="title">每一份反馈都有进展</view>
      <view class="muted">已查询到 {{ total }} 条反馈，下拉可刷新。</view>
      <navigator url="/pages/new/index"><button class="primary">＋ 反馈空气质量</button></navigator>
    </view>
    <view class="tabs">
      <view
        v-for="(label, index) in ['全部', '待指派', '待实测', '已完成']"
        :key="label"
        :class="['tab', { active: state === (index === 0 ? '' : index - 1) }]"
        @tap="filter(index === 0 ? '' : index - 1)"
      >
        {{ label }}
      </view>
    </view>
    <view v-if="error" class="error" @tap="load()">{{ error }} · 点击重试</view>
    <view
      v-for="item in rows"
      :key="item.id"
      class="card"
      @tap="uni.navigateTo({ url: '/pages/detail/index?id=' + item.id })"
    >
      <view class="row">
        <text class="small">#{{ item.id }} · {{ item.cityName }}</text>
        <text class="tag">{{ stateName(item.state) }}</text>
      </view>
      <view class="address">{{ item.address }}</view>
      <view class="body">{{ item.information }}</view>
      <view class="sep" />
      <view class="row">
        <text class="small">{{ dateTime(item.submittedAt) }}</text>
        <text class="link">查看进展 →</text>
      </view>
    </view>
    <view v-if="!busy && !rows.length && !error" class="empty">
      暂无反馈，点击上方按钮提交第一条。
    </view>
    <button v-if="rows.length < total" class="secondary" :loading="busy" @tap="load(true)">
      加载更多
    </button>
    <view v-if="busy" class="empty">正在加载…</view>
  </view>
</template>
