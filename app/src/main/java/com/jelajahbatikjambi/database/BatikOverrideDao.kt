package com.jelajahbatikjambi.database

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

/**
 * Access to [BatikOverrideEntity] rows. One row per edited built-in motif;
 * no row means the motif still shows its original `batik.json` content.
 */
@Dao
interface BatikOverrideDao {

    @Query("SELECT * FROM batik_overrides")
    fun observeAll(): Flow<List<BatikOverrideEntity>>

    @Query("SELECT * FROM batik_overrides WHERE batikId = :batikId")
    suspend fun getByBatikId(batikId: Int): BatikOverrideEntity?

    /** Insert or update by primary key — saving an edit twice must not duplicate rows. */
    @Upsert
    suspend fun upsert(override: BatikOverrideEntity)

    @Query("DELETE FROM batik_overrides WHERE batikId = :batikId")
    suspend fun deleteByBatikId(batikId: Int)
}
