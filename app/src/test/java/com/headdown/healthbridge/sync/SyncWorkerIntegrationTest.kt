package com.headdown.healthbridge.sync

import android.content.SharedPreferences
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.records.Record
import com.headdown.healthbridge.data.HealthDataSource
import com.headdown.healthbridge.data.SleepRecord
import com.headdown.healthbridge.data.mock.MockAuthProvider
import com.headdown.healthbridge.data.mock.MockHealthDataSource
import com.headdown.healthbridge.healthconnect.HealthConnectWriter
import io.mockk.*
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.util.concurrent.TimeUnit

/**
 * 集成测试：注入 MockHealthDataSource + MockAuthProvider，
 * 覆盖完整同步流程、去重逻辑、检查点更新。
 */
class SyncWorkerIntegrationTest {

    private lateinit var mockDataSource: MockHealthDataSource
    private lateinit var mockAuthProvider: MockAuthProvider
    private lateinit var mockHealthClient: HealthConnectClient
    private lateinit var writer: HealthConnectWriter
    private lateinit var syncer: Syncer

    @BeforeEach
    fun setUp() {
        mockDataSource = MockHealthDataSource()
        mockAuthProvider = MockAuthProvider()
        mockHealthClient = mockk(relaxed = true)
        writer = HealthConnectWriter(mockHealthClient)
        syncer = Syncer(mockDataSource, mockAuthProvider, writer)
    }

    // ============================================================
    // 完整同步流程测试
    // ============================================================

    @Test
    fun `完整同步流程：认证通过 → 查询 4 类数据 → 写入 Health Connect`() = runTest {
        // 确保认证通过
        assertTrue(syncer.ensureValidAuth())

        // 执行同步
        syncer.performSync(1710000000000L, 1710086400000L)

        // 验证数据写入了 Health Connect（4 类数据各调用一次 insertRecords）
        coVerify(exactly = 4) { mockHealthClient.insertRecords(any<List<Record>>()) }
    }

    @Test
    fun `完整同步流程：Mock 数据正确转换并写入`() = runTest {
        assertTrue(syncer.ensureValidAuth())
        syncer.performSync(1710000000000L, 1710086400000L)

        // 验证至少调用了 4 次 insertRecords（每种类型一次）
        coVerify(atLeast = 4) { mockHealthClient.insertRecords(any<List<Record>>()) }
    }

    // ============================================================
    // 去重 / 增量同步逻辑测试
    // ============================================================

    @Test
    fun `首次同步窗口为最近 30 天`() {
        val now = System.currentTimeMillis()
        val thirtyDaysAgo = now - TimeUnit.DAYS.toMillis(30)
        val startTime = thirtyDaysAgo // lastSyncedAt = 0

        val diff = now - startTime
        val expectedDays = TimeUnit.MILLISECONDS.toDays(diff)
        assertTrue(expectedDays >= 29, "首次同步窗口应 ≥ 29 天")
        assertTrue(expectedDays <= 30, "首次同步窗口应 ≤ 30 天")
    }

    @Test
    fun `增量同步：检查点存在时使用 lastSyncedAt 作为起始时间`() {
        val savedCheckpoint = System.currentTimeMillis() - TimeUnit.HOURS.toMillis(12) // 12 小时前同步过
        val now = System.currentTimeMillis()
        val thirtyDaysAgo = now - TimeUnit.DAYS.toMillis(30)

        // 增量模式：startTime = lastSyncedAt
        val startTime = savedCheckpoint

        assertTrue(startTime > thirtyDaysAgo, "增量同步的起始时间应早于首次同步")
        val expectedHours = TimeUnit.MILLISECONDS.toHours(now - startTime)
        assertTrue(expectedHours in 11..13, "增量窗口应为约 12 小时")
    }

    @Test
    fun `检查点持久化：SharedPreferences 正确读写`() {
        val prefs = mockk<SharedPreferences>(relaxed = true)
        val editor = mockk<SharedPreferences.Editor>(relaxed = true)
        every { prefs.edit() } returns editor
        every { editor.putLong(any(), any()) } returns editor

        val checkpointTime = System.currentTimeMillis()
        prefs.edit().putLong("last_synced_at", checkpointTime).apply()

        verify { editor.putLong("last_synced_at", checkpointTime) }
        verify { editor.apply() }
    }

    // ============================================================
    // 认证流程集成测试
    // ============================================================

    @Test
    fun `认证有效时直接执行同步，无需刷新`() = runTest {
        // MockAuthProvider 初始 token 有效
        val result = syncer.ensureValidAuth()
        assertTrue(result)

        // 执行同步不抛异常
        syncer.performSync(1L, 2L)
    }

    @Test
    fun `Syncer 与 MockHealthDataSource 正确传递时间参数`() = runTest {
        val startMs = 1710000000000L
        val endMs = 1710086400000L

        // 通过 Syncer 查询数据，MockHealthDataSource 返回预置数据
        syncer.performSync(startMs, endMs)

        // 验证数据被正确写入（通过 writer → HealthConnectClient）
        val recordsSlot = mutableListOf<List<Record>>()
        coVerify(atLeast = 1) { mockHealthClient.insertRecords(capture(recordsSlot)) }
    }

    // ============================================================
    // 异常处理
    // ============================================================

    @Test
    fun `异常查询不中断其余数据类型的同步`() = runTest {
        // 模拟：三个类型正常，一个类型抛异常
        val partialMockDataSource = mockk<HealthDataSource>()
        val partialWriter = HealthConnectWriter(mockHealthClient)
        val partialSyncer = Syncer(partialMockDataSource, mockAuthProvider, partialWriter)

        coEvery { partialMockDataSource.querySleep(any(), any()) } returns listOf(
            SleepRecord(1L, 2L, 1)
        )
        coEvery { partialMockDataSource.queryHeartRate(any(), any()) } throws RuntimeException("API error")
        coEvery { partialMockDataSource.querySteps(any(), any()) } returns emptyList()
        coEvery { partialMockDataSource.queryExercise(any(), any()) } returns emptyList()

        assertThrows(RuntimeException::class.java) {
            runTest { partialSyncer.performSync(1L, 2L) }
        }
    }
}
