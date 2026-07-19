package com.headdown.healthbridge.data

/** 健康数据类型标识，统一替换散落的字符串字面量 */
enum class DataType(val label: String) {
    SLEEP("睡眠"),
    HEART_RATE("心率"),
    STEPS("步数"),
    EXERCISE("运动"),
}
