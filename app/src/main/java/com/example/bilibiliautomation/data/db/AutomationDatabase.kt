package com.example.bilibiliautomation.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.SkipQueryVerification
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.bilibiliautomation.data.db.dao.AccountDao
import com.example.bilibiliautomation.data.db.dao.ActionHistoryDao
import com.example.bilibiliautomation.data.db.dao.CommentDao
import com.example.bilibiliautomation.data.db.dao.TargetDao
import com.example.bilibiliautomation.data.db.dao.TaskDao
import com.example.bilibiliautomation.data.db.dao.UserDao
import com.example.bilibiliautomation.data.db.dao.VideoDao
import com.example.bilibiliautomation.data.db.entity.AccountEntity
import com.example.bilibiliautomation.data.db.entity.ActionHistoryEntity
import com.example.bilibiliautomation.data.db.entity.CommentEntity
import com.example.bilibiliautomation.data.db.entity.TargetEntity
import com.example.bilibiliautomation.data.db.entity.TaskCheckpointEntity
import com.example.bilibiliautomation.data.db.entity.TaskEntity
import com.example.bilibiliautomation.data.db.entity.UserEntity
import com.example.bilibiliautomation.data.db.entity.VideoEntity

@Database(
    entities = [
        AccountEntity::class,
        TargetEntity::class,
        VideoEntity::class,
        CommentEntity::class,
        UserEntity::class,
        ActionHistoryEntity::class,
        TaskEntity::class,
        TaskCheckpointEntity::class,
    ],
    version = 4,
    exportSchema = true,
)
@TypeConverters(DbConverters::class)
@SkipQueryVerification
abstract class AutomationDatabase : RoomDatabase() {
    abstract fun accountDao(): AccountDao
    abstract fun targetDao(): TargetDao
    abstract fun videoDao(): VideoDao
    abstract fun commentDao(): CommentDao
    abstract fun userDao(): UserDao
    abstract fun actionHistoryDao(): ActionHistoryDao
    abstract fun taskDao(): TaskDao

    companion object {
        const val NAME = "automation.db"

        /**
         * Future schema changes must add a real [Migration] here.
         * Destructive fallback is intentionally not the default.
         */
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE accounts ADD COLUMN fansCount INTEGER NOT NULL DEFAULT -1")
                db.execSQL("ALTER TABLE targets ADD COLUMN fansCount INTEGER NOT NULL DEFAULT -1")
            }
        }

        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE task_checkpoints ADD COLUMN followed INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE task_checkpoints ADD COLUMN skipped INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE task_checkpoints ADD COLUMN failed INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE task_checkpoints ADD COLUMN videoTotal INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE task_checkpoints ADD COLUMN commentTotal INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE task_checkpoints ADD COLUMN currentVideoTitle TEXT")
                db.execSQL("ALTER TABLE task_checkpoints ADD COLUMN currentVideoCover TEXT")
            }
        }

        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE accounts ADD COLUMN followingCount INTEGER NOT NULL DEFAULT -1")
            }
        }

        val ALL_MIGRATIONS: Array<Migration> = arrayOf(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4)

        fun create(context: Context): AutomationDatabase {
            return Room.databaseBuilder(context, AutomationDatabase::class.java, NAME)
                .addMigrations(*ALL_MIGRATIONS)
                .build()
        }
    }
}
