// Each client keeps its own session. Never share cookies with the administrator website.
let cookie = uni.getStorageSync('neps-cookie') || ''
export const baseUrl = () => uni.getStorageSync('neps-server') || 'http://localhost:8080'
export function clearSession() {
  cookie = ''
  uni.removeStorageSync('neps-cookie')
  uni.removeStorageSync('neps-user')
}
export function setServer(value) {
  const url = value.trim().replace(/\/+$/, '')
  if (!/^https?:\/\/[^/\s?#@]+(?::\d+)?$/.test(url))
    throw new Error('请输入服务器地址，例如 http://localhost:8080，不含 /api')
  clearSession()
  uni.setStorageSync('neps-server', url)
}
export function request(path, method = 'GET', data) {
  return new Promise((resolve, reject) =>
    uni.request({
      url: baseUrl() + '/api' + path,
      method,
      data,
      header: { 'content-type': 'application/json', ...(cookie ? { Cookie: cookie } : {}) },
      timeout: 15000,
      success(res) {
        const headers = res.header || {}
        const raw = res.cookies?.length
          ? res.cookies.join(';')
          : headers['Set-Cookie'] || headers['set-cookie'] || ''
        const match = raw.match(/(?:^|[;,]\s*)JSESSIONID=([^;,\s]*)/i)
        if (match) {
          cookie = 'JSESSIONID=' + match[1]
          uni.setStorageSync('neps-cookie', cookie)
        }
        if (res.statusCode >= 200 && res.statusCode < 300 && res.data?.code === 0)
          return resolve(res.data.data)
        if (res.statusCode === 401) {
          clearSession()
          if (path !== '/auth/login') uni.reLaunch({ url: '/pages/login/index' })
        }
        reject(new Error(res.data?.message || '请求失败，请稍后重试'))
      },
      fail() {
        reject(new Error('无法连接服务，请检查后端、服务地址及开发工具的域名校验设置'))
      },
    }),
  )
}
export const stateName = (state) => ['待指派', '待实测', '已完成'][state] || '未知'
export const gradeName = (grade) =>
  ['未实测', '优', '良', '轻度污染', '中度污染', '重度污染', '严重污染'][grade || 0]
export const dateTime = (value) => (value ? String(value).replace('T', ' ').slice(0, 19) : '—')
