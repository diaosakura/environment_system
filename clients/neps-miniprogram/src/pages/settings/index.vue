<script setup>
import { ref } from 'vue'
import { baseUrl, setServer, request } from '../../api'
const server = ref(baseUrl()),
  error = ref(''),
  message = ref(''),
  busy = ref(false)
async function save() {
  if (busy.value) return
  busy.value = true
  error.value = ''
  message.value = ''
  try {
    setServer(server.value)
    await request('/health')
    message.value = '连接成功，返回登录即可使用'
  } catch (e) {
    error.value = e.message
  } finally {
    busy.value = false
  }
}
</script>
<template>
  <view class="page">
    <view class="hero"><view class="title">连接青境服务</view></view>
    <view class="card">
      <text class="label">后端地址（不含 /api）</text>
      <input v-model="server" placeholder="http://localhost:8080" />
      <view class="muted">
        电脑模拟器可使用 localhost；手机调试请使用电脑的局域网 IP。更改地址将退出原有账号。
      </view>
      <view v-if="error" class="error">{{ error }}</view>
      <view v-if="message" class="tag">{{ message }}</view>
      <button class="primary" :loading="busy" @tap="save">保存并检查连接</button>
    </view>
  </view>
</template>
