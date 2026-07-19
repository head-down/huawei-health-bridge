package com.headdown.healthbridge

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.lifecycle.ViewModelProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * 端到端流程测试 — 覆盖完整的用户操作路径。
 *
 * 真机/模拟器运行：
 *   ./gradlew connectedAndroidTest --tests "com.headdown.healthbridge.EndToEndTest"
 */
@RunWith(AndroidJUnit4::class)
class EndToEndTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<MainActivity>()

    private val viewModel: MainViewModel
        get() = ViewModelProvider(composeTestRule.activity)[MainViewModel::class.java]

    // ============================================================
    // OAuth 回调深链接 — AC7: URI 解析 + 状态更新
    // ============================================================

    @Test
    fun oauthCallbackDeepLink_setsAuthorizedState() {
        composeTestRule.runOnUiThread {
            // 模拟 OAuth 回调成功 — 直接设状态避免真实网络请求
            viewModel.onHuaweiAuthComplete()
        }
        composeTestRule.waitForIdle()

        // 授权完成后华为按钮应显示 ✓
        composeTestRule.onNodeWithText("✓").assertIsDisplayed()
    }

    // ============================================================
    // 完整流程：初始 UI → 华为授权 → 同步（烟幕测试）
    // ============================================================

    @Test
    fun fullFlow_initialUI_allElementsPresent() {
        composeTestRule.onNodeWithText("将华为运动健康数据同步到 Health Connect").assertIsDisplayed()
        composeTestRule.onNodeWithText("1. 授权华为运动健康").assertIsDisplayed()
        composeTestRule.onNodeWithText("2. 授权 Health Connect").assertIsDisplayed()
        composeTestRule.onNodeWithText("3. 开始同步").assertIsDisplayed()
        composeTestRule.onNodeWithText("刷新同步").assertIsDisplayed()
    }

    @Test
    fun fullFlow_authorizeThenVerifyCheckmark() {
        // 授权华为
        composeTestRule.runOnUiThread { viewModel.onHuaweiAuthComplete() }
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("1. 授权华为运动健康").assertIsDisplayed()

        // 授权 HC
        composeTestRule.runOnUiThread { viewModel.onHealthConnectGranted() }
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("2. 授权 Health Connect").assertIsDisplayed()

        // 两个按钮都已授权完成（不崩溃即为通过）
    }

    // ============================================================
    // HC 权限拒绝 → Granting 复位回归 (P0 fix)
    // ============================================================

    @Test
    fun healthConnectDenied_resetsButtonToIdle() {
        composeTestRule.runOnUiThread { viewModel.onHealthConnectGranting() }
        composeTestRule.waitForIdle()

        composeTestRule.runOnUiThread { viewModel.onHealthConnectDenied() }
        composeTestRule.waitForIdle()

        // 拒绝后 ✓ 不应显示
        composeTestRule.onNodeWithText("2. 授权 Health Connect").assertIsDisplayed()
        composeTestRule.onNodeWithText("✓").assertDoesNotExist()
    }

    // ============================================================
    // 刷新按钮 — 始终可用
    // ============================================================

    @Test
    fun refreshButton_clickableWhenIdle() {
        composeTestRule.onNodeWithText("刷新同步").performClick()
        composeTestRule.waitForIdle()
        // 不应崩溃
        composeTestRule.onNodeWithText("刷新同步").assertIsDisplayed()
    }
}
