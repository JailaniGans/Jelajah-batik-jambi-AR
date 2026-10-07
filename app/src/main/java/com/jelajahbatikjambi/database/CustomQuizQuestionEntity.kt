package com.jelajahbatikjambi.database

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.jelajahbatikjambi.data.model.CUSTOM_QUESTION_BATIK_ID

/**
 * A quiz question the user authored themselves (§ user request — "opsi untuk
 * buat kuis nya"), as opposed to the ones [com.jelajahbatikjambi.data.model.buildQuizQuestions]
 * auto-generates from discovered motifs. The user writes both the prompt and
 * all four options directly, so there's no "correct motif name" to key it to.
 *
 * [batikId] optionally ties the question to one motif (added with a motif
 * from the Add Motif form, or later); it stores the *combined* id space used
 * by [BatikData][com.jelajahbatikjambi.data.model.BatikData] — offset 1000
 * for custom motifs — or [CUSTOM_QUESTION_BATIK_ID] when untied. A tied
 * question is offered when quizzing that motif right after an AR scan;
 * an untied one joins the general quiz.
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
