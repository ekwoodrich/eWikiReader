package org.nsh07.wikireader

import androidx.compose.ui.test.junit4.createAndroidComposeRule
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.nsh07.wikireader.ui.settingsScreen.viewModel.SettingsAction

class SettingsPersistenceTest {
    @get:Rule val compose = createAndroidComposeRule<MainActivity>()
    @Test fun readingModePersistsAndResets() {
        compose.waitUntil(30000) { compose.activity.viewModel.appStatus.value == org.nsh07.wikireader.data.AppStatus.INITIALIZED }
        val repository = (compose.activity.application as WikiReaderApplication).container.appPreferencesRepository
        compose.runOnUiThread { compose.activity.settingsViewModel.onAction(SettingsAction.SavePagedReading(false)) }
        compose.waitUntil(10000) { !compose.activity.settingsViewModel.preferencesState.value.pagedReading }
        compose.runOnUiThread { compose.activity.settingsViewModel.onAction(SettingsAction.SavePagedReading(true)) }
        compose.waitUntil(10000) { compose.activity.settingsViewModel.preferencesState.value.pagedReading }
        assertEquals(true, runBlocking { repository.readBooleanPreference("paged-reading") })
        compose.activityRule.scenario.recreate()
        compose.waitUntil(10000) { compose.activity.settingsViewModel.preferencesState.value.pagedReading }
        compose.runOnUiThread { compose.activity.settingsViewModel.onAction(SettingsAction.ResetSettings) }
        compose.waitUntil(10000) { !compose.activity.settingsViewModel.preferencesState.value.pagedReading }
        assertNull(runBlocking { repository.readBooleanPreference("paged-reading") })
        compose.runOnUiThread { compose.activity.settingsViewModel.onAction(SettingsAction.SavePagedReading(true)) }
        compose.waitUntil(10000) { compose.activity.settingsViewModel.preferencesState.value.pagedReading }
    }

    @Test fun volumePageTurnSettingPersistsAndResets() {
        compose.waitUntil(30000) { compose.activity.viewModel.appStatus.value == org.nsh07.wikireader.data.AppStatus.INITIALIZED }
        val repository = (compose.activity.application as WikiReaderApplication).container.appPreferencesRepository
        compose.runOnUiThread { compose.activity.settingsViewModel.onAction(SettingsAction.SaveVolumePageTurns(false)) }
        compose.waitUntil(10000) { !compose.activity.settingsViewModel.preferencesState.value.volumePageTurns }
        assertEquals(false, runBlocking { repository.readBooleanPreference("volume-page-turns") })
        compose.activityRule.scenario.recreate()
        compose.waitUntil(10000) { !compose.activity.settingsViewModel.preferencesState.value.volumePageTurns }
        compose.runOnUiThread { compose.activity.settingsViewModel.onAction(SettingsAction.ResetSettings) }
        compose.waitUntil(10000) { compose.activity.settingsViewModel.preferencesState.value.volumePageTurns }
        assertNull(runBlocking { repository.readBooleanPreference("volume-page-turns") })
    }
}
