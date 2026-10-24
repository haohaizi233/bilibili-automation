package com.example.bilibiliautomation.domain.model

data class TaskGroup(
    val id: Long,
    val name: String,
    val createdAt: Long,
    val updatedAt: Long,
)

data class TaskGroupDetail(
    val group: TaskGroup,
    val members: List<Target>,
) {
    val memberCount: Int get() = members.size
}
