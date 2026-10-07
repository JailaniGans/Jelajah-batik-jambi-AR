package com.jelajahbatikjambi.database

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.jelajahbatikjambi.data.model.CUSTOM_QUESTION_BATIK_ID

/**
 * A quiz question the user authored themselves (§ user request — "opsi untuk
 * buat kuis nya"). The user writes both the prompt and all four options
 * directly; the app generates no questions of its own.
 *
 * [batikId] ties the question to one motif — the *combined* id space used
 * by [BatikData][com.jelajahbatikjambi.data.model.BatikData] — offset 1000
 * for custom motifs. A tied question is offered when quizzing that motif:
 * right after an AR scan, or from that motif's edit screen. Questions stored
 * before ties became mandatory keep [CUSTOM_QUESTION_BATIK_ID] and only
 * join the general quiz — editing one asks for a motif to be picked.
 *
 * `defaultValue` must match the `ALTER TABLE ... DEFAULT -1` in
 * [MIGRATION_4_5][com.jelajahbatikjambi.database.AppDatabase], otherwise
 * Room's schema check rejects the migrated table.
 */
@Entity(tableName = "custom_quiz_questions")
data class CustomQuizQuestionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val prompt: String,
    val optionA: String,
    val optionB: String,
    val optionC: String,
    val optionD: String,
    val correctOptionIndex: Int,
    @ColumnInfo(defaultValue = "-1") val batikId: Int = CUSTOM_QUESTION_BATIK_ID,
    val createdAt: Long
)
