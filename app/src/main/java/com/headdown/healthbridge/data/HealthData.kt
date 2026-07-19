package com.headdown.healthbridge.data

/** 睡眠记录 */
data class SleepRecord(
    val startTime: Long,  // 毫秒时间戳
    val endTime: Long,    // 毫秒时间戳
    val sleepState: Int   // 0=清醒, 1=浅睡, 2=深睡, 3=REM
)

/** 心率记录 */
data class HeartRateData(
    val timestamp: Long,  // 毫秒时间戳
    val bpm: Float
)

/** 步数增量记录（按间隔） */
data class StepsData(
    val startTime: Long,  // 毫秒时间戳
    val endTime: Long,    // 毫秒时间戳
    val steps: Int
)

/** 运动记录 */
data class ExerciseRecord(
    val startTime: Long,  // 毫秒时间戳
    val endTime: Long,    // 毫秒时间戳
    val exerciseType: Int // 运动类型代码
)
