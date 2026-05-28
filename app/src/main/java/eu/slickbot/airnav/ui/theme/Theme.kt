package eu.slickbot.airnav.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val DarkColors = darkColorScheme(
    primary = AccentBlue,
    secondary = AccentAmber,
    background = MapBackground,
    surface = SurfaceDark,
    onSurface = OnSurfaceDark,
)

private val LightColors = lightColorScheme(
    primary = AccentBlue,
    secondary = AccentAmber,
)

@Composable
fun AirnavTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        content = content,
    )
}
