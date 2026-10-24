package com.example.bilibiliautomation.data.db.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index

@Entity(
    tableName = "task_group_members",
    primaryKeys = ["groupId", "targetUid"],
    foreignKeys = [
        ForeignKey(
            entity = TaskGroupEntity::class,
            parentColumns = ["id"],
            childColumns = ["groupId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [
        Index(value = ["groupId"]),
        Index(value = ["targetUid"]),
    ],
)
data class TaskGroupMemberEntity(
    val groupId: Long,
    val targetUid: String,
    val sortOrder: Int,
    val createdAt: Long,
)
