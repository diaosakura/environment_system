# 接口说明

接口前缀 /api，JSON编码UTF-8。登录使用JSESSIONID Cookie；浏览器自动携带，命令行测试需要保存Cookie。前端与后端同源或通过开发代理访问。

2026-10-20客户端调整：PC仅有管理员登录和大屏；公众接口由微信小程序调用，网格员接口由Android App调用。小程序请求封装自行保存并携带JSESSIONID，Android原生HTTP桥独立维护Cookie；各客户端不共用会话。API路径、权限和数据库结构保持兼容，移动部署见MOBILE.md。

## 统一响应

```json
{"code":0,"message":"操作成功","data":{}}
```

成功HTTP200且code=0。错误使用HTTP400参数错误、401未登录/登录失效、403无权限、404不存在、409状态冲突或重复记录、500服务异常。错误响应data为null，不返回数据库异常或密码。

分页data为：

```json
{"records":[],"total":0,"page":1,"size":10}
```

page从1开始，size默认10、最大100。列表日期格式为YYYY-MM-DD HH:mm:ss或ISO日期时间，前端统一格式化。

## 认证与字典

| 方法 | 路径 | 参数 | 权限 |
| --- | --- | --- | --- |
| GET | /health | 无 | 公开；实际执行SELECT 1 |
| POST | /auth/register | phone,name,birthday可空,sex,password | 公开，监督员自助注册 |
| POST | /auth/login | role,account,password | 公开；role为admin/grid/supervisor |
| GET | /auth/me | 无 | 已登录 |
| POST | /auth/logout | 无 | 已登录 |
| GET | /dictionaries/provinces | 无 | 已登录 |
| GET | /dictionaries/cities | provinceId | 已登录 |
| GET | /dictionaries/aqi-levels | 无 | 已登录；含阈值与颜色 |

登录：

```json
{"role":"supervisor","account":"13800000001","password":"User123456"}
```

返回用户摘要id/name/role。注册手机号为中国大陆格式的11位数字，姓名最长40字，密码6—32字符且UTF-8不超过72字节。

## 反馈与任务

| 方法 | 路径 | 作用 |
| --- | --- | --- |
| POST | /supervisor/feedbacks | 提交本人反馈 |
| GET | /supervisor/feedbacks | 本人反馈分页 |
| GET | /supervisor/feedbacks/{id} | 本人反馈详情与结果 |
| GET | /grid/tasks | 本人任务分页 |
| GET | /grid/tasks/{id} | 本人任务详情 |
| POST | /grid/tasks/{id}/measurement | 提交本人任务实测 |
| GET | /admin/feedbacks | 管理员查询全部反馈 |
| GET | /admin/feedbacks/{id} | 管理员查看详情 |
| POST | /admin/feedbacks/{id}/assignment | 管理员指派 |

反馈请求：

```json
{"provinceId":6,"cityId":6,"address":"演示街道1号","information":"附近有扬尘，请核查","estimatedGrade":3}
```

地址最长200字符、描述最长400字符，均不能为空；estimatedGrade为1—6。省市必须对应。用户编号、状态、时间由后端生成。

列表支持page/size/state/provinceId/cityId/start/end/keyword；start、end为YYYY-MM-DD，包含结束日期整天。关键词匹配地址或反馈描述。公众与网格员的数据范围由后端固定，不能通过参数扩大。

指派：

```json
{"gridMemberId":1}
```

只允许待指派反馈，目标人员必须可用且位于同一城市。

实测：

```json
{"so2":20.5,"co":8,"pm25":90}
```

三项必填、非负、最多3位小数，最大9999999.999。单位依次为μg/m³、mg/m³、μg/m³。后端重算等级，不接受客户端给定最终等级或用户身份。返回id和grade；重复提交返回409。

## 人员管理

| 方法 | 路径 | 作用 |
| --- | --- | --- |
| GET | /admin/grid-members | 查询网格员；可选cityId、keyword |
| POST | /admin/grid-members | 新增网格员 |
| PUT | /admin/grid-members/{id} | 修改网格员 |
| PUT | /admin/grid-members/{id}/password | 重置密码 |
| GET | /admin/supervisors | 查询监督员；可选keyword |
| PUT | /admin/supervisors/{id} | 修改监督员 |
| PUT | /admin/supervisors/{id}/password | 重置密码 |

网格员新增/编辑：

```json
{"account":"grid5","name":"演示网格员","phone":"13900000005","provinceId":6,"cityId":6,"enabled":true}
```

account为3—32位字母/数字/下划线；新增初始密码Grid123456。存在待实测任务时禁止停用或换城市。

监督员编辑：

```json
{"name":"演示监督员甲","birthday":"2002-05-08","sex":1,"enabled":true}
```

监督员手机号为固定主键，不在编辑接口更改。重置密码body为 `{"password":"New123456"}`。

## 统计与日志

| 方法 | 路径 | 作用 |
| --- | --- | --- |
| GET | /admin/statistics | 管理员统计 |
| GET | /screen/statistics | 公开大屏聚合统计，无人员信息 |
| GET | /admin/operation-logs | 日志分页，page/size |

统计data含summary、distribution、trend、regions、updatedAt。summary含实测总量、优良、污染、待指派、待实测、反馈总量、已覆盖和字典省市数量。trend始终含最近12个月，空月份补零。regions含各省的实测数、综合污染数及三指标分别达到三级的数量。

公开大屏接口只返回聚合结果，不包含手机号、姓名或详细地址。
