package com.example.bilibiliautomation.domain.model

object ResultCode {
    const val OK = "OK"

    const val ELEMENT_NOT_FOUND = "ELEMENT_NOT_FOUND"
    const val ELEMENT_NOT_VISIBLE = "ELEMENT_NOT_VISIBLE"
    const val ELEMENT_DISABLED = "ELEMENT_DISABLED"
    const val TIMEOUT = "TIMEOUT"
    const val NAVIGATION_FAILED = "NAVIGATION_FAILED"
    const val PAGE_LOAD_FAILED = "NAVIGATION_FAILED"
    const val JS_ERROR = "JS_ERROR"
    const val PAGE_CHANGED = "PAGE_CHANGED"

    const val NETWORK_ERROR = "NETWORK_ERROR"

    const val LOGIN_REQUIRED = "LOGIN_REQUIRED"
    const val LOGIN_EXPIRED = "LOGIN_EXPIRED"

    const val RATE_LIMITED = "RATE_LIMITED"
    const val ACCOUNT_RESTRICTED = "ACCOUNT_RESTRICTED"

    const val CANCELLED = "CANCELLED"
    const val UNKNOWN = "UNKNOWN"
    const val INVALID_ARGUMENT = "INVALID_ARGUMENT"
    const val UNSAFE_STATE = "UNSAFE_STATE"
    const val SKIPPED = "SKIPPED"
    const val ASSERT = "ASSERT"
    const val BUSY = "BUSY"
}
