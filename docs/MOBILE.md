# 移动客户端部署与演示

当前交付形态：NEPS为微信小程序，NEPG为Android App，NEPM和NEPV为PC网页。三个客户端工程共用原Spring Boot API和MySQL，数据库结构与账号保持兼容。

## 1. 目录与成品

| 目录/文件 | 说明 |
| --- | --- |
| clients/neps-miniprogram | Vue3 / uni-app公众端源码 |
| clients/neps-miniprogram/dist/build/mp-weixin | 编译后可导入微信开发者工具的工程 |
| clients/nepg-app/src | Vue3网格员移动界面，独立于PC前端 |
| clients/nepg-app/android | Android原生容器、HTTP桥及资源，不依赖云打包 |
| release/qingjing-nepg-1.1.0.apk | 可安装Android课程演示包 |
| release/qingjing-neps-1.1.0-weixin.zip | 编译后微信小程序工程压缩包 |
| web | 仅管理端和只读大屏 |

App采用Vue移动界面加Android WebView原生容器，界面资源包含在APK中。打开App不依赖PC网页或Vite开发服务器；请求通过Java HttpURLConnection访问后端，独立管理Session。它是混合App，不是Qt/C++应用，也未接入真实传感器。Android最低版本8.0（API26），当前构建目标API34；iOS不在本次交付范围。

## 2. 先启动后端

在项目根目录：

```powershell
powershell.exe -NoProfile -ExecutionPolicy Bypass -File ./scripts/start-mysql.ps1
powershell.exe -NoProfile -ExecutionPolicy Bypass -File ./scripts/start.ps1
```

确认 http://localhost:8080/api/health 返回UP。已有数据库不重复初始化，日常启动不清空数据。

## 3. 公众监督微信小程序

### 首次导入

1. 安装微信官方开发者工具。本机已下载并安装在项目`.runtime/wechat-devtools`；该大体积工具不包含在移植包中。
2. 使用微信扫码登录，准备自己可用的小程序AppID或测试号AppID。**不需要提供AppSecret。**
3. 导入`clients/neps-miniprogram/dist/build/mp-weixin`；也可以解压`release/qingjing-neps-1.1.0-weixin.zip`后导入。
4. 在导入界面填写可用AppID。源码manifest默认不保存个人AppID；无AppID/游客模式的能力受工具限制，完整业务演示建议使用测试号。
5. 在开发工具“详情→本地设置”启用“不校验合法域名、web-view业务域名、TLS版本及HTTPS证书”对应选项，用于本地HTTP调试。菜单名称可能随工具版本略有调整。
6. 编译后进入“青境·公众监督”，默认服务器为`http://localhost:8080`。需要修改时进入“服务设置”，保存并检查连接。
7. 使用`13800000001 / User123456`登录，可注册、提交反馈、筛选本人记录、查询状态和实测结果。下拉刷新列表，详情页可刷新进展。

如果需要通过CLI或自动化工具验证，在微信开发者工具“设置→安全设置”中由使用者开启“服务端口”。扫码登录及相关授权应由账号持有人完成，脚本不修改微信工具的隐藏安全配置。

开启后，可以执行`scripts/open-miniprogram.ps1`打开本机编译工程；通过`-AppId wx你的AppID`设置导入工程的AppID。其他电脑可用`WECHAT_DEVTOOLS_HOME`指定微信开发者工具目录。

### 修改与编译

```powershell
cd clients/neps-miniprogram
npm ci --no-audit --no-fund
npm run dev:mp-weixin
# 或一次性生成可导入成品
npm run build:mp-weixin
```

开发产物在`dist/dev/mp-weixin`，生产产物在`dist/build/mp-weixin`。在HBuilderX中可打开此CLI工程，再运行到微信开发者工具；使用项目锁定依赖，不依赖旧HBuilderX内置Node18。当前验证使用系统Node24。

统一构建脚本支持`-WeChatAppId wx你的AppID`或环境变量`WECHAT_APPID`；未传入时使用游客标识，导入后应在工具中选择自己的测试号。个人AppID不是密码，但不要将AppSecret写入前端。

注册使用业务手机号和密码；这里的登录不使用微信一键登录，不需要AppSecret或微信手机号权限。小程序请求封装保存并携带自己的JSESSIONID，和管理网页及网格员App的会话分开。

## 4. 网格员Android App

### 直接安装成品

将`release/qingjing-nepg-1.1.0.apk`安装到Android手机或模拟器。电脑本地演示可打开Android Studio → Device Manager，启动已有Pixel_6设备，然后执行：

```powershell
powershell.exe -NoProfile -ExecutionPolicy Bypass -File ./scripts/run-android.ps1
# 有多个设备时指定adb devices中对应的序列号
powershell.exe -NoProfile -ExecutionPolicy Bypass -File ./scripts/run-android.ps1 -Device emulator-5556
```

也可以把APK拖入已启动的Android模拟器安装。当前测试实例使用本机已有Pixel_6镜像的只读模式，不写回原模拟器快照。测试时的模拟器可能无可见窗口；需要现场演示时可在Android Studio正常启动设备，或执行以下脚本打开可见模拟器并安装App：

```powershell
powershell.exe -NoProfile -ExecutionPolicy Bypass -File ./scripts/start-android-demo.ps1
```

脚本默认使用Pixel_6、端口5558，只读运行以保留原模拟器配置；可用`-Avd`指定另一设备名。

App默认后端地址为`http://10.0.2.2:8080`，这是Android Studio模拟器访问宿主电脑的地址。首次登录前可在“设置服务器地址”修改。登录`grid1 / Grid123456`，默认负责辽宁省沈阳市。

