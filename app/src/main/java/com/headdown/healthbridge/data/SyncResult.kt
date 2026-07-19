package com.headdown.healthbridge.data

/**
 * 单次同步的结果统计，记录各类型数据同步的条数。
 */
data class SyncResult(
    val sleep: Int = 0,
    val heartRate: Int = 0,
    val steps: Int = 0,
    val exercise: Int = 0
) {
    val total: Int get() = sleep + heartRate + steps + exercise

    val hasData: Boolean get() = total > 0
}
