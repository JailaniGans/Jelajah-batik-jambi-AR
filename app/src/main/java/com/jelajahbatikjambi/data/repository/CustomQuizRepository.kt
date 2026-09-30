package com.jelajahbatikjambi.data.repository

import com.jelajahbatikjambi.data.model.CUSTOM_QUESTION_BATIK_ID
import com.jelajahbatikjambi.data.model.QuizQuestion
import com.jelajahbatikjambi.database.CustomQuizQuestionDao
import com.jelajahbatikjambi.database.CustomQuizQuestionEntity

/**
 * Wraps [CustomQuizQuestionDao], exposing user-authored quiz questions
 * (§ user request: "opsi untuk buat kuis nya") as plain [QuizQuestion] so
 * [QuizViewModel][com.jelajahbatikjambi.ui.quiz.QuizViewModel] can merge them
 * with the auto-generated ones with no extra branching.
 */
class CustomQuizRepository(private val dao: CustomQuizQuestionDao) {

    suspend fun getAllOnceAsQuizQuestions(): List<QuizQuestion> =
        dao.getAllOnce().map { it.toQuizQuestion() }

    suspend fun addQuestion(
        prompt: String,
        options: List<String>,
        correctOptionIndex: Int
    ): Long {
        require(options.size == 4) { "A quiz question needs exactly 4 options" }
        return dao.insert(
            CustomQuizQuestionEntity(
                prompt = prompt,
                optionA = options[0],
                optionB = options[1],
                optionC = options[2],
                optionD = options[3],
                correctOptionIndex = correctOptionIndex,
                createdAt = System.currentTimeMillis()
            )
        )
    }
}

private fun CustomQuizQuestionEntity.toQuizQuestion() = QuizQuestion(
    batikId = CUSTOM_QUESTION_BATIK_ID,
    prompt = prompt,
    options = listOf(optionA, optionB, optionC, optionD),
    correctOptionIndex = correctOptionIndex
)
