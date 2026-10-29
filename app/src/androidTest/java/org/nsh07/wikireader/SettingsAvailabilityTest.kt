package org.nsh07.wikireader

import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.nsh07.wikireader.ui.homeScreen.viewModel.HomeSubscreen
import org.nsh07.wikireader.ui.settingsScreen.SettingsScreen
import org.nsh07.wikireader.ui.settingsScreen.viewModel.PreferencesState
import org.nsh07.wikireader.ui.settingsScreen.viewModel.SettingsAction
import org.nsh07.wikireader.ui.theme.LocalAppAnimationsEnabled
import org.nsh07.wikireader.ui.theme.WikiReaderTheme

class SettingsAvailabilityTest {
    @get:Rule val compose = createComposeRule()

    private fun toggle(label: Int): SemanticsNodeInteraction {
        val tag = "settings-toggle-$label"
        compose.onNodeWithTag("settings-list").performScrollToNode(hasTestTag(tag))
        return compose.onNodeWithTag(tag).assertIsDisplayed()
    }

    @Test fun repeatedModeChangesKeepRowsVisibleAndDisableOnlyUnusedOptions() {
        var preferences by mutableStateOf(PreferencesState(pagedReading = true,
            expandedSections = true, immersiveMode = true, disableAnimations = true))
        compose.setContent {
            val context = LocalContext.current
            val themeLabel = context.getString(R.string.themeSystemDefault)
            WikiReaderTheme(disableAnimations = preferences.disableAnimations) {
                CompositionLocalProvider(LocalAppAnimationsEnabled provides !preferences.disableAnimations) {
                    SettingsScreen(
                        preferencesState = preferences, lastBackStackEntry = HomeSubscreen.Logo,
                        recentLangs = emptyList(), languageSearchStr = "", languageSearchQuery = "",
                        themeMap = mapOf("auto" to (R.drawable.brightness_auto to themeLabel)),
                        reverseThemeMap = mapOf(themeLabel to "auto"),
                        fontStyles = listOf("Sans-serif", "Serif"),
                        fontStyleMap = mapOf("sans" to "Sans-serif", "serif" to "Serif"),
                        reverseFontStyleMap = mapOf("Sans-serif" to "sans", "Serif" to "serif"),
                        onAction = { action -> preferences = when (action) {
                            is SettingsAction.SavePagedReading -> preferences.copy(pagedReading = action.value)
                            is SettingsAction.SaveDisableAnimations -> preferences.copy(disableAnimations = action.value)
                            else -> preferences
                        } }, onHomeAction = {}, onBack = {}
                    )
                }
            }
        }
        repeat(4) { cycle ->
            toggle(R.string.volume_page_turns).assertIsEnabled().assertIsOn()
            toggle(R.string.disable_animations).assertIsEnabled()
            toggle(R.string.settingImmersiveMode).assertIsNotEnabled().assertIsOn()
            toggle(R.string.settingExpandSections).assertIsNotEnabled().assertIsOn()
            toggle(R.string.paged_reading).performClick()
            toggle(R.string.volume_page_turns).assertIsNotEnabled().assertIsOn()
            toggle(R.string.disable_animations).assertIsEnabled()
            toggle(R.string.settingImmersiveMode).assertIsEnabled().assertIsOn()
            toggle(R.string.settingExpandSections).assertIsEnabled().assertIsOn()
            toggle(R.string.paged_reading).performClick()
            val volumeBounds = toggle(R.string.volume_page_turns).assertIsEnabled().getUnclippedBoundsInRoot()
            assertTrue("Volume row collapsed after cycle $cycle", (volumeBounds.bottom - volumeBounds.top).value > 20)
            // Exercise both motion settings while changing the mode.
            if (cycle == 1) toggle(R.string.disable_animations).performClick()
        }
    }
}
