package org.nsh07.wikireader.ui.homeScreen

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.snap
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import org.nsh07.wikireader.ui.theme.LocalAppAnimationsEnabled
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import ir.ehsannarmani.compose_charts.LineChart
import ir.ehsannarmani.compose_charts.models.DividerProperties
import ir.ehsannarmani.compose_charts.models.DotProperties
import ir.ehsannarmani.compose_charts.models.GridProperties
import ir.ehsannarmani.compose_charts.models.HorizontalIndicatorProperties
import ir.ehsannarmani.compose_charts.models.LabelHelperProperties
import ir.ehsannarmani.compose_charts.models.LabelProperties
import ir.ehsannarmani.compose_charts.models.Line
import ir.ehsannarmani.compose_charts.models.PopupProperties

@Composable
fun ArticleViewsGraph(viewCounts: List<Int>, modifier: Modifier = Modifier) {
    val colorScheme = MaterialTheme.colorScheme
    val animationsEnabled = LocalAppAnimationsEnabled.current
    LineChart(
        modifier = modifier,
        data = remember(viewCounts, animationsEnabled) {
            listOf(
                Line(
                    label = "",
                    values = viewCounts.map { it.toDouble() },
                    color = SolidColor(colorScheme.primary),
                    strokeAnimationSpec = if (animationsEnabled) tween(1500, easing = FastOutSlowInEasing) else snap(),
                    curvedEdges = true
                )
            )
        },
        labelHelperProperties = LabelHelperProperties(enabled = false),
        gridProperties = GridProperties(enabled = false),
        popupProperties = PopupProperties(enabled = false),
        labelProperties = LabelProperties(enabled = false),
        indicatorProperties = HorizontalIndicatorProperties(enabled = false),
        dotsProperties = DotProperties(enabled = false),
        dividerProperties = DividerProperties(enabled = false)
    )
}
