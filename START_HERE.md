# 青境环保公众监督系统：新设备安装、配置与操作指南

更新时间：2026年10月20日。适用版本：四端迁移后的完整交付版。

本指南以Windows 10/11 x64为主要移植环境。后端使用Java17和MySQL，PC网页只有管理端与大屏，公众端使用微信小程序，网格员端使用Android App。所有客户端共用同一个后端和数据库。

**第一次接触项目：按第1—6节运行成品；需要搬迁已有记录：按第7节；需要修改源码：按第8节。** 本目录另有`START_HERE.html`，可用浏览器离线阅读和打印。

## 1. 要带走哪些文件

| 文件 | 包含内容 | 用途 |
| --- | --- | --- |
| environment-system-20261020-full-source.zip | 后端源码、三个前端工程、数据库SQL、成品JAR、APK、编译后小程序ZIP、脚本、完整指南与校验清单 | 必带，完整源码与成品包 |
| environment-system-20261020-windows-x64-env.zip | 已校验的JDK17.0.9和MySQL8.4.11官方ZIP、微信开发工具官方安装文件、离线环境说明与准备脚本 | 新电脑网络不方便时携带；不包含项目源码 |
| environment-system-20261020-current-data.sql | 本次从原设备导出的数据库记录、账号及密码哈希 | 想保留原有记录时另带；不自动恢复 |
| SHA256SUMS.txt | 上述文件及成品的SHA256 | 复制后检查完整性 |

源码包不包含本机运行中的数据库文件、个人微信登录资料、Android模拟器镜像、JDK/MySQL展开目录、npm/Maven缓存、node_modules和电脑专属编辑器设置。原有业务数据使用SQL备份迁移，不复制运行中的MySQL数据目录。

推荐新设备解压后目录为`C:/Projects/environment_system`。不要直接在ZIP中运行文件，也不要把项目叠加覆盖到另一个正在运行的版本中。

## 2. 新电脑需要安装什么

### 2.1 运行成品的最小环境

| 要运行的部分 | 必须准备 | 不需要额外准备 |
| --- | --- | --- |
| 后端、管理网页、大屏 | JDK17.0.9、MySQL8、浏览器、Windows PowerShell5.1或PowerShell7 | Node、全局Maven、IDEA、Navicat、Docker |
| 公众微信小程序 | 微信开发者工具、自己的微信登录和可用小程序AppID/测试号 | AppSecret、重新编译源码 |
| 网格员App：手机运行 | Android8.0或更高版本，允许安装本次演示APK | Node、Android SDK、Android Studio |
| 网格员App：电脑运行 | Android Studio、Android Emulator和已创建的虚拟设备 | 重新编译APK |

离线环境包只为Windows x64提供JDK/MySQL和微信工具安装文件。它不包含Android Studio、SDK、虚拟设备、VC++系统运行库，也不包含开发依赖缓存。**因此它支持后端离线部署，不代表一台完全空白电脑能离线配置所有开发工具。** 需要纯电脑演示App时，提前准备Android环境；使用真实Android手机安装APK可以减少电脑配置工作。

### 2.2 修改源码才需要的开发环境

| 工具 | 本项目约定 |
| --- | --- |
| Java | 编译目标Java17；本机验证JDK17.0.9 |
| Node | 建议24.x LTS；本机验证24.21.0，PC/App构建最低22.12.0 |
| Maven | 自带Maven Wrapper3.8.6，不要求全局安装 |
| PC前端 | Vue3.5.22、Vite7.1.7、Element Plus2.11.4、ECharts5.6.0 |
| 公众小程序 | Vue3.4.21、uni-app固定版本，依赖以package-lock.json为准 |
| 网格员App | Vue3.5.22移动页面＋Android原生容器；不使用Qt/C++ |
| 重建APK | Android SDK Platform34、Build Tools34.0.0、Java17、Node |
| 编辑器 | 可复用VS Code；HBuilderX用于小程序开发可选；IDEA也可导入Maven项目 |

