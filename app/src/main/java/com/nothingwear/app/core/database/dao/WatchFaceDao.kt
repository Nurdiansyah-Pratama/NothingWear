package com.nothingwear.app.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.nothingwear.app.core.database.entity.WatchFaceProjectEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface WatchFaceDao {

    @Query("SELECT * FROM watch_face_projects ORDER BY updated_at_millis DESC")
    fun observeAllProjects(): Flow<List<WatchFaceProjectEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertProject(project: WatchFaceProjectEntity): Long

    @Query("DELETE FROM watch_face_projects WHERE id = :projectId")
    suspend fun deleteProjectById(projectId: Long)
}
