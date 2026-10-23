// Exercise the installed APK through its real WebView and native HTTP bridge.
// Start an Android emulator and install the debuggable course APK first.
import { _android } from 'playwright-core'
import assert from 'node:assert/strict'
import { writeFileSync } from 'node:fs'
const api = 'http://localhost:8080/api'
async function session(role, account, password) {
  const result = await fetch(api + '/auth/login', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ role, account, password }),
  })
  assert.equal(result.status, 200)
  return result.headers
    .getSetCookie()
    .map((v) => v.split(';')[0])
    .join('; ')
}
async function call(cookie, path, method = 'GET', body) {
  const result = await fetch(api + path, {
    method,
    headers: { Cookie: cookie, 'Content-Type': 'application/json' },
    body: body ? JSON.stringify(body) : undefined,
  })
  assert.equal(result.status, 200, await result.clone().text())
  return (await result.json()).data
}
const admin = await session('admin', 'admin', 'Admin123456')
const publicCookie = await session('supervisor', '13800000001', 'User123456')
const member = (await call(admin, '/admin/grid-members')).find((item) => item.account === 'grid1')
assert.ok(member)
const feedback = await call(publicCookie, '/supervisor/feedbacks', 'POST', {
  provinceId: member.provinceId,
  cityId: member.cityId,
  address: '移动客户端联调演示点 ' + new Date().toISOString().slice(0, 10),
  information: '微信公众反馈与 Android 网格实测的联调验证记录。',
  estimatedGrade: 3,
})
const id = typeof feedback === 'object' ? feedback.id : feedback
assert.ok(id)
await call(admin, `/admin/feedbacks/${id}/assignment`, 'POST', { gridMemberId: member.id })
const devices = await _android.devices({ omitDriverInstall: true })
const device = devices.find((d) => d.serial() === (process.env.ANDROID_DEVICE || 'emulator-5556'))
assert.ok(device, 'Start the Android emulator and install the App first')
const webview = await device.webView({ pkg: 'edu.net.environment.grid' })
const page = await webview.page()
assert.ok(page)
const errors = []
page.on('pageerror', (e) => errors.push(e.message))
await page.reload()
await page.locator('#account').fill('grid1')
await page.locator('#password').fill('incorrect-password')
await page.getByRole('button', { name: '登录工作台', exact: true }).click()
await page.locator('.error').waitFor()
await page.locator('#password').fill('Grid123456')
await page.getByRole('button', { name: '登录工作台', exact: true }).click()
await page.getByRole('heading', { name: '我的核查任务' }).waitFor()
assert.equal(await page.evaluate(() => document.documentElement.scrollWidth <= innerWidth), true)
await page.screenshot({ path: 'docs/screenshots/nepg-app-tasks.png' })
await page
  .locator('.task-card')
  .filter({ hasText: '#' + id + ' ·' })
  .click()
await page.locator('#so2').fill('-1')
await page.locator('#co').fill('8')
await page.locator('#pm25').fill('90')
assert.equal(
  await page.getByRole('button', { name: '确认并提交实测', exact: true }).isDisabled(),
  true,
)
await page.locator('#so2').fill('20.5')
assert.ok((await page.locator('.preview').innerText()).includes('轻度污染'))
await page.screenshot({ path: 'docs/screenshots/nepg-app-measurement.png' })
await page.getByRole('button', { name: '确认并提交实测', exact: true }).click()
await page.getByRole('dialog', { name: '确认实测数据' }).waitFor()
await page.getByRole('button', { name: '确认提交', exact: true }).click()
await page
  .getByRole('heading', { name: '实测结果 · 轻度污染', exact: true })
  .waitFor({ timeout: 60000 })
assert.equal(await page.locator('#so2').count(), 0)
await page.getByRole('heading', { name: '实测结果 · 轻度污染', exact: true }).scrollIntoViewIfNeeded()
await page.screenshot({ path: 'docs/screenshots/nepg-app-result.png' })
const actual = await call(publicCookie, '/supervisor/feedbacks/' + id)
assert.equal(actual.state, 2)
assert.equal(actual.actualGrade, 3)
assert.equal(actual.so2, 20.5)
const screen = await call(admin, '/admin/statistics')
assert.ok(screen.summary.total > 0)
assert.deepEqual(errors, [])
await page.getByRole('button', { name: '返回任务列表', exact: true }).click()
await page.getByRole('button', { name: '退出', exact: true }).click()
await page.locator('#account').waitFor()
const report = {
  passed: true,
  checkedAt: new Date().toISOString(),
  platform: 'Android emulator, real APK WebView + Java HttpURLConnection',
  feedbackId: id,
  checks: [
    'Invalid credentials rejected',
    'Native login and session cookie maintained',
    'Assigned task list loads',
    'Negative measurement prevents submission',
    'Live grade preview',
    'App confirmation dialog',
    'Measurement saved and task completed',
    'Supervisor reads matching confirmed result',
    'Statistics updated',
    'Logout returns to login',
  ],
  errors,
}
writeFileSync('docs/android-verification.json', JSON.stringify(report, null, 2))
console.log(JSON.stringify(report, null, 2))
await device.close()
