package com.headdown.healthbridge.data.mock

import com.headdown.healthbridge.data.*

/**
 * Mock 健康数据源，返回结构正确的假数据。
 *
 * 数据基于华为 Health Kit API 官方文档示例，覆盖全部四种健康数据类型。
 */
class MockHealthDataSource : HealthDataSource {

    override suspend fun querySleep(startTime: Long, endTime: Long): List<SleepRecord> = listOf(
        SleepRecord(
            startTime = 1710000000000L,
            endTime   = 1710036000000L,
            sleepState = 1  // 浅睡
        ),
        SleepRecord(
            startTime = 1710036000000L,
            endTime   = 1710043200000L,
            sleepState = 2  // 深睡
        ),
        SleepRecord(
            startTime = 1710043200000L,
            endTime   = 1710046800000L,
            sleepState = 3  // REM
        )
    )

    override suspend fun queryHeartRate(startTime: Long, endTime: Long): List<HeartRateData> = listOf(
        HeartRateData(timestamp = 1710000000000L, bpm = 72.0f),
        HeartRateData(timestamp = 1710000600000L, bpm = 75.5f),
        HeartRateData(timestamp = 1710001200000L, bpm = 68.0f)
    )

    override suspend fun querySteps(startTime: Long, endTime: Long): List<StepsData> = listOf(
        StepsData(startTime = 1710000000000L, endTime = 1710003600000L, steps = 1200),
        StepsData(startTime = 1710003600000L, endTime = 1710007200000L, steps = 850)
    )

    override suspend fun queryExercise(startTime: Long, endTime: Long): List<ExerciseRecord> = listOf(
        ExerciseRecord(
            startTime = 1710000000000L,
            endTime   = 1710001800000L,
            exerciseType = 1  // 跑步
        ),
        ExerciseRecord(
            startTime = 1710003600000L,
            endTime   = 1710005400000L,
            exerciseType = 3  // 骑车
        )
    )
}
