<script setup>
import { ArrowRight, Monitor, Histogram } from '@element-plus/icons-vue'
const entries = [
  {
    role: 'admin',
    icon: Monitor,
    title: '系统管理',
    text: '统筹反馈与人员，跟进核查进度和监测结果。',
    action: '进入管理中心',
    number: '01',
  },
  {
    role: 'screen',
    icon: Histogram,
    title: '可视化大屏',
    text: '实时汇总核查进度与空气质量，为环境治理提供依据。',
    action: '查看数据大屏',
    number: '02',
  },
]
</script>
<template>
  <div class="landing">
    <header class="landing-header">
      <a href="#/" class="brand">
        <span class="brand-icon">青</span>
        青境
        <span class="brand-sub">环保公众监督系统</span>
      </a>
      <router-link to="/screen" class="screen-link">
        <el-icon><Histogram /></el-icon>
        空气质量大屏
        <el-icon><ArrowRight /></el-icon>
      </router-link>
    </header>
    <main class="landing-main">
      <div class="hero-copy">
        <div class="eyebrow">
          <i></i>
          公众参与 · 协同守护
        </div>
        <h1>
          让每一份关注，
          <br />
          成为清新空气的力量。
        </h1>
        <p>
          从一条公众反馈，到一次现场核查。
          <br />
          连接公众、网格员与管理者，共同守护身边的环境。
        </p>
        <div class="hero-meta">
          <span>公众上报</span>
          <b>→</b>
          <span>网格核查</span>
          <b>→</b>
          <span>数据可视</span>
        </div>
      </div>
      <div class="hero-art">
        <div class="art-caption">OUR ENVIRONMENT, OUR RESPONSIBILITY</div>
        <img src="/images/environment.png" alt="绿色城市与环保地球插画" />
        <div class="art-label">
          <span class="status-dot"></span>
          保护环境，人人有责
        </div>
      </div>
      <div class="entry-grid">
        <router-link
          v-for="item in entries"
          :key="item.role"
          :to="item.role === 'screen' ? '/screen' : '/login'"
          class="entry-card"
        >
          <span class="entry-number">{{ item.number }}</span>
          <div class="entry-icon">
            <el-icon><component :is="item.icon" /></el-icon>
          </div>
          <h2>{{ item.title }}</h2>
          <p>{{ item.text }}</p>
          <span class="entry-action">
            {{ item.action }}
            <el-icon><ArrowRight /></el-icon>
          </span>
        </router-link>
      </div>
    </main>
    <footer class="landing-footer">
      <span>青境 · 环保公众监督系统</span>
      <span>公众监督：微信小程序 · 网格核查：Android App</span>
    </footer>
  </div>
</template>
