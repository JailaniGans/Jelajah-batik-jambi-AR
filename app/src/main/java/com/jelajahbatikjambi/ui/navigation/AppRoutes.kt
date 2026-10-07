package com.jelajahbatikjambi.ui.navigation

object AppRoutes {
    const val HOME = "home"
    const val AR = "ar"
    const val COLLECTION = "collection"
    const val ABOUT = "about"
    const val ADD_MOTIF = "add_motif"
    const val MANAGE_CUSTOM_QUIZ = "manage_custom_quiz"

    // Same absent-vs-present convention as QUIZ below: no {batikId}
    // placeholder in the base route, an absent arg (Collection's manage
    // screen) vs. a present one (created for a just-scanned motif, or from
    // that motif's edit screen) is told apart by defaultValue = -1.
    const val CREATE_QUIZ_BATIK_ID_ARG = "batikId"
    const val CREATE_QUIZ = "create_quiz?$CREATE_QUIZ_BATIK_ID_ARG={$CREATE_QUIZ_BATIK_ID_ARG}"

    fun createQuiz(batikId: Int? = null) =
        if (batikId != null) "create_quiz?$CREATE_QUIZ_BATIK_ID_ARG=$batikId" else "create_quiz"

    const val DETAIL_ARG = "batikId"
    const val DETAIL = "detail/{$DETAIL_ARG}"

    fun detail(batikId: Int) = "detail/$batikId"

    const val EDIT_MOTIF_ARG = "batikId"
    const val EDIT_MOTIF = "edit_motif/{$EDIT_MOTIF_ARG}"

    fun editMotif(batikId: Int) = "edit_motif/$batikId"

    const val EDIT_QUIZ_ARG = "questionId"
    const val EDIT_QUIZ = "edit_quiz_question/{$EDIT_QUIZ_ARG}"

    fun editQuizQuestion(questionId: Long) = "edit_quiz_question/$questionId"

    // No {batikId} placeholder in the base route: NavType.IntType arguments
    // can't be declared nullable, so an absent query param (Collection's
    // general quiz) vs. a present one (scoped to a just-scanned motif) is
    // told apart by defaultValue = -1 instead — see AppNavHost.
    const val QUIZ_BATIK_ID_ARG = "batikId"
    const val QUIZ = "quiz?$QUIZ_BATIK_ID_ARG={$QUIZ_BATIK_ID_ARG}"

    fun quiz(batikId: Int? = null) = if (batikId != null) "quiz?$QUIZ_BATIK_ID_ARG=$batikId" else "quiz"
}
