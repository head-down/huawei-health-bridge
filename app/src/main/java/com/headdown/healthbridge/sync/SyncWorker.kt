package com.headdown.healthbridge.sync

import android.content.Context
import androidx.health.connect.client.HealthConnectClient
import androidx.work.*
import com.headdown.healthbridge.data.AuthProvider
import com.headdown.healthbridge.healthconnect.HealthConnectWriter
import com.headdown.healthbridge.huawei.HuaweiHealthClient
import kotlinx.coroutines.coroutineScope
import java.util.concurrent.TimeUnit

/**
 * WorkManager 后台同步 Worker。
 *
 * 每次触发时：确保认证有效 → 计算增量窗口 → 拉取华为数据 → 写入 Health Connect → 保存检查点。
 * token 过期自动刷新一次；失败不阻塞后续运行。
 */
class SyncWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    companion object {
        private const val PREFS_NAME = "sync_prefs"
        private const val KEY_LAST_SYNC = "last_synced_at"
        private const val SYNC_WINDOW_DAYS = 30L
    }

    override suspend fun doWork(): Result = coroutineScope {
        val dataSource = HuaweiHealthClient(applicationContext)
        val healthClient = HealthConnectClient.getOrCreate(applicationContext)
        val writer = HealthConnectWriter(healthClient)

        // 生产环境：华为客户端自行管理 token，AuthProvider 始终有效
        val authProvider = object : AuthProvider {
            override suspend fun getToken() = "internal"
            override fun isTokenValid() = true
            override suspend fun refreshToken() = "internal"
        }

        val syncer = Syncer(dataSource, authProvider, writer)

        try {
            // Step 1: 确保认证有效（401 时自动刷新一次）
            if (!syncer.ensureValidAuth()) {
                return@coroutineScope Result.success() // 不阻塞后续同步
            }

            // Step 2: 计算同步窗口
            val prefs = applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            val lastSyncedAt = prefs.getLong(KEY_LAST_SYNC, 0L)
            val endTime = System.currentTimeMillis()
            val thirtyDaysAgo = endTime - TimeUnit.DAYS.toMillis(SYNC_WINDOW_DAYS)
            val startTime = if (lastSyncedAt > 0) lastSyncedAt else thirtyDaysAgo

            // Step 3: 执行同步
            syncer.performSync(startTime, endTime)

            // Step 4: 保存检查点
            prefs.edit().putLong(KEY_LAST_SYNC, endTime).apply()

            Result.success()
        } catch (e: Exception) {
            Result.retry()
        }
    }

    /** 清除同步检查点（调试用） */
    fun clearCheckpoint() {
        applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit().remove(KEY_LAST_SYNC).apply()
    }
}
