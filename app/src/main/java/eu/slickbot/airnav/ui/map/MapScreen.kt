package eu.slickbot.airnav.ui.map

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@Composable
fun MapScreen(modifier: Modifier = Modifier) {
    LeafletWebView(modifier = modifier.fillMaxSize())
}
