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
            val stage = when (sleep.sleepState) {
                1 -> SleepSession.Stage.SLEEPING_LIGHT
                2 -> SleepSession.Stage.SLEEPING_DEEP
                3 -> SleepSession.Stage.SLEEPING_REM
                else -> SleepSession.Stage.AWAKE
            }

            SleepSession(
                startTime = Instant.ofEpochMilli(sleep.startTime).atOffset(ZoneOffset.UTC),
                endTime = Instant.ofEpochMilli(sleep.endTime).atOffset(ZoneOffset.UTC),
                stages = listOf(
                    SleepSession.StageDuration(
                        stage = stage,
                        duration = java.time.Duration.ofMillis(sleep.endTime - sleep.startTime)
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
                startTime = Instant.ofEpochMilli(hr.timestamp).atOffset(ZoneOffset.UTC),
                endTime = Instant.ofEpochMilli(hr.timestamp).atOffset(ZoneOffset.UTC),
                samples = listOf(
                    HeartRateRecord.Sample(
                        time = Instant.ofEpochMilli(hr.timestamp).atOffset(ZoneOffset.UTC),
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
                startTime = Instant.ofEpochMilli(step.startTime).atOffset(ZoneOffset.UTC),
                endTime = Instant.ofEpochMilli(step.endTime).atOffset(ZoneOffset.UTC),
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
            val type = mapHuaweiExerciseType(ex.exerciseType)
            ExerciseSession(
                startTime = Instant.ofEpochMilli(ex.startTime).atOffset(ZoneOffset.UTC),
                endTime = Instant.ofEpochMilli(ex.endTime).atOffset(ZoneOffset.UTC),
                exerciseType = type
            )
        }
        client.insertRecords(records)
    }

    /** 华为运动类型 → Health Connect 运动类型 */
    private fun mapHuaweiExerciseType(huaweiType: Int): Int {
        return when (huaweiType) {
            1 -> ExerciseSession.EXERCISE_TYPE_RUNNING
            2 -> ExerciseSession.EXERCISE_TYPE_WALKING
            3 -> ExerciseSession.EXERCISE_TYPE_CYCLING
            4 -> ExerciseSession.EXERCISE_TYPE_SWIMMING_OPEN_WATER
            5 -> ExerciseSession.EXERCISE_TYPE_HIKING
            6 -> ExerciseSession.EXERCISE_TYPE_YOGA
            7 -> ExerciseSession.EXERCISE_TYPE_STRENGTH_TRAINING
            8 -> ExerciseSession.EXERCISE_TYPE_BADMINTON
            9 -> ExerciseSession.EXERCISE_TYPE_BASKETBALL
            10 -> ExerciseSession.EXERCISE_TYPE_TABLE_TENNIS
            11 -> ExerciseSession.EXERCISE_TYPE_VOLLEYBALL
            12 -> ExerciseSession.EXERCISE_TYPE_JUMP_ROPE
            else -> ExerciseSession.EXERCISE_TYPE_OTHER_WORKOUT
        }
    }

    // ============================================================
    // 体重
    // ============================================================

    suspend fun writeWeightRecords(weightList: List<HuaweiHealthClient.WeightData>) {
        val records = weightList.map { w ->
            WeightRecord(
                time = Instant.ofEpochMilli(w.timestamp).atOffset(ZoneOffset.UTC),
                weight = Mass.kilograms(w.weightKg.toDouble())
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
                time = Instant.ofEpochMilli(s.timestamp).atOffset(ZoneOffset.UTC),
                percentage = s.saturation.toDouble()
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
                time = Instant.ofEpochMilli(t.timestamp).atOffset(ZoneOffset.UTC),
                temperature = Temperature.celsius(t.celsius.toDouble())
            )
        }
        client.insertRecords(records)
    }
}
