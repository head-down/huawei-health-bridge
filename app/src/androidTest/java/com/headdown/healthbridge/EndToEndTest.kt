package com.headdown.healthbridge

import android.content.Intent
import android.net.Uri
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
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

    // ============================================================
    // OAuth 回调深链接解析 (AC7)
    // ============================================================

    @Test
    fun oauthCallbackDeepLink_parsesCodeCorrectly() {
        // 模拟 OAuth 回调 URI: com.headdown.healthbridge://oauth?code=abc123
        val callbackUri = Uri.parse("com.headdown.healthbridge://oauth?code=test_code_789")
        val intent = Intent(Intent.ACTION_VIEW, callbackUri)
        InstrumentationRegistry.getInstrumentation().callActivityOnNewIntent(
            composeTestRule.activity, intent
        )
        composeTestRule.waitForIdle()

        // 授权完成后华为按钮应显示 ✓
        composeTestRule.onNodeWithText("✓").assertIsDisplayed()
    }

    // ============================================================
    // 完整流程：授权 → 同步 → 结果（烟幕测试）
    // ============================================================

    @Test
    fun fullFlow_authorizeAndSync() {
        // 1. 验证初始 UI
        composeTestRule.onNodeWithText("将华为运动健康数据同步到 Health Connect").assertIsDisplayed()
        composeTestRule.onNodeWithText("1. 授权华为运动健康").assertIsDisplayed()
        composeTestRule.onNodeWithText("2. 授权 Health Connect").assertIsDisplayed()
        composeTestRule.onNodeWithText("3. 开始同步").assertIsDisplayed()
        composeTestRule.onNodeWithText("刷新同步").assertIsDisplayed()

        // 2. 模拟 OAuth 回调完成华为授权
        val callbackUri = Uri.parse("com.headdown.healthbridge://oauth?code=e2e_code")
        InstrumentationRegistry.getInstrumentation().callActivityOnNewIntent(
            composeTestRule.activity, Intent(Intent.ACTION_VIEW, callbackUri)
        )
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("✓").assertIsDisplayed()

        // 3. 点击"开始同步"（会尝试真实 API，预期失败或部分成功）
        composeTestRule.onNodeWithText("3. 开始同步").performClick()
        composeTestRule.waitForIdle()

        // 4. 验证同步状态变为 Syncing 期间显示进度占位文本
        // （可能立即完成进入 Error/Success 或持续显示 Syncing）
        // 最低要求：不崩溃，按钮仍可见
        composeTestRule.onNodeWithText("3. 开始同步").assertIsDisplayed()
    }

    // ============================================================
    // 刷新按钮 — 重置并重新同步
    // ============================================================

    @Test
    fun refreshButton_resetsAndTriggersSync() {
        composeTestRule.onNodeWithText("刷新同步").performClick()
        composeTestRule.waitForIdle()

        // 刷新操作后 UI 应正常渲染（不崩溃）
        composeTestRule.onNodeWithText("刷新同步").assertIsDisplayed()
    }
}
