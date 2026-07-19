# 04 — SyncWorker 重写 + 集成测试

## Parent

[#1 华为健康桥 v2 PRD](https://github.com/head-down/huawei-health-bridge/issues/1)

## What to build

同步工作器按计划（或手动触发）运行：通过数据源接口查询最近 30 天的华为健康数据，写入 Health Connect，并持久化同步检查点，使下次运行仅拉取增量数据。token 过期时自动重试一次。失败的同步不会阻塞之后的运行——重试走 WorkManager 内置退避策略。

## Acceptance criteria

- [ ] 同步窗口为最近 30 天（首次启动全量拉取）
- [ ] 同步完成后检查点被持久化，下次仅同步增量数据
- [ ] 通过接口调用数据查询和认证（注入 Mock 依赖即可验证完整流程）
- [ ] 401 错误时自动 refresh token 一次，成功则重试，失败则跳过本次同步
- [ ] 集成测试：注入 MockHealthDataSource + MockAuthProvider，验证完整同步流程、去重逻辑、检查点更新
- [ ] `./gradlew test` 全部通过

## Blocked by

- [#2 测试基础设施搭建](https://github.com/head-down/huawei-health-bridge/issues/2)
- [#3 接口 + Mock 实现](https://github.com/head-down/huawei-health-bridge/issues/3)
- [#4 HuaweiHealthClient v2 修正](https://github.com/head-down/huawei-health-bridge/issues/4)

**GitHub:** [#5](https://github.com/head-down/huawei-health-bridge/issues/5)

**Status:** ready-for-agent
