import axios from 'axios'
import { reactive } from 'vue'
import { ElMessage } from 'element-plus'

export const session = reactive({ user: null, checked: false })
const client = axios.create({
  baseURL: '/api',
  timeout: 15000,
  withCredentials: true,
})
client.interceptors.response.use(
  (response) => response.data.data,
  (error) => {
    const message =
      error.response?.data?.message ||
      (error.code === 'ECONNABORTED' ? '请求超时，请稍后重试' : '无法连接服务，请检查后端是否启动')
    if (error.response?.status === 401) {
      session.user = null
      if (!error.config?.url?.startsWith('/auth/')) window.location.hash = '#/login'
    }
    if (!error.config?.silent) ElMessage.error(message)
    return Promise.reject(new Error(message))
  },
)
export const api = {
  get: (url, params, options = {}) => client.get(url, { params, ...options }),
  post: (url, data) => client.post(url, data),
  put: (url, data) => client.put(url, data),
}
export async function checkSession() {
  try {
    session.user = await api.get('/auth/me', null, { silent: true })
  } catch {
    session.user = null
  }
  session.checked = true
}
export const roleName = (role) =>
  ({ admin: '系统管理员', supervisor: '公众监督员', grid: 'AQI 网格员' })[role] || role
export const home = (role) => (role === 'admin' ? '/admin' : '/')
export const stateName = (value) => ['待指派', '待实测', '已完成'][value] || '未知'
export const gradeName = (value) =>
  ['', '优', '良', '轻度污染', '中度污染', '重度污染', '严重污染'][value] || '未实测'
export const gradeColor = (value) =>
  ['#94a3b8', '#22c55e', '#eab308', '#f97316', '#ef4444', '#a855f7', '#881337'][value] || '#94a3b8'
export const dateTime = (value) => (value ? value.replace('T', ' ').slice(0, 19) : '—')
export function previewLevel(value, levels, key) {
  if (value === null || value === undefined || value === '' || Number(value) < 0) return 0
  return levels.find((row) => row[key] == null || Number(value) <= Number(row[key]))?.id || 0
}
