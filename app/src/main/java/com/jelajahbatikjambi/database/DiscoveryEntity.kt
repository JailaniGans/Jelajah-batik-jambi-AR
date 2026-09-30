package com.jelajahbatikjambi.database

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * One row per Batik motif the user has discovered via AR (§16). [batikId]
 * matches [com.jelajahbatikjambi.data.model.BatikData.id]; [discoveredAt] is
 * the epoch millis of the *first* successful scan (re-scans don't overwrite it).
 */
@Entity(tableName = "discoveries")
data class DiscoveryEntity(
    @PrimaryKey val batikId: Int,
    val discoveredAt: Long
)
