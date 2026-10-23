# Windows x64 离线环境包

此包与`environment-system-20261020-full-source.zip`配套，**不含项目源码**。完整操作步骤见源码包内`START_HERE.html`。

包含已校验的原始安装文件：

- `archives/jdk-17.0.9.zip`：Azul Zulu OpenJDK17.0.9 Windows x64。
- `archives/mysql-8.4.11-winx64.zip`：MySQL8.4.11 Windows x64核心ZIP。
- `installers/wechat_devtools_2.02.2609231_win32_x64.exe`：微信官方开发者工具安装文件。

全部保留原始发行包，不包含个人微信登录、数据库记录或本机设置。JDK来源为Azul官方CDN，MySQL为MySQL官方CDN，微信工具为腾讯官方下载地址；许可证随原始安装文件保留。

## 使用方法

1. 将源码包完整解压，例如`C:/Projects/environment_system`。
2. 将本环境包完整解压，例如`C:/Transfer/qingjing-env`。
3. 从PowerShell执行：

```powershell
powershell.exe -NoProfile -ExecutionPolicy Bypass -File C:/Transfer/qingjing-env/prepare-runtime.ps1 -ProjectPath C:/Projects/environment_system
```

脚本先检查SHA256，再把JDK/MySQL压缩包放进项目缓存，调用原项目bootstrap解压。两个压缩包在本地存在且校验正确时无需联网下载。此过程不创建数据库业务表、不导入记录、不安装MySQL系统服务。

4. 按源码包START_HERE第4节启动MySQL，再选择初始化演示数据或恢复业务SQL，随后启动后端。
5. 公众端需要微信开发工具时，手工运行installers目录中的EXE安装；用自己的微信扫码并选择可用AppID/测试号。手工演示不要求开启CLI服务端口。

## 此包不包含的环境

- Android Studio、SDK、模拟器及系统镜像；电脑演示网格App需提前准备，也可以直接在Android8.0+手机安装APK。
- Node与npm/Maven依赖缓存；它们只在修改源码、重建时需要，运行现成JAR/APK不需要。
- Visual C++运行库；MySQL提示缺少VCRUNTIME/MSVCP时，需要准备微软VC++2015—2022 x64运行库。
- 业务数据备份；请另带本次导出的current-data.sql或更新的SQL备份。

此包仅面向Windows x64，不适用于macOS、Linux或Windows ARM作为原生运行包。文件校验结果不能代替目标电脑的权限、网络和系统组件验证。
