package com.jelajahbatikjambi.data.repository

import com.jelajahbatikjambi.data.model.QuizQuestion
import com.jelajahbatikjambi.database.CustomQuizQuestionDao
import com.jelajahbatikjambi.database.CustomQuizQuestionEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * Wraps [CustomQuizQuestionDao], exposing user-authored quiz questions
 * (§ user request: "opsi untuk buat kuis nya") as plain [QuizQuestion] —
 * the whole question set, since the app generates none of its own — for
 * [QuizViewModel][com.jelajahbatikjambi.ui.quiz.QuizViewModel].
 */
class CustomQuizRepository(private val dao: CustomQuizQuestionDao) {

    suspend fun getAllOnceAsQuizQuestions(): List<QuizQuestion> =
        dao.getAllOnce().map { it.toQuizQuestion() }

    /** Live list of raw entities — used by the manage screen so an edit/delete shows up immediately. */
    fun observeAllAsEntities(): Flow<List<CustomQuizQuestionEntity>> = dao.observeAll()

    suspend fun getById(id: Long): CustomQuizQuestionEntity? = dao.getById(id)

    /** Live list of the questions keyed to [batikId] — used by the motif edit screen. */
    fun observeByBatikId(batikId: Int): Flow<List<CustomQuizQuestionEntity>> = dao.observeByBatikId(batikId)

    /** Persists an edit to an existing question; the row keeps its id so the combined id is stable. */
    suspend fun updateQuestion(question: CustomQuizQuestionEntity) = dao.update(question)

    suspend fun deleteQuestion(question: CustomQuizQuestionEntity) = dao.delete(question)

    /** Deletes every question keyed to [batikId] — cascade used when a motif is deleted. */
    suspend fun deleteByBatikId(batikId: Int) = dao.deleteByBatikId(batikId)

    suspend fun addQuestion(
        prompt: String,
        options: List<String>,
        correctOptionIndex: Int,
        /** Combined motif id ([BatikData][com.jelajahbatikjambi.data.model.BatikData] id space) this question is about — every question must name a motif. */
        batikId: Int
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
