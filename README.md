# WebDAV 云同步插件 for LightNovelReader

一个为 LightNovelReader（轻小说阅读器）开发的 WebDAV 云同步插件，支持同步阅读记录、书架内容和软件设置。

## 功能特性

### 同步内容
- **阅读记录**：同步阅读进度、章节位置、阅读时长等
- **书架内容**：同步书架中的书籍、订阅状态等
- **软件设置**：同步阅读器的偏好设置

### 同步模式
- **手动同步**：用户手动触发同步
- **自动同步**：按设定的时间间隔（5-120分钟）自动同步
- **Wi-Fi 节省模式**：可设置仅在 Wi-Fi 环境下自动同步

### 数据安全
- 所有数据通过 WebDAV 协议传输
- 支持用户名密码认证
- 数据以 JSON 格式存储，可手动查看和备份

## 支持的 WebDAV 服务器

- Nextcloud
- ownCloud
- Synology Drive
- 任何标准 WebDAV 协议服务器

## 安装

### 方式一：从源码构建

1. 克隆仓库：
```bash
git clone https://github.com/dobao/WebDAVSyncPlugin.git
cd WebDAVSyncPlugin/plugin
```

2. 配置 Android SDK 并构建：
```bash
./gradlew assembleDebug
```

3. 将生成的 `.apk.lnrp` 文件安装到 LightNovelReader

### 方式二：使用 Gradle 任务

```bash
./gradlew runDebugHost  # 安装到调试版 LightNovelReader
./gradlew runReleaseHost  # 安装到正式版 LightNovelReader
```

## 使用方法

1. 安装插件后，在 LightNovelReader 的设置中找到 "WebDAV 云同步"
2. 配置 WebDAV 服务器地址、用户名和密码
3. 选择需要同步的内容（阅读记录、书架、设置）
4. 点击"立即同步"进行首次同步
5. 如需自动同步，开启"启用自动同步"并设置间隔

## 数据存储结构

在 WebDAV 服务器上，数据按以下结构存储：

```
/LightNovelReader/
├── reading_history/
│   └── reading_history.json
├── bookshelf/
│   └── bookshelf.json
├── settings/
│   └── settings.json
└── sync_manifest.json
```

## 技术栈

- Kotlin 2.0+
- Jetpack Compose
- OkHttp3 (WebDAV 请求)
- Kotlinx Serialization (JSON 编解码)
- LightNovelReader Plugin API

## 开发

### 项目结构

```
WebDAVSyncPlugin/
└── plugin/
    ├── build.gradle.kts
    ├── settings.gradle.kts
    ├── gradle.properties
    └── src/main/
        ├── kotlin/com/dobao/webdavsync/
        │   ├── WebDAVSyncPlugin.kt    # 主插件类
        │   ├── data/                   # 数据模型
        │   │   ├── SyncData.kt
        │   │   └── WebDAVConfig.kt
        │   ├── webdav/                 # WebDAV 客户端
        │   │   └── WebDAVClient.kt
        │   ├── sync/                   # 同步逻辑
        │   │   └── SyncManager.kt
        │   └── ui/                     # 设置界面
        │       └── SyncSettingsContent.kt
        └── resources/
            └── META-INF/services/      # 服务配置文件
```

### API 版本

- 需要 LightNovelReader API 版本: 1

## 许可证

Apache License 2.0

## 联系方式

- GitHub Issues: [提交问题](https://github.com/dobao/WebDAVSyncPlugin/issues)
- QQ 群: 867785526 (LightNovelReader 官方群)

## 致谢

- [LightNovelReader](https://github.com/dmzz-yyhyy/LightNovelReader) - 优秀的开源轻小说阅读器
- [LightNovelReaderPlugin-Template](https://github.com/dmzz-yyhyy/LightNovelReaderPlugin-Template) - 插件开发模板

<!-- Trigger build check: 2026-04-20T18:43:48.525756 -->

<!-- Trigger build check: 2026-04-20T18:52:59.242334 -->
