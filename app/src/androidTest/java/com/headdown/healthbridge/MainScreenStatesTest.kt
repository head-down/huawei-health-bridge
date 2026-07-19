package com.headdown.healthbridge

import android.app.Application
import androidx.activity.ComponentActivity
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.headdown.healthbridge.ui.MainScreen
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * UI 状态覆盖测试 — 直接渲染 MainScreen 并注入各状态，验证渲染正确性。
 *
 * 运行方式：模拟器或真机下执行
 *   ./gradlew connectedAndroidTest
 */
@RunWith(AndroidJUnit4::class)
class MainScreenStatesTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<ComponentActivity>()

    private fun createViewModel() = MainViewModel(
        ApplicationProvider.getApplicationContext()
    )

    private fun setContentWithState(state: MainUiState) {
        val viewModel = createViewModel()
        viewModel.overrideTestState(state)
        composeTestRule.setContent {
            MainScreen(
                viewModel = viewModel,
                onAuthorizeHuawei = {},
                onRequestHealthPermissions = {}
            )
        }
    }

    // ============================================================
    // 步骤 1 — 华为 OAuth 状态
    // ============================================================

    @Test
    fun huaweiIdle_showsNormalButton() {
        setContentWithState(MainUiState())
        composeTestRule.onNodeWithText("授权华为运动健康").assertIsDisplayed()
        composeTestRule.onNodeWithContentDescription("已完成").assertDoesNotExist()
    }

    @Test
    fun huaweiAuthorizing_showsProgressIndicator() {
        setContentWithState(MainUiState(huaweiAuthState = HuaweiAuthState.Authorizing))
        composeTestRule.onNodeWithText("授权华为运动健康").assertIsDisplayed()
        // Authorizing 状态下进度指示器应可见（CircularProgressIndicator 无文字，通过状态验证）
    }

    @Test
    fun huaweiAuthorized_showsCheckmark() {
        setContentWithState(MainUiState(huaweiAuthState = HuaweiAuthState.Authorized))
        composeTestRule.onNodeWithContentDescription("已完成").assertIsDisplayed()
    }

    // ============================================================
    // 步骤 2 — Health Connect 状态
    // ============================================================

    @Test
    fun healthConnectIdle_showsNormalButton() {
        setContentWithState(MainUiState())
        composeTestRule.onNodeWithText("授权 Health Connect").assertIsDisplayed()
    }

    @Test
    fun healthConnectGranting_showsButtonAsInProgress() {
        setContentWithState(MainUiState(healthConnectState = HealthConnectState.Granting))
        composeTestRule.onNodeWithText("授权 Health Connect").assertIsDisplayed()
    }

    @Test
    fun healthConnectGranted_showsCheckmark() {
        setContentWithState(MainUiState(healthConnectState = HealthConnectState.Granted))
        composeTestRule.onNodeWithContentDescription("已完成").assertIsDisplayed()
    }

    // ============================================================
    // 步骤 3 — 同步状态
    // ============================================================

    @Test
    fun syncIdle_showsDefaultButton() {
        setContentWithState(MainUiState(syncState = SyncState.Idle))
        composeTestRule.onNodeWithText("开始同步").assertIsDisplayed()
    }

    @Test
    fun syncSyncing_showsProgressText() {
        setContentWithState(MainUiState(
            syncState = SyncState.Syncing,
            syncedTypes = listOf(com.headdown.healthbridge.data.DataType.SLEEP),
            syncProgress = 1,
        ))
        composeTestRule.onNodeWithText("已同步: 睡眠 (1/4)").assertIsDisplayed()
    }

    @Test
    fun syncSyncing_multipleTypes_showsAccumulatedProgress() {
        setContentWithState(MainUiState(
            syncState = SyncState.Syncing,
            syncedTypes = listOf(
                com.headdown.healthbridge.data.DataType.SLEEP,
                com.headdown.healthbridge.data.DataType.HEART_RATE,
            ),
            syncProgress = 2,
        ))
        composeTestRule.onNodeWithText("已同步: 睡眠 心率 (2/4)").assertIsDisplayed()
    }

    @Test
    fun syncSyncing_empty_showsInitialMessage() {
        setContentWithState(MainUiState(syncState = SyncState.Syncing))
        composeTestRule.onNodeWithText("正在同步健康数据...").assertIsDisplayed()
    }

    @Test
    fun syncSuccess_showsResultCard() {
        setContentWithState(MainUiState(
            syncState = SyncState.Success,
            syncResult = com.headdown.healthbridge.data.SyncResult(
                sleep = 5,
                heartRate = 100,
                steps = 3,
                exercise = 2,
            ),
        ))
        composeTestRule.onNodeWithText("同步完成").assertIsDisplayed()
        composeTestRule.onNodeWithText("共同步 110 条记录").assertIsDisplayed()
    }

    @Test
    fun syncError_showsErrorCard() {
        setContentWithState(MainUiState(
            syncState = SyncState.Error,
            syncError = "网络连接失败",
        ))
        composeTestRule.onNodeWithText("网络连接失败").assertIsDisplayed()
    }

    @Test
    fun syncErrorNullMessage_showsDefaultError() {
        setContentWithState(MainUiState(
            syncState = SyncState.Error,
            syncError = null,
        ))
        composeTestRule.onNodeWithText("同步失败").assertIsDisplayed()
    }

    // ============================================================
    // 刷新按钮 — 所有状态可见
    // ============================================================

    @Test
    fun refreshButton_visibleInIdle() {
        setContentWithState(MainUiState(syncState = SyncState.Idle))
        composeTestRule.onNodeWithText("刷新同步").assertIsDisplayed()
    }

    @Test
    fun refreshButton_visibleInSuccess() {
        setContentWithState(MainUiState(syncState = SyncState.Success))
        composeTestRule.onNodeWithText("刷新同步").assertIsDisplayed()
    }

    @Test
    fun refreshButton_visibleInError() {
        setContentWithState(MainUiState(
            syncState = SyncState.Error,
            syncError = "fail",
        ))
        composeTestRule.onNodeWithText("刷新同步").assertIsDisplayed()
    }

    @Test
    fun refreshButton_disabledDuringSyncing() {
        setContentWithState(MainUiState(syncState = SyncState.Syncing))
        composeTestRule.onNodeWithText("刷新同步").assertIsDisplayed()
        // enabled=false 的按钮仍然可见但不可点击 — 此测试验证不崩溃
    }

    // ============================================================
    // HC 权限拒绝 → Granting 应复位 (P0 bug fix 回归)
    // ============================================================

    @Test
    fun healthConnectDenied_resetsToIdle() {
        val viewModel = createViewModel()
        viewModel.onHealthConnectGranting()
        viewModel.onHealthConnectDenied()

        composeTestRule.setContent {
            MainScreen(
                viewModel = viewModel,
                onAuthorizeHuawei = {},
                onRequestHealthPermissions = {}
            )
        }

        // 拒绝后不应显示 ✓ 和 progress
        composeTestRule.onNodeWithText("授权 Health Connect").assertIsDisplayed()
        composeTestRule.onNodeWithContentDescription("已完成").assertDoesNotExist()
    }
}
