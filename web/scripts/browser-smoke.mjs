import { chromium } from 'playwright-core'
import assert from 'node:assert/strict'
import { existsSync, mkdirSync, writeFileSync } from 'node:fs'
import { fileURLToPath } from 'node:url'
import path from 'node:path'
const base = process.env.APP_URL || 'http://localhost:8080'
const executablePath =
  process.env.CHROME_PATH ||
  [
    'C:/Program Files/Google/Chrome/Application/chrome.exe',
    'C:/Program Files (x86)/Microsoft/Edge/Application/msedge.exe',
  ].find(existsSync)
if (!executablePath) throw new Error('Set CHROME_PATH to an installed Chromium executable')
const project = fileURLToPath(new URL('../../', import.meta.url))
const screenshots = path.join(project, 'docs/screenshots')
mkdirSync(screenshots, { recursive: true })
const browser = await chromium.launch({ executablePath, headless: true })
const errors = [],
  checks = []
try {
  const context = await browser.newContext({
    viewport: { width: 1440, height: 1000 },
    locale: 'zh-CN',
  })
  const page = await context.newPage()
  page.on('pageerror', (e) => errors.push(e.message))
  page.on('response', (r) => {
    if (r.url().startsWith(base + '/api/') && r.status() >= 500)
      errors.push(r.status() + ' ' + r.url())
  })
  await page.goto(base)
  await page.locator('.entry-card').first().waitFor()
  assert.equal(await page.locator('.entry-card').count(), 2)
  assert.ok((await page.locator('.entry-grid').innerText()).includes('系统管理'))
  assert.ok((await page.locator('.entry-grid').innerText()).includes('可视化大屏'))
  await page.screenshot({ path: path.join(screenshots, 'pc-home.png'), fullPage: true })
  for (const old of ['/supervisor', '/supervisor/new', '/grid', '/register']) {
    await page.goto(base + '/#' + old)
    await page.waitForURL('**/#/')
    assert.equal(await page.locator('.entry-card').count(), 2)
  }
  checks.push('PC exposes only management and visualization; removed mobile routes redirect home')
  await page.goto(base + '/#/login?role=grid')
  assert.equal(await page.locator('.el-segmented').count(), 0)
  await page.getByRole('textbox', { name: '登录账号' }).fill('grid1')
  await page.getByRole('textbox', { name: '登录密码' }).fill('Grid123456')
  await page.locator('button[type=submit]').click()
  await page.locator('.el-message--error').waitFor()
  assert.ok(page.url().includes('/login'))
  await page.getByRole('textbox', { name: '登录账号' }).fill('admin')
  await page.getByRole('textbox', { name: '登录密码' }).fill('Admin123456')
  await page.locator('button[type=submit]').click()
  await page.waitForURL('**/#/admin')
  await page.locator('.el-table__row').first().waitFor()
  await page.screenshot({ path: path.join(screenshots, 'pc-admin.png'), fullPage: true })
  checks.push('PC login forces administrator role even with old role query parameters')
  await page.locator('.filter-bar .el-select').nth(0).click()
  await page.getByRole('option', { name: '辽宁省', exact: true }).click()
  await page.locator('.filter-bar .el-select').nth(1).click()
  await page.getByRole('option', { name: '大连市', exact: true }).click()
  const filtered = page.waitForResponse(
    (r) => r.url().includes('/api/admin/feedbacks?') && r.url().includes('cityId=17'),
  )
  await page.getByRole('button', { name: '查询', exact: true }).click()
  assert.equal((await filtered).status(), 200)
  await page.locator('.el-table__row').first().waitFor()
  assert.ok((await page.locator('.el-table__body').innerText()).includes('大连市'))
  for (const [title, marker] of [
    ['网格员管理', '沈阳网格员'],
    ['监督员管理', '演示监督员'],
    ['操作日志', '初始化演示数据'],
  ]) {
    await page.getByRole('button', { name: title, exact: true }).click()
    await page.locator('.el-table__row').first().waitFor()
    assert.ok((await page.locator('.el-table__body').innerText()).includes(marker))
  }
  await page.getByRole('button', { name: '统计分析', exact: true }).click()
  await page.waitForFunction(() => document.querySelectorAll('canvas').length >= 6)
  checks.push(
    'Administrator filtering, personnel, logs and six statistics charts remain functional',
  )
  const screen = await context.newPage()
  await screen.goto(base + '/#/screen')
  await screen.waitForFunction(() => document.querySelectorAll('canvas').length >= 6)
  await screen.screenshot({ path: path.join(screenshots, 'pc-screen.png'), fullPage: true })
  const updated = await screen.locator('.stats-toolbar').innerText()
  await screen.waitForTimeout(11000)
  assert.notEqual(await screen.locator('.stats-toolbar').innerText(), updated)
  checks.push('Read-only screen renders and refreshes after ten seconds')
  assert.deepEqual(errors, [])
  const report = { passed: true, checkedAt: new Date().toISOString(), base, checks, errors }
  writeFileSync(
    path.join(project, 'docs/browser-verification.json'),
    JSON.stringify(report, null, 2),
  )
  console.log(JSON.stringify(report, null, 2))
} finally {
  await browser.close()
}
