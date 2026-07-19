package com.headdown.healthbridge.sync

import com.headdown.healthbridge.data.AuthProvider
import com.headdown.healthbridge.data.DataType
import com.headdown.healthbridge.data.HealthDataSource
import com.headdown.healthbridge.data.SyncResult
import com.headdown.healthbridge.healthconnect.HealthConnectWriter
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope

/**
 * 同步进度回调，每完成一种数据类型后调用。
 */
fun interface SyncProgress {
    suspend fun onTypeSynced(type: DataType)
}

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
     *
     * @param progress 可选，每完成一种数据类型后回调 typeName
     * @return 各类型数据同步的条数统计
     */
    suspend fun performSync(
        startTimeMs: Long,
        endTimeMs: Long,
        progress: SyncProgress? = null
    ): SyncResult = coroutineScope {
        val sleepDeferred = async {
            syncType(DataType.SLEEP, dataSource::querySleep, writer::writeSleepRecords, startTimeMs, endTimeMs, progress)
        }
        val hrDeferred = async {
            syncType(DataType.HEART_RATE, dataSource::queryHeartRate, writer::writeHeartRateRecords, startTimeMs, endTimeMs, progress)
        }
        val stepsDeferred = async {
            syncType(DataType.STEPS, dataSource::querySteps, writer::writeStepsRecords, startTimeMs, endTimeMs, progress)
        }
        val exerciseDeferred = async {
            syncType(DataType.EXERCISE, dataSource::queryExercise, writer::writeExerciseRecords, startTimeMs, endTimeMs, progress)
        }

        SyncResult(
            sleep = sleepDeferred.await(),
            heartRate = hrDeferred.await(),
            steps = stepsDeferred.await(),
            exercise = exerciseDeferred.await()
        )
    }

    private suspend fun <T> syncType(
        type: DataType,
        query: suspend (Long, Long) -> List<T>,
        write: suspend (List<T>) -> Unit,
        startTimeMs: Long,
        endTimeMs: Long,
        progress: SyncProgress?
    ): Int {
        val data = query(startTimeMs, endTimeMs)
        if (data.isNotEmpty()) write(data)
        return data.size.also { progress?.onTypeSynced(type) }
    }
}
