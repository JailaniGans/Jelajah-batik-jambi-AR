package com.jelajahbatikjambi.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [
        DiscoveryEntity::class,
        CustomMotifEntity::class,
        CustomQuizQuestionEntity::class,
        BatikOverrideEntity::class
    ],
    version = 4,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun discoveryDao(): DiscoveryDao
    abstract fun customMotifDao(): CustomMotifDao
    abstract fun customQuizQuestionDao(): CustomQuizQuestionDao
    abstract fun batikOverrideDao(): BatikOverrideDao

    companion object {
        /**
         * v3 → v4: adds the built-in-motif edit overlay table and the two
         * editable content columns on custom motifs. Written explicitly —
         * [fallbackToDestructiveMigration] would otherwise wipe every
         * discovery record, custom motif and custom quiz question the moment
         * an existing install upgrades, which is exactly the data this
         * release is about persisting. The destructive fallback stays for
         * any *other* version gap.
         */
        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `batik_overrides` (" +
                        "`batikId` INTEGER NOT NULL, " +
                        "`name` TEXT NOT NULL, " +
                        "`category` TEXT NOT NULL, " +
                        "`shortDescription` TEXT NOT NULL, " +
                        "`meaning` TEXT NOT NULL, " +
                        "`history` TEXT NOT NULL, " +
                        "`imagePath` TEXT, " +
                        "`updatedAt` INTEGER NOT NULL, " +
                        "PRIMARY KEY(`batikId`))"
                )
                db.execSQL("ALTER TABLE `custom_motifs` ADD COLUMN `meaning` TEXT")
                db.execSQL("ALTER TABLE `custom_motifs` ADD COLUMN `history` TEXT")
            }
        }

        @Volatile
        private var instance: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase =
            instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "jelajah_batik_jambi.db"
                )
                    // Explicit migrations cover every version bump shipped so
                    // far; destructive fallback remains only as a safety net
                    // for gaps with no migration path (§56) — revisit before
                    // any release where wiping local data would matter.
                    .addMigrations(MIGRATION_3_4)
                    .fallbackToDestructiveMigration(true)
                    .build()
                    .also { instance = it }
            }
    }
}
