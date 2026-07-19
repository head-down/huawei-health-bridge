# 真机 & 模拟器测试指南

## 前提条件

- **华为健康数据**：华为手机上已安装并登录华为运动健康 App，且同步了数据
- **Health Connect**：手机上已安装并启用 Health Connect App（Android 14+ 系统内置）
- **OAuth 配置**：项目根目录有有效的 `HuaweiConfig.kt`（开发者账号凭证）
- **USB 调试**：手机已启用开发者选项 → USB 调试，并通过数据线连接电脑
- **ADB**：电脑已安装 Android SDK Platform-Tools，`adb devices` 能识别手机

## 环境检查

```bash
# 确认设备连接
adb devices
# 预期输出：
# List of devices attached
# <device_id>    device

# 确认 Health Connect App 已安装
adb shell pm list packages | grep healthconnect
```

## 构建 & 安装

```bash
cd d:/DevelopTools/mine/huawei-health-bridge

# 1. 编译调试版 APK
./gradlew assembleDebug

# 2. 安装到手机
adb install -r app/build/outputs/apk/debug/app-debug.apk

# 3. 启动应用
adb shell am start -n com.headdown.healthbridge/.MainActivity
```

## 手动测试清单

按以下步骤逐项验证，记录结果：

### 步骤 A: 华为 OAuth 授权

| # | 操作 | 预期结果 | 通过 |
|---|------|----------|------|
| A1 | 点击"授权华为运动健康" | 浏览器打开华为登录页面 | [ ] |
| A2 | 在浏览器中完成华为账号登录 | 浏览器重定向回应用（或显示回调成功的 toast） | [ ] |
| A3 | 回到应用，查看华为授权按钮状态 | 按钮显示 ✓，颜色变为完成态 | [ ] |

### 步骤 B: Health Connect 授权

| # | 操作 | 预期结果 | 通过 |
|---|------|----------|------|
| B1 | 点击"授权 Health Connect" | 弹出 HC 权限对话框，列出所有数据类型权限 | [ ] |
| B2 | 在对话框中勾选所有权限 → 确认 | HC 按钮显示 ✓ | [ ] |
| B3 | **重新测试**: 在对话框中取消权限 → 拒绝 | HC 按钮恢复为初始状态（Granting → Idle 回归验证） | [ ] |

### 步骤 C: 同步与进度

| # | 操作 | 预期结果 | 通过 |
|---|------|----------|------|
| C1 | 完成 A 和 B 后，点击"开始同步" | 按钮禁用，显示进度条 + "正在同步健康数据..." | [ ] |
| C2 | 等待同步进行 | 进度文本逐步更新："已同步: 睡眠 (1/4)" → "...心率 (2/4)" → 依此类推 | [ ] |
| C3 | 同步完成后 | 显示"同步完成"卡片，列出各类型数据条数 + 总计 | [ ] |
| C4 | 检查 Health Connect App | 打开 Health Connect → 在"数据与访问权限"中应能看到写入的华为健康数据 | [ ] |

### 步骤 D: 手动刷新

| # | 操作 | 预期结果 | 通过 |
|---|------|----------|------|
| D1 | 同步成功后，点击"刷新同步" | 重新开始同步，显示进度 | [ ] |
| D2 | 在 Idle 状态下点击"刷新同步" | 同样开始同步流程（不应只显示空白） | [ ] |

### 步骤 E: 深链接解析

| # | 操作 | 预期结果 | 通过 |
|---|------|----------|------|
| E1 | 通过 adb 模拟回调 | 华为授权按钮变为已完成状态（显示 ✓） | [ ] |

```bash
# 模拟 OAuth 回调深链接
adb shell am start -a android.intent.action.VIEW \
  -d "com.headdown.healthbridge://oauth?code=test_code_abc"
```

## 运行 instrumented 测试

```bash
# 在所有已连接设备上运行 UI 测试
./gradlew connectedAndroidTest

# 运行特定测试类
./gradlew connectedAndroidTest --tests "com.headdown.healthbridge.MainScreenStatesTest"

# 运行端到端测试
./gradlew connectedAndroidTest --tests "com.headdown.healthbridge.EndToEndTest"

# 查看测试报告
# 报告位置: app/build/reports/androidTests/connected/index.html
```

## 运行 JVM 单元测试

```bash
# 纯 JVM 测试（不依赖设备）
./gradlew test

# 查看报告
# app/build/reports/tests/testDebugUnitTest/index.html
```

## 故障排查

| 症状 | 可能原因 | 解决方式 |
|------|----------|----------|
| "开始同步" 立即失败 | 华为 OAuth 未授权或 token 过期 | 重新执行步骤 A |
| HC 权限对话框无法弹出 | Health Connect App 未安装 | `adb shell pm list packages healthconnect` 检查 |
| 进度显示卡在 0/4 | 华为 API 无数据或 token 失效 | 检查华为运动健康 App 是否有数据 |
| `adb devices` 显示 unauthorized | 手机未信任电脑 | 手机上弹出对话框 → 允许 |
| Gralde `connectedAndroidTest` 找不到设备 | 设备未连接或 ADB 未启动 | 重插 USB 线，运行 `adb kill-server && adb start-server` |
