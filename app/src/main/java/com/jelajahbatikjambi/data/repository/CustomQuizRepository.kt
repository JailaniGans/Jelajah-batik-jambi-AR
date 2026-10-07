package com.jelajahbatikjambi.data.repository

import com.jelajahbatikjambi.data.model.CUSTOM_QUESTION_BATIK_ID
import com.jelajahbatikjambi.data.model.QuizQuestion
import com.jelajahbatikjambi.database.CustomQuizQuestionDao
import com.jelajahbatikjambi.database.CustomQuizQuestionEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * Wraps [CustomQuizQuestionDao], exposing user-authored quiz questions
 * (§ user request: "opsi untuk buat kuis nya") as plain [QuizQuestion] so
 * [QuizViewModel][com.jelajahbatikjambi.ui.quiz.QuizViewModel] can merge them
 * with the auto-generated ones with no extra branching.
 */
class CustomQuizRepository(private val dao: CustomQuizQuestionDao) {

    suspend fun getAllOnceAsQuizQuestions(): List<QuizQuestion> =
        dao.getAllOnce().map { it.toQuizQuestion() }

    /** Live list of raw entities — used by the manage screen so an edit/delete shows up immediately. */
    fun observeAllAsEntities(): Flow<List<CustomQuizQuestionEntity>> = dao.observeAll()

    suspend fun getById(id: Long): CustomQuizQuestionEntity? = dao.getById(id)

    /** Persists an edit to an existing question; the row keeps its id so the combined id is stable. */
    suspend fun updateQuestion(question: CustomQuizQuestionEntity) = dao.update(question)

    suspend fun deleteQuestion(question: CustomQuizQuestionEntity) = dao.delete(question)

    suspend fun addQuestion(
        prompt: String,
        options: List<String>,
        correctOptionIndex: Int,
        /** Combined motif id ([BatikData][com.jelajahbatikjambi.data.model.BatikData] id space) this question is about, or [CUSTOM_QUESTION_BATIK_ID] for a free-standing one. */
        batikId: Int = CUSTOM_QUESTION_BATIK_ID
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
                batikId = batikId,
                createdAt = System.currentTimeMillis()
            )
        )
    }
}

private fun CustomQuizQuestionEntity.toQuizQuestion() = QuizQuestion(
    batikId = batikId,
    prompt = prompt,
    options = listOf(optionA, optionB, optionC, optionD),
    correctOptionIndex = correctOptionIndex
)
