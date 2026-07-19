package com.headdown.healthbridge.data

import android.content.Context
import java.util.concurrent.TimeUnit

/**
 * 同步检查点持久化。
 * 封装 SharedPreferences 操作，避免裸 Long + 字符串键散落各处。
 */
class SyncPreferences(context: Context) {

    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    /** 获取上次同步的时间戳（毫秒），无记录返回 0 */
    fun getLastSyncTime(): Long = prefs.getLong(KEY_LAST_SYNCED_AT, 0L)

    /** 保存本次同步时间戳 */
    fun saveSyncTime(endTimeMs: Long) {
        prefs.edit().putLong(KEY_LAST_SYNCED_AT, endTimeMs).apply()
    }

    /** 清除同步检查点（调试用） */
    fun clearSyncTime() {
        prefs.edit().remove(KEY_LAST_SYNCED_AT).apply()
    }

    /** 计算同步窗口起始时间：最近一次同步时间，或 30 天前 */
    fun computeStartTime(endTimeMs: Long): Long {
        val lastSync = getLastSyncTime()
        val thirtyDaysAgo = endTimeMs - TimeUnit.DAYS.toMillis(30)
        return if (lastSync > 0) lastSync else thirtyDaysAgo
    }

    companion object {
        private const val PREFS_NAME = "sync_prefs"
        private const val KEY_LAST_SYNCED_AT = "last_synced_at"
    }
}