支持待实测/已完成筛选、地址或描述搜索、分页、任务详情、实测录入、等级预览、提交前确认和完成结果查看。每次App进程重启需重新登录；服务器地址保留，密码不保存。

### 从源码生成APK

需Java17、Node22.12+、Android SDK Platform34和Build Tools34.0.0。本机已有SDK，脚本优先读取ANDROID_HOME/ANDROID_SDK_ROOT，其次寻找本机既有路径。迁移到其他电脑时设置自己的SDK路径：

```powershell
$env:ANDROID_HOME='C:/Android/Sdk'
powershell.exe -NoProfile -ExecutionPolicy Bypass -File ./scripts/build-mobile.ps1
# 只构建App
powershell.exe -NoProfile -ExecutionPolicy Bypass -File ./scripts/build-mobile.ps1 -SkipMiniProgram
# 只构建微信小程序
powershell.exe -NoProfile -ExecutionPolicy Bypass -File ./scripts/build-mobile.ps1 -SkipApp
```

构建脚本使用SDK自带aapt、javac、d8、zipalign、apksigner生成APK，避免再次下载Gradle和云端打包依赖。Android源码可以用Android Studio或VS Code阅读；当前重建入口为脚本，没有配置Gradle工程同步。

演示签名密钥首次生成到`data/android-signing/qingjing-demo.keystore`，不进入源码发布包。新电脑重新构建会生成新的演示签名；覆盖安装旧签名APK会提示不兼容，需要卸载旧演示App再安装，或自行保留原签名密钥。卸载App不删除后端业务数据，但会清除App本地服务器地址。

本APK是课程演示构建，启用Android/WebView调试以支持模拟器自动化，允许HTTP连接本地后端。正式发布前应关闭debuggable、采用正式签名并配置HTTPS。

## 5. 一台电脑完整演示

1. 启动MySQL和Spring Boot，打开管理端 http://localhost:8080/#/admin 。
2. 微信开发工具中以监督员登录，选择辽宁省/沈阳市，提交一条反馈。
3. 浏览器管理员`admin / Admin123456`将该记录指派给`grid1`。
4. Android App中`grid1 / Grid123456`刷新待实测任务，进入详情，填写SO₂=20.5、CO=8、PM2.5=90并确认提交。
5. 微信小程序刷新详情，显示已完成、三级轻度污染及对应实测值。
6. 浏览器检查管理统计与日志，大屏最多10秒后刷新。

四类角色的账号和数据库仍然共用，PC网页只提供管理员登录。旧`/#/supervisor`、`/#/grid`、`/#/register`地址回到PC首页，不能通过修改role参数恢复旧移动端页面。

## 6. 真机地址与迁移

| 客户端运行位置 | 后端地址示例 |
| --- | --- |
| 电脑微信开发者工具 | http://localhost:8080 |
| Android Studio模拟器 | http://10.0.2.2:8080 |
| Android手机 / 微信真机调试 | http://电脑的局域网IPv4:8080 |

真机与电脑应处于可互通网络，后端HTTP端口需能通过专用网络防火墙；无需开放MySQL3306。微信真机调试还受开发工具调试模式及平台域名规则限制。正式上线时按微信平台要求配置合法HTTPS域名。

只运行现有成品不需要Node或Android SDK：后端需要Java、MySQL；公众端需要微信开发工具和自己的开发身份；网格员端安装APK即可。修改后再重建才需要相应Node/SDK依赖。

## 7. 常见问题

| 问题 | 处理 |
| --- | --- |
| 小程序提示域名不合法 | 检查开发工具本地调试的域名校验选项；正式发布需配置合法域名 |
| App无法连接 | 模拟器用10.0.2.2，手机用电脑局域网IP，勿在手机中填写localhost |
| 小程序登录成功后又401 | 确认请求封装携带JSESSIONID；重启后端后需重新登录 |
| App安装签名冲突 | 使用原演示密钥重建，或卸载旧演示App后再装；后端记录保留 |
| adb出现多个设备 | 使用run-android.ps1 -Device指定目标 |
| 编译提示Android SDK缺失 | 设置ANDROID_HOME，安装Platform34与Build Tools34.0.0 |
| 小程序CLI服务关闭 | 使用者在微信开发工具安全设置中手动开启；不修改隐藏配置文件 |
| 无待实测任务 | 在管理端先将同城待指派反馈分配给当前网格员，再刷新App |

## 8. 自动化验证入口

从项目根目录运行：

```powershell
# 后端已启动，使用电脑现有Chrome/Edge
node ./web/scripts/browser-smoke.mjs
# Android演示App已安装且模拟器运行；只连接，不安装额外测试驱动
$env:ANDROID_DEVICE='emulator-5558'
node ./web/scripts/android-smoke.mjs
# 无需微信工具登录，检查实际编译页面事件及真实后端；不替代微信模拟器验证
node ./web/scripts/wechat-compiled-smoke.mjs
```

微信工具需先登录并开启服务端口，在允许的AppID项目上启动自动化：

```powershell
& './.runtime/wechat-devtools/cli.bat' auto --project './clients/neps-miniprogram/dist/build/mp-weixin' --auto-port 9421 --trust-project
node ./web/scripts/wechat-smoke.mjs
```

Android和微信脚本会创建明确标记的虚构演示反馈，并验证真实业务保存，不清空数据库。Android测试覆盖实测完成和结果回显；微信测试覆盖登录、地区选择、反馈提交及进度详情。微信脚本只有在实际运行通过后才将wechat-verification.json更新为通过；编译成功的记录不代表运行时验证完成。
