package com.xharma.clipmanager.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface ClipDao {
    @Query("SELECT * FROM clips ORDER BY timestamp DESC")
    fun getAllClips(): Flow<List<ClipEntry>>

    @Query("SELECT * FROM clips ORDER BY timestamp DESC LIMIT 1")
    fun getLatestClip(): Flow<ClipEntry?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(clip: ClipEntry)

    @Query("DELETE FROM clips")
    suspend fun deleteAll()
}
