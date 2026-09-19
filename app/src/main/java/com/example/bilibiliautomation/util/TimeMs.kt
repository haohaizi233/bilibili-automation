package com.example.bilibiliautomation.util

object TimeMs {
    fun now(): Long {
        return try {
            android.os.SystemClock.elapsedRealtime()
        } catch (_: Throwable) {
            System.nanoTime() / 1_000_000L
        }
    }

    fun wallClock(): Long = System.currentTimeMillis()
}
