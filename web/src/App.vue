<script setup>
import { computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { api, session, roleName } from './api'
const route = useRoute(),
  router = useRouter()
const bare = computed(() => ['/', '/login', '/register', '/screen'].includes(route.path))
async function logout() {
  try {
    await api.post('/auth/logout')
    session.user = null
    session.checked = false
    router.push('/')
  } catch {}
}
</script>
<template>
  <div v-if="!bare" class="app-header">
    <router-link to="/" class="brand">
      <span class="brand-icon">青</span>
      青境
      <span class="brand-sub">环保公众监督系统</span>
    </router-link>
    <div class="header-user">
      <span>
        {{ session.user?.name }}
        <small>{{ roleName(session.user?.role) }}</small>
      </span>
      <button class="text-button" @click="logout">退出</button>
    </div>
  </div>
  <router-view />
</template>
