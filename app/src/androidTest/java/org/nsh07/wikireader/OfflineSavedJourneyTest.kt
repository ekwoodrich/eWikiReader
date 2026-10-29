package org.nsh07.wikireader

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assume.assumeTrue
import org.junit.Rule
import org.junit.Test
import org.nsh07.wikireader.ui.homeScreen.viewModel.HomeAction
import org.nsh07.wikireader.ui.settingsScreen.viewModel.SettingsAction

/** Run explicitly with -e offline true after the host disables Wi-Fi and mobile data. */
class OfflineSavedJourneyTest {
    @get:Rule val compose = createAndroidComposeRule<MainActivity>()
    @Test fun savedArticlePagesWithoutNetwork() {
        assumeTrue(InstrumentationRegistry.getArguments().getString("offline") == "true")
        compose.waitUntil(30000) { compose.activity.viewModel.appStatus.value == org.nsh07.wikireader.data.AppStatus.INITIALIZED }
        val app = compose.activity.application as WikiReaderApplication
        val article = runBlocking { app.container.appDatabaseRepository.getSavedArticles().first().first { it.title == "E Ink" } }
        compose.runOnUiThread {
            compose.activity.settingsViewModel.onAction(SettingsAction.SavePagedReading(true))
            compose.activity.viewModel.onAction(HomeAction.StopAll)
            compose.activity.viewModel.onAction(HomeAction.LoadSavedArticle(article.pageId, article.lang))
        }
        compose.waitUntil(30000) { compose.onAllNodesWithTag("reader-page").fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithTag("next-page").assertIsEnabled().performClick()
        compose.onNodeWithTag("previous-page").assertIsEnabled().performClick()
    }
}
