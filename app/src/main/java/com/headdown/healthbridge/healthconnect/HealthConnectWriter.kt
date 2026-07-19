package com.headdown.healthbridge.healthconnect

import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.records.*
import androidx.health.connect.client.units.*
import com.headdown.healthbridge.data.ExerciseRecord
import com.headdown.healthbridge.data.HeartRateData
import com.headdown.healthbridge.data.SleepRecord
import com.headdown.healthbridge.data.SpO2Data
import com.headdown.healthbridge.data.StepsData
import com.headdown.healthbridge.data.TemperatureData
import com.headdown.healthbridge.data.WeightData
import java.time.Instant
import java.time.ZoneOffset

/**
 * 将华为健康数据写入 Google Health Connect
 */
class HealthConnectWriter(private val client: HealthConnectClient) {

    private fun Long.toUtcInstant() = Instant.ofEpochMilli(this)
    private val utc = ZoneOffset.UTC

    // ============================================================
    // 睡眠
    // ============================================================

    suspend fun writeSleepRecords(sleepList: List<SleepRecord>) {
        val records = sleepList.map { sleep ->
            val stageType = when (sleep.sleepState) {
                1 -> SleepSessionRecord.STAGE_TYPE_LIGHT
                2 -> SleepSessionRecord.STAGE_TYPE_DEEP
                3 -> SleepSessionRecord.STAGE_TYPE_REM
                else -> SleepSessionRecord.STAGE_TYPE_AWAKE
            }

            SleepSessionRecord(
                startTime = sleep.startTime.toUtcInstant(),
                startZoneOffset = utc,
                endTime = sleep.endTime.toUtcInstant(),
                endZoneOffset = utc,
                stages = listOf(
                    SleepSessionRecord.Stage(
                        startTime = sleep.startTime.toUtcInstant(),
                        endTime = sleep.endTime.toUtcInstant(),
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

    suspend fun writeHeartRateRecords(hrList: List<HeartRateData>) {
        val records = hrList.map { hr ->
            HeartRateRecord(
                startTime = hr.timestamp.toUtcInstant(),
                startZoneOffset = utc,
                endTime = hr.timestamp.toUtcInstant(),
                endZoneOffset = utc,
                samples = listOf(
                    HeartRateRecord.Sample(
                        time = hr.timestamp.toUtcInstant(),
                        beatsPerMinute = Math.round(hr.bpm.toDouble()).toLong()
                    )
                )
            )
        }
        client.insertRecords(records)
    }

    // ============================================================
    // 步数
    // ============================================================

    suspend fun writeStepsRecords(stepsList: List<StepsData>) {
        val records = stepsList.map { step ->
            StepsRecord(
                startTime = step.startTime.toUtcInstant(),
                startZoneOffset = utc,
                endTime = step.endTime.toUtcInstant(),
                endZoneOffset = utc,
                count = step.steps.toLong()
            )
        }
        client.insertRecords(records)
    }

    // ============================================================
    // 运动
    // ============================================================

    suspend fun writeExerciseRecords(exerciseList: List<ExerciseRecord>) {
        val records = exerciseList.map { ex ->
            ExerciseSessionRecord(
                startTime = ex.startTime.toUtcInstant(),
                startZoneOffset = utc,
                endTime = ex.endTime.toUtcInstant(),
                endZoneOffset = utc,
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
            else -> ExerciseSessionRecord.EXERCISE_TYPE_OTHER_WORKOUT
        }
    }

    // ============================================================
    // 体重
    // ============================================================

    suspend fun writeWeightRecords(weightList: List<WeightData>) {
        val records = weightList.map { w ->
            WeightRecord(
                time = w.timestamp.toUtcInstant(),
                zoneOffset = utc,
                weight = w.weightKg.kilograms
            )
        }
        client.insertRecords(records)
    }

    // ============================================================
    // 血氧
    // ============================================================

    suspend fun writeSpO2Records(spo2List: List<SpO2Data>) {
        val records = spo2List.map { s ->
            OxygenSaturationRecord(
                time = s.timestamp.toUtcInstant(),
                zoneOffset = utc,
                percentage = s.saturation.toDouble().percent
            )
        }
        client.insertRecords(records)
    }

    // ============================================================
    // 体温
    // ============================================================

    suspend fun writeTemperatureRecords(tempList: List<TemperatureData>) {
        val records = tempList.map { t ->
            BodyTemperatureRecord(
                time = t.timestamp.toUtcInstant(),
                zoneOffset = utc,
                temperature = t.celsius.toDouble().celsius
            )
        }
        client.insertRecords(records)
    }
}
