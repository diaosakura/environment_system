<script setup>
import { reactive, ref } from 'vue'
import { request } from '../../api'
const form = reactive({ phone: '', name: '', password: '', sex: 0 }),
  busy = ref(false),
  error = ref('')
async function submit() {
  if (busy.value) return
  error.value = ''
  if (!/^1\d{10}$/.test(form.phone) || !form.name.trim() || form.password.length < 6) {
    error.value = '请填写有效手机号、姓名和至少6位密码'
    return
  }
  busy.value = true
  try {
    await request('/auth/register', 'POST', { ...form, name: form.name.trim() })
    uni.showToast({ title: '注册成功，请登录', icon: 'none' })
    setTimeout(() => uni.navigateBack(), 800)
  } catch (e) {
    error.value = e.message
  } finally {
    busy.value = false
  }
}
</script>
<template>
  <view class="page">
    <view class="hero">
      <view class="title">成为公众监督员</view>
      <view class="muted">共同参与环境保护</view>
    </view>
    <view class="card">
      <text class="label">手机号</text>
      <input v-model="form.phone" type="number" maxlength="11" placeholder="11位手机号" />
      <text class="label">姓名</text>
      <input v-model="form.name" maxlength="40" placeholder="真实姓名" />
      <text class="label">性别</text>
      <picker
        :range="['女', '男']"
        :value="form.sex"
        @change="form.sex = Number($event.detail.value)"
      >
        <view class="picker">{{ form.sex === 0 ? '女' : '男' }}</view>
      </picker>
      <text class="label">密码</text>
      <input v-model="form.password" password maxlength="32" placeholder="6—32位密码" />
      <view v-if="error" class="error">{{ error }}</view>
      <button class="primary" :loading="busy" :disabled="busy" @tap="submit">注册</button>
    </view>
  </view>
</template>
