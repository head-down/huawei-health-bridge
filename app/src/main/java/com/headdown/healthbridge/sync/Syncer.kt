package com.headdown.healthbridge.sync

import com.headdown.healthbridge.data.AuthProvider
import com.headdown.healthbridge.data.HealthDataSource
import com.headdown.healthbridge.healthconnect.HealthConnectWriter
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch

/**
 * 同步核心业务逻辑，不依赖 Android 框架，便于通过 Mock 接口测试。
 *
 * SyncWorker 持有该类实例，负责检查点持久化和 WorkManager 生命周期；
 * 本类专注于数据同步流程和认证管理。
 */
class Syncer(
    private val dataSource: HealthDataSource,
    private val authProvider: AuthProvider,
    private val writer: HealthConnectWriter
) {

    /**
     * 确保 Access Token 有效，过期则刷新一次。
     *
     * @return true 表示 token 可用，false 表示刷新失败
     */
    suspend fun ensureValidAuth(): Boolean {
        if (authProvider.isTokenValid()) return true
        val newToken = authProvider.refreshToken()
        return newToken != null
    }

    /**
     * 执行一次同步：并行查询 4 类健康数据并写入 Health Connect。
     *
     * 调用方负责计算时间窗口和检查点持久化。
     */
    suspend fun performSync(startTimeMs: Long, endTimeMs: Long) = coroutineScope {
        launch { writer.writeSleepRecords(dataSource.querySleep(startTimeMs, endTimeMs)) }
        launch { writer.writeHeartRateRecords(dataSource.queryHeartRate(startTimeMs, endTimeMs)) }
        launch { writer.writeStepsRecords(dataSource.querySteps(startTimeMs, endTimeMs)) }
        launch { writer.writeExerciseRecords(dataSource.queryExercise(startTimeMs, endTimeMs)) }
    }
}
