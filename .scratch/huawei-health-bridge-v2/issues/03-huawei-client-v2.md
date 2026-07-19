# 03 — HuaweiHealthClient v2 修正

## Parent

[#1 华为健康桥 v2 PRD](https://github.com/head-down/huawei-health-bridge/issues/1)

## What to build

现有 HuaweiHealthClient 使用了 v1 风格的 API 路径和毫秒级时间戳。此 ticket 完成后，传入日期范围调用客户端，将以纳秒级时间戳正确查询 v2 统一端点，并返回符合官方 API 响应格式的四种健康数据。

## Acceptance criteria

- [ ] API 调用使用 v2 统一端点 `/healthkit/v2/healthRecords`
- [ ] 时间参数以纳秒精度发送（毫秒 × 1,000,000）
- [ ] 响应中 `healthRecords` 数组被正确解析
- [ ] `value` 字段按 `fieldName` 匹配对应类型取值
- [ ] 四种数据类型的 dataType 查询参数正确
- [ ] 不再依赖 Moshi，JSON 解析为手写实现
- [ ] 实现了 HealthDataSource 接口
- [ ] `./gradlew test` 全部通过

## Blocked by

- [#3 接口 + Mock 实现](https://github.com/head-down/huawei-health-bridge/issues/3)

**GitHub:** [#4](https://github.com/head-down/huawei-health-bridge/issues/4)

**Status:** ready-for-agent
