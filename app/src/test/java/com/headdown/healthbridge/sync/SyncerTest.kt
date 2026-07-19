package com.headdown.healthbridge.sync

import androidx.health.connect.client.HealthConnectClient
import com.headdown.healthbridge.data.AuthProvider
import com.headdown.healthbridge.data.HealthDataSource
import com.headdown.healthbridge.data.SleepRecord
import com.headdown.healthbridge.data.HeartRateData
import com.headdown.healthbridge.data.StepsData
import com.headdown.healthbridge.data.ExerciseRecord
import com.headdown.healthbridge.healthconnect.HealthConnectWriter
import io.mockk.*
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class SyncerTest {

    private val mockDataSource = mockk<HealthDataSource>()
    private val mockAuthProvider = mockk<AuthProvider>()
    private val mockHealthClient = mockk<HealthConnectClient>(relaxed = true)
    private val writer = HealthConnectWriter(mockHealthClient)

    private fun createSyncer() = Syncer(mockDataSource, mockAuthProvider, writer)

    // ============================================================
    // ensureValidAuth 测试
    // ============================================================

    @Test
    fun `token 有效时返回 true，不刷新`() = runTest {
        every { mockAuthProvider.isTokenValid() } returns true

        val result = createSyncer().ensureValidAuth()

        assertTrue(result)
        coVerify(exactly = 0) { mockAuthProvider.refreshToken() }
    }

    @Test
    fun `token 过期刷新成功后返回 true`() = runTest {
        every { mockAuthProvider.isTokenValid() } returns false
        coEvery { mockAuthProvider.refreshToken() } returns "new_token"

        val result = createSyncer().ensureValidAuth()

        assertTrue(result)
        coVerify(exactly = 1) { mockAuthProvider.refreshToken() }
    }

    @Test
    fun `token 过期刷新失败后返回 false`() = runTest {
        every { mockAuthProvider.isTokenValid() } returns false
        coEvery { mockAuthProvider.refreshToken() } returns null

        val result = createSyncer().ensureValidAuth()

        assertFalse(result)
        coVerify(exactly = 1) { mockAuthProvider.refreshToken() }
    }

    // ============================================================
    // performSync 测试
    // ============================================================

    @Test
    fun `performSync 并行查询 4 类数据并写入 Health Connect`() = runTest {
        val sleepData = listOf(SleepRecord(1710000000000L, 1710036000000L, 2))
        val hrData = listOf(HeartRateData(1710000000000L, 72.0f))
        val stepsData = listOf(StepsData(1710000000000L, 1710003600000L, 1200))
        val exerciseData = listOf(ExerciseRecord(1710000000000L, 1710001800000L, 1))

        coEvery { mockDataSource.querySleep(any(), any()) } returns sleepData
        coEvery { mockDataSource.queryHeartRate(any(), any()) } returns hrData
        coEvery { mockDataSource.querySteps(any(), any()) } returns stepsData
        coEvery { mockDataSource.queryExercise(any(), any()) } returns exerciseData

        createSyncer().performSync(1710000000000L, 1720000000000L)

        coVerify { mockDataSource.querySleep(1710000000000L, 1720000000000L) }
        coVerify { mockDataSource.queryHeartRate(1710000000000L, 1720000000000L) }
        coVerify { mockDataSource.querySteps(1710000000000L, 1720000000000L) }
        coVerify { mockDataSource.queryExercise(1710000000000L, 1720000000000L) }

        // HealthConnectWriter 内部调用 insertRecords，验证数据经过转换
        coVerify(atLeast = 1) { mockHealthClient.insertRecords(any()) }
    }

    @Test
    fun `performSync 部分数据为空不影响整体流程`() = runTest {
        coEvery { mockDataSource.querySleep(any(), any()) } returns emptyList()
        coEvery { mockDataSource.queryHeartRate(any(), any()) } returns emptyList()
        coEvery { mockDataSource.querySteps(any(), any()) } returns emptyList()
        coEvery { mockDataSource.queryExercise(any(), any()) } returns emptyList()

        // 不应抛异常
        createSyncer().performSync(1L, 2L)

        coVerify(exactly = 1) { mockDataSource.querySleep(any(), any()) }
        coVerify(exactly = 1) { mockDataSource.queryHeartRate(any(), any()) }
        coVerify(exactly = 1) { mockDataSource.querySteps(any(), any()) }
        coVerify(exactly = 1) { mockDataSource.queryExercise(any(), any()) }
    }

    @Test
    fun `performSync 查询异常时向上传播（由 SyncWorker 处理重试）`() = runTest {
        coEvery { mockDataSource.querySleep(any(), any()) } throws RuntimeException("network error")
        // 其他查询正常
        coEvery { mockDataSource.queryHeartRate(any(), any()) } returns emptyList()
        coEvery { mockDataSource.querySteps(any(), any()) } returns emptyList()
        coEvery { mockDataSource.queryExercise(any(), any()) } returns emptyList()

        assertThrows(RuntimeException::class.java) {
            runTest {
                createSyncer().performSync(1L, 2L)
            }
        }
    }
}
