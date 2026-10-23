<script setup>
import { ref, watch } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { api } from '../api'
import RegionFields from './RegionFields.vue'
const props = defineProps({ type: String })
const rows = ref([]),
  keyword = ref(''),
  busy = ref(false),
  error = ref(''),
  dialog = ref(false),
  saving = ref(false),
  editingId = ref(null)
const form = ref({})
const base = () => (props.type === 'grid' ? '/admin/grid-members' : '/admin/supervisors')
async function load() {
  busy.value = true
  error.value = ''
  try {
    rows.value = await api.get(base(), { keyword: keyword.value })
  } catch (e) {
    error.value = e.message
  } finally {
    busy.value = false
  }
}
watch(
  () => props.type,
  () => {
    keyword.value = ''
    dialog.value = false
    load()
  },
  { immediate: true },
)
function edit(row) {
  editingId.value = row?.id || null
  form.value = row
    ? {
        ...row,
        enabled: !!row.enabled,
        birthday: row.birthday?.slice(0, 10) || null,
      }
    : {
        account: '',
        name: '',
        phone: '',
        provinceId: null,
        cityId: null,
        enabled: true,
      }
  dialog.value = true
}
async function save() {
  const r = form.value
  if (!r.name?.trim()) return ElMessage.warning('请输入姓名')
  if (
    props.type === 'grid' &&
    (!r.cityId || !/^[a-zA-Z0-9_]{3,32}$/.test(r.account) || !/^1[3-9]\d{9}$/.test(r.phone))
  )
    return ElMessage.warning('请正确填写账号、手机号和所属省市')
  if (saving.value) return
  saving.value = true
  try {
    if (editingId.value) await api.put(base() + '/' + editingId.value, r)
    else await api.post(base(), r)
    ElMessage.success('保存成功')
    dialog.value = false
    await load()
  } catch {
  } finally {
    saving.value = false
  }
}
async function resetPassword(row) {
  try {
    const { value } = await ElMessageBox.prompt(
      '设置6—32位新密码。请通过合适方式告知该用户。',
      '重置 ' + row.name + ' 的密码',
      {
        inputType: 'password',
        inputPattern: /^.{6,32}$/,
        inputErrorMessage: '密码长度应为6—32位',
        confirmButtonText: '确认重置',
        cancelButtonText: '取消',
      },
    )
    await api.put(base() + '/' + row.id + '/password', { password: value })
    ElMessage.success('密码已重置')
  } catch {}
}
</script>
<template>
  <section class="panel">
    <div class="table-toolbar">
      <el-input
        v-model="keyword"
        clearable
        :placeholder="type === 'grid' ? '搜索姓名或登录账号' : '搜索姓名或手机号'"
        @keyup.enter="load"
      />
      <el-button @click="load">查询</el-button>
      <el-button v-if="type === 'grid'" type="primary" @click="edit(null)">＋ 新增网格员</el-button>
      <span class="muted">共 {{ rows.length }} 人</span>
    </div>
    <el-alert v-if="error" :title="error" type="error" :closable="false" />
    <el-table :data="rows" v-loading="busy" stripe empty-text="暂无人员">
      <el-table-column prop="name" label="姓名" min-width="100" />
      <el-table-column
        :prop="type === 'grid' ? 'account' : 'id'"
        :label="type === 'grid' ? '登录账号' : '手机号'"
        min-width="130"
      />
      <el-table-column v-if="type === 'grid'" prop="phone" label="联系电话" min-width="130" />
      <el-table-column v-if="type === 'grid'" label="所属地区" min-width="170">
        <template #default="{ row }">{{ row.provinceName }} · {{ row.cityName }}</template>
      </el-table-column>
      <el-table-column v-if="type === 'grid'" prop="pendingCount" label="待实测" width="90" />
      <el-table-column label="账号状态" width="100">
        <template #default="{ row }">
          <el-tag :type="row.enabled ? 'success' : 'info'">
            {{ row.enabled ? '可用' : '已停用' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="180">
        <template #default="{ row }">
          <el-button link type="primary" @click="edit(row)">编辑</el-button>
          <el-button link @click="resetPassword(row)">重置密码</el-button>
        </template>
      </el-table-column>
    </el-table>
    <el-dialog
      v-model="dialog"
      :title="editingId ? '编辑人员信息' : '新增网格员'"
      width="min(520px,94vw)"
      destroy-on-close
    >
      <form @submit.prevent="save" class="stack-form">
        <label v-if="type === 'grid'">
          登录账号
          <el-input v-model="form.account" maxlength="32" placeholder="3—32位字母、数字或下划线" />
        </label>
        <label>
          姓名
          <el-input v-model="form.name" maxlength="40" />
        </label>
        <template v-if="type === 'grid'">
          <label>
            联系电话
            <el-input v-model="form.phone" maxlength="11" />
          </label>
          <label>
            所属地区
            <div class="region-row">
              <RegionFields v-model="form" />
            </div>
          </label>
        </template>
        <template v-else>
          <label>
            手机号
            <el-input :model-value="editingId" disabled />
          </label>
          <label>
            出生日期
            <el-date-picker
              v-model="form.birthday"
              type="date"
              value-format="YYYY-MM-DD"
              clearable
              :disabled-date="(date) => date >= new Date()"
            />
          </label>
          <label>
            性别
            <el-radio-group v-model="form.sex">
              <el-radio :value="1">男</el-radio>
              <el-radio :value="0">女</el-radio>
            </el-radio-group>
          </label>
        </template>
        <label>
          账号状态
          <el-switch v-model="form.enabled" active-text="启用" inactive-text="停用" />
        </label>
        <el-alert
          v-if="type === 'grid' && !editingId"
          title="新网格员初始密码：Grid123456"
          type="info"
          :closable="false"
        />
        <el-button type="primary" native-type="submit" :loading="saving">保存</el-button>
      </form>
    </el-dialog>
  </section>
</template>
