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
    version = 5,
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

        /**
         * v4 → v5: lets a user-authored quiz question be tied to one motif
         * (the Add Motif form now offers "buat soal untuk motif ini").
         * Written explicitly rather than via [fallbackToDestructiveMigration]
         * for the same reason as [MIGRATION_3_4]: destructive fallback would
         * wipe discoveries, custom motifs and every existing quiz question on
         * upgrade. `-1` is [com.jelajahbatikjambi.data.model.CUSTOM_QUESTION_BATIK_ID],
         * so rows created before this migration read as "not tied to a motif"
         * — exactly their old behaviour. The `DEFAULT -1` must match the
         * entity's `@ColumnInfo(defaultValue = "-1")` or Room's schema check
         * fails after the migration.
         */
        val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "ALTER TABLE `custom_quiz_questions` " +
                        "ADD COLUMN `batikId` INTEGER NOT NULL DEFAULT -1"
                )
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
                    .addMigrations(MIGRATION_3_4, MIGRATION_4_5)
                    .fallbackToDestructiveMigration(true)
                    .build()
                    .also { instance = it }
            }
    }
}
