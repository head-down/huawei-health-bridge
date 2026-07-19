# 05 — UI 完善

## Parent

[#1 华为健康桥 v2 PRD](https://github.com/head-down/huawei-health-bridge/issues/1)

## What to build

用户打开应用后看到三个引导按钮。点击每个按钮执行其对应操作：授权打开华为 OAuth 授权页面，连接打开 Health Connect 权限对话框，开始同步触发同步工作器。同步期间 UI 显示进度，完成后展示结果摘要。另有手动刷新按钮可重新启动流程。

## Acceptance criteria

- [ ] 点击"授权"按钮可启动浏览器打开华为 OAuth 授权页面
- [ ] 点击"连接 Health Connect"按钮可弹出 HC 权限对话框
- [ ] 点击"开始同步"按钮触发全量同步，显示加载状态
- [ ] 同步过程中显示进度指示
- [ ] 同步完成后显示结果摘要
- [ ] 存在手动刷新按钮，可重新触发同步
- [ ] OAuth 回调深链接被正确解析

## Blocked by

- [#5 SyncWorker 重写 + 集成测试](https://github.com/head-down/huawei-health-bridge/issues/5)

**GitHub:** [#6](https://github.com/head-down/huawei-health-bridge/issues/6)

**Status:** ready-for-agent
