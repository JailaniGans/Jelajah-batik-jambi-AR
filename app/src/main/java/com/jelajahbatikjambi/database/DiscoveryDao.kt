package com.jelajahbatikjambi.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface DiscoveryDao {

    @Query("SELECT * FROM discoveries ORDER BY discoveredAt DESC")
    fun observeAll(): Flow<List<DiscoveryEntity>>

    // IGNORE on conflict: re-scanning an already-discovered marker must not
    // overwrite its original discoveredAt.
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(discovery: DiscoveryEntity)
}
