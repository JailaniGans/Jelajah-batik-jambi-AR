package com.jelajahbatikjambi.ui.navigation

import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.core.tween
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.jelajahbatikjambi.ui.about.AboutScreen
import com.jelajahbatikjambi.ui.addmotif.AddMotifScreen
import com.jelajahbatikjambi.ui.ar.ArScreen
import com.jelajahbatikjambi.ui.collection.CollectionScreen
import com.jelajahbatikjambi.ui.detail.DetailScreen
import com.jelajahbatikjambi.ui.editmotif.EditMotifScreen
import com.jelajahbatikjambi.ui.home.HomeScreen
import com.jelajahbatikjambi.ui.quiz.CreateQuizScreen
import com.jelajahbatikjambi.ui.quiz.QuizScreen
import com.jelajahbatikjambi.ui.editquizquestion.EditQuizQuestionScreen
import com.jelajahbatikjambi.ui.managecustomquiz.ManageCustomQuizScreen
import com.jelajahbatikjambi.ui.managemotifs.ManageMotifsScreen

private const val TRANSITION_DURATION_MS = 200

@Composable
fun AppNavHost(
    navController: NavHostController = rememberNavController()
) {
    NavHost(
        navController = navController,
        startDestination = AppRoutes.HOME,
        // Light, consistent screen transitions (§59 "UI refinement") — a
        // subtle fade+slide, not flashy, matching the app's minimal style.
        enterTransition = {
            fadeIn(tween(TRANSITION_DURATION_MS)) +
                slideInHorizontally(tween(TRANSITION_DURATION_MS)) { it / 8 }
        },
        exitTransition = { fadeOut(tween(TRANSITION_DURATION_MS)) },
        popEnterTransition = { fadeIn(tween(TRANSITION_DURATION_MS)) },
        popExitTransition = {
            fadeOut(tween(TRANSITION_DURATION_MS)) +
                slideOutHorizontally(tween(TRANSITION_DURATION_MS)) { it / 8 }
        }
    ) {
        composable(AppRoutes.HOME) {
            HomeScreen(
                onMulaiJelajah = { navController.navigate(AppRoutes.AR) },
                onKoleksiBatik = { navController.navigate(AppRoutes.COLLECTION) },
                onTentang = { navController.navigate(AppRoutes.ABOUT) }
            )
        }
        composable(AppRoutes.AR) {
            ArScreen(
                onBack = { navController.popBackStack() },
                onViewDetail = { batikId -> navController.navigate(AppRoutes.detail(batikId)) },
                onStartQuiz = { batikId -> navController.navigate(AppRoutes.quiz(batikId)) }
            )
        }
        composable(AppRoutes.COLLECTION) {
            CollectionScreen(
                onBack = { navController.popBackStack() },
                onViewDetail = { batikId -> navController.navigate(AppRoutes.detail(batikId)) },
                onStartQuiz = { navController.navigate(AppRoutes.quiz()) },
                onAddMotif = { navController.navigate(AppRoutes.ADD_MOTIF) },
                onManageMotifs = { navController.navigate(AppRoutes.MANAGE_MOTIFS) },
                onManageQuiz = { navController.navigate(AppRoutes.MANAGE_CUSTOM_QUIZ) }
            )
        }
        composable(AppRoutes.ADD_MOTIF) {
            AddMotifScreen(
                onBack = { navController.popBackStack() },
                onSaved = { navController.popBackStack() }
            )
        }
        composable(
            route = AppRoutes.QUIZ,
            arguments = listOf(navArgument(AppRoutes.QUIZ_BATIK_ID_ARG) {
                type = NavType.IntType
                defaultValue = -1
            })
        ) { backStackEntry ->
            val batikId = backStackEntry.arguments?.getInt(AppRoutes.QUIZ_BATIK_ID_ARG) ?: -1
            QuizScreen(
                batikId = batikId.takeIf { it >= 0 },
                onBack = { navController.popBackStack() }
            )
        }
        composable(
            route = AppRoutes.CREATE_QUIZ,
            arguments = listOf(navArgument(AppRoutes.CREATE_QUIZ_BATIK_ID_ARG) {
                type = NavType.IntType
                defaultValue = -1
            })
        ) { backStackEntry ->
            val batikId = backStackEntry.arguments?.getInt(AppRoutes.CREATE_QUIZ_BATIK_ID_ARG) ?: -1
            CreateQuizScreen(
                batikId = batikId.takeIf { it >= 0 },
                onBack = { navController.popBackStack() },
                onSaved = { navController.popBackStack() }
            )
        }
        composable(AppRoutes.MANAGE_CUSTOM_QUIZ) {
            ManageCustomQuizScreen(
                onBack = { navController.popBackStack() },
                onAddQuestion = { navController.navigate(AppRoutes.createQuiz()) },
                onEditQuestion = { questionId -> navController.navigate(AppRoutes.editQuizQuestion(questionId)) }
            )
        }
        composable(AppRoutes.MANAGE_MOTIFS) {
            ManageMotifsScreen(
                onBack = { navController.popBackStack() },
                onEdit = { batikId -> navController.navigate(AppRoutes.editMotif(batikId)) },
                onAddMotif = { navController.navigate(AppRoutes.ADD_MOTIF) }
            )
        }
        composable(
            route = AppRoutes.EDIT_QUIZ,
            arguments = listOf(navArgument(AppRoutes.EDIT_QUIZ_ARG) { type = NavType.LongType })
        ) { backStackEntry ->
            val questionId = backStackEntry.arguments?.getLong(AppRoutes.EDIT_QUIZ_ARG) ?: return@composable
            EditQuizQuestionScreen(
                questionId = questionId,
                onBack = { navController.popBackStack() },
                onSaved = { navController.popBackStack() }
            )
        }
        composable(AppRoutes.ABOUT) {
            AboutScreen(
                onBack = { navController.popBackStack() }
            )
        }
        composable(
            route = AppRoutes.DETAIL,
            arguments = listOf(navArgument(AppRoutes.DETAIL_ARG) { type = NavType.IntType })
        ) { backStackEntry ->
            val batikId = backStackEntry.arguments?.getInt(AppRoutes.DETAIL_ARG) ?: return@composable
            DetailScreen(
                batikId = batikId,
                onBack = { navController.popBackStack() },
                onJelajahiDenganAr = { navController.popBackStack() }
            )
        }
        composable(
            route = AppRoutes.EDIT_MOTIF,
            arguments = listOf(navArgument(AppRoutes.EDIT_MOTIF_ARG) { type = NavType.IntType })
        ) { backStackEntry ->
            val batikId = backStackEntry.arguments?.getInt(AppRoutes.EDIT_MOTIF_ARG) ?: return@composable
            EditMotifScreen(
                batikId = batikId,
                onBack = { navController.popBackStack() },
                // Back on Detail, which re-reads the repository's Flow and
                // therefore shows the freshly saved values immediately.
                onSaved = { navController.popBackStack() },
                onEditQuestion = { questionId ->
                    navController.navigate(AppRoutes.editQuizQuestion(questionId))
                },
                onAddQuestion = { navController.navigate(AppRoutes.createQuiz(batikId)) },
                onDeleted = {
                    // Detail would now point at nothing — pop past it too,
                    // landing back on whatever opened it (Collection or AR).
                    navController.popBackStack()
                    navController.popBackStack()
                }
            )
        }
    }
}
