package com.jelajahbatikjambi.ui.home

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.jelajahbatikjambi.ui.theme.JelajahBatikJambiTheme
import org.junit.Rule
import org.junit.Test

/** Covers §52's "Home screen" instrumentation item: content renders and each button fires its callback. */
class HomeScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun title_and_tagline_are_displayed() {
        composeTestRule.setContent {
            JelajahBatikJambiTheme {
                HomeScreen(onMulaiJelajah = {}, onKoleksiBatik = {}, onTentang = {})
            }
        }

        composeTestRule.onNodeWithText("JELAJAH BATIK JAMBI").assertExists()
        composeTestRule.onNodeWithText("Kenali budaya Jambi melalui pengalaman augmented reality.").assertExists()
    }

    @Test
    fun mulai_jelajah_button_invokes_callback() {
        var clicked = false
        composeTestRule.setContent {
            JelajahBatikJambiTheme {
                HomeScreen(onMulaiJelajah = { clicked = true }, onKoleksiBatik = {}, onTentang = {})
            }
        }

        composeTestRule.onNodeWithText("Mulai Jelajah").performClick()

        assert(clicked)
    }

    @Test
    fun koleksi_batik_button_invokes_callback() {
        var clicked = false
        composeTestRule.setContent {
            JelajahBatikJambiTheme {
                HomeScreen(onMulaiJelajah = {}, onKoleksiBatik = { clicked = true }, onTentang = {})
            }
        }

        composeTestRule.onNodeWithText("Koleksi Batik").performClick()

        assert(clicked)
    }

    @Test
    fun tentang_button_invokes_callback() {
        var clicked = false
        composeTestRule.setContent {
            JelajahBatikJambiTheme {
                HomeScreen(onMulaiJelajah = {}, onKoleksiBatik = {}, onTentang = { clicked = true })
            }
        }

        composeTestRule.onNodeWithText("Tentang").performClick()

        assert(clicked)
    }
}
