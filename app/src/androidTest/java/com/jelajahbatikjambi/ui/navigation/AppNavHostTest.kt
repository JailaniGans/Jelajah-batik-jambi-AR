package com.jelajahbatikjambi.ui.navigation

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.jelajahbatikjambi.ui.theme.JelajahBatikJambiTheme
import org.junit.Rule
import org.junit.Test

/**
 * Covers §52's "Navigation" instrumentation item using the About route,
 * which needs no runtime permission — unlike AR, so it's safe to drive
 * end-to-end without a permission-granting test rule.
 */
class AppNavHostTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun navigating_to_about_and_back_returns_to_home() {
        composeTestRule.setContent {
            JelajahBatikJambiTheme {
                AppNavHost()
            }
        }

        composeTestRule.onNodeWithText("JELAJAH BATIK JAMBI").assertExists()

        composeTestRule.onNodeWithText("Tentang").performClick()
        composeTestRule.onNodeWithText("Tentang Jelajah Batik Jambi").assertExists()

        composeTestRule.onNodeWithText("JELAJAH BATIK JAMBI").assertDoesNotExist()
    }
}
