<script setup>
import { ref, watch } from 'vue'
import { ElMessage } from 'element-plus'
import { api, stateName, dateTime } from '../api'
import GradeBadge from './GradeBadge.vue'
const props = defineProps({ id: [Number, String], role: String })
const emit = defineEmits(['close', 'changed'])
const detail = ref(null),
  loading = ref(false),
  error = ref(''),
  busy = ref(false)
const candidates = ref([]),
  gridMemberId = ref(null)
async function load() {
  if (!props.id) return
  loading.value = true
  error.value = ''
  detail.value = null
  gridMemberId.value = null
  try {
    detail.value = await api.get('/admin/feedbacks/' + props.id)
    if (props.role === 'admin' && detail.value.state === 0) {
      candidates.value = (
        await api.get('/admin/grid-members', { cityId: detail.value.cityId })
      ).filter((g) => g.enabled)
    }
  } catch (e) {
    error.value = e.message
  } finally {
    loading.value = false
  }
}
watch(() => props.id, load, { immediate: true })
async function assign() {
  if (!gridMemberId.value || busy.value) return
  busy.value = true
  try {
    await api.post('/admin/feedbacks/' + props.id + '/assignment', {
      gridMemberId: gridMemberId.value,
    })
    ElMessage.success('已指派网格员')
    emit('changed')
    await load()
  } catch {
  } finally {
    busy.value = false
  }
}
</script>
<template>
  <el-dialog
    :model-value="!!id"
    title="反馈详情"
    width="min(640px, 94vw)"
    destroy-on-close
    @close="emit('close')"
  >
    <div v-loading="loading" class="detail-body">
      <el-alert v-if="error" :title="error" type="error" :closable="false" />
      <el-button v-if="error" @click="load">重试</el-button>
      <template v-if="detail">
        <div class="detail-title">
          <span>反馈 #{{ detail.id }}</span>
          <el-tag :type="detail.state === 2 ? 'success' : 'warning'">
            {{ stateName(detail.state) }}
          </el-tag>
        </div>
        <div class="detail-grid">
          <label>
            反馈人
            <strong>{{ detail.supervisorName }}</strong>
          </label>
          <label>
            反馈地区
            <strong>{{ detail.provinceName }} · {{ detail.cityName }}</strong>
          </label>
          <label class="wide">
            详细地址
            <strong>{{ detail.address }}</strong>
          </label>
          <label class="wide">
            反馈描述
            <strong>{{ detail.information }}</strong>
          </label>
          <label>
            预估等级
            <GradeBadge :value="detail.estimatedGrade" />
          </label>
          <label>
            负责网格员
            <strong>{{ detail.gridMemberName || '尚未指派' }}</strong>
          </label>
        </div>
        <div class="process-times">
          <span>上报 {{ dateTime(detail.submittedAt) }}</span>
          <span v-if="detail.assignedAt">指派 {{ dateTime(detail.assignedAt) }}</span>
          <span v-if="detail.confirmedAt">确认 {{ dateTime(detail.confirmedAt) }}</span>
        </div>
        <section v-if="role === 'admin' && detail.state === 0" class="action-section">
          <h3>指派核查任务</h3>
          <p class="muted">选择本城市可用的网格员</p>
          <el-select v-model="gridMemberId" placeholder="选择网格员" class="full-width">
            <el-option
              v-for="g in candidates"
              :key="g.id"
              :value="g.id"
              :label="g.name + ' · 待实测 ' + g.pendingCount + ' 项'"
            />
          </el-select>
          <el-alert
            v-if="!candidates.length"
            title="该城市暂无可用网格员，请先在人员管理中新增。"
            type="warning"
            :closable="false"
          />
          <el-button type="primary" :disabled="!gridMemberId" :loading="busy" @click="assign">
            确认指派
          </el-button>
        </section>
        <section v-if="detail.state === 2" class="action-section">
          <h3>现场核查结果</h3>
          <div class="result-values">
            <div>
              <small>SO₂ · μg/m³</small>
              <strong>{{ detail.so2 }}</strong>
              <GradeBadge :value="detail.so2Level" />
            </div>
            <div>
              <small>CO · mg/m³</small>
              <strong>{{ detail.co }}</strong>
              <GradeBadge :value="detail.coLevel" />
            </div>
            <div>
              <small>PM2.5 · μg/m³</small>
              <strong>{{ detail.pm25 }}</strong>
              <GradeBadge :value="detail.pm25Level" />
            </div>
          </div>
          <div class="preview-grade">
            最终空气等级
            <GradeBadge :value="detail.actualGrade" />
          </div>
        </section>
      </template>
    </div>
  </el-dialog>
</template>
