# 02 — 接口 + Mock 实现 + HealthConnectWriter 转换测试

## Parent

[#1 华为健康桥 v2 PRD](https://github.com/head-down/huawei-health-bridge/issues/1)

## What to build

同步管线有两个可插拔的接口——数据源和认证。开发者调用四种健康数据方法中的任意一种时，可获取到结构正确的假数据（使用 Mock 实现时）。HealthConnectWriter 能将四种华为数据类型正确转换为 Health Connect 记录，并通过单元测试验证。

## Acceptance criteria

- [ ] HealthDataSource 接口定义完成，覆盖四种健康数据类型的查询方法
- [ ] AuthProvider 接口定义完成，覆盖 token 获取、校验、刷新三个方法
- [ ] MockHealthDataSource 对四种数据类型均返回结构正确的假数据
- [ ] MockAuthProvider 返回有效期为 3600 秒的假 token
- [ ] 睡眠转换测试通过：华为睡眠记录 → Health Connect SleepSession
- [ ] 心率转换测试通过：华为心率记录 → Health Connect HeartRateRecord
- [ ] 步数转换测试通过：华为步数增量记录 → Health Connect StepsRecord
- [ ] 运动转换测试通过：华为运动记录 → Health Connect ExerciseSession
- [ ] `./gradlew test` 全部通过

## Blocked by

- [#2 测试基础设施搭建](https://github.com/head-down/huawei-health-bridge/issues/2)

**GitHub:** [#3](https://github.com/head-down/huawei-health-bridge/issues/3)

**Status:** ready-for-agent
