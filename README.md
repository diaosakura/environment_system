# 青境 · 环保公众监督系统

**新设备请先阅读 [START_HERE.html](START_HERE.html) 或 [START_HERE.md](START_HERE.md)。** 指南包含软件安装、离线环境、初始化/恢复数据库、四端操作、源码构建及常见问题。更新日期：2026年10月20日。

## 本次交付形态

| 模块 | 实现 | 业务 |
| --- | --- | --- |
| NEPS公众监督端 | Vue / uni-app微信小程序 | 注册登录、公众上报、本人反馈和实测结果查询 |
| NEPG网格员端 | Vue移动界面＋Android原生容器，APK内置界面资源 | 接收任务、录入浓度、确认等级、查询结果 |
| NEPM管理端 | PC Vue网页 | 反馈指派、人员管理、统计、操作日志 |
| NEPV大屏端 | PC只读网页，约10秒刷新 | 展示聚合统计，无个人明细 |

所有客户端共用Spring Boot＋MySQL后端。PC网页已移除公众端、网格员端和注册页。流程为：小程序反馈 → 管理员指派 → App实测 → 小程序结果查询 → 管理统计及大屏刷新。

## 移植包

- `environment-system-20261020-full-source.zip`：完整源码＋JAR＋APK＋编译后微信工程＋本指南。
- `environment-system-20261020-windows-x64-env.zip`：可选Java/MySQL离线环境及微信工具安装文件，不含项目源码或Android SDK。
- `environment-system-20261020-current-data.sql`：打包时的业务数据快照，需要保留旧记录时携带。
- `SHA256SUMS.txt`：交付文件校验；源码包内另有`release/CONTENTS-SHA256SUMS.txt`及验证脚本。

源码包与业务备份分开，启动脚本不自动恢复或清空数据库。旧1.0.0/1.1.0归档是历史包，新设备使用本次日期命名的完整包。

## 软件与成品

后端使用Java17（验证17.0.9）、Spring Boot3.3.5、MyBatis-Plus3.5.7、MySQL8.4.11、Maven Wrapper3.8.6。PC和App使用Vue3.5.22/Vite7.1.7；小程序依赖固定在其package-lock.json中。

运行成品不需要Node或全局Maven。修改前端需Node22.12+，建议24.x；重建APK另需Android Platform34和Build Tools34.0.0。Android最低8.0，当前为课程演示签名及可调试构建，未提供iOS。

成品文件：

```text
target/environment_system-1.0.0.jar
release/qingjing-nepg-1.1.0.apk
release/qingjing-neps-1.1.0-weixin.zip
```

JAR内部版本与移动客户端版本不同；本次交付包以20261020识别。

## 已初始化后的日常启动

在项目根目录运行：

```powershell
powershell.exe -NoProfile -ExecutionPolicy Bypass -File ./scripts/start-mysql.ps1
powershell.exe -NoProfile -ExecutionPolicy Bypass -File ./scripts/start.ps1
```

首次新设备必须先按START_HERE准备运行环境，并选择“初始化演示数据”或“恢复SQL备份”，两者不重复执行。

PC地址：[首页](http://localhost:8080/)、[管理端](http://localhost:8080/#/admin)、[大屏](http://localhost:8080/#/screen)、[健康检查](http://localhost:8080/api/health)。默认数据库localhost:3306、root/root。

微信开发工具导入解压后的`release/qingjing-neps-1.1.0-weixin.zip`，服务地址使用`http://localhost:8080`。App安装APK，Android Studio模拟器地址为`http://10.0.2.2:8080`，真实手机使用电脑局域网IP。

| 身份 | 演示账号 | 密码 |
| --- | --- | --- |
| 管理员 | admin | Admin123456 |
| 公众监督员 | 13800000001 / 13800000002 | User123456 |
| 网格员 | grid1 / grid2 / grid3 / grid4 | Grid123456 |

四个网格员依次负责沈阳、大连、杭州、南京。已恢复且修改过密码的数据库使用恢复时的密码。

## 开发、检查与资料

```powershell
# 基础运行环境检查
powershell.exe -NoProfile -ExecutionPolicy Bypass -File ./scripts/check-environment.ps1
# 检查完整包文件（建议首次解压后、修改源码前执行）
powershell.exe -NoProfile -ExecutionPolicy Bypass -File ./scripts/verify-package.ps1
# PC与后端重建：先停止正在运行的JAR
powershell.exe -NoProfile -ExecutionPolicy Bypass -File ./scripts/stop.ps1
powershell.exe -NoProfile -ExecutionPolicy Bypass -File ./scripts/build.ps1
# 两个移动端重建；可用-SkipApp或-SkipMiniProgram选择一端
powershell.exe -NoProfile -ExecutionPolicy Bypass -File ./scripts/build-mobile.ps1
```

- [完整环境与操作指南](START_HERE.md)：首次安装、备份恢复、地址端口、重建与移植验收。
- [移动端说明](docs/MOBILE.md)：小程序导入、App安装、模拟器及自动化。
- [接口文档](docs/API.md)、[数据库ER图](docs/DATABASE.md)、[验收记录](docs/ACCEPTANCE.md)。
- `scripts/backup.ps1`：导出当前数据；`scripts/restore.ps1`：显式恢复备份。
- `config/environment.example.ps1`：环境变量示例，不自动加载。

已有验证：30项后端测试、PC浏览器和Android真实模拟器流程通过；微信小程序编译及编译事件/真实后端检查通过。微信SDK模拟器验证仍待使用者完成登录等设置，详见验收记录。异目录启动验证不替代另一台物理设备的环境验收。

等级采用课程三污染物简化规则；演示业务记录来自实训数据，页面插画复用课程材料。
