package com.headdown.healthbridge.data.mock

import com.headdown.healthbridge.data.AuthProvider

/**
 * Mock OAuth 认证提供者。
 *
 * 返回写死的 access token，有效期 3600 秒（1 小时），不真正刷新。
 */
class MockAuthProvider : AuthProvider {

    private val fakeToken = "mock_access_token_abc123"
    private var expiryTime: Long = System.currentTimeMillis() + 3_600_000L

    override suspend fun getToken(): String? {
        if (!isTokenValid()) return null
        return fakeToken
    }

    override fun isTokenValid(): Boolean {
        return System.currentTimeMillis() < expiryTime - 60_000L
    }

    override suspend fun refreshToken(): String? {
        expiryTime = System.currentTimeMillis() + 3_600_000L
        return fakeToken
    }
}
