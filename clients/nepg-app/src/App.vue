<script setup>
import { ref, reactive, computed, onMounted, onUnmounted } from 'vue'
import { request, stateName, gradeName, dateTime, previewLevel } from './api'
const view = ref('login'),
  user = ref(null),
  busy = ref(false),
  error = ref(''),
  message = ref(''),
  confirming = ref(false)
const login = reactive({ account: '', password: '' }),
  server = ref(window.GridNative?.getBaseUrl() || 'http://10.0.2.2:8080')
const rows = ref([]),
  total = ref(0),
  page = ref(1),
  state = ref(1),
  keyword = ref(''),
  detail = ref(null),
  levels = ref([])
const values = reactive({ so2: '', co: '', pm25: '' })
const preview = computed(() =>
  Math.max(
    ...['so2', 'co', 'pm25'].map((key, i) =>
      previewLevel(values[key], levels.value, ['so2Max', 'coMax', 'pm25Max'][i]),
    ),
  ),
)
const validValues = computed(() =>
  Object.entries(values).every(
    ([key, value], i) => previewLevel(value, levels.value, ['so2Max', 'coMax', 'pm25Max'][i]) > 0,
  ),
)
function fail(e) {
  error.value = e.message
  if (e.status === 401) {
    user.value = null
    login.password = ''
    view.value = 'login'
  }
}
function changeView(next) {
  error.value = ''
  message.value = ''
  view.value = next
}
async function signIn() {
  if (busy.value) return
  error.value = ''
  if (!login.account.trim() || !login.password) {
    error.value = '请输入账号与密码'
    return
  }
  busy.value = true
  try {
    user.value = await request('/auth/login', 'POST', {
      role: 'grid',
      account: login.account.trim(),
      password: login.password,
    })
    login.password = ''
    levels.value = await request('/dictionaries/aqi-levels')
    changeView('tasks')
    await load()
  } catch (e) {
    fail(e)
  } finally {
    busy.value = false
  }
}
async function load(more = false) {
  error.value = ''
  const next = more ? page.value + 1 : 1
  const query = new URLSearchParams({
    page: next,
    size: 10,
    ...(state.value !== '' ? { state: state.value } : {}),
    ...(keyword.value.trim() ? { keyword: keyword.value.trim() } : {}),
  })
  try {
    const data = await request('/grid/tasks?' + query)
    rows.value = more ? [...rows.value, ...data.records] : data.records
    total.value = data.total
    page.value = next
  } catch (e) {
    fail(e)
  }
}
async function refresh() {
  if (busy.value) return
  busy.value = true
  await load()
  busy.value = false
}
async function loadMore() {
  if (busy.value) return
  busy.value = true
  await load(true)
  busy.value = false
}
async function filter(n) {
  if (busy.value) return
  state.value = n
  await refresh()
}
async function openTask(id) {
  if (busy.value) return
  busy.value = true
  error.value = ''
  try {
    detail.value = await request('/grid/tasks/' + id)
    Object.assign(values, { so2: '', co: '', pm25: '' })
    changeView('detail')
    window.scrollTo(0, 0)
  } catch (e) {
    fail(e)
  } finally {
    busy.value = false
  }
}
function submit() {
  if (busy.value) return
  error.value = ''
  if (!validValues.value) {
    error.value = '请填写三项非负浓度，最多3位小数，最大9999999.999'
    return
  }
  confirming.value = true
}
async function confirmMeasurement() {
  if (busy.value) return
  confirming.value = false
  busy.value = true
  try {
    const result = await request(
      '/grid/tasks/' + detail.value.id + '/measurement',
      'POST',
      Object.fromEntries(Object.entries(values).map(([k, v]) => [k, Number(v)])),
    )
    detail.value = await request('/grid/tasks/' + detail.value.id)
    message.value = '已提交，实测等级为“' + gradeName(result.grade) + '”'
    await load()
  } catch (e) {
    fail(e)
  } finally {
    busy.value = false
  }
}
async function logout() {
  if (busy.value) return
  busy.value = true
  try {
    await request('/auth/logout', 'POST')
  } catch {}
  user.value = null
  rows.value = []
  detail.value = null
  changeView('login')
  busy.value = false
}
async function saveServer() {
  if (busy.value) return
  error.value = ''
  message.value = ''
  try {
    const result = window.GridNative?.configure(server.value.trim())
    if (result !== 'ok') throw new Error(result || '请在 App 内配置服务')
    user.value = null
    busy.value = true
    await request('/health')
    message.value = '连接成功，请返回登录'
  } catch (e) {
    fail(e)
  } finally {
    busy.value = false
  }
}
function back() {
  if (busy.value) return
  if (confirming.value) {
    confirming.value = false
    return
  }
  if (view.value === 'detail') {
    changeView('tasks')
    load()
  } else if (view.value === 'settings') {
    changeView('login')
  } else if (view.value === 'tasks') logout()
  else window.GridNative?.closeApp()
}
onMounted(() => {
  window.appBack = back
})
onUnmounted(() => {
  delete window.appBack
})
</script>
<template>
  <div class="app-shell">
    <div
      v-if="confirming"
      class="confirm-overlay"
      role="dialog"
      aria-modal="true"
      aria-label="确认实测数据"
    >
      <section class="card">
        <h2>确认提交实测数据？</h2>
        <p>综合等级：{{ gradeName(preview) }}。提交后本任务将完成，请核对浓度与单位。</p>
        <button class="primary" @click="confirmMeasurement">确认提交</button>
        <button class="secondary" @click="confirming = false">继续核对</button>
      </section>
    </div>
    <header class="topbar">
      <button
        v-if="view === 'detail' || view === 'settings'"
        class="icon-button"
        aria-label="返回"
        @click="back"
      >
        ‹
      </button>
      <div class="brand">
        <span class="logo">青</span>
        <div>
          青境网格
          <small>AQI FIELD WORKSPACE</small>
        </div>
      </div>
      <button
        v-if="user && view !== 'settings'"
        class="text-button"
        :disabled="busy"
        @click="logout"
      >
        退出
      </button>
    </header>
    <main v-if="view === 'login'" class="page login-page">
      <div class="hero">
        <div class="eyebrow">NEPG / 现场核查</div>
        <h1>
          到现场，
          <br />
          让数据回应关切。
        </h1>
        <p>接收区域任务，记录实测结果。</p>
      </div>
      <form class="card" @submit.prevent="signIn">
        <h2>网格员登录</h2>
        <label for="account">账号</label>
        <input
          id="account"
          v-model="login.account"
          maxlength="32"
          autocomplete="username"
          placeholder="请输入网格员账号"
        />
        <label for="password">密码</label>
        <input
          id="password"
          v-model="login.password"
          type="password"
          maxlength="32"
          autocomplete="current-password"
          placeholder="请输入密码"
        />
        <p v-if="error" class="error" role="alert">{{ error }}</p>
        <button class="primary" :disabled="busy">{{ busy ? '正在连接…' : '登录工作台' }}</button>
        <button type="button" class="text-button settings-link" @click="changeView('settings')">
          设置服务器地址
        </button>
      </form>
      <p class="footnote">青境网格 · 移动现场核查</p>
    </main>
    <main v-else-if="view === 'settings'" class="page">
      <div class="hero">
        <h1>连接工作服务</h1>
        <p>首次使用时确认后端地址。</p>
      </div>
      <form class="card" @submit.prevent="saveServer">
        <label for="server">服务器地址（不含 /api）</label>
        <input id="server" v-model="server" type="url" placeholder="http://10.0.2.2:8080" />
        <p class="hint">
          Android Studio 模拟器访问电脑请使用 10.0.2.2；真实手机请填写电脑的局域网
          IP。更改地址将退出原账号。
        </p>
        <p v-if="error" class="error">{{ error }}</p>
        <p v-if="message" class="success">{{ message }}</p>
        <button class="primary" :disabled="busy">保存并检查连接</button>
      </form>
    </main>
    <main v-else-if="view === 'tasks'" class="page tasks-page">
      <div class="hero compact">
        <div class="eyebrow">你好，{{ user?.name }}</div>
        <h1>我的核查任务</h1>
        <p>仅展示指派给你的任务，共 {{ total }} 项。</p>
      </div>
      <div class="tabs">
        <button :class="{ active: state === 1 }" :disabled="busy" @click="filter(1)">待实测</button>
        <button :class="{ active: state === 2 }" :disabled="busy" @click="filter(2)">已完成</button>
        <button :class="{ active: state === '' }" :disabled="busy" @click="filter('')">全部</button>
      </div>
      <form class="search" @submit.prevent="refresh">
        <input v-model="keyword" placeholder="搜索地址或描述" aria-label="搜索地址或描述" />
        <button :disabled="busy">查询</button>
      </form>
      <div class="row list-meta">
        <small>{{ busy ? '正在加载…' : '点击任务查看详情' }}</small>
        <button class="text-button" :disabled="busy" @click="refresh">刷新</button>
      </div>
      <p v-if="error" class="error">{{ error }}</p>
      <button
        v-for="item in rows"
        :key="item.id"
        class="task-card"
        :disabled="busy"
        @click="openTask(item.id)"
      >
        <div class="row">
          <small>#{{ item.id }} · {{ item.cityName }}</small>
          <span class="tag" :class="{ done: item.state === 2 }">{{ stateName(item.state) }}</span>
        </div>
        <h2>{{ item.address }}</h2>
        <p>{{ item.information }}</p>
        <div class="row">
          <small>{{ dateTime(item.assignedAt) }}</small>
          <span class="arrow">→</span>
        </div>
      </button>
      <div v-if="!rows.length && !busy && !error" class="empty">
        暂无{{ state === 1 ? '待实测' : '' }}任务
      </div>
      <button v-if="rows.length < total" class="secondary" :disabled="busy" @click="loadMore">
        加载更多
      </button>
    </main>
    <main v-else-if="view === 'detail' && detail" class="page">
      <div class="hero compact">
        <span class="tag">{{ stateName(detail.state) }}</span>
        <h1>现场核查 #{{ detail.id }}</h1>
        <p>{{ detail.provinceName }} · {{ detail.cityName }}</p>
      </div>
      <section class="card">
        <h2>{{ detail.address }}</h2>
        <p class="description">{{ detail.information }}</p>
        <div class="divider" />
        <dl>
          <dt>反馈人</dt>
          <dd>{{ detail.supervisorName }}</dd>
          <dt>公众预估</dt>
          <dd>{{ gradeName(detail.estimatedGrade) }}</dd>
          <dt>上报时间</dt>
          <dd>{{ dateTime(detail.submittedAt) }}</dd>
          <dt>指派时间</dt>
          <dd>{{ dateTime(detail.assignedAt) }}</dd>
        </dl>
      </section>
      <form v-if="detail.state === 1" class="card" @submit.prevent="submit">
        <div class="eyebrow">FIELD MEASUREMENT</div>
        <h2>填写现场实测</h2>
        <p class="hint">请确认单位。等级按课程三污染物阈值确认，以后端结果为准。</p>
        <label for="so2">
          SO₂
          <span>μg/m³</span>
        </label>
        <input id="so2" v-model="values.so2" inputmode="decimal" placeholder="输入二氧化硫浓度" />
        <label for="co">
          CO
          <span>mg/m³</span>
        </label>
        <input id="co" v-model="values.co" inputmode="decimal" placeholder="输入一氧化碳浓度" />
        <label for="pm25">
          PM2.5
          <span>μg/m³</span>
        </label>
        <input id="pm25" v-model="values.pm25" inputmode="decimal" placeholder="输入细颗粒物浓度" />
        <div class="preview">
          <span>综合等级预览</span>
          <strong>{{ validValues ? gradeName(preview) : '等待完整数据' }}</strong>
        </div>
        <p v-if="error" class="error">{{ error }}</p>
        <button class="primary" :disabled="busy || !validValues">
          {{ busy ? '正在提交…' : '确认并提交实测' }}
        </button>
      </form>
      <section v-else class="card">
        <div class="eyebrow">CONFIRMED RESULT</div>
        <h2>实测结果 · {{ gradeName(detail.actualGrade) }}</h2>
        <dl>
          <dt>SO₂</dt>
          <dd>{{ detail.so2 }} μg/m³</dd>
          <dt>CO</dt>
          <dd>{{ detail.co }} mg/m³</dd>
          <dt>PM2.5</dt>
          <dd>{{ detail.pm25 }} μg/m³</dd>
          <dt>确认时间</dt>
          <dd>{{ dateTime(detail.confirmedAt) }}</dd>
        </dl>
        <p v-if="message" class="success">{{ message }}</p>
        <p v-if="error" class="error">{{ error }}</p>
      </section>
      <button class="secondary" :disabled="busy" @click="back">返回任务列表</button>
    </main>
  </div>
</template>
