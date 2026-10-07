package com.jelajahbatikjambi.ui.quiz

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.jelajahbatikjambi.data.model.QuizQuestion
import com.jelajahbatikjambi.ui.theme.JelajahBatikJambiTheme
import org.junit.Rule
import org.junit.Test

/**
 * Tests the stateless quiz composables directly with hand-built [QuizUiState]
 * values, rather than driving [QuizScreen] end-to-end through [QuizViewModel].
 * The ViewModel reads persisted Room discovery state, which `adb install -r`
 * keeps across app installs — an end-to-end test asserting "nothing
 * discovered yet" would pass or fail depending on what earlier manual
 * testing left in the database, not on the code under test.
 */
class QuizScreenComposablesTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private val sampleQuestion = QuizQuestion(
        batikId = 1,
        prompt = "Motif ini terinspirasi dari bentuk buah durian.",
        options = listOf("Angso Duo", "Durian Pecah", "Kapal Sanggat", "Tampuk Manggis"),
        correctOptionIndex = 1
    )

    @Test
    fun emptyState_shows_prompt_to_explore_first() {
        composeTestRule.setContent {
            JelajahBatikJambiTheme {
                EmptyState()
            }
        }

        composeTestRule.onNodeWithText("Belum ada motif untuk dikuiskan").assertIsDisplayed()
    }

    @Test
    fun questionState_shows_prompt_and_all_options() {
        val uiState = QuizUiState(isLoading = false, questions = listOf(sampleQuestion))

        composeTestRule.setContent {
            JelajahBatikJambiTheme {
                QuestionState(uiState = uiState, onSelectOption = {}, onNext = {})
            }
        }

        composeTestRule.onNodeWithText(sampleQuestion.prompt).assertIsDisplayed()
        sampleQuestion.options.forEach { option ->
            composeTestRule.onNodeWithText(option).assertIsDisplayed()
        }
    }

    @Test
    fun questionState_selecting_an_option_invokes_callback_with_its_index() {
        val uiState = QuizUiState(isLoading = false, questions = listOf(sampleQuestion))
        var selectedIndex: Int? = null

        composeTestRule.setContent {
            JelajahBatikJambiTheme {
                QuestionState(uiState = uiState, onSelectOption = { selectedIndex = it }, onNext = {})
            }
        }

        composeTestRule.onNodeWithText("Durian Pecah").performClick()

        assert(selectedIndex == 1) { "expected index 1, got $selectedIndex" }
    }

    @Test
    fun questionState_after_answering_shows_next_button() {
        val uiState = QuizUiState(
            isLoading = false,
            questions = listOf(sampleQuestion),
            selectedOptionIndex = 1
        )

        composeTestRule.setContent {
            JelajahBatikJambiTheme {
                QuestionState(uiState = uiState, onSelectOption = {}, onNext = {})
            }
        }

        composeTestRule.onNodeWithText("Lihat Skor").assertIsDisplayed()
    }

    @Test
    fun resultState_shows_score_and_invokes_onBack() {
        var backClicked = false

        composeTestRule.setContent {
            JelajahBatikJambiTheme {
                ResultState(score = 3, total = 4, onBack = { backClicked = true })
            }
        }

        composeTestRule.onNodeWithText("3 / 4").assertIsDisplayed()
        composeTestRule.onNodeWithText("Selesai").performClick()

        assert(backClicked)
    }

    @Test
    fun resultState_shows_play_again_and_invokes_it_without_leaving() {
        var playAgainClicked = false
        var backClicked = false

        composeTestRule.setContent {
            JelajahBatikJambiTheme {
                ResultState(
                    score = 3,
                    total = 4,
                    onBack = { backClicked = true },
                    onPlayAgain = { playAgainClicked = true }
                )
            }
        }

        composeTestRule.onNodeWithText("Main Lagi").assertIsDisplayed()
        composeTestRule.onNodeWithText("Main Lagi").performClick()

        assert(playAgainClicked) { "expected Main Lagi to invoke onPlayAgain" }
        assert(!backClicked) { "Main Lagi must not also pop the quiz screen" }
    }
}
