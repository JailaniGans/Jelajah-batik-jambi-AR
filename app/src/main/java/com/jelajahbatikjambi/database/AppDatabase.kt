package com.jelajahbatikjambi.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [DiscoveryEntity::class, CustomMotifEntity::class, CustomQuizQuestionEntity::class],
    version = 3,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun discoveryDao(): DiscoveryDao
    abstract fun customMotifDao(): CustomMotifDao
    abstract fun customQuizQuestionDao(): CustomQuizQuestionDao

    companion object {
        @Volatile
        private var instance: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase =
            instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "jelajah_batik_jambi.db"
                )
                    // No production users yet (still active development) and
                    // no migration path is needed for a simple schema
                    // addition — acceptable for MVP (§56); revisit before any
                    // release where wiping local discovery/custom-motif data
                    // on upgrade would matter.
                    .fallbackToDestructiveMigration(true)
                    .build()
                    .also { instance = it }
            }
    }
}
