package com.jelajahbatikjambi.database

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * A quiz question the user authored themselves (§ user request — "opsi untuk
 * buat kuis nya"), as opposed to the ones [com.jelajahbatikjambi.data.model.buildQuizQuestions]
 * auto-generates from discovered motifs. Not tied to a specific motif id —
 * the user writes both the prompt and all four options directly, so there's
 * no "correct motif name" to key it to.
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
    val createdAt: Long
)