官方入口：[Node.js](https://nodejs.org/en/download)、[Android Studio](https://developer.android.com/studio)、[VS Code](https://code.visualstudio.com/download)、[微信开发者工具](https://developers.weixin.qq.com/miniprogram/dev/devtools/download.html)。本指南采用已验证的固定构建配置，不要求追随最新工具版本。

## 3. 准备Java与MySQL运行环境

以下项目命令都在**解压后的environment_system根目录**执行。先打开PowerShell并切换目录：

```powershell
cd C:/Projects/environment_system
```

### 3.1 方式一：联网自动准备

```powershell
powershell.exe -NoProfile -ExecutionPolicy Bypass -File ./scripts/bootstrap.ps1
```

脚本下载、校验并解压JDK17.0.9和MySQL8.4.11到项目`.runtime`；不要求配置全局PATH，不注册MySQL系统服务。首次下载可能需要等待。

### 3.2 方式二：使用离线环境包

1. 将环境包解压到单独目录，例如`C:/Transfer/qingjing-env`，不要把内部三个安装文件再次解压到项目根目录。
2. 在PowerShell运行该环境目录中的准备脚本，并传入本项目根目录：

```powershell
powershell.exe -NoProfile -ExecutionPolicy Bypass -File C:/Transfer/qingjing-env/prepare-runtime.ps1 -ProjectPath C:/Projects/environment_system
```

脚本核对安装文件SHA256，将JDK和MySQL原始ZIP复制进项目`.downloads`，再调用项目bootstrap。由于文件已在本地，Java/MySQL准备不需要联网。环境包中的微信开发工具EXE由你手工运行安装，准备脚本不代替微信扫码登录或修改工具安全设置。

### 3.3 方式三：复用新电脑已有环境

已有可用JDK和MySQL客户端时，可以跳过下载，在当前PowerShell设置：

```powershell
$env:JAVA_HOME='C:/Tools/jdk-17.0.9'
$env:MYSQL_HOME='C:/Tools/mysql-8.4.11-winx64'
```

JAVA_HOME应直接包含`bin/java.exe`，MYSQL_HOME应直接包含`bin/mysql.exe`。项目`.runtime/jdk-17.0.9`存在时优先使用项目JDK。MYSQL_HOME指向MySQL软件目录，不是数据库data目录。

如果新电脑已有正在运行的MySQL，应使用它的真实端口与密码，不能假定一定为root/root；配置方法见第9节。即使连接已有数据库，初始化和备份脚本仍需要MySQL客户端程序。

### 3.4 环境检查

```powershell
powershell.exe -NoProfile -ExecutionPolicy Bypass -File ./scripts/check-environment.ps1
```

运行成品重点检查Java、MySQL客户端和三个成品文件。尚未启动时，健康检查显示“未运行”属正常情况；启动后应返回UP。检查开发环境时另加`-Development`，检查APK重建工具时另加`-Android`。

## 4. 初始化或恢复数据库，然后启动

### 4.1 启动MySQL

```powershell
powershell.exe -NoProfile -ExecutionPolicy Bypass -File ./scripts/start-mysql.ps1
```

默认值：主机localhost，端口3306，用户root，密码root，库名environment_system。项目自建MySQL的数据保存在`data/mysql`，只监听127.0.0.1。

若3306已有服务，脚本先尝试用当前配置连接；不会擅自重置外部数据库密码。连接失败时，应查清已有服务并调整配置，而不是删除其他软件的数据。

### 4.2 只选择一种数据来源

**A：第一次运行，用演示数据。** 仅在空库执行：

```powershell
powershell.exe -NoProfile -ExecutionPolicy Bypass -File ./scripts/init-db.ps1
```

初始化9张表、管理员、监督员、4个网格员及演示反馈/实测。包内已有JAR，初始化无需先安装Node和Maven。已有数据时初始化器会拒绝重复播种，不用于恢复旧业务数据。

**B：保留原设备数据。** 将本次导出的SQL备份复制到新电脑，在确认目标库可被恢复后执行：

```powershell
powershell.exe -NoProfile -ExecutionPolicy Bypass -File ./scripts/restore.ps1 -BackupFile 'C:/Transfer/environment-system-20261020-current-data.sql' -ConfirmRestore
```

恢复会替换目标库中备份对应的表和数据；新电脑已有业务记录时应先备份。**恢复后不要再运行init-db.ps1，不要再导入seed.sql。** 如果只是日常重新启动，两种数据导入命令都不需要执行。

### 4.3 运行现成JAR

```powershell
powershell.exe -NoProfile -ExecutionPolicy Bypass -File ./scripts/start.ps1
```

等待几秒后访问[健康检查](http://localhost:8080/api/health)。返回包含`status: UP`表示应用和数据库均连通。日志在`logs/app-stdout.log`、`logs/app-stderr.log`及`logs/environment-system.log`。

PC访问：[首页](http://localhost:8080/)、[管理端](http://localhost:8080/#/admin)、[大屏](http://localhost:8080/#/screen)。PC不再提供公众登录、网格员登录或注册页面。

## 5. 导入公众监督微信小程序

1. 安装微信开发者工具并使用自己的微信扫码登录。
2. 解压项目`release/qingjing-neps-1.1.0-weixin.zip`到一个独立文件夹，例如`C:/Projects/qingjing-neps-weixin`。
3. 在微信工具中选择“导入项目”，目录选刚解压出来、直接包含`app.json`和`project.config.json`的文件夹；不要选择ZIP本身、项目总根目录或Vue源码目录。
4. 在工具中选择自己的小程序AppID或可用测试号。本包使用游客标识，不携带原电脑的个人开发身份；游客模式的可用能力取决于工具，完整演示建议使用测试号。无需填写AppSecret。
5. 在工具“详情→本地设置”勾选“不校验合法域名、web-view业务域名、TLS版本及HTTPS证书”对应选项，仅用于本地HTTP开发调试。
6. 点击“编译”，在小程序登录页进入“服务设置”，填写`http://localhost:8080`，**不要加/api**，保存并检查连接。
7. 使用`13800000001 / User123456`登录，验证提交反馈、本人记录、状态和实测结果。

手工演示**不需要开启服务端口**。只有CLI导入或自动化验证时，才由使用者在微信工具“设置→安全设置”手动开启服务端口。工具扫码和业务手机号登录属于两个不同的登录步骤。

如果已经在新电脑重新编译，导入目录改为`clients/neps-miniprogram/dist/build/mp-weixin`。已导入的目录不会自动跟随原ZIP更新，换版本时需要重新解压/导入。

## 6. 安装网格员Android App，并验证完整流程

### 6.1 使用真实Android手机

将项目`release/qingjing-nepg-1.1.0.apk`复制到手机并安装；最低Android8.0。App登录页点击“设置服务器地址”，填电脑局域网地址，例如`http://192.168.1.20:8080`。手机与电脑需处于可互通网络。

### 6.2 只在电脑上演示

1. 新电脑安装Android Studio，在SDK Manager准备Android Emulator、Platform Tools及一个可运行的系统镜像。
2. 在Device Manager创建虚拟设备，示例设备名为Pixel_6。无需和原电脑使用相同分辨率，但系统版本应至少API26。
3. 可直接启动该设备，将APK拖入模拟器安装。
4. 也可在当前终端设置SDK路径，再用项目脚本启动并安装：

```powershell
$env:ANDROID_HOME='C:/Users/你的用户名/AppData/Local/Android/Sdk'
powershell.exe -NoProfile -ExecutionPolicy Bypass -File ./scripts/start-android-demo.ps1 -Avd Pixel_6
```

该脚本用只读方式启动可见模拟器，默认端口5558，不修改原虚拟设备快照。已有设备时执行`run-android.ps1 -Device emulator-5558`可重新安装。设备序列号以`adb devices`输出为准。

Android Studio模拟器的App服务地址默认为`http://10.0.2.2:8080`。这是模拟器访问电脑的地址；不要在模拟器App中填localhost。

### 6.3 四端演示账号

| 身份 | 账号 | 密码 | 说明 |
| --- | --- | --- | --- |
| 管理员 | admin | Admin123456 | PC网页登录 |
| 公众监督员 | 13800000001 | User123456 | 微信小程序登录 |
| 公众监督员 | 13800000002 | User123456 | 可验证他人数据隔离 |
| 网格员 | grid1 | Grid123456 | 辽宁省沈阳市 |
| 网格员 | grid2 | Grid123456 | 辽宁省大连市 |
| 网格员 | grid3 | Grid123456 | 浙江省杭州市 |
| 网格员 | grid4 | Grid123456 | 江苏省南京市 |

以上是初始化演示账号；如恢复的是修改过密码的业务备份，应使用备份时的实际密码。

### 6.4 最小完整验证

| 顺序 | 端 | 操作 | 应看到的结果 |
| --- | --- | --- | --- |
| 1 | 微信公众端 | 在辽宁省沈阳市提交一条带明显标记的反馈 | 本人列表出现记录，待指派 |
| 2 | PC管理端 | 将同一反馈指派给grid1 | 变为待实测，记录指派时间 |
| 3 | Android网格端 | grid1登录并刷新，输入SO₂=20.5、CO=8、PM2.5=90，确认提交 | 综合等级三级轻度污染，任务完成 |
| 4 | 微信公众端 | 刷新同一反馈详情 | 已完成，可查看三项实测、等级与时间 |
| 5 | 管理端与大屏 | 查看实测、操作日志和统计 | 成功操作有日志；大屏约10秒内更新 |
| 6 | 启停检查 | 重启后端并重新登录 | 数据保留，内存登录会话需重新建立 |

单位：SO₂和PM2.5为μg/m³，CO为mg/m³。等级采用课堂三污染物简化规则，不是完整国标AQI计算。当前App为包含本地Vue资源的Android混合App，传感器硬件及iOS不属于此交付包。

## 7. 以后怎样备份和再次迁移

在原设备MySQL运行时，从项目根目录执行：

```powershell
powershell.exe -NoProfile -ExecutionPolicy Bypass -File ./scripts/backup.ps1
```

结果位于`backups/environment_system-日期时间.sql`。停机前或重要演示后均可导出。当前交付附带的current-data.sql是打包时快照，不会包含之后新增的记录。

新设备先准备运行环境、启动MySQL，再按第4.2节B恢复。若需要保留两套项目，使用不同库名、端口和目录，不要让两个项目共享同一个正在写入的数据目录。

Android默认演示签名密钥不进入源码包；成品APK已签名，复制即可安装。新电脑重新构建会生成新的演示密钥。若出现更新签名不一致，可卸载旧演示App再安装；这会清除手机端服务地址，但不删除后端记录。需要持续覆盖升级时，应由项目维护者单独保管并迁移原签名密钥。

## 8. 修改源码、重新构建与测试

### 8.1 目录导航

```text
environment_system/
  START_HERE.md / START_HERE.html  本指南
  README.md                       项目概览
  pom.xml / mvnw.cmd / .mvn/       Maven构建与版本锁定
  src/main/java/                  Spring Boot后端
  src/main/resources/mapper/      多表查询XML
  src/main/resources/application.yml  YAML配置
  src/test/                       后端业务和真实MySQL测试
  web/                            PC管理端、大屏源码
  clients/neps-miniprogram/        微信公众端Vue/uni-app源码
  clients/nepg-app/src/            网格员App的Vue移动界面
  clients/nepg-app/android/        Android容器、网络桥与资源
  sql/                            表结构与原始演示数据SQL
  scripts/                        环境、构建、启停、备份和移植脚本
  docs/                           API、ER、移动说明和验证记录
  target/environment_system-1.0.0.jar  后端与PC网页成品
  release/qingjing-nepg-1.1.0.apk      网格员成品
  release/qingjing-neps-1.1.0-weixin.zip 公众端成品
  release/CONTENTS-SHA256SUMS.txt   包内文件校验清单
```

交付包日期为20261020；JAR内部工程版本仍为1.0.0，两个移动客户端版本为1.1.0。这是不同构件的版本号，不是遗漏更新。

### 8.2 后端和PC网页一起重建

先安装Node24.x并确认`node -v`、`npm -v`可执行。在Windows用`npm.cmd`可避开npm.ps1执行策略问题。依赖版本以三个工程各自的package-lock.json为准。

```powershell
powershell.exe -NoProfile -ExecutionPolicy Bypass -File ./scripts/stop.ps1
powershell.exe -NoProfile -ExecutionPolicy Bypass -File ./scripts/build.ps1
powershell.exe -NoProfile -ExecutionPolicy Bypass -File ./scripts/start.ps1
```

脚本会构建PC静态页面、打包进Spring Boot并运行后端测试。初次源码构建需要联网下载npm/Maven依赖；Maven使用项目`.mvn/settings.xml`镜像，无法连接时可改为Maven Central。不要在JAR正在运行时覆盖它。

### 8.3 公众端重建

```powershell
powershell.exe -NoProfile -ExecutionPolicy Bypass -File ./scripts/build-mobile.ps1 -SkipApp
```

生成`clients/neps-miniprogram/dist/build/mp-weixin`及`release/qingjing-neps-1.1.0-weixin.zip`。需要写入自己的AppID时可传`-WeChatAppId wx你的AppID`。源码CLI工程可在VS Code或HBuilderX中编辑；请使用系统Node，不使用旧HBuilderX内置Node18执行本工程构建。

### 8.4 网格员App重建

在Android SDK Manager安装Platform34和Build Tools34.0.0，设置ANDROID_HOME后：

```powershell
$env:ANDROID_HOME='C:/Android/Sdk'
powershell.exe -NoProfile -ExecutionPolicy Bypass -File ./scripts/build-mobile.ps1 -SkipMiniProgram
```

生成相同release路径下的APK。脚本使用aapt、javac、d8、zipalign和apksigner，不依赖云打包及Gradle下载。Android源文件可以阅读和编辑，但本项目没有配置可直接同步的Gradle工程，重建入口是上述脚本。

### 8.5 开发模式与测试

PC热更新：进入`web`，执行`npm ci`后`npm run dev`，访问5173端口；API代理到8080。后端单独运行。

小程序热更新：进入`clients/neps-miniprogram`，执行`npm ci`后`npm run dev:mp-weixin`，微信工具导入`dist/dev/mp-weixin`。

后端完整测试（真实MySQL需先运行，自动建立独立测试库）：

```powershell
. ./scripts/env.ps1
$env:RUN_MYSQL_TESTS='true'
./mvnw.cmd '-Dmaven.repo.local=.m2' test
Remove-Item Env:RUN_MYSQL_TESTS
```

PC浏览器检查：`node ./web/scripts/browser-smoke.mjs`。Android及微信自动化入口见`docs/MOBILE.md`；它们会创建带标记的虚构业务记录。微信模拟器自动化需使用者登录、合适AppID及手动开启服务端口。

本机既有验证：30项后端测试、PC浏览器、Android真实模拟器流程已通过；小程序已通过编译以及编译事件与真实后端检查。**微信SDK模拟器完整验证尚待账号持有人完成工具设置，不将编译通过当作运行时验收通过。** 详见`docs/ACCEPTANCE.md`和各verification.json。

## 9. 地址、端口与数据库配置

### 9.1 同一个后端，不同客户端地址

| 访问位置 | 默认服务地址 |
| --- | --- |
| 电脑浏览器 | http://localhost:8080 |
| 电脑微信开发工具 | http://localhost:8080 |
| Android Studio模拟器 | http://10.0.2.2:8080 |
| Android手机、微信真机调试 | http://电脑的局域网IPv4:8080 |

MySQL3306只供后端连接，客户端不连接MySQL。真机需要HTTP端口可达，局域网防火墙按需允许8080，不需开放数据库端口。正式微信发布还需要配置平台允许的HTTPS域名；本地调试设置不能替代正式发布要求。

### 9.2 新电脑已有数据库或端口冲突

这些环境变量在**当前PowerShell**有效，应在同一个终端执行准备、初始化/恢复、启动命令：

```powershell
$env:DB_HOST='127.0.0.1'
$env:DB_PORT='3307'
$env:DB_NAME='environment_system'
$env:DB_USER='root'
$env:DB_PASSWORD='新设备数据库的真实密码'
$env:SERVER_PORT='8081'
```

模板见`config/environment.example.ps1`，可参考修改，不会自动加载。项目新建便携MySQL采用root/root；外部数据库使用其现有密码。SERVER_PORT变化后，小程序和App的服务地址也需改为8081，开发代理目标亦应同步修改。

## 10. 日常启动、停止与常见问题

以后日常启动只需：

```powershell
powershell.exe -NoProfile -ExecutionPolicy Bypass -File ./scripts/start-mysql.ps1
powershell.exe -NoProfile -ExecutionPolicy Bypass -File ./scripts/start.ps1
```

停止后端：`powershell.exe -NoProfile -ExecutionPolicy Bypass -File ./scripts/stop.ps1`。

同时停止脚本启动的项目MySQL：在命令后加`-MySql`；脚本不用于关闭其他软件的数据库服务。关闭Android模拟器不删除后端数据。

| 现象 | 处理 |
| --- | --- |
| Java未找到 | 运行bootstrap或设置JAVA_HOME；应指向含bin目录的JDK根目录 |
| MySQL缺少VCRUNTIME/MSVCP DLL | 安装微软Visual C++ 2015—2022 x64运行库；离线环境包未附该系统组件 |
| 3306占用、Access denied | 核对已有MySQL、端口、实际密码；按第9节配置，不删除其他软件数据 |
| PowerShell拒绝运行脚本 | 使用本指南单次-ExecutionPolicy Bypass；无需修改全局执行策略 |
| 8080打不开 | 检查应用日志、Java版本、数据库连接和端口占用；健康检查必须UP |
| 小程序显示域名不合法 | 勾选开发工具本地调试选项；先只在电脑模拟器演示 |
| 微信工具无法导入 | 目录应直接有app.json和project.config.json；选编译结果而非Vue源码 |
| App无法连接 | 模拟器用10.0.2.2，手机用电脑IP；地址不加/api；确认8080后端可达 |
| App安装更新失败 | 检查Android版本及签名；不同演示签名可卸载旧App后安装 |
| App白屏或显示异常 | 使用较新的Android系统/WebView，并查看模拟器日志；原设备已用API35验证 |
| 找不到Pixel_6 | 新电脑要自行创建虚拟设备，或用-Avd传入现有设备名 |
| 没有待实测任务 | 管理员先指派同城反馈给当前网格员，再刷新App |
| 启动后账号退出 | 后端Session为内存会话，重启后重新登录，数据库记录仍保留 |
| 初始化提示已有数据 | 不重复初始化；需要旧记录应恢复SQL备份 |
| 离线构建仍尝试联网 | 离线环境包不含npm/Maven依赖缓存，成品运行和源码重建是两种需求 |

## 11. 其他操作系统

成品JAR及Java源码可用于Linux/macOS，但Windows的EXE、便携运行包及PowerShell脚本不是这些系统的对应安装环境。准备目标系统自身的JDK17和MySQL8，初始化空库时可执行：

```bash
mysql --default-character-set=utf8mb4 -u root -p < sql/schema.sql
# 仅首次演示初始化；已有备份应恢复备份并跳过此行
java -jar target/environment_system-1.0.0.jar --app.seed=true --spring.main.web-application-type=none
java -jar target/environment_system-1.0.0.jar
```

备份SQL可用MySQL客户端导入目标库；从源码重建需手工构建web、将dist复制到后端static目录，再运行Maven Wrapper。APK可在Android设备安装；微信工具的操作系统支持以官方提供的版本为准。本次只验证Windows x64，不宣称已在其他系统完成测试。

## 12. 移植完成的判定

- 复制后的ZIP校验值与随附SHA256SUMS一致；包内文件可用verify-package.ps1检查。
- 新电脑健康接口为UP，管理端与只读大屏可访问。
- 小程序能登录、提交本人反馈并查询进展。
- App能取得指派任务，校验浓度并完成实测。
- 管理端、公众结果、大屏和日志对同一条反馈保持一致。
- 停止/重启后业务记录保留，备份文件能在独立目标库恢复。

在完全不同的物理电脑上仍需执行上述检查。本次发布包会进行同机不同目录、不同HTTP端口的迁移验证，这不能代替另一台电脑的系统组件、网络与微信开发身份验证。
