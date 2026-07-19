package com.headdown.healthbridge.healthconnect

import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.records.*
import com.headdown.healthbridge.data.ExerciseRecord
import com.headdown.healthbridge.data.HeartRateData
import com.headdown.healthbridge.data.SleepRecord
import com.headdown.healthbridge.data.StepsData
import io.mockk.*
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class HealthConnectWriterTest {

    // ============================================================
    // 睡眠转换测试
    // ============================================================

    @Test
    fun `睡眠转换 — 华为浅睡记录 → Health Connect SleepSession(浅睡阶段)`() = runTest {
        val mockClient = mockk<HealthConnectClient>(relaxed = true)
        val writer = HealthConnectWriter(mockClient)

        val sleepRecords = listOf(
            SleepRecord(startTime = 1700000000000L, endTime = 1700003600000L, sleepState = 1)
        )
        writer.writeSleepRecords(sleepRecords)

        val slot = slot<List<Record>>()
        coVerify { mockClient.insertRecords(capture(slot)) }

        val records = slot.captured
        assertEquals(1, records.size)

        val session = records[0] as SleepSessionRecord
        assertEquals(1700000000000L, session.startTime.toEpochMilli())
        assertEquals(1700003600000L, session.endTime.toEpochMilli())
        assertEquals(1, session.stages.size)
        assertEquals(SleepSessionRecord.STAGE_TYPE_LIGHT, session.stages[0].stage)
    }

    @Test
    fun `睡眠转换 — 华为深睡和 REM 记录 → Health Connect SleepSession`() = runTest {
        val mockClient = mockk<HealthConnectClient>(relaxed = true)
        val writer = HealthConnectWriter(mockClient)

        val sleepRecords = listOf(
            SleepRecord(startTime = 1710036000000L, endTime = 1710043200000L, sleepState = 2),
            SleepRecord(startTime = 1710043200000L, endTime = 1710046800000L, sleepState = 3)
        )
        writer.writeSleepRecords(sleepRecords)

        val slot = slot<List<Record>>()
        coVerify { mockClient.insertRecords(capture(slot)) }

        val records = slot.captured
        assertEquals(2, records.size)

        val deep = records[0] as SleepSessionRecord
        assertEquals(SleepSessionRecord.STAGE_TYPE_DEEP, deep.stages[0].stage)

        val rem = records[1] as SleepSessionRecord
        assertEquals(SleepSessionRecord.STAGE_TYPE_REM, rem.stages[0].stage)
    }

    @Test
    fun `睡眠转换 — 华为清醒记录 → Health Connect SleepSession(清醒阶段)`() = runTest {
        val mockClient = mockk<HealthConnectClient>(relaxed = true)
        val writer = HealthConnectWriter(mockClient)

        val sleepRecords = listOf(
            SleepRecord(startTime = 1700000000000L, endTime = 1700003600000L, sleepState = 0)
        )
        writer.writeSleepRecords(sleepRecords)

        val slot = slot<List<Record>>()
        coVerify { mockClient.insertRecords(capture(slot)) }

        val records = slot.captured
        assertEquals(1, records.size)

        val session = records[0] as SleepSessionRecord
        assertEquals(SleepSessionRecord.STAGE_TYPE_AWAKE, session.stages[0].stage)
    }

    // ============================================================
    // 心率转换测试
    // ============================================================

    @Test
    fun `心率转换 — 华为心率记录 → Health Connect HeartRateRecord`() = runTest {
        val mockClient = mockk<HealthConnectClient>(relaxed = true)
        val writer = HealthConnectWriter(mockClient)

        val hrRecords = listOf(
            HeartRateData(timestamp = 1710000000000L, bpm = 72.0f),
            HeartRateData(timestamp = 1710000600000L, bpm = 75.5f),
            HeartRateData(timestamp = 1710001200000L, bpm = 68.0f)
        )
        writer.writeHeartRateRecords(hrRecords)

        val slot = slot<List<Record>>()
        coVerify { mockClient.insertRecords(capture(slot)) }

        val records = slot.captured
        assertEquals(3, records.size)

        val hr1 = records[0] as HeartRateRecord
        assertEquals(1710000000000L, hr1.startTime.toEpochMilli())
        assertEquals(1, hr1.samples.size)
        assertEquals(72, hr1.samples[0].beatsPerMinute)

        val hr2 = records[1] as HeartRateRecord
        assertEquals(76, hr2.samples[0].beatsPerMinute)

        val hr3 = records[2] as HeartRateRecord
        assertEquals(68, hr3.samples[0].beatsPerMinute)
    }

    // ============================================================
    // 步数转换测试
    // ============================================================

    @Test
    fun `步数转换 — 华为步数增量 → Health Connect StepsRecord`() = runTest {
        val mockClient = mockk<HealthConnectClient>(relaxed = true)
        val writer = HealthConnectWriter(mockClient)

        val stepsRecords = listOf(
            StepsData(startTime = 1710000000000L, endTime = 1710003600000L, steps = 1200),
            StepsData(startTime = 1710003600000L, endTime = 1710007200000L, steps = 850)
        )
        writer.writeStepsRecords(stepsRecords)

        val slot = slot<List<Record>>()
        coVerify { mockClient.insertRecords(capture(slot)) }

        val records = slot.captured
        assertEquals(2, records.size)

        val step1 = records[0] as StepsRecord
        assertEquals(1710000000000L, step1.startTime.toEpochMilli())
        assertEquals(1710003600000L, step1.endTime.toEpochMilli())
        assertEquals(1200, step1.count)

        val step2 = records[1] as StepsRecord
        assertEquals(1710003600000L, step2.startTime.toEpochMilli())
        assertEquals(1710007200000L, step2.endTime.toEpochMilli())
        assertEquals(850, step2.count)
    }

    // ============================================================
    // 运动转换测试
    // ============================================================

    @Test
    fun `运动转换 — 华为跑步记录 → Health Connect ExerciseSession`() = runTest {
        val mockClient = mockk<HealthConnectClient>(relaxed = true)
        val writer = HealthConnectWriter(mockClient)

        val exerciseRecords = listOf(
            ExerciseRecord(startTime = 1710000000000L, endTime = 1710001800000L, exerciseType = 1)
        )
        writer.writeExerciseRecords(exerciseRecords)

        val slot = slot<List<Record>>()
        coVerify { mockClient.insertRecords(capture(slot)) }

        val records = slot.captured
        assertEquals(1, records.size)

        val session = records[0] as ExerciseSessionRecord
        assertEquals(1710000000000L, session.startTime.toEpochMilli())
        assertEquals(1710001800000L, session.endTime.toEpochMilli())
        assertEquals(ExerciseSessionRecord.EXERCISE_TYPE_RUNNING, session.exerciseType)
    }

    @Test
    fun `运动转换 — 华为骑车和未知运动 → Health Connect ExerciseSession`() = runTest {
        val mockClient = mockk<HealthConnectClient>(relaxed = true)
        val writer = HealthConnectWriter(mockClient)

        val exerciseRecords = listOf(
            ExerciseRecord(startTime = 1710003600000L, endTime = 1710005400000L, exerciseType = 3),
            ExerciseRecord(startTime = 1710007200000L, endTime = 1710009000000L, exerciseType = 99)
        )
        writer.writeExerciseRecords(exerciseRecords)

        val slot = slot<List<Record>>()
        coVerify { mockClient.insertRecords(capture(slot)) }

        val records = slot.captured
        assertEquals(2, records.size)

        val biking = records[0] as ExerciseSessionRecord
        assertEquals(ExerciseSessionRecord.EXERCISE_TYPE_BIKING, biking.exerciseType)

        val unknown = records[1] as ExerciseSessionRecord
        assertEquals(ExerciseSessionRecord.EXERCISE_TYPE_OTHER_WORKOUT, unknown.exerciseType)
    }
}
