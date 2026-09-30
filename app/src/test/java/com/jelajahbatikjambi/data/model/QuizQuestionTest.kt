package com.jelajahbatikjambi.data.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class QuizQuestionTest {

    private val allBatik = listOf(
        BatikData(1, 0, "Angso Duo", "c", "desc A", "m", "h", null, null),
        BatikData(2, 1, "Tampuk Manggis", "c", "desc B", "m", "h", null, null),
        BatikData(3, 2, "Durian Pecah", "c", "desc C", "m", "h", null, null),
        BatikData(4, 3, "Kapal Sanggat", "c", "desc D", "m", "h", null, null)
    )

    @Test
    fun `only discovered motifs get a question`() {
        val questions = buildQuizQuestions(allBatik, discoveredIds = setOf(1, 3))

        assertEquals(setOf(1, 3), questions.map { it.batikId }.toSet())
    }

    @Test
    fun `nothing discovered means no questions`() {
        val questions = buildQuizQuestions(allBatik, discoveredIds = emptySet())

        assertTrue(questions.isEmpty())
    }

    @Test
    fun `each question has exactly one correct option matching the motif name`() {
        val questions = buildQuizQuestions(allBatik, discoveredIds = setOf(1, 2, 3, 4))

        for (question in questions) {
            val batik = allBatik.first { it.id == question.batikId }
            assertEquals(4, question.options.size)
            assertEquals(batik.name, question.options[question.correctOptionIndex])
            // Options must be distinct (no duplicate distractor == correct answer).
            assertEquals(question.options.size, question.options.toSet().size)
        }
    }

    @Test
    fun `question prompt is the motif's short description, never meaning or history`() {
        val questions = buildQuizQuestions(allBatik, discoveredIds = setOf(1))

        val question = questions.single()
        val batik = allBatik.first { it.id == question.batikId }
        assertEquals(batik.shortDescription, question.prompt)
    }

    @Test
    fun `scoped quiz only contains questions about the scanned motif`() {
        val scanned = allBatik.first { it.id == 2 }
        val questions = buildQuizQuestionsForMotif(scanned, allBatik)

        assertTrue(questions.isNotEmpty())
        assertTrue(questions.all { it.batikId == scanned.id })
    }

    @Test
    fun `scoped quiz includes a category question when other categories exist`() {
        val varied = listOf(
            BatikData(1, 0, "Angso Duo", "Flora", "desc A", "m", "h", null, null),
            BatikData(2, 1, "Tampuk Manggis", "Fauna", "desc B", "m", "h", null, null)
        )
        val scanned = varied.first { it.id == 1 }
        val questions = buildQuizQuestionsForMotif(scanned, varied)

        assertEquals(2, questions.size)
        val categoryQuestion = questions.first { it.prompt.contains("kategori") }
        assertEquals(scanned.category, categoryQuestion.options[categoryQuestion.correctOptionIndex])
    }

    @Test
    fun `scoped quiz skips the category question when every motif shares one category`() {
        val scanned = allBatik.first { it.id == 1 }
        val questions = buildQuizQuestionsForMotif(scanned, allBatik)

        assertEquals(1, questions.size)
        assertEquals(scanned.shortDescription, questions.single().prompt)
    }
}
