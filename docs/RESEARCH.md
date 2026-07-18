# 华为健康桥 — 调研报告

> 2026-07-18 | 调研人：CodeBuddy | 状态：方案已确认，等待华为 API 申请

## 一、需求背景

### 最终目标

为 **Migraine Buddy**（偏头痛追踪 App）提供来自华为运动健康的生理数据（睡眠、心率等），用于偏头痛触发因素分析。

### 为什么不做健康数据导出通用工具

调研发现 Migraine Buddy 只支持 Oura Ring 等少数设备的专属集成，**不**提供公开 API。但它会从 Google Health Connect 读取数据——所以我们的策略是：

```
华为运动健康 → Health Connect → Migraine Buddy
```

只做第一步（华为→HC），第二步由 Migraine Buddy 自动完成。

---

## 二、技术可行性

### 2.1 华为运动健康侧

| 项目 | 结论 |
|---|---|
| 数据源 | 华为 Health Kit REST API（云端） |
| 支持平台 | Android / iOS / Web / HarmonyOS |
| 认证方式 | OAuth 2.0（授权码模式） |
| API 地址 | `https://health-api.cloud.huawei.com/healthkit/v2` |
| 提供的数据类型 | 睡眠、心率、步数、运动、体重、体脂、身高、血氧、体温、血压、血糖、压力 ⋯ |
| 是否免费 | 是（华为 Health Kit 对齐 HMS 生态免费开放策略） |
| 限制 | 需注册华为开发者账号，应用需通过审核 |

**重要**：荣耀手机（分离后）**没有 HMS**，只能使用 REST API（云端读取），无法使用本地 SDK。

### 2.2 Google Health Connect 侧

| 项目 | 结论 |
|---|---|
| 写入方式 | **仅 Android SDK**（无 REST API） |
| 最低版本 | Health Connect 随 Android 14+ 内置，也可从 Play Store 安装 |
| 支持的数据类型 | 60+ 种（睡眠、心率、步数、运动、体重、血氧、体温……），与华为侧高度对应 |
| 是否免费 | 是 |
| 限制 | App 必须安装在手机上，不能是 Windows/Linux 服务 |

### 2.3 关键约束

> **结论：只能是 Android App，不能是 Python 脚本或服务器端服务。**

- 华为侧：可以用 REST API（任何语言都能调）
- Google 侧：**必须用 Health Connect SDK（仅 Android / Kotlin / Java）**

---

## 三、数据映射

| 华为 Health Kit | → | Google Health Connect | 偏头痛分析价值 |
|---|---|---|---|
| 睡眠记录（时长+阶段） | → | Sleep Session | 高（睡眠质量是关键触发因素） |
| 心率（运动心率/静息心率） | → | Heart Rate / Resting Heart Rate | 中 |
| 步数 | → | Steps | 低 |
| 运动记录（60+ 种类型） | → | Exercise Session | 中（剧烈运动可能触发） |
| 血氧饱和度 | → | Oxygen Saturation | 低 |
| 体重 | → | Weight | 低 |
| 体脂 | → | Body Fat | 低 |
| 身高 | → | Height | 低 |
| 体温 | → | Body Temperature | 低 |
| **压力** | → | ❌ Health Connect 无对应类型 | — |
| **血糖** | → | Blood Glucose（有对应，暂不做） | 低 |
| **血压** | → | Blood Pressure（有对应，暂不做） | 低 |

**首批同步**：睡眠 + 心率 + 步数 + 运动（核心四项，覆盖偏头痛监控需求）。

---

## 四、技术方案

### 4.1 架构

```
┌────────────────────┐     OAuth 2.0     ┌─────────────────────┐
│  华为云 REST API    │ ◄─────────────── │  HuaweiHealthClient  │
│  health-api.cloud   │    Access Token   │  (OkHttp + JSON)     │
│  .huawei.com        │                   └──────────┬──────────┘
└────────────────────┘                              │
                                                    │ 数据转换
                                                    ▼
┌────────────────────┐     Android SDK    ┌─────────────────────┐
│  Health Connect     │ ◄─────────────── │  HealthConnectWriter │
│  (手机本地)          │    insertRecords  │  (健康数据映射)       │
└────────────────────┘                   └──────────┬──────────┘
                                                    │
                                                    ▼
                                          ┌─────────────────────┐
                                          │  Migraine Buddy     │
                                          │  (自动读取 HC 数据)   │
                                          └─────────────────────┘
```

