package com.jelajahbatikjambi.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface CustomMotifDao {

    @Query("SELECT * FROM custom_motifs ORDER BY createdAt ASC")
    fun observeAll(): Flow<List<CustomMotifEntity>>

    @Query("SELECT * FROM custom_motifs ORDER BY createdAt ASC")
    suspend fun getAllOnce(): List<CustomMotifEntity>

    @Insert
    suspend fun insert(motif: CustomMotifEntity): Long
}
