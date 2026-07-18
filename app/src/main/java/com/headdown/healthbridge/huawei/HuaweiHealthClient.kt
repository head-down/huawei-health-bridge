package com.headdown.healthbridge.huawei

import android.content.Context
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import org.json.JSONObject
import java.io.IOException

/**
 * 华为 Health Kit REST API 客户端
 *
 * 文档：https://developer.huawei.com/consumer/cn/doc/HMSCore-Guides/health-rest-api-0000001051069434
 */
class HuaweiHealthClient(private val context: Context) {

    private val client = OkHttpClient()
    private val jsonMediaType = "application/json".toMediaType()

    /** 华为账号的 Access Token */
    private var accessToken: String? = null

    /** Access Token 过期时间（毫秒时间戳） */
    private var tokenExpiry: Long = 0

    // ============================================================
    // OAuth 2.0
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
    // 数据读取 API
    // ============================================================

    /** 将时间戳转为 ISO 8601 字符串 */
    private fun toIsoTime(epochMillis: Long): String {
        val sdf = java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", java.util.Locale.US)
        sdf.timeZone = java.util.TimeZone.getTimeZone("UTC")
        return sdf.format(java.util.Date(epochMillis))
    }

    /** 通用的 GET 请求封装 */
    private fun getJson(path: String, params: Map<String, String>): JSONObject? {
        val token = getValidToken() ?: return null

        val urlBuilder = StringBuilder("${HuaweiConfig.BASE_URL}$path")
        if (params.isNotEmpty()) {
            urlBuilder.append("?")
            params.forEach { (k, v) -> urlBuilder.append("$k=$v&") }
            urlBuilder.setLength(urlBuilder.length - 1)
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

    // ---------- 睡眠 ----------

    data class SleepData(
        val startTime: Long,
        val endTime: Long,
        val sleepState: Int  // 0=清醒, 1=浅睡, 2=深睡, 3=REM
    )

    /** 查询睡眠数据 */
    fun querySleepData(startMillis: Long, endMillis: Long): List<SleepData> {
        val result = mutableListOf<SleepData>()
        val json = getJson("/sleepRecords", mapOf(
            "startTime" to toIsoTime(startMillis),
            "endTime" to toIsoTime(endMillis)
        ))
        json?.optJSONArray("records")?.let { records ->
            for (i in 0 until records.length()) {
                val record = records.getJSONObject(i)
                result.add(SleepData(
                    startTime = record.optLong("startTime"),
                    endTime = record.optLong("endTime"),
                    sleepState = record.optInt("sleepState")
                ))
            }
        }
        return result
    }

    // ---------- 心率 ----------

    data class HeartRateData(
        val timestamp: Long,
        val bpm: Float
    )

    /** 查询心率数据 */
    fun queryHeartRate(startMillis: Long, endMillis: Long): List<HeartRateData> {
        val result = mutableListOf<HeartRateData>()
        val json = getJson("/heartRateRecords", mapOf(
            "startTime" to toIsoTime(startMillis),
            "endTime" to toIsoTime(endMillis)
        ))
        json?.optJSONArray("records")?.let { records ->
            for (i in 0 until records.length()) {
                val record = records.getJSONObject(i)
                result.add(HeartRateData(
                    timestamp = record.optLong("timestamp"),
                    bpm = record.optDouble("bpm").toFloat()
                ))
            }
        }
        return result
    }

    // ---------- 步数 ----------

    data class StepsData(
        val startTime: Long,
        val endTime: Long,
        val steps: Int
    )

    /** 查询步数 */
    fun querySteps(startMillis: Long, endMillis: Long): List<StepsData> {
        val result = mutableListOf<StepsData>()
        val json = getJson("/stepRecords", mapOf(
            "startTime" to toIsoTime(startMillis),
            "endTime" to toIsoTime(endMillis)
        ))
        json?.optJSONArray("records")?.let { records ->
            for (i in 0 until records.length()) {
                val record = records.getJSONObject(i)
                result.add(StepsData(
                    startTime = record.optLong("startTime"),
                    endTime = record.optLong("endTime"),
                    steps = record.optInt("steps")
                ))
            }
        }
        return result
    }

    // ---------- 运动 ----------

    data class ExerciseData(
        val startTime: Long,
        val endTime: Long,
        val exerciseType: Int  // 运动类型代码
    )

    /** 查询运动记录 */
    fun queryExercise(startMillis: Long, endMillis: Long): List<ExerciseData> {
        val result = mutableListOf<ExerciseData>()
        val json = getJson("/exerciseRecords", mapOf(
            "startTime" to toIsoTime(startMillis),
            "endTime" to toIsoTime(endMillis)
        ))
        json?.optJSONArray("records")?.let { records ->
            for (i in 0 until records.length()) {
                val record = records.getJSONObject(i)
                result.add(ExerciseData(
                    startTime = record.optLong("startTime"),
                    endTime = record.optLong("endTime"),
                    exerciseType = record.optInt("exerciseType")
                ))
            }
        }
        return result
    }

    // ---------- 体重 ----------

    data class WeightData(
        val timestamp: Long,
        val weightKg: Float
    )

    /** 查询体重 */
    fun queryWeight(startMillis: Long, endMillis: Long): List<WeightData> {
        val result = mutableListOf<WeightData>()
        val json = getJson("/weightRecords", mapOf(
            "startTime" to toIsoTime(startMillis),
            "endTime" to toIsoTime(endMillis)
        ))
        json?.optJSONArray("records")?.let { records ->
            for (i in 0 until records.length()) {
                val record = records.getJSONObject(i)
                result.add(WeightData(
                    timestamp = record.optLong("timestamp"),
                    weightKg = record.optDouble("weight").toFloat()
                ))
            }
        }
        return result
    }

    // ---------- 血氧 ----------

    data class SpO2Data(
        val timestamp: Long,
        val saturation: Float
    )

    /** 查询血氧 */
    fun querySpO2(startMillis: Long, endMillis: Long): List<SpO2Data> {
        val result = mutableListOf<SpO2Data>()
        val json = getJson("/oxygenSaturationRecords", mapOf(
            "startTime" to toIsoTime(startMillis),
            "endTime" to toIsoTime(endMillis)
        ))
        json?.optJSONArray("records")?.let { records ->
            for (i in 0 until records.length()) {
                val record = records.getJSONObject(i)
                result.add(SpO2Data(
                    timestamp = record.optLong("timestamp"),
                    saturation = record.optDouble("saturation").toFloat()
                ))
            }
        }
        return result
    }

    // ---------- 体温 ----------

    data class TemperatureData(
        val timestamp: Long,
        val celsius: Float
    )

    /** 查询体温 */
    fun queryTemperature(startMillis: Long, endMillis: Long): List<TemperatureData> {
        val result = mutableListOf<TemperatureData>()
        val json = getJson("/temperatureRecords", mapOf(
            "startTime" to toIsoTime(startMillis),
            "endTime" to toIsoTime(endMillis)
        ))
        json?.optJSONArray("records")?.let { records ->
            for (i in 0 until records.length()) {
                val record = records.getJSONObject(i)
                result.add(TemperatureData(
                    timestamp = record.optLong("timestamp"),
                    celsius = record.optDouble("temperature").toFloat()
                ))
            }
        }
        return result
    }
}
