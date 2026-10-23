<script setup>
import { reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { api, session, roleName, home } from '../api'
const router = useRouter()
const role = 'admin'
const form = reactive({ role, account: '', password: '' }),
  busy = ref(false)
async function submit() {
  if (!form.account.trim() || !form.password || busy.value) return
  busy.value = true
  try {
    session.user = await api.post('/auth/login', form)
    session.checked = true
    router.push(home(form.role))
  } catch {
  } finally {
    busy.value = false
  }
}
</script>
<template>
  <main class="auth-page">
    <router-link to="/" class="auth-back">← 返回首页</router-link>
    <div class="auth-shell">
      <aside class="auth-art">
        <div class="eyebrow">共同守护 · 绿色生活</div>
        <h1>
          每一次参与
          <br />
          都让环境更好一点。
        </h1>
        <img src="/images/environment.png" alt="绿色环保城市插画" />
        <p>连接公众监督与现场核查</p>
      </aside>
      <section class="auth-form">
        <span class="brand-icon">青</span>
        <h2>欢迎来到青境</h2>
        <p class="muted">系统管理端 · 管理员登录</p>
        <form @submit.prevent="submit">
          <label>登录账号</label>
          <el-input
            v-model.trim="form.account"
            :placeholder="form.role === 'supervisor' ? '请输入手机号' : '请输入登录账号'"
            maxlength="32"
            autocomplete="username"
            size="large"
            aria-label="登录账号"
          />
          <label>登录密码</label>
          <el-input
            v-model="form.password"
            type="password"
            show-password
            placeholder="请输入密码"
            maxlength="32"
            autocomplete="current-password"
            size="large"
            aria-label="登录密码"
          />
          <el-button
            native-type="submit"
            type="primary"
            size="large"
            class="full-button"
            :loading="busy"
          >
            登录{{ roleName(form.role) }}端
          </el-button>
        </form>
        <p class="auth-register">公众请使用微信小程序，网格员请使用青境网格 App。</p>
        <router-link class="muted" to="/screen">查看空气质量大屏 →</router-link>
      </section>
    </div>
  </main>
</template>