### 4.2 技术选型

| 层 | 技术 | 理由 |
|---|---|---|
| 语言 | Kotlin | Android 首选，协程支持异步 |
| UI | Jetpack Compose | 现代 UI 框架，声明式 |
| 网络 | OkHttp 4 | 调用华为 REST API |
| JSON | 内置 `org.json` | 零依赖 |
| 定时任务 | WorkManager | 官方推荐的保活方式 |
| 构建 | Gradle + AGP 8.5 | 标准 Android 构建 |

### 4.3 同步触发

| 方式 | 实现 |
|---|---|
| **前台同步** | 打开 App 时自动执行一次 |
| **后台定时** | WorkManager PeriodicWork，每天一次 |
| **手动刷新** | UI 按钮，用户可以随时点击 |

参考了 Health Sync 的设计——不做常驻后台服务（耗电、容易被杀），用 WorkManager 保持最低限度的后台同步。

---

## 五、代码结构

项目已创建并推送到 GitHub：
```
https://github.com/head-down/huawei-health-bridge
```

```
app/src/main/java/com/headdown/healthbridge/
├── MainActivity.kt                 # Compose UI，三步引导流程
├── huawei/
│   ├── HuaweiConfig.kt             # OAuth 凭据（待替换真实值）
│   └── HuaweiHealthClient.kt       # REST API 客户端，7 种数据查询方法
├── healthconnect/
│   └── HealthConnectWriter.kt      # 数据转换 + 写入 Health Connect
└── sync/
    └── SyncWorker.kt               # WorkManager 定时任务
```

---

## 六、前置准备清单

### 你需要做的

- [ ] **注册华为开发者账号**：https://developer.huawei.com/
- [ ] **创建应用**：在 AppGallery Connect 中创建 Android 应用
- [ ] **开通 Health Kit 服务**：申请以下 API 权限
  - `healthkit.sleep.read`
  - `healthkit.activity.read`
  - `healthkit.heartrate.read`
  - `healthkit.weight.read`
  - `healthkit.oxygenaturation.read`
- [ ] **获取 OAuth 凭据**：`client_id` + `client_secret`
- [ ] **安装 Android Studio**：https://developer.android.com/studio
- [ ] **手机上装好 Health Connect**：Play Store 或 APK
- [ ] **确认 Migraine Buddy 已授权读取 Health Connect**

### 代码层待实现

- [ ] 替换 `HuaweiConfig.kt` 中的真实凭据
- [ ] 完善 OAuth 浏览器跳转流程（目前框架已写，OAuth 回调 URL 已声明）
- [ ] Token 刷新逻辑
- [ ] 同步进度 UI 状态
- [ ] 错误处理与重试
- [ ] 首次全量同步 vs 增量同步逻辑
- [ ] 测试各华为 API 端点的实际返回格式，调整字段映射

---

## 七、参考资源

| 资源 | 链接 |
|---|---|
| 华为 Health Kit 开发指南 | https://developer.huawei.com/consumer/cn/hms/huaweihealth |
| 华为 REST API 示例（睡眠） | https://developer.huawei.com/consumer/cn/doc/HMSCore-Guides/sleep-record-scene-0000001055511884 |
| 华为 REST API 示例（心率） | https://developer.huawei.com/consumer/cn/doc/HMSCore-Guides/exercise-heart-rate-0000001274012996 |
| Health Connect 数据类型 | https://developer.android.com/health-and-fitness/health-connect/data-types |
| Health Connect 接入指南 | https://developer.android.com/health-and-fitness/health-connect/get-started |
| 现有开源项目（体重同步，基于 Tasker） | https://github.com/christianeirich/huawei-health-to-health-connect |
| Health Sync（商业竞品参考） | https://healthsync.app/home-cn/ |
| Migraine Buddy | https://migrainebuddy.com/ |
