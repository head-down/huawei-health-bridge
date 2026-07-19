package com.headdown.healthbridge.data

/**
 * 健康数据源接口
 *
 * 抽象华为健康数据读取，开发期用 Mock 实现，凭证就位后替换真实 REST API 客户端。
 * startTime/endTime 为毫秒级时间戳。
 */
interface HealthDataSource {

    /** 查询睡眠数据 */
    suspend fun querySleep(startTime: Long, endTime: Long): List<SleepRecord>

    /** 查询心率数据 */
    suspend fun queryHeartRate(startTime: Long, endTime: Long): List<HeartRateData>

    /** 查询步数数据 */
    suspend fun querySteps(startTime: Long, endTime: Long): List<StepsData>

    /** 查询运动记录 */
    suspend fun queryExercise(startTime: Long, endTime: Long): List<ExerciseRecord>
}
