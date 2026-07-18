package com.headdown.healthbridge.huawei

/**
 * 华为 Health Kit OAuth 2.0 配置
 *
 * 前置条件：
 * 1. 注册华为开发者账号：https://developer.huawei.com/
 * 2. 在 AppGallery Connect 创建应用
 * 3. 开通 Health Kit 服务
 * 4. 将下面的值替换为你的真实配置
 */
object HuaweiConfig {
    /** 华为开发者控制台获取的 App ID */
    const val CLIENT_ID = "YOUR_HUAWEI_CLIENT_ID"

    /** 华为开发者控制台获取的 App Secret */
    const val CLIENT_SECRET = "YOUR_HUAWEI_CLIENT_SECRET"

    /** OAuth 授权回调地址（需与华为控制台配置一致） */
    const val REDIRECT_URI = "com.headdown.healthbridge://oauth"

    /** 请求的权限范围 */
    val SCOPES = listOf(
        "https://www.huawei.com/healthkit/sleep.read",
        "https://www.huawei.com/healthkit/activity.read",
        "https://www.huawei.com/healthkit/heartrate.read",
        "https://www.huawei.com/healthkit/weight.read",
        "https://www.huawei.com/healthkit/bodyfat.read",
        "https://www.huawei.com/healthkit/height.read",
        "https://www.huawei.com/healthkit/bodytemperature.read",
        "https://www.huawei.com/healthkit/oxygenaturation.read",
    )

    /** 华为 Health Kit REST API 基础地址 */
    const val BASE_URL = "https://health-api.cloud.huawei.com/healthkit/v2"
    const val AUTH_URL = "https://oauth-login.cloud.huawei.com/oauth2/v3/authorize"
    const val TOKEN_URL = "https://oauth-login.cloud.huawei.com/oauth2/v3/token"
}
