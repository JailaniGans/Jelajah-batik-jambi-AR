package com.jelajahbatikjambi.data.model

/**
 * [QuizQuestion.batikId] for a question stored before tying every question
 * to a motif became mandatory — it isn't tied to any motif, so it only ever
 * shows up in the general quiz.
 */
const val CUSTOM_QUESTION_BATIK_ID = -1

/** One user-authored multiple-choice quiz question, tied to a motif via [batikId]. */
data class QuizQuestion(
    val batikId: Int,
    val prompt: String,
    val options: List<String>,
    val correctOptionIndex: Int
)
