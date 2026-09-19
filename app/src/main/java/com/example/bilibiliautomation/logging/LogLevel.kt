package com.example.bilibiliautomation.logging

enum class LogLevel {
    DEBUG,
    INFO,
    SUCCESS,
    WARN,
    ERROR,
    ;

    val filterGroup: String
        get() = when (this) {
            DEBUG, INFO -> "INFO"
            SUCCESS -> "SUCCESS"
            WARN -> "WARN"
            ERROR -> "ERROR"
        }
}
