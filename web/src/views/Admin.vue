<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import {
  ChatDotSquare,
  DataAnalysis,
  User,
  Location,
  Document,
  ArrowRight,
} from '@element-plus/icons-vue'
import { api, stateName, dateTime, roleName } from '../api'
import GradeBadge from '../components/GradeBadge.vue'
import FeedbackDetail from '../components/FeedbackDetail.vue'
import RegionFields from '../components/RegionFields.vue'
import PeopleManagement from '../components/PeopleManagement.vue'
import StatsDashboard from '../components/StatsDashboard.vue'
const route = useRoute(),
  router = useRouter()
const tabs = [
  {
    id: 'feedback',
    title: '公众反馈管理',
    icon: ChatDotSquare,
    description: '查看公众反馈，安排网格员现场核查。',
  },
  {
    id: 'measurements',
    title: '实测数据管理',
    icon: Document,
    description: '查阅已经确认的空气质量检测结果。',
  },
  {
    id: 'grid',
    title: '网格员管理',
    icon: Location,
    description: '维护人员账号、负责区域与可用状态。',
  },
  {
    id: 'supervisor',
    title: '监督员管理',
    icon: User,
    description: '管理公众监督员的基本信息与账号状态。',
  },
  {
    id: 'statistics',
    title: '统计分析',
    icon: DataAnalysis,
    description: '根据实测结果分析空气质量与核查进度。',
  },
  {
    id: 'logs',
    title: '操作日志',
    icon: Document,
    description: '追踪系统内所有成功的数据写入操作。',
  },
]
const active = ref(tabs.some((t) => t.id === route.query.tab) ? route.query.tab : 'feedback')
const current = computed(() => tabs.find((t) => t.id === active.value))
const filters = ref({
  provinceId: null,
  cityId: null,
  state: '',
  keyword: '',
  start: '',
  end: '',
  page: 1,
  size: 10,
})
const result = ref({ records: [], total: 0 }),
  summary = ref({}),
  loading = ref(false),
  error = ref(''),
  selected = ref(null)
