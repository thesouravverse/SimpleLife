package com.thesouravverse.simplelife.data.db

import androidx.room.Dao
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query

@Entity(tableName = "synced_tasks")
data class SyncedTaskEntity(
    @PrimaryKey val uid: String,
    val localId: Long,
    val syncedAtMillis: Long = System.currentTimeMillis()
)

@Dao
interface SyncedTaskDao {
    @Query("SELECT EXISTS(SELECT 1 FROM synced_tasks WHERE uid = :uid)")
    suspend fun exists(uid: String): Boolean

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(row: SyncedTaskEntity)

    @Query("SELECT * FROM synced_tasks")
    suspend fun all(): List<SyncedTaskEntity>
}
