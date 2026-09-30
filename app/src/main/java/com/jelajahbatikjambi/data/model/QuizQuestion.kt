package com.jelajahbatikjambi.data.model

/** [QuizQuestion.batikId] for a user-authored question — it isn't tied to any motif. */
const val CUSTOM_QUESTION_BATIK_ID = -1

/** One "guess the motif" quiz question — see [buildQuizQuestions]. */
data class QuizQuestion(
    val batikId: Int,
    val prompt: String,
    val options: List<String>,
    val correctOptionIndex: Int
)

/**
 * Builds "guess the motif from its description" quiz questions from
 * already-discovered motifs — a knowledge quiz tied to Collection progress
 * (only quiz on what's actually been found). Distractor options are drawn
 * from all known motif names, not just discovered ones — a wrong-answer
 * option doesn't need to have been learned, only the correct answer does.
 *
 * Deliberately quizzes on name<->[BatikData.shortDescription] matching only,
 * never [BatikData.meaning]/[BatikData.history] — those fields are
 * explicitly unverified placeholder content (§38), and presenting them as a
 * quiz's "correct answer" would assert them as fact in a way the rest of the
 * app is careful not to.
 */
fun buildQuizQuestions(allBatik: List<BatikData>, discoveredIds: Set<Int>): List<QuizQuestion> {
    val discovered = allBatik.filter { it.id in discoveredIds }
    val allNames = allBatik.map { it.name }
    return discovered.map { batik -> buildNameGuessQuestion(batik, allNames) }
}

/**
 * Builds the small set of questions about exactly one motif — used to start
 * a quiz right after scanning it in AR (§ user request: "ketika klik mulai
 * kuis pertanyaan sesuai dengan motif apa yang saya scan"), instead of the
 * general "guess which discovered motif" mix [buildQuizQuestions] produces.
 * Draws two independent angles on the same verified fields
 * (name<->description, name<->category) rather than inventing new facts
 * about the motif just to pad out the question count.
 */
fun buildQuizQuestionsForMotif(batik: BatikData, allBatik: List<BatikData>): List<QuizQuestion> {
    val questions = mutableListOf(buildNameGuessQuestion(batik, allBatik.map { it.name }))

    val categoryOptions = (allBatik.map { it.category }.filter { it != batik.category }.distinct().shuffled().take(3) + batik.category)
        .distinct()
        .shuffled()
    // Only ask a category question when there's at least one real
    // alternative category to pick from — otherwise every option would be
    // the same and it wouldn't be a real question.
    if (categoryOptions.size > 1) {
        questions += QuizQuestion(
            batikId = batik.id,
            prompt = "Motif \"${batik.name}\" termasuk dalam kategori apa?",
            options = categoryOptions,
            correctOptionIndex = categoryOptions.indexOf(batik.category)
        )
    }
    return questions.shuffled()
}

private fun buildNameGuessQuestion(batik: BatikData, allNames: List<String>): QuizQuestion {
    val distractors = allNames.filter { it != batik.name }.shuffled().take(3)
    val options = (distractors + batik.name).shuffled()
    return QuizQuestion(
        batikId = batik.id,
        prompt = batik.shortDescription,
        options = options,
        correctOptionIndex = options.indexOf(batik.name)
    )
}
