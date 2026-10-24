package com.example.bilibiliautomation.ui.target

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.bilibiliautomation.app.AppContainer
import com.example.bilibiliautomation.data.api.dto.SpaceUser
import com.example.bilibiliautomation.domain.api.ApiResult
import com.example.bilibiliautomation.domain.model.Target
import com.example.bilibiliautomation.domain.model.TaskGroupDetail
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class TargetViewModel(
    private val container: AppContainer,
) : ViewModel() {
    val targets: StateFlow<List<Target>> = container.targetRepository.observeAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val groups: StateFlow<List<TaskGroupDetail>> = container.taskGroupRepository.observeDetails()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val _search = MutableStateFlow(SearchState())
    val search: StateFlow<SearchState> = _search.asStateFlow()

    fun add(uid: String, nickname: String, avatarUrl: String = "", fansCount: Long = -1L) {
        if (uid.isBlank()) return
        viewModelScope.launch {
            container.targetRepository.upsert(uid.trim(), nickname.trim(), avatarUrl, fansCount)
        }
    }

    fun delete(id: Long) {
        viewModelScope.launch { container.targetRepository.delete(id) }
    }

    fun deleteByUid(uid: String) {
        viewModelScope.launch { container.targetRepository.deleteByUid(uid) }
    }

    fun createGroup(name: String) {
        viewModelScope.launch { container.taskGroupRepository.create(name) }
    }

    fun renameGroup(id: Long, name: String) {
        viewModelScope.launch { container.taskGroupRepository.rename(id, name) }
    }

    fun deleteGroup(id: Long) {
        viewModelScope.launch { container.taskGroupRepository.delete(id) }
    }

    fun addToGroup(groupId: Long, targetUid: String) {
        viewModelScope.launch { container.taskGroupRepository.addMember(groupId, targetUid) }
    }

    fun removeFromGroup(groupId: Long, targetUid: String) {
        viewModelScope.launch { container.taskGroupRepository.removeMember(groupId, targetUid) }
    }

    fun setKeyword(value: String) {
        _search.value = _search.value.copy(keyword = value.filter { it.isDigit() }, error = null)
    }

    fun setUidInput(value: String) {
        _search.value = _search.value.copy(uidInput = value.filter { it.isDigit() }, error = null)
    }

    fun select(uid: String) {
        _search.value = _search.value.copy(selectedUid = uid)
    }

    fun searchKeyword() = lookupUid()

    fun lookupUid() {
        val uid = _search.value.keyword.filter { it.isDigit() }
            .ifBlank { _search.value.uidInput.filter { it.isDigit() } }
        if (uid.isBlank()) {
            _search.value = _search.value.copy(
                error = "请输入 UID",
                results = emptyList(),
                selectedUid = "",
                loading = false,
            )
            return
        }
        viewModelScope.launch {
            _search.value = _search.value.copy(loading = true, error = null, uidInput = uid, keyword = uid)
            val accountId = container.apiSessionManager.ensureActiveAccountId() ?: 0L
            when (val result = container.biliApiProvider.getUserCard(accountId, uid.toLong())) {
                is ApiResult.Ok -> {
                    val user = result.value
                    _search.value = _search.value.copy(
                        loading = false,
                        results = listOf(user),
                        selectedUid = user.mid.toString(),
                        error = null,
                    )
                }
                is ApiResult.Err -> _search.value = _search.value.copy(
                    loading = false,
                    error = "未找到该 UID",
                    results = emptyList(),
                    selectedUid = "",
                )
            }
        }
    }

    fun confirmSelected() {
        val state = _search.value
        val selected = state.results.firstOrNull { it.mid.toString() == state.selectedUid }
        if (selected != null) {
            add(selected.mid.toString(), selected.name, selected.face, selected.fans)
            return
        }
        val uid = state.uidInput.filter { it.isDigit() }
            .ifBlank { state.selectedUid.filter { it.isDigit() } }
        if (uid.isBlank()) return
        viewModelScope.launch {
            val accountId = container.apiSessionManager.ensureActiveAccountId() ?: 0L
            when (val result = container.biliApiProvider.getUserCard(accountId, uid.toLong())) {
                is ApiResult.Ok -> add(result.value.mid.toString(), result.value.name, result.value.face, result.value.fans)
                is ApiResult.Err -> add(uid, uid)
            }
        }
    }

    fun refreshProfiles() {
        viewModelScope.launch {
            val accountId = container.apiSessionManager.ensureActiveAccountId() ?: 0L
            container.userProfileSync.refreshTargets(accountId)
        }
    }

    init {
        refreshProfiles()
    }

    data class SearchState(
        val keyword: String = "",
        val uidInput: String = "",
        val selectedUid: String = "",
        val results: List<SpaceUser> = emptyList(),
        val loading: Boolean = false,
        val error: String? = null,
    )
}
