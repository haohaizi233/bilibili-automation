package com.example.bilibiliautomation.data.api

import com.example.bilibiliautomation.data.api.session.ApiSessionManager
import com.example.bilibiliautomation.data.repository.AccountRepository
import com.example.bilibiliautomation.data.repository.TargetRepository
import com.example.bilibiliautomation.domain.api.ApiResult
import com.example.bilibiliautomation.logging.AutomationLogger
import com.example.bilibiliautomation.util.MediaUrls

class UserProfileSync(
    private val api: BiliApiProvider,
    private val accounts: AccountRepository,
    private val targets: TargetRepository,
    private val sessions: ApiSessionManager,
    private val logger: AutomationLogger = AutomationLogger,
) {
    suspend fun refreshAccount(accountId: Long, force: Boolean = false) {
        val account = accounts.getById(accountId) ?: return
        if (account.uid.startsWith("pending-")) return
        val uid = account.uid.toLongOrNull() ?: return
        if (!force && account.avatarUrl.isNotBlank() && account.fansCount >= 0L) return
        // Ensure this account's own cookies are in the jar before fetching its profile.
        // activate() is not used here on purpose: refreshing account B must not switch
        // the active account away from A.
        sessions.loadJar(accountId)
        when (val card = api.getUserCard(accountId, uid)) {
            is ApiResult.Ok -> {
                val user = card.value
                accounts.updateProfile(
                    id = accountId,
                    nickname = user.name,
                    avatarUrl = MediaUrls.httpsFace(user.face) ?: user.face,
                    fansCount = user.fans.coerceAtLeast(0L),
                )
                logger.i("account profile uid=$uid fans=${user.fans} face=${user.face.isNotBlank()}")
            }
            is ApiResult.Err -> {
                when (val nav = api.getNav(accountId)) {
                    is ApiResult.Ok -> {
                        val face = nav.value.face
                        val name = nav.value.uname
                        if (!face.isNullOrBlank() || !name.isNullOrBlank()) {
                            accounts.updateProfile(
                                id = accountId,
                                nickname = name,
                                avatarUrl = MediaUrls.httpsFace(face) ?: face,
                                followingCount = nav.value.following,
                            )
                        } else {
                            accounts.updateProfile(accountId, followingCount = nav.value.following)
                        }
                    }
                    is ApiResult.Err -> logger.w("account profile failed uid=$uid ${card.error.message}")
                }
            }
        }
    }

    suspend fun refreshAccounts() {
        accounts.getAll().forEach { refreshAccount(it.id, force = it.avatarUrl.isBlank() || it.fansCount < 0L) }
    }

    /**
     * Fetch the account's own following + follower counts via relation/stat
     * (Bilibili's space/nav APIs do NOT return the logged-in user's following
     * count — only relation/stat gives both following and follower). Called
     * on startup / entering the account panel so "关注：N  粉丝：N" reflects the
     * real numbers without a login or task run.
     */
    suspend fun refreshStats(accountId: Long) {
        val account = accounts.getById(accountId) ?: return
        if (account.uid.startsWith("pending-")) return
        val uid = account.uid.toLongOrNull() ?: return
        sessions.loadJar(accountId)
        when (val stat = api.getRelationStat(accountId, uid)) {
            is ApiResult.Ok -> accounts.updateProfile(
                accountId,
                followingCount = stat.value.following.coerceAtLeast(0L),
                fansCount = stat.value.follower.coerceAtLeast(0L),
            )
            is ApiResult.Err -> logger.w("account stats refresh failed id=$accountId ${stat.error.message}")
        }
    }

    suspend fun refreshTarget(accountId: Long, uid: String, force: Boolean = false) {
        val mid = uid.toLongOrNull() ?: return
        val existing = targets.getByUid(uid)
        if (!force && existing != null && existing.avatarUrl.isNotBlank() && existing.fansCount >= 0L) return
        sessions.loadJar(accountId)
        when (val card = api.getUserCard(accountId, mid)) {
            is ApiResult.Ok -> {
                val user = card.value
                targets.upsert(
                    user.mid.toString(),
                    user.name,
                    MediaUrls.httpsFace(user.face) ?: user.face,
                    user.fans.coerceAtLeast(0L),
                )
            }
            is ApiResult.Err -> logger.w("target profile failed uid=$uid ${card.error.message}")
        }
    }

    suspend fun refreshTargets(accountId: Long) {
        targets.getAll().forEach { refreshTarget(accountId, it.uid) }
    }
}
