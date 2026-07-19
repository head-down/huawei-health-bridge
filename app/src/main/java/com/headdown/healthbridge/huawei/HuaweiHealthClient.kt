package com.headdown.healthbridge.huawei

import android.content.Context
import androidx.annotation.VisibleForTesting
import com.headdown.healthbridge.data.HealthDataSource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import org.json.JSONObject
import java.io.IOException

/**
 * 华为 Health Kit REST API 客户端 (v2)
 *
 * 使用 v2 统一端点，纳秒级时间戳，手写 org.json 解析动态 value 类型。
 *
 * 文档：https://developer.huawei.com/consumer/cn/doc/HMSCore-Guides/health-rest-api-0000001051069434
 */
class HuaweiHealthClient @JvmOverloads constructor(
    private val context: Context,
    @Volatile private var client: OkHttpClient = OkHttpClient()
) : HealthDataSource {

    private val jsonMediaType = "application/json".toMediaType()

    /** 华为账号的 Access Token */
    private var accessToken: String? = null

    /** Access Token 过期时间（毫秒时间戳） */
    private var tokenExpiry: Long = 0

    @VisibleForTesting
    internal fun setTokenForTesting(token: String) {
        accessToken = token
        tokenExpiry = System.currentTimeMillis() + 3_600_000
    }

    // ============================================================
    // v2 数据类型的 dataType 常量
    // ============================================================

    companion object {
        /** 睡眠：睡眠 session 聚合数据 */
        const val DATA_TYPE_SLEEP = "com.huawei.health.record.sleep"

        /** 睡眠子类型：连续睡眠片段 */
        const val SUB_DATA_TYPE_SLEEP = "com.huawei.continuous.sleep.fragment"

        /** 心率：瞬时心率值 */
        const val DATA_TYPE_HEART_RATE = "com.huawei.instantaneous.heart_rate"

        /** 步数：增量步数 */
        const val DATA_TYPE_STEPS = "com.huawei.continuous.steps.delta"

        /** 运动：连续活动片段 */
        const val DATA_TYPE_EXERCISE = "com.huawei.continuous.activity.segment"

        /** 毫秒 → 纳秒 转换系数 */
        const val MS_TO_NS = 1_000_000L

        /** 纳秒 → 毫秒 转换系数 */
        const val NS_TO_MS = 1_000_000L
    }

    // ============================================================
    // OAuth 2.0（保持向后兼容）
    // ============================================================

    /** 生成华为 OAuth 授权 URL */
    fun getAuthorizationUrl(): String {
        val scopes = HuaweiConfig.SCOPES.joinToString("+")
        return "${HuaweiConfig.AUTH_URL}?" +
            "client_id=${HuaweiConfig.CLIENT_ID}&" +
            "redirect_uri=${HuaweiConfig.REDIRECT_URI}&" +
            "response_type=code&" +
            "scope=$scopes&" +
            "access_type=offline"
    }

    /** 用授权码换取 Access Token */
    fun exchangeCodeForToken(code: String) {
        val body = FormBody.Builder()
            .add("grant_type", "authorization_code")
            .add("client_id", HuaweiConfig.CLIENT_ID)
            .add("client_secret", HuaweiConfig.CLIENT_SECRET)
            .add("redirect_uri", HuaweiConfig.REDIRECT_URI)
            .add("code", code)
            .build()

        val request = Request.Builder()
            .url(HuaweiConfig.TOKEN_URL)
            .post(body)
            .build()

        client.newCall(request).enqueue(object : okhttp3.Callback {
            override fun onFailure(call: Call, e: IOException) { /* TODO */ }
            override fun onResponse(call: Call, response: Response) {
                val json = JSONObject(response.body!!.string())
                accessToken = json.getString("access_token")
                tokenExpiry = System.currentTimeMillis() + json.getLong("expires_in") * 1000
            }
        })
    }

    /** 获取有效的 Token，过期则刷新 */
    private fun getValidToken(): String? {
        if (accessToken != null && System.currentTimeMillis() < tokenExpiry - 60_000) {
            return accessToken
        }
        // TODO: 用 Refresh Token 刷新
        return accessToken
    }

    // ============================================================
    // v2 API 请求
    // ============================================================

    /**
     * 调用 v2 统一端点，返回 JSON 响应或 null（失败时）。
     *
     * @param dataType 华为 Health Kit 数据类型
     * @param startTimeMs 开始时间（毫秒）
     * @param endTimeMs 结束时间（毫秒）
     * @param extraParams 额外查询参数（如 subDataType）
     */
    private fun queryV2(
        dataType: String,
        startTimeMs: Long,
        endTimeMs: Long,
        extraParams: Map<String, String> = emptyMap()
    ): JSONObject? {
        val token = getValidToken() ?: return null

        val urlBuilder = StringBuilder(HuaweiConfig.BASE_URL)
            .append("/healthRecords?")
            .append("dataType=$dataType&")
            .append("startTime=${startTimeMs * MS_TO_NS}&")
            .append("endTime=${endTimeMs * MS_TO_NS}")

        extraParams.forEach { (k, v) ->
            urlBuilder.append("&$k=$v")
        }

        val request = Request.Builder()
            .url(urlBuilder.toString())
            .header("Authorization", "Bearer $token")
            .header("x-client-id", HuaweiConfig.CLIENT_ID)
            .get()
            .build()

        return try {
            val response = client.newCall(request).execute()
            JSONObject(response.body!!.string())
        } catch (e: Exception) {
            null
        }
    }

    // ============================================================
    // value 数组解析工具
    // ============================================================

    /**
     * 从 value 数组中按 fieldName 取值。
     *
     * value 格式：[{"fieldName": "xxx", "<typeTag>": <value>}, ...]
     * typeTag 可能是：integerValue, floatValue, longValue, stringValue
     */
    private fun getFieldValue(valueArray: org.json.JSONArray, fieldName: String): Any? {
        for (i in 0 until valueArray.length()) {
            val entry = valueArray.getJSONObject(i)
            if (entry.optString("fieldName") == fieldName) {
                return entry.opt("longValue")
                    ?: entry.opt("integerValue")
                    ?: entry.opt("floatValue")
                    ?: entry.optString("stringValue", null)
            }
        }
        return null
    }

    /** 从 value 数组中按 fieldName 取 long 值并转换纳秒→毫秒 */
    private fun getFieldLongAsMs(valueArray: org.json.JSONArray, fieldName: String): Long {
        val v = getFieldValue(valueArray, fieldName) ?: return 0L
        return when (v) {
            is Long -> v / NS_TO_MS
            is Int -> (v / NS_TO_MS).toLong()
            is Double -> (v / NS_TO_MS).toLong()
            else -> 0L
        }
    }

    /** 从 value 数组中按 fieldName 取 int 值 */
    private fun getFieldInt(valueArray: org.json.JSONArray, fieldName: String): Int {
        val v = getFieldValue(valueArray, fieldName) ?: return 0
        return when (v) {
            is Int -> v
            is Long -> v.toInt()
            is Double -> v.toInt()
            is String -> v.toIntOrNull() ?: 0
            else -> 0
        }
    }

    /** 从 value 数组中按 fieldName 取 float 值 */
    private fun getFieldFloat(valueArray: org.json.JSONArray, fieldName: String): Float {
        val v = getFieldValue(valueArray, fieldName) ?: return 0f
        return when (v) {
            is Float -> v
            is Double -> v.toFloat()
            is Int -> v.toFloat()
            is Long -> v.toFloat()
            else -> 0f
        }
    }

    // ============================================================
    // HealthDataSource 接口实现
    // ============================================================

    override suspend fun querySleep(
        startTime: Long,
        endTime: Long
    ): List<com.headdown.healthbridge.data.SleepRecord> =
        withContext(Dispatchers.IO) {
            val json = queryV2(
                DATA_TYPE_SLEEP, startTime, endTime,
                mapOf("subDataType" to SUB_DATA_TYPE_SLEEP)
            ) ?: return@withContext emptyList()

            val records = mutableListOf<com.headdown.healthbridge.data.SleepRecord>()
            json.optJSONArray("healthRecords")?.let { healthRecords ->
                for (i in 0 until healthRecords.length()) {
                    val record = healthRecords.getJSONObject(i)
                    val value = record.optJSONArray("value") ?: continue

                    val fallAsleep = getFieldLongAsMs(value, "fall_asleep_time")
                    val wakeUp = getFieldLongAsMs(value, "wakeup_time")
                    val deep = getFieldInt(value, "deep_sleep_time")
                    val light = getFieldInt(value, "light_sleep_time")
                    val dream = getFieldInt(value, "dream_time")

                    // 取占时最长的阶段
                    val sleepState = when {
                        deep >= light && deep >= dream -> 2
                        dream > deep && dream >= light -> 3
                        else -> 1
                    }

                    records.add(
                        com.headdown.healthbridge.data.SleepRecord(
                            fallAsleep, wakeUp, sleepState
                        )
                    )
                }
            }
            records
        }

    override suspend fun queryHeartRate(
        startTime: Long,
        endTime: Long
    ): List<com.headdown.healthbridge.data.HeartRateData> =
        withContext(Dispatchers.IO) {
            val json = queryV2(DATA_TYPE_HEART_RATE, startTime, endTime)
                ?: return@withContext emptyList()

            val records = mutableListOf<com.headdown.healthbridge.data.HeartRateData>()
            json.optJSONArray("healthRecords")?.let { healthRecords ->
                for (i in 0 until healthRecords.length()) {
                    val record = healthRecords.getJSONObject(i)
                    val value = record.optJSONArray("value") ?: continue
                    val startTimeNs = record.optLong("startTime", 0L)

                    val bpm = getFieldFloat(value, "bpm")

                    records.add(
                        com.headdown.healthbridge.data.HeartRateData(
                            timestamp = startTimeNs / NS_TO_MS,
                            bpm = bpm
                        )
                    )
                }
            }
            records
        }

    override suspend fun querySteps(
        startTime: Long,
        endTime: Long
    ): List<com.headdown.healthbridge.data.StepsData> =
        withContext(Dispatchers.IO) {
            val json = queryV2(DATA_TYPE_STEPS, startTime, endTime)
                ?: return@withContext emptyList()

            val records = mutableListOf<com.headdown.healthbridge.data.StepsData>()
            json.optJSONArray("healthRecords")?.let { healthRecords ->
                for (i in 0 until healthRecords.length()) {
                    val record = healthRecords.getJSONObject(i)
                    val value = record.optJSONArray("value") ?: continue
                    val startTimeNs = record.optLong("startTime", 0L)
                    val endTimeNs = record.optLong("endTime", 0L)

                    val steps = getFieldInt(value, "steps_delta")

                    records.add(
                        com.headdown.healthbridge.data.StepsData(
                            startTime = startTimeNs / NS_TO_MS,
                            endTime = endTimeNs / NS_TO_MS,
                            steps = steps
                        )
                    )
                }
            }
            records
        }

    override suspend fun queryExercise(
        startTime: Long,
        endTime: Long
    ): List<com.headdown.healthbridge.data.ExerciseRecord> =
        withContext(Dispatchers.IO) {
            val json = queryV2(DATA_TYPE_EXERCISE, startTime, endTime)
                ?: return@withContext emptyList()

            val records = mutableListOf<com.headdown.healthbridge.data.ExerciseRecord>()
            json.optJSONArray("healthRecords")?.let { healthRecords ->
                for (i in 0 until healthRecords.length()) {
                    val record = healthRecords.getJSONObject(i)
                    val value = record.optJSONArray("value") ?: continue
                    val startTimeNs = record.optLong("startTime", 0L)
                    val endTimeNs = record.optLong("endTime", 0L)

                    val activityType = getFieldInt(value, "activity_type")

                    records.add(
                        com.headdown.healthbridge.data.ExerciseRecord(
                            startTime = startTimeNs / NS_TO_MS,
                            endTime = endTimeNs / NS_TO_MS,
                            exerciseType = activityType
                        )
                    )
                }
            }
            records
        }

    // ============================================================
    // 向后兼容：out-of-scope 数据类型（v1 风格，后续迭代迁移）
    // ============================================================

    /** 查询体重（v1 端点，暂未迁移到 v2） */
    fun queryWeight(startMillis: Long, endMillis: Long): List<WeightData> {
        // out of scope for v2, return empty
        return emptyList()
    }

    /** 查询血氧（v1 端点，暂未迁移到 v2） */
    fun querySpO2(startMillis: Long, endMillis: Long): List<SpO2Data> {
        return emptyList()
    }

    /** 查询体温（v1 端点，暂未迁移到 v2） */
    fun queryTemperature(startMillis: Long, endMillis: Long): List<TemperatureData> {
        return emptyList()
    }

    // ============================================================
    // 向后兼容：内部数据类（供测试和 HealthConnectWriter 使用）
    // ============================================================

    data class SleepData(
        val startTime: Long,
        val endTime: Long,
        val sleepState: Int
    )

    data class WeightData(
        val timestamp: Long,
        val weightKg: Float
    )

    data class SpO2Data(
        val timestamp: Long,
        val saturation: Float
    )

    data class TemperatureData(
        val timestamp: Long,
        val celsius: Float
    )
}
