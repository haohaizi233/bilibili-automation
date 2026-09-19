package com.example.bilibiliautomation.data.api

import com.example.bilibiliautomation.domain.api.ApiError
import com.example.bilibiliautomation.domain.api.ApiResult
import com.example.bilibiliautomation.logging.AutomationLogger

/**
 * Explicit follow action — never a UI toggle.
 * Endpoint / act values are TODO_UNVERIFIED until confirmed by capture or 参考.
 */
class RelationActionClient(
    private val provider: BiliApiProvider,
    private val logger: AutomationLogger = AutomationLogger,
) {
    suspend fun followUser(accountId: Long, uid: Long, dryRun: Boolean): ApiResult<Unit> {
        if (dryRun) {
            logger.i("[DRY_RUN] would follow uid=$uid accountId=$accountId")
            return ApiResult.ok(Unit)
        }
        return provider.followUser(accountId, uid, dryRun = false)
    }

    fun unverifiedGuard(): ApiError? {
        if (BiliApiEndpoints.RELATION_MODIFY_VERIFIED) return null
        return ApiError.UnverifiedOperation(
            "RELATION_MODIFY",
            "TODO_UNVERIFIED: endpoint/action semantics not verified",
        )
    }
}
