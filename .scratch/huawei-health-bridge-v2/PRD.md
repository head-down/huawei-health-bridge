# 华为健康桥 v2 — 无凭证即可开发的完整同步引擎

> Status: ready-for-agent
> GitHub: [#1](https://github.com/head-down/huawei-health-bridge/issues/1)
> 创建: 2026-07-19 | 来源: `/grill-with-docs` 对话

## 问题陈述

华为 Health Kit API 需要开发者审核，无法立即拿到真实凭据。但数据映射、同步引擎、Health Connect 写入、OAuth 流程这些逻辑与凭证无关——等待审核期间所有开发工作应该可以独立推进，凭证就位后只需替换配置即可运行。

## 解决方案

将同步引擎拆分为三个可测试接缝，开发期用 Mock 实现驱动，凭证就位后替换真实实现：

1. **HealthDataSource 接口** — 抽象华为数据读取。开发期用 Mock 返回官方文档格式 JSON，接入后替换为真实 REST API 客户端。
2. **AuthProvider 接口** — 抽象 OAuth 认证。开发期用 Mock 返回假 token，接入后替换为真实华为 OAuth 流程。
3. **HealthConnectWriter** — 不需要改，已有独立类且接口合适。

同步策略：最近 30 天滚动窗口 + lastSyncedAt 时间断点去重 + WorkManager 每日定时 + 首次启动自动全量同步。

## 用户故事

1. 作为用户，我希望 App 将华为睡眠数据同步到 Health Connect，以便 Migraine Buddy 分析我的睡眠模式与偏头痛触发因素。
2. 作为用户，我希望 App 将华为心率数据同步到 Health Connect，以便 Migraine Buddy 关联心率与偏头痛发作。
3. 作为用户，我希望 App 将华为步数数据同步到 Health Connect，以便 Migraine Buddy 跟踪我的活动水平。
4. 作为用户，我希望 App 将华为运动记录同步到 Health Connect，以便 Migraine Buddy 识别运动相关的偏头痛模式。
5. 作为用户，我希望同步每天自动在后台执行一次，这样我不需要记得手动操作。
6. 作为用户，我希望安装 App 后的首次同步自动拉取最近 30 天数据，这样我不需要任何配置就能开始使用。
7. 作为用户，我希望 App 中有手动刷新按钮，以便我可以随时强制同步。
8. 作为用户，我希望同步时显示进度，以便知道已同步了多少条记录。
9. 作为用户，我希望后续同步自动跳过重复数据，以免 Health Connect 中累积重复记录。
10. 作为用户，我希望通过标准浏览器跳转完成华为 Health Kit 的 OAuth 授权，以便我控制共享哪些数据。
11. 作为用户，我希望 App 能优雅处理华为 API 的失败请求，以免一次失败阻塞后续同步尝试。
12. 作为用户，我希望 App 能优雅处理 Health Connect 写入失败，以免部分失败中断整个同步流程。

## 实现决策

### 接口抽象

新增两个接口来隔离与凭证相关的部分：

**HealthDataSource** — 华为健康数据读取器，四个查询方法均返回对应数据类列表，参数为纳秒级 startTime 和 endTime：睡眠记录、心率记录、步数记录、运动记录。

**MockHealthDataSource** — 返回与真实华为 API 结构完全一致的假数据，来源为官方文档示例，覆盖全部四种数据类型。

**AuthProvider** — OAuth 认证，三个方法：获取当前 access token、检查是否有效、刷新 token。

**MockAuthProvider** — 返回写死的 access token，有效期 1 小时。

真实 `HuaweiHealthClient` 必须实现 `HealthDataSource`，真实 OAuth 流程必须实现 `AuthProvider`。

### 华为 API v2 端点

现有客户端使用 v1 风格的多端点路径。修正后为统一端点：

```
GET /healthkit/v2/healthRecords?startTime=<纳秒>&endTime=<纳秒>&dataType=<类型>
```

| 数据类型 | dataType 参数 | 关键字段 |
|---------|-------------|---------|
| 睡眠 | `com.huawei.health.record.sleep` + subDataType `com.huawei.continuous.sleep.fragment` | fall_asleep_time（long）、wakeup_time（long）、all_sleep_time（int）、deep_sleep_time（int）、light_sleep_time（int）、dream_time（int） |
| 心率 | `com.huawei.instantaneous.heart_rate` | bpm（float） |
| 步数 | `com.huawei.continuous.steps.delta` | steps_delta（int） |
| 运动 | `com.huawei.continuous.activity.segment` | activity_type（int） |

### 时间戳转换

API 使用纳秒级 Unix 时间戳（19 位整数）。所有时间参数需从毫秒转换：`毫秒 × 1,000,000`。

### 响应解析 — 强类型 value 数组

API 返回的 `healthRecords` 数组使用动态类型 value 条目。每个字段带有类型标签，需要按 fieldName 分派取值，不能直接按 JSON 字段名取值。这是核心解析决策，此处保留官方文档的 schema 摘要：

```json
{
  "healthRecords": [{
    "startTime": <纳秒时间戳>,
    "endTime": <纳秒时间戳>,
    "dataTypeName": "<类型>",
    "value": [
      {"fieldName": "字段名", "<类型标签>": <值>}
    ]
  }]
}
```

其中 `<类型标签>` 为 `integerValue`、`floatValue`、`longValue`、`stringValue` 之一，根据 fieldName 匹配该数据类型的已知字段定义选取。

移除 Moshi 依赖，手写解析更适应动态类型标签的匹配模式。

### 同步窗口与去重

- **同步窗口**：最近 30 天
- **去重机制**：lastSyncedAt 断点存储在 SharedPreferences 中，仅本 App 私有，与其他软件无关
- **API 约束**：单次查询最多 31 天，30 天窗口一次请求即可覆盖
- **首次启动**：lastSyncedAt 为空 → 拉满 30 天 → 设 lastSyncedAt = now
- **定时同步**：只拉取时间戳 > lastSyncedAt 的数据

### 错误处理

| 错误类型 | 策略 |
|---------|------|
| 401（token 过期） | 尝试 refresh_token 刷新一次；成功→重试，失败→跳过 |
| 网络错误 / 5xx | WorkManager 指数退避重试（10s → 30s → 2.5min）；最终跳过 |
| Health Connect 写入失败 | 单条记录失败不阻断其余记录写入 |

漏一次同步第二天自动补上，不需要复杂的恢复逻辑。

## 测试决策

### 测试原则

只测外部行为，不测实现细节。每个测试：给定一组华为文档格式的输入数据 → 验证向 Health Connect 写入了正确的记录。

### 测试接缝

1. **HealthConnectWriter 单元测试** — 注入假的 Health Connect 客户端，验证全部四种数据类型的转换正确性（睡眠阶段、心率 bpm、步数聚合、运动类型映射）。

2. **SyncWorker 集成测试** — 注入 MockHealthDataSource + MockAuthProvider，验证完整编排逻辑：四种数据全部拉取并写入、lastSyncedAt 被更新、重复同步不产生重复数据。

3. **真实 API 客户端测试** — 等到拿到凭证后验证真实响应解析。

### 测试框架

新增依赖：JUnit 5、MockK、Kotlin Coroutines Test。项目当前无测试基础设施，需从零搭建。

## 不在范围内

- 体重、体脂、身高、体温、血氧同步（延后到后续迭代；首批只做睡眠 + 心率 + 步数 + 运动）
- 压力数据同步（Health Connect 无对应数据类型）
- 超过 30 天的历史数据全量导入
- 完整的华为 OAuth 浏览器跳转流程（接入真实凭证时再完善，开发期用 Mock）
- 同步推送通知 / 订阅
- 完整 60+ 种运动类型映射（首批覆盖 12 种常见运动）

## 补充说明

- 真实凭证存在版本控制排除的配置文件中，有示例模板可供参考
- 强类型 value 数组格式是核心解析难点 —— Mock 数据必须覆盖全部四种类型标签，以便尽早暴露映射错误
- 步数增量数据按间隔给出（如每分钟步数），Health Connect 需要天级统计，写入前需聚合
