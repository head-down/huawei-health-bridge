package com.headdown.healthbridge.huawei

import android.content.Context
import com.headdown.healthbridge.data.HealthDataSource
import io.mockk.*
import kotlinx.coroutines.test.runTest
import okhttp3.Call
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class HuaweiHealthClientTest {

    // ============================================================
    // 辅助工具
    // ============================================================

    private fun mockContext(): Context = mockk(relaxed = true)

    private fun createClientWithToken(mockClient: OkHttpClient): HuaweiHealthClient {
        val client = HuaweiHealthClient(mockContext(), mockClient)
        client.setTokenForTesting("test-token")
        return client
    }

    private fun mockOkHttpWithJson(json: String, requestSlot: MutableList<Request>): OkHttpClient {
        val mockClient = mockk<OkHttpClient>()
        val mockCall = mockk<Call>()
        val mockResponse = mockk<Response>()

        every { mockResponse.isSuccessful } returns true
        every { mockResponse.code } returns 200
        every { mockResponse.body } returns json.toResponseBody("application/json".toMediaType())

        every { mockClient.newCall(capture(requestSlot)) } returns mockCall
        every { mockCall.execute() } returns mockResponse

        return mockClient
    }

    // ============================================================
    // URL 端点验证
    // ============================================================

    @Test
    fun `API 调用使用 v2 统一端点`() = runTest {
        val requestSlot = mutableListOf<Request>()
        val mockClient = mockOkHttpWithJson("{}", requestSlot)
        val client = createClientWithToken(mockClient)

        client.queryHeartRate(1710000000000L, 1710086400000L)

        val url = requestSlot[0].url.toString()
        assertTrue(url.contains("/healthkit/v2/healthRecords?"), "应使用 v2 端点: $url")
        assertTrue(url.startsWith("${HuaweiConfig.BASE_URL}/healthRecords"), "URL 应以 BASE_URL 开头: $url")
    }

    // ============================================================
    // 时间戳验证
    // ============================================================

    @Test
    fun `时间参数以纳秒精度发送（毫秒 x 1_000_000）`() = runTest {
        val requestSlot = mutableListOf<Request>()
        val mockClient = mockOkHttpWithJson("{}", requestSlot)
        val client = createClientWithToken(mockClient)

        client.queryHeartRate(1710000000000L, 1710086400000L)

        val url = requestSlot[0].url.toString()
        assertTrue(url.contains("startTime=1710000000000000000"), "纳秒 startTime: $url")
        assertTrue(url.contains("endTime=1710086400000000000"), "纳秒 endTime: $url")
    }

    @Test
    fun `时间 — 毫秒到纳秒转换正确`() = runTest {
        val requestSlot = mutableListOf<Request>()
        val mockClient = mockOkHttpWithJson("{}", requestSlot)
        val client = createClientWithToken(mockClient)

        client.querySteps(1L, 100L)

        val url = requestSlot[0].url.toString()
        assertTrue(url.contains("startTime=1000000"), "1ms → 1,000,000ns: $url")
        assertTrue(url.contains("endTime=100000000"), "100ms → 100,000,000ns: $url")
    }

    // ============================================================
    // 四种数据类型验证
    // ============================================================

    @Test
    fun `睡眠 — dataType + subDataType 查询参数正确`() = runTest {
        val requestSlot = mutableListOf<Request>()
        val mockClient = mockOkHttpWithJson("{}", requestSlot)
        val client = createClientWithToken(mockClient)

        client.querySleep(1710000000000L, 1710086400000L)

        val url = requestSlot[0].url.toString()
        assertTrue(url.contains("dataType=com.huawei.health.record.sleep"), "dataType: $url")
        assertTrue(url.contains("subDataType=com.huawei.continuous.sleep.fragment"), "subDataType: $url")
    }

    @Test
    fun `心率 — dataType 查询参数正确`() = runTest {
        val requestSlot = mutableListOf<Request>()
        val mockClient = mockOkHttpWithJson("{}", requestSlot)
        val client = createClientWithToken(mockClient)

        client.queryHeartRate(1710000000000L, 1710086400000L)

        val url = requestSlot[0].url.toString()
        assertTrue(url.contains("dataType=com.huawei.instantaneous.heart_rate"), "dataType: $url")
    }

    @Test
    fun `步数 — dataType 查询参数正确`() = runTest {
        val requestSlot = mutableListOf<Request>()
        val mockClient = mockOkHttpWithJson("{}", requestSlot)
        val client = createClientWithToken(mockClient)

        client.querySteps(1710000000000L, 1710086400000L)

        val url = requestSlot[0].url.toString()
        assertTrue(url.contains("dataType=com.huawei.continuous.steps.delta"), "dataType: $url")
    }

    @Test
    fun `运动 — dataType 查询参数正确`() = runTest {
        val requestSlot = mutableListOf<Request>()
        val mockClient = mockOkHttpWithJson("{}", requestSlot)
        val client = createClientWithToken(mockClient)

        client.queryExercise(1710000000000L, 1710086400000L)

        val url = requestSlot[0].url.toString()
        assertTrue(url.contains("dataType=com.huawei.continuous.activity.segment"), "dataType: $url")
    }

    // ============================================================
    // 接口实现验证
    // ============================================================

    @Test
    fun `实现了 HealthDataSource 接口`() {
        val mockClient = mockk<OkHttpClient>(relaxed = true)
        val client = HuaweiHealthClient(mockContext(), mockClient)
        assertTrue(client is HealthDataSource, "HuaweiHealthClient 应实现 HealthDataSource")
    }

    @Test
    fun `四个接口方法签名正确`() = runTest {
        val requestSlot = mutableListOf<Request>()
        val mockClient = mockOkHttpWithJson("{}", requestSlot)
        val client = createClientWithToken(mockClient)

        // 所有方法应返回正确的类型且不抛异常
        val sleep = client.querySleep(1L, 2L)
        val hr = client.queryHeartRate(1L, 2L)
        val steps = client.querySteps(1L, 2L)
        val exercise = client.queryExercise(1L, 2L)

        // returnDefaultValues=true → optJSONArray 返回 null → 空列表
        assertNotNull(sleep)
        assertNotNull(hr)
        assertNotNull(steps)
        assertNotNull(exercise)
    }

    // ============================================================
    // OAuth 方法保持存在
    // ============================================================

    @Test
    fun `OAuth — getAuthorizationUrl 包含必要参数`() {
        val client = HuaweiHealthClient(mockContext())

        val url = client.getAuthorizationUrl()

        assertTrue(url.contains("client_id=${HuaweiConfig.CLIENT_ID}"))
        assertTrue(url.contains("response_type=code"))
        assertTrue(url.contains("access_type=offline"))
        assertTrue(url.contains("scope="))
    }
}
