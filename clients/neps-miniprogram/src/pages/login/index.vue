<script setup>
import { ref } from 'vue'
import { request } from '../../api'
const account = ref(''),
  password = ref(''),
  busy = ref(false),
  error = ref('')
async function login() {
  if (busy.value) return
  if (!account.value.trim() || !password.value) {
    error.value = '请填写手机号和密码'
    return
  }
  busy.value = true
  error.value = ''
  try {
    const user = await request('/auth/login', 'POST', {
      role: 'supervisor',
      account: account.value.trim(),
      password: password.value,
    })
    uni.setStorageSync('neps-user', user)
    password.value = ''
    uni.reLaunch({ url: '/pages/feedbacks/index' })
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
      <view class="eyebrow">青境 / NEPS</view>
      <view class="title">关心身边的空气</view>
      <view class="muted">
        一条反馈，一次行动。
        <br />
        让环境问题得到回应。
      </view>
    </view>
    <view class="card">
      <view class="heading">公众监督员登录</view>
      <text class="label">手机号</text>
      <input v-model="account" type="number" maxlength="11" placeholder="请输入手机号" />
      <text class="label">密码</text>
      <input v-model="password" password maxlength="32" placeholder="请输入密码" @confirm="login" />
      <view v-if="error" class="error">{{ error }}</view>
      <button class="primary" :loading="busy" :disabled="busy" @tap="login">登录</button>
      <view class="row">
        <navigator class="link" url="/pages/register/index">注册监督员</navigator>
        <navigator class="link" url="/pages/settings/index">服务设置</navigator>
      </view>
    </view>
    <view class="muted">使用本系统账号登录，无需微信手机号授权。</view>
  </view>
</template>
