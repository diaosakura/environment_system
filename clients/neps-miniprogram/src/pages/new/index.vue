<script setup>
import { ref, reactive } from 'vue'
import { onLoad } from '@dcloudio/uni-app'
import { request, gradeName } from '../../api'
const provinces = ref([]),
  cities = ref([]),
  pIndex = ref(-1),
  cIndex = ref(-1),
  form = reactive({ address: '', information: '', estimatedGrade: 1 }),
  busy = ref(false),
  error = ref(''),
  regionsBusy = ref(false)
onLoad(async () => {
  try {
    provinces.value = await request('/dictionaries/provinces')
  } catch (e) {
    error.value = e.message
  }
})
async function provinceChanged(e) {
  if (regionsBusy.value) return
  pIndex.value = Number(e.detail.value)
  cIndex.value = -1
  cities.value = []
  regionsBusy.value = true
  error.value = ''
  try {
    cities.value = await request('/dictionaries/cities', 'GET', {
      provinceId: provinces.value[pIndex.value].id,
    })
  } catch (e) {
    error.value = e.message
  } finally {
    regionsBusy.value = false
  }
}
async function submit() {
  if (busy.value) return
  error.value = ''
  if (pIndex.value < 0 || cIndex.value < 0 || !form.address.trim() || !form.information.trim()) {
    error.value = '请选择省市并填写地址和问题描述'
    return
  }
  busy.value = true
  try {
    await request('/supervisor/feedbacks', 'POST', {
      ...form,
      address: form.address.trim(),
      information: form.information.trim(),
      provinceId: provinces.value[pIndex.value].id,
      cityId: cities.value[cIndex.value].id,
    })
    uni.showToast({ title: '反馈已提交' })
    setTimeout(() => uni.navigateBack(), 700)
  } catch (e) {
    error.value = e.message
    busy.value = false
  }
}
</script>
<template>
  <view class="page">
    <view class="hero">
      <view class="title">记录身边的空气问题</view>
      <view class="muted">请尽量准确描述位置和观察到的情况。</view>
    </view>
    <view class="card">
      <text class="label">所在省份</text>
      <picker :range="provinces" range-key="name" :disabled="regionsBusy" @change="provinceChanged">
        <view class="picker">{{ provinces[pIndex]?.name || '选择省份' }}</view>
      </picker>
      <text class="label">所在城市</text>
      <picker
        :range="cities"
        range-key="name"
        :disabled="!cities.length || regionsBusy"
        @change="cIndex = Number($event.detail.value)"
      >
        <view class="picker">
          {{ cities[cIndex]?.name || (regionsBusy ? '正在加载…' : '选择城市') }}
        </view>
      </picker>
      <text class="label">详细地址</text>
      <input v-model="form.address" maxlength="200" placeholder="街道、附近地标等" />
      <text class="label">问题描述</text>
      <textarea
        v-model="form.information"
        maxlength="400"
        placeholder="例如：附近施工扬尘明显，空气有异味"
      />
      <text class="label">预估污染等级</text>
      <picker
        :range="[1, 2, 3, 4, 5, 6].map(gradeName)"
        :value="form.estimatedGrade - 1"
        @change="form.estimatedGrade = Number($event.detail.value) + 1"
      >
        <view class="picker">{{ gradeName(form.estimatedGrade) }}</view>
      </picker>
      <view v-if="error" class="error">{{ error }}</view>
      <button class="primary" :disabled="busy || regionsBusy" :loading="busy" @tap="submit">
        提交反馈
      </button>
    </view>
  </view>
</template>
