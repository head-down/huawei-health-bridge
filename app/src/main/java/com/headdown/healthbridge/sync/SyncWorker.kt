package com.headdown.healthbridge.sync

import android.content.Context
import androidx.health.connect.client.HealthConnectClient
import androidx.work.*
import com.headdown.healthbridge.huawei.HuaweiHealthClient
import com.headdown.healthbridge.healthconnect.HealthConnectWriter
import kotlinx.coroutines.*
import java.util.concurrent.TimeUnit

/**
 * WorkManager 后台同步任务
 * 每天执行一次，从华为云端拉数据写入 Health Connect
 */
class SyncWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result = coroutineScope {
        try {
            val huaweiClient = HuaweiHealthClient(applicationContext)
            val healthClient = HealthConnectClient.getOrCreate(applicationContext)
            val writer = HealthConnectWriter(healthClient)

            // 同步最近 3 天的数据
            val endTime = System.currentTimeMillis()
            val startTime = endTime - TimeUnit.DAYS.toMillis(3)

            // 拉取各类型数据并写入 Health Connect
            launch { writer.writeSleepRecords(huaweiClient.querySleep(startTime, endTime)) }
            launch { writer.writeHeartRateRecords(huaweiClient.queryHeartRate(startTime, endTime)) }
            launch { writer.writeStepsRecords(huaweiClient.querySteps(startTime, endTime)) }
            launch { writer.writeExerciseRecords(huaweiClient.queryExercise(startTime, endTime)) }
            launch { writer.writeWeightRecords(huaweiClient.queryWeight(startTime, endTime)) }
            launch { writer.writeSpO2Records(huaweiClient.querySpO2(startTime, endTime)) }
            launch { writer.writeTemperatureRecords(huaweiClient.queryTemperature(startTime, endTime)) }

            Result.success()
        } catch (e: Exception) {
            if (runAttemptCount < 3) {
                Result.retry()
            } else {
                Result.failure()
            }
        }
    }
}
