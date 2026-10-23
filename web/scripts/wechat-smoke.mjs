// Requires WeChat DevTools opened on the compiled project with automation port 9421.
import automator from 'miniprogram-automator'
import assert from 'node:assert/strict'
import {writeFileSync} from 'node:fs'
const mini=await automator.connect({wsEndpoint:process.env.WECHAT_AUTOMATION_URL||'ws://127.0.0.1:9421'})
const errors=[],checks=[]
mini.on('exception',e=>errors.push(e.message||String(e)))
async function until(fn,message){for(let i=0;i<150;i++){const value=await fn();if(value)return value;await new Promise(resolve=>setTimeout(resolve,150))}throw new Error(message)}
async function current(path){return until(async()=>{const page=await mini.currentPage();return page?.path===path?page:null},'Page did not open: '+path)}
async function element(page,selector){return until(()=>page.$(selector),'Element missing: '+selector)}
async function listProperty(el,key){const value=await el.property(key);return typeof value==='string'?JSON.parse(value):value}
try{
  await mini.callWxMethod('setStorageSync','neps-server','http://localhost:8080')
  await mini.callWxMethod('removeStorageSync','neps-cookie')
  await mini.callWxMethod('removeStorageSync','neps-user')
  let page=await mini.reLaunch('/pages/login/index')
  await (await element(page,'.primary')).tap()
  assert.ok((await (await element(page,'.error')).text()).includes('请填写'))
  const inputs=await page.$$('input')
  await inputs[0].input('13800000001');await inputs[1].input('wrong-password')
  await (await page.$('.primary')).tap()
  await until(async()=>{const e=await page.$('.error');return e&&(await e.text()).includes('密码')},'Invalid password should fail')
  await inputs[1].input('User123456');await (await page.$('.primary')).tap()
  page=await current('pages/feedbacks/index')
  await until(async()=>!(await page.$('.empty'))||(await (await page.$('.empty')).text()).includes('暂无'),'Feedbacks did not load')
  await mini.screenshot({path:'docs/screenshots/neps-mini-feedbacks.png'})
  checks.push('Login validation, invalid password and authenticated feedback list')
  await mini.navigateTo('/pages/new/index');page=await current('pages/new/index')
  const pickers=await page.$$('picker')
  const provinces=await until(async()=>{const values=await listProperty(pickers[0],'range');return values?.length?values:null},'Provinces did not load')
  const province=provinces.findIndex(p=>p.name==='辽宁省');assert.ok(province>=0)
  await pickers[0].trigger('change',{value:province})
  const cities=await until(async()=>{const values=await listProperty(pickers[1],'range');return values?.length?values:null},'Cities did not load')
  const city=cities.findIndex(c=>c.name==='沈阳市');assert.ok(city>=0)
  await pickers[1].trigger('change',{value:city})
  const marker='微信小程序演示 '+Date.now()
  await (await page.$('input')).input(marker)
  await (await page.$('textarea')).input('小程序实际页面提交的空气质量核查演示反馈。')
  await pickers[2].trigger('change',{value:2})
  await mini.screenshot({path:'docs/screenshots/neps-mini-new.png'})
  await (await page.$('.primary')).tap()
  page=await current('pages/feedbacks/index')
  const card=await until(async()=>{for(const card of await page.$$('.card')){if((await card.text()).includes(marker))return card}return null},'New feedback did not appear')
  const id=Number((await card.text()).match(/#(\d+)/)?.[1]);assert.ok(id)
  await card.tap();page=await current('pages/detail/index')
  await element(page,'.heading')
  assert.ok((await (await element(page,'.page')).text()).includes('待指派'))
  checks.push('Province/city cascade, grade selection, real feedback submission and progress detail')
  const cookie=await mini.callWxMethod('getStorageSync','neps-cookie')
  assert.ok(String(cookie).startsWith('JSESSIONID='))
  const response=await fetch('http://localhost:8080/api/supervisor/feedbacks/'+id,{headers:{Cookie:cookie}})
  const actual=(await response.json()).data
  assert.equal(actual.address,marker);assert.equal(actual.state,0)
  await mini.screenshot({path:'docs/screenshots/neps-mini-detail.png'})
  assert.deepEqual(errors,[])
  const report={passed:true,checkedAt:new Date().toISOString(),platform:'WeChat DevTools, real compiled uni-app pages',feedbackId:id,checks,errors}
  writeFileSync('docs/wechat-verification.json',JSON.stringify(report,null,2))
  console.log(JSON.stringify(report,null,2))
}finally{mini.disconnect()}
