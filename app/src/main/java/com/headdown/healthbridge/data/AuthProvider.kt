package com.headdown.healthbridge.data

/**
 * OAuth 认证提供者接口
 *
 * 抽象华为 OAuth 认证流程，开发期用 Mock 实现，接入后替换为真实 OAuth。
 */
interface AuthProvider {

    /** 获取当前有效的 Access Token，无效则返回 null */
    suspend fun getToken(): String?

    /** 检查当前 Token 是否有效（距离过期至少 1 分钟） */
    fun isTokenValid(): Boolean

    /** 使用 Refresh Token 刷新 Access Token，刷新失败返回 null */
    suspend fun refreshToken(): String?
}
