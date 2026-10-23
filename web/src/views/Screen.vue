<script setup>
import StatsDashboard from '../components/StatsDashboard.vue'
function fullscreen() {
  if (document.fullscreenElement) document.exitFullscreen()
  else document.documentElement.requestFullscreen?.()
}
</script>
<template>
  <main class="screen-page">
    <StatsDashboard screen />
    <button class="fullscreen-button" @click="fullscreen">全屏 / 退出全屏</button>
  </main>
</template>
