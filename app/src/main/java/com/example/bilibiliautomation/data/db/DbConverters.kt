package com.example.bilibiliautomation.data.db

import androidx.room.TypeConverter
import com.example.bilibiliautomation.domain.model.AccountStatus
import com.example.bilibiliautomation.domain.model.RelationshipState
import com.example.bilibiliautomation.domain.model.TaskStatus

class DbConverters {
    @TypeConverter
    fun accountStatusToString(value: AccountStatus): String = value.name

    @TypeConverter
    fun toAccountStatus(value: String): AccountStatus = AccountStatus.valueOf(value)

    @TypeConverter
    fun taskStatusToString(value: TaskStatus): String = value.name

    @TypeConverter
    fun toTaskStatus(value: String): TaskStatus = TaskStatus.valueOf(value)

    @TypeConverter
    fun relationshipToString(value: RelationshipState): String = value.name

    @TypeConverter
    fun toRelationship(value: String): RelationshipState = RelationshipState.valueOf(value)
}
