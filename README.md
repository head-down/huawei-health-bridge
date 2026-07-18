# Huawei Health Bridge

将华为运动健康数据同步到 Google Health Connect，供 Migraine Buddy 等第三方健康应用使用。

## 背景

华为/荣耀手机用户在 Huawei Health App 中积累了大量健康数据（睡眠、心率、运动等），但这些数据无法直接导入 Google Health Connect。Health Sync 需要付费订阅。

本 App 从华为云端 REST API 读取健康数据，写入 Health Connect，让 **Migraine Buddy** 等 App 能自动获取数据用于偏头痛分析。

## 技术栈

| 层 | 技术 |
|---|---|
| 语言 | Kotlin |
| 构建 | Gradle + AGP 8.5 |
| 最低 SDK | Android 9 (API 28) |
| 华为侧 | Health Kit REST API (OAuth 2.0) |
| Google 侧 | Health Connect SDK |
| 定时任务 | WorkManager |
| UI | Jetpack Compose |

## 支持的同步方向

```
华为运动健康 (云端 REST API)
    │  OAuth 2.0 授权
    ▼
读取数据 → 去重 → 写入
    │
    ▼
Google Health Connect (Android SDK)
    │  Migraine Buddy 自动读取
    ▼
偏头痛触发分析
```

## 同步的数据类型

| 类别 | 华为 → Health Connect |
|---|---|
| 睡眠 | Sleep segment → Sleep Session |
| 心率 | Heart rate → Heart Rate / Resting Heart Rate |
| 运动 | Exercise record → Exercise Session |
| 步数 | Step count → Steps |
| 血氧 | SpO2 → Oxygen Saturation |
| 体重/体脂/身高 | Body data → Weight / Body Fat / Height |
| 体温 | Temperature → Body Temperature |
| 卡路里 | Calories → Active/Total Calories Burned |

## 同步机制

- **前台同步**：打开 App 自动执行一次同步
- **后台同步**：WorkManager 每天定时同步一次
- **手动同步**：App 内提供刷新按钮

## 前置工作

1. 注册 [华为开发者账号](https://developer.huawei.com/)
2. 创建应用 → 申请 Health Kit REST API 权限
3. 安装 Android Studio ([下载](https://developer.android.com/studio))
4. 手机上安装 [Health Connect](https://play.google.com/store/apps/details?id=com.google.android.apps.healthdata)
5. 授予 Migraine Buddy 读取 Health Connect 的权限

## 构建 & 运行

```bash
# 克隆
git clone https://github.com/head-down/huawei-health-bridge.git
cd huawei-health-bridge

# Android Studio 中打开项目
# 1. 修改 local.properties 中的 SDK 路径
# 2. 修改 app/src/main/java/com/headdown/healthbridge/huawei/HuaweiConfig.kt 中的 client_id 和 client_secret
# 3. Run → Run 'app'
```

## 项目结构

```
app/src/main/java/com/headdown/healthbridge/
├── MainActivity.kt                 # 主界面 (Jetpack Compose)
├── huawei/
│   ├── HuaweiConfig.kt             # 华为 OAuth 配置
│   └── HuaweiHealthClient.kt       # 华为 REST API 客户端
├── healthconnect/
│   └── HealthConnectWriter.kt      # Health Connect 数据写入
└── sync/
    └── SyncWorker.kt               # WorkManager 定时同步

app/src/main/
├── AndroidManifest.xml
└── res/
    ├── values/strings.xml
    └── layout/activity_main.xml
```

## License

MIT
