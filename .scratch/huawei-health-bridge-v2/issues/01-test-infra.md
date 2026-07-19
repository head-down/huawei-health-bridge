# 01 — 测试基础设施搭建

## Parent

[#1 华为健康桥 v2 PRD](https://github.com/head-down/huawei-health-bridge/issues/1)

## What to build

项目当前无任何测试基础设施。此 ticket 完成后，执行 `./gradlew test` 可成功运行并通过，证明测试框架已就绪。提供一个示例测试用例，后续任何 ticket 可立即开始编写测试。

## Acceptance criteria

- [x] 执行 `./gradlew test` 成功运行并通过
- [x] 存在示例单元测试，证明框架正常工作（含断言和协程支持）
- [x] 测试依赖（JUnit 5、MockK、Coroutines Test）已配置完毕

## Blocked by

None — can start immediately.

**GitHub:** [#2](https://github.com/head-down/huawei-health-bridge/issues/2)

**Status:** done
