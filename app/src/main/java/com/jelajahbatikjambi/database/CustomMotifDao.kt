package com.jelajahbatikjambi.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface CustomMotifDao {

    @Query("SELECT * FROM custom_motifs ORDER BY createdAt ASC")
    fun observeAll(): Flow<List<CustomMotifEntity>>

    @Query("SELECT * FROM custom_motifs ORDER BY createdAt ASC")
    suspend fun getAllOnce(): List<CustomMotifEntity>

    @Query("SELECT * FROM custom_motifs WHERE id = :id")
    suspend fun getById(id: Long): CustomMotifEntity?

    @Insert
    suspend fun insert(motif: CustomMotifEntity): Long

    /** Edits an existing motif in place (matched on the primary key). */
    @Update
    suspend fun update(motif: CustomMotifEntity)
}
