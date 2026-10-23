let sequence = 0
const pending = new Map()
window.__nativeResult = (id, status, body) => {
  const item = pending.get(id)
  if (!item) return
  clearTimeout(item.timer)
  pending.delete(id)
  try {
    const value = JSON.parse(body)
    if (status >= 200 && status < 300 && value.code === 0) item.resolve(value.data)
    else {
      const error = new Error(value.message || '请求失败')
      error.status = status
      item.reject(error)
    }
  } catch {
    item.reject(new Error('服务器返回格式不正确'))
  }
}
export function request(path, method = 'GET', data) {
  if (!window.GridNative) return Promise.reject(new Error('请安装青境网格 APK，并在 App 内操作'))
  const id = ++sequence
  return new Promise((resolve, reject) => {
    const timer = setTimeout(() => {
      pending.delete(id)
      reject(new Error('请求超时，请检查服务地址与网络'))
    }, 20000)
    pending.set(id, { resolve, reject, timer })
    try {
      window.GridNative.request(id, method, path, data ? JSON.stringify(data) : '')
    } catch (e) {
      clearTimeout(timer)
      pending.delete(id)
      reject(new Error('无法调用 App 网络服务'))
    }
  })
}
export const stateName = (n) => ['待指派', '待实测', '已完成'][n] || '未知'
export const gradeName = (n) =>
  ['待填写', '优', '良', '轻度污染', '中度污染', '重度污染', '严重污染'][n || 0]
export const dateTime = (value) => (value ? String(value).replace('T', ' ').slice(0, 19) : '—')
export function previewLevel(value, levels, key) {
  if (
    value === '' ||
    value == null ||
    !/^\d+(\.\d{1,3})?$/.test(String(value)) ||
    Number(value) > 9999999.999
  )
    return 0
  return levels.find((level) => level[key] == null || Number(value) <= Number(level[key]))?.id || 0
}
