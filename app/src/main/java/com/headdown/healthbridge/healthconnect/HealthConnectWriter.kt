package com.headdown.healthbridge.healthconnect

import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.records.*
import androidx.health.connect.client.units.*
import com.headdown.healthbridge.huawei.HuaweiHealthClient
import java.time.Instant
import java.time.ZoneOffset

/**
 * 将华为健康数据写入 Google Health Connect
 */
class HealthConnectWriter(private val client: HealthConnectClient) {

    // ============================================================
    // 睡眠
    // ============================================================

    suspend fun writeSleepRecords(sleepList: List<HuaweiHealthClient.SleepData>) {
        val records = sleepList.map { sleep ->
            val stageType = when (sleep.sleepState) {
                1 -> SleepSessionRecord.STAGE_TYPE_LIGHT
                2 -> SleepSessionRecord.STAGE_TYPE_DEEP
                3 -> SleepSessionRecord.STAGE_TYPE_REM
                else -> SleepSessionRecord.STAGE_TYPE_AWAKE
            }

            SleepSessionRecord(
                startTime = Instant.ofEpochMilli(sleep.startTime),
                startZoneOffset = ZoneOffset.UTC,
                endTime = Instant.ofEpochMilli(sleep.endTime),
                endZoneOffset = ZoneOffset.UTC,
                stages = listOf(
                    SleepSessionRecord.Stage(
                        startTime = Instant.ofEpochMilli(sleep.startTime),
                        endTime = Instant.ofEpochMilli(sleep.endTime),
                        stage = stageType
                    )
                )
            )
        }
        client.insertRecords(records)
    }

    // ============================================================
    // 心率
    // ============================================================

    suspend fun writeHeartRateRecords(hrList: List<HuaweiHealthClient.HeartRateData>) {
        val records = hrList.map { hr ->
            HeartRateRecord(
                startTime = Instant.ofEpochMilli(hr.timestamp),
                startZoneOffset = ZoneOffset.UTC,
                endTime = Instant.ofEpochMilli(hr.timestamp),
                endZoneOffset = ZoneOffset.UTC,
                samples = listOf(
                    HeartRateRecord.Sample(
                        time = Instant.ofEpochMilli(hr.timestamp),
                        beatsPerMinute = hr.bpm.toLong()
                    )
                )
            )
        }
        client.insertRecords(records)
    }

    // ============================================================
    // 步数
    // ============================================================

    suspend fun writeStepsRecords(stepsList: List<HuaweiHealthClient.StepsData>) {
        val records = stepsList.map { step ->
            StepsRecord(
                startTime = Instant.ofEpochMilli(step.startTime),
                startZoneOffset = ZoneOffset.UTC,
                endTime = Instant.ofEpochMilli(step.endTime),
                endZoneOffset = ZoneOffset.UTC,
                count = step.steps.toLong()
            )
        }
        client.insertRecords(records)
    }

    // ============================================================
    // 运动
    // ============================================================

    suspend fun writeExerciseRecords(exerciseList: List<HuaweiHealthClient.ExerciseData>) {
        val records = exerciseList.map { ex ->
            ExerciseSessionRecord(
                startTime = Instant.ofEpochMilli(ex.startTime),
                startZoneOffset = ZoneOffset.UTC,
                endTime = Instant.ofEpochMilli(ex.endTime),
                endZoneOffset = ZoneOffset.UTC,
                exerciseType = mapHuaweiExerciseType(ex.exerciseType)
            )
        }
        client.insertRecords(records)
    }

    /** 华为运动类型 → Health Connect 运动类型 */
    private fun mapHuaweiExerciseType(huaweiType: Int): Int {
        return when (huaweiType) {
            1 -> ExerciseSessionRecord.EXERCISE_TYPE_RUNNING
            2 -> ExerciseSessionRecord.EXERCISE_TYPE_WALKING
            3 -> ExerciseSessionRecord.EXERCISE_TYPE_BIKING
            4 -> ExerciseSessionRecord.EXERCISE_TYPE_SWIMMING_OPEN_WATER
            5 -> ExerciseSessionRecord.EXERCISE_TYPE_HIKING
            6 -> ExerciseSessionRecord.EXERCISE_TYPE_YOGA
            7 -> ExerciseSessionRecord.EXERCISE_TYPE_STRENGTH_TRAINING
            8 -> ExerciseSessionRecord.EXERCISE_TYPE_BADMINTON
            9 -> ExerciseSessionRecord.EXERCISE_TYPE_BASKETBALL
            10 -> ExerciseSessionRecord.EXERCISE_TYPE_TABLE_TENNIS
            11 -> ExerciseSessionRecord.EXERCISE_TYPE_VOLLEYBALL
            12 -> ExerciseSessionRecord.EXERCISE_TYPE_HIGH_INTENSITY_INTERVAL_TRAINING
            else -> ExerciseSessionRecord.EXERCISE_TYPE_OTHER_WORKOUT
        }
    }

    // ============================================================
    // 体重
    // ============================================================

    suspend fun writeWeightRecords(weightList: List<HuaweiHealthClient.WeightData>) {
        val records = weightList.map { w ->
            WeightRecord(
                time = Instant.ofEpochMilli(w.timestamp),
                zoneOffset = ZoneOffset.UTC,
                weight = w.weightKg.kilograms
            )
        }
        client.insertRecords(records)
    }

    // ============================================================
    // 血氧
    // ============================================================

    suspend fun writeSpO2Records(spo2List: List<HuaweiHealthClient.SpO2Data>) {
        val records = spo2List.map { s ->
            OxygenSaturationRecord(
                time = Instant.ofEpochMilli(s.timestamp),
                zoneOffset = ZoneOffset.UTC,
                percentage = s.saturation.toDouble().percent
            )
        }
        client.insertRecords(records)
    }

    // ============================================================
    // 体温
    // ============================================================

    suspend fun writeTemperatureRecords(tempList: List<HuaweiHealthClient.TemperatureData>) {
        val records = tempList.map { t ->
            BodyTemperatureRecord(
                time = Instant.ofEpochMilli(t.timestamp),
                zoneOffset = ZoneOffset.UTC,
                temperature = t.celsius.toDouble().celsius
            )
        }
        client.insertRecords(records)
    }
}
