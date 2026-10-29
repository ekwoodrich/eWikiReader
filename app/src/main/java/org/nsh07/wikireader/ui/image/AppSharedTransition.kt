package org.nsh07.wikireader.ui.image

import androidx.compose.animation.SharedTransitionScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import org.nsh07.wikireader.ui.theme.LocalAppAnimationsEnabled

@Composable
fun SharedTransitionScope.rememberAppSharedContentState(key: Any): SharedTransitionScope.SharedContentState {
    val enabled = LocalAppAnimationsEnabled.current
    val config = remember(enabled) {
        object : SharedTransitionScope.SharedContentConfig {
            override val SharedTransitionScope.SharedContentState.isEnabled: Boolean
                get() = enabled

            override val shouldKeepEnabledForOngoingAnimation: Boolean
                get() = enabled
        }
    }
    return rememberSharedContentState(key, config)
}