const tableTab = computed(() => ['feedback', 'measurements', 'logs'].includes(active.value))
async function load() {
  if (!tableTab.value) return
  loading.value = true
  error.value = ''
  try {
    const params = Object.fromEntries(
      Object.entries(filters.value).filter(([, v]) => v !== null && v !== ''),
    )
    if (active.value === 'measurements') params.state = 2
    result.value = await api.get(
      active.value === 'logs' ? '/admin/operation-logs' : '/admin/feedbacks',
      params,
    )
    summary.value = (await api.get('/admin/statistics')).summary
  } catch (e) {
    error.value = e.message
  } finally {
    loading.value = false
  }
}
function changeTab(id) {
  active.value = id
  router.replace({ query: { tab: id } })
  filters.value.page = 1
  filters.value.state = ''
  load()
}
function search() {
  filters.value.page = 1
  load()
}
function reset() {
  filters.value = {
    provinceId: null,
    cityId: null,
    state: '',
    keyword: '',
    start: '',
    end: '',
    page: 1,
    size: 10,
  }
  load()
}
onMounted(load)
</script>
<template>
  <div class="admin-shell">
    <aside class="admin-sidebar">
      <div class="sidebar-heading">
        管理工作台
        <small>MANAGEMENT</small>
      </div>
      <nav>
        <button
          v-for="tab in tabs"
          :key="tab.id"
          :class="{ active: active === tab.id }"
          @click="changeTab(tab.id)"
        >
          <el-icon><component :is="tab.icon" /></el-icon>
          {{ tab.title }}
        </button>
      </nav>
      <router-link to="/screen" target="_blank" class="sidebar-screen">
        <el-icon><DataAnalysis /></el-icon>
        <span>打开可视化大屏</span>
        <el-icon><ArrowRight /></el-icon>
      </router-link>
      <div class="sidebar-bottom">
        <span class="status-dot"></span>
        公众参与 · 协同守护
      </div>
    </aside>
    <main class="admin-main">
      <div class="breadcrumb">
        管理中心
        <span>/</span>
        {{ current.title }}
      </div>
      <div class="page-heading">
        <div>
          <div class="eyebrow">ENVIRONMENT MANAGEMENT</div>
          <h1>{{ current.title }}</h1>
          <p>{{ current.description }}</p>
        </div>
        <div class="today-label">
          {{ new Date().toLocaleDateString('zh-CN') }}
        </div>
      </div>
      <template v-if="active === 'feedback'">
        <div class="admin-metrics">
          <div>
            <span>累计反馈</span>
            <strong>{{ summary.feedbackTotal || 0 }}</strong>
          </div>
          <div>
            <span>待指派</span>
            <strong class="amber-text">{{ summary.pending || 0 }}</strong>
          </div>
          <div>
            <span>待实测</span>
            <strong>{{ summary.assigned || 0 }}</strong>
          </div>
          <div>
            <span>已完成</span>
            <strong class="green-text">{{ summary.total || 0 }}</strong>
          </div>
        </div>
      </template>
      <section v-if="tableTab" class="panel">
        <form v-if="active !== 'logs'" @submit.prevent="search" class="filter-bar">
          <RegionFields v-model="filters" optional />
          <el-select
            v-if="active !== 'measurements'"
            v-model="filters.state"
            placeholder="全部状态"
            clearable
          >
            <el-option
              v-for="value in [0, 1, 2]"
              :key="value"
              :value="String(value)"
              :label="stateName(value)"
            />
          </el-select>
          <el-date-picker
            v-model="filters.start"
            type="date"
            value-format="YYYY-MM-DD"
            placeholder="开始日期"
          />
          <el-date-picker
            v-model="filters.end"
            type="date"
            value-format="YYYY-MM-DD"
            placeholder="结束日期"
          />
          <el-input v-model="filters.keyword" placeholder="搜索地址或描述" clearable />
          <el-button type="primary" native-type="submit">查询</el-button>
          <el-button @click="reset">重置</el-button>
        </form>
        <div v-else class="table-toolbar">
          <span class="muted">记录注册、上报、指派、实测与人员维护操作</span>
          <el-button @click="load">刷新</el-button>
        </div>
        <el-alert v-if="error" :title="error" type="error" :closable="false" />
        <el-button v-if="error" @click="load">重新加载</el-button>
        <el-table
          v-if="active !== 'logs'"
          :data="result.records"
          v-loading="loading"
          stripe
          empty-text="暂无符合条件的反馈"
        >
          <el-table-column prop="id" label="编号" width="75" />
          <el-table-column label="反馈地区" min-width="160">
            <template #default="{ row }">{{ row.provinceName }} · {{ row.cityName }}</template>
          </el-table-column>
          <el-table-column prop="address" label="详细地址" min-width="170" show-overflow-tooltip />
          <el-table-column label="预估等级" min-width="115">
            <template #default="{ row }"><GradeBadge :value="row.estimatedGrade" /></template>
          </el-table-column>
          <el-table-column v-if="active === 'measurements'" label="实测等级" min-width="115">
            <template #default="{ row }"><GradeBadge :value="row.actualGrade" /></template>
          </el-table-column>
          <el-table-column prop="supervisorName" label="反馈人" min-width="110" />
          <el-table-column prop="gridMemberName" label="网格员" min-width="110" />
          <el-table-column label="状态" min-width="95">
            <template #default="{ row }">
              <el-tag :type="row.state === 2 ? 'success' : row.state === 1 ? 'primary' : 'warning'">
                {{ stateName(row.state) }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column label="上报时间" min-width="165">
            <template #default="{ row }">{{ dateTime(row.submittedAt) }}</template>
          </el-table-column>
          <el-table-column label="操作" width="100" fixed="right">
            <template #default="{ row }">
              <el-button link type="primary" @click="selected = row.id">
                {{ row.state === 0 ? '详情 / 指派' : '查看详情' }}
              </el-button>
            </template>
          </el-table-column>
        </el-table>
        <el-table
          v-else
          :data="result.records"
          v-loading="loading"
          stripe
          empty-text="暂无操作日志"
        >
          <el-table-column prop="id" label="编号" width="80" />
          <el-table-column prop="actorName" label="操作者" min-width="120" />
          <el-table-column label="角色" width="120">
            <template #default="{ row }">
              {{ row.role === 'system' ? '系统初始化' : roleName(row.role) }}
            </template>
          </el-table-column>
          <el-table-column prop="action" label="操作" min-width="160" />
          <el-table-column prop="targetId" label="记录编号" width="120" />
          <el-table-column label="结果" width="100">
            <template #default="{ row }">
              {{ row.result === 'SUCCESS' ? '成功' : row.result }}
            </template>
          </el-table-column>
          <el-table-column label="操作时间" min-width="170">
            <template #default="{ row }">{{ dateTime(row.createdAt) }}</template>
          </el-table-column>
        </el-table>
        <div class="table-pagination">
          <span class="muted">共 {{ result.total }} 条记录</span>
          <el-pagination
            v-model:current-page="filters.page"
            :page-size="filters.size"
            :total="result.total"
            layout="prev, pager, next"
            @current-change="load"
          />
        </div>
      </section>
      <PeopleManagement v-if="active === 'grid' || active === 'supervisor'" :type="active" />
      <StatsDashboard v-if="active === 'statistics'" />
      <FeedbackDetail :id="selected" role="admin" @close="selected = null" @changed="load" />
    </main>
  </div>
</template>
