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

/** 睡眠数据（原始数据，非聚合） */
data class SleepData(
    val startTime: Long,
    val endTime: Long,
    val sleepState: Int
)

/** 体重记录 */
data class WeightData(
    val timestamp: Long,  // 毫秒时间戳
    val weightKg: Float
)

/** 血氧饱和度记录 */
data class SpO2Data(
    val timestamp: Long,  // 毫秒时间戳
    val saturation: Float
)

/** 体温记录 */
data class TemperatureData(
    val timestamp: Long,  // 毫秒时间戳
    val celsius: Float
)
