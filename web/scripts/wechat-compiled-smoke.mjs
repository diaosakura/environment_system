// Validate emitted mini-program page events against the real backend when DevTools
// is waiting for interactive login. This does NOT replace WeChat simulator QA.
import {ref,reactive,computed,unref} from 'vue'
import vm from 'node:vm'
import path from 'node:path'
import {readFileSync,writeFileSync} from 'node:fs'
import assert from 'node:assert/strict'
const root=path.resolve('clients/neps-miniprogram/dist/build/mp-weixin')
const storage=new Map([['neps-server','http://localhost:8080']])
let route='',hooks={},component
const uni={
  getStorageSync:key=>storage.get(key),setStorageSync:(key,value)=>storage.set(key,value),removeStorageSync:key=>storage.delete(key),
  reLaunch:({url})=>{route=url},navigateTo:({url})=>{route=url},navigateBack:()=>{route='back'},showToast:()=>{},stopPullDownRefresh:()=>{},
  request(options){(async()=>{
    try{
      const url=new URL(options.url)
      if(options.method==='GET'&&options.data)for(const [key,value]of Object.entries(options.data))url.searchParams.set(key,value)
      const response=await fetch(url,{method:options.method,headers:options.header,body:options.method==='GET'?undefined:JSON.stringify(options.data||{}),signal:AbortSignal.timeout(15000)})
      options.success({statusCode:response.status,data:await response.json(),header:Object.fromEntries(response.headers),cookies:response.headers.getSetCookie()})
    }catch(e){options.fail(e)}
  })()}
}
const vendor={ref,reactive,computed,unref,index:uni,onLoad:fn=>{hooks.load=fn},onShow:fn=>{hooks.show=fn},onPullDownRefresh:fn=>{hooks.pull=fn},onReachBottom:fn=>{hooks.bottom=fn},e:(...values)=>Object.assign({},...values),o:fn=>fn,t:value=>String(value??''),n:value=>value,f:(values,fn)=>values.map(fn)}
const cache=new Map()
function load(file){
  file=path.resolve(file)
  if(file.endsWith(path.join('common','vendor.js')))return vendor
  if(!file.startsWith(root+path.sep))throw new Error('Unexpected module '+file)
  if(cache.has(file))return cache.get(file)
  const module={exports:{}};cache.set(file,module.exports)
  vm.runInNewContext(readFileSync(file,'utf8'),{module,exports:module.exports,require:name=>load(path.resolve(path.dirname(file),name)),wx:{createPage:value=>{component=value}},uni,setTimeout,clearTimeout,console},{filename:file})
  return module.exports
}
async function page(name){hooks={};component=null;load(path.join(root,`pages/${name}/index.js`));const render=component.setup({});const ownHooks=hooks;if(ownHooks.load)await ownHooks.load({});if(ownHooks.show)await ownHooks.show();const wxml=readFileSync(path.join(root,`pages/${name}/index.wxml`),'utf8');return {render:()=>render({},{}),hooks:ownHooks,wxml}}
function tags(page,name){return page.wxml.match(new RegExp('<'+name+'\\b[^>]*>','g'))||[]}
function bound(tag,attribute){const result=tag.match(new RegExp(attribute+'="\\{\\{([^}]+)\\}\\}"'));assert.ok(result,'Missing event '+attribute);return result[1]}
function event(page,tag,event,value){return page.render()[bound(tag,'bind'+event)]({detail:{value}})}
const login=await page('login')
const loginInputs=tags(login,'input'),loginButton=tags(login,'button')[0]
await event(login,loginButton,'tap');assert.match(JSON.stringify(login.render()),/请填写/)
event(login,loginInputs[0],'input','13800000001');event(login,loginInputs[1],'input','invalid-password')
await event(login,loginButton,'tap');assert.match(JSON.stringify(login.render()),/密码|账号/)
event(login,loginInputs[1],'input','User123456');await event(login,loginButton,'tap')
assert.equal(route,'/pages/feedbacks/index');assert.match(storage.get('neps-cookie'),/^JSESSIONID=/)
const form=await page('new'),pickers=tags(form,'picker')
const provinces=form.render()[bound(pickers[0],'range')]
const province=provinces.findIndex(p=>p.name==='辽宁省');assert.ok(province>=0)
await event(form,pickers[0],'change',province)
const cities=form.render()[bound(pickers[1],'range')]
const city=cities.findIndex(c=>c.name==='沈阳市');assert.ok(city>=0)
event(form,pickers[1],'change',city)
const marker='小程序编译产物联调 '+Date.now()
event(form,tags(form,'input')[0],'input',marker)
event(form,tags(form,'textarea')[0],'input','通过编译后页面真实事件和后端验证的虚构反馈。')
event(form,pickers[2],'change',2)
await event(form,tags(form,'button')[0],'tap')
assert.doesNotMatch(JSON.stringify(form.render()),/无法连接|参数错误/)
const result=await fetch('http://localhost:8080/api/supervisor/feedbacks',{headers:{Cookie:storage.get('neps-cookie')}})
const data=(await result.json()).data
const created=data.records.find(item=>item.address===marker)
assert.ok(created,'Feedback must be persisted through the compiled page and request wrapper')
assert.equal(created.state,0);assert.equal(created.estimatedGrade,3)
const report={passed:true,checkedAt:new Date().toISOString(),platform:'Compiled WeChat page JavaScript + WXML event bindings, mocked platform transport, real Spring Boot/MySQL',wechatSimulatorVerified:false,feedbackId:created.id,checks:['Empty-field validation','Invalid credentials rejected','JSESSIONID parsed and forwarded','Province and city change events','Grade selection event','Feedback input and real submission','Persisted feedback has matching fields and pending state']}
writeFileSync('docs/wechat-compiled-verification.json',JSON.stringify(report,null,2))
console.log(JSON.stringify(report,null,2))
