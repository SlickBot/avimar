package eu.slickbot.airnav.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColors = darkColorScheme(
    primary = AccentBlue,
    onPrimary = Color(0xFF002C40),
    secondary = AccentTeal,
    onSecondary = Color(0xFF00322D),
    tertiary = AccentAmber,
    onTertiary = Color(0xFF3D2200),
    background = MapBackground,
    onBackground = OnSurfaceDark,
    surface = SurfaceDark,
    onSurface = OnSurfaceDark,
    surfaceVariant = SurfaceElevatedDark,
    onSurfaceVariant = OnSurfaceVariantDark,
    outline = OutlineDark,
)

@Composable
fun AirnavTheme(
    content: @Composable () -> Unit,
) {
    // We render on top of a dark Leaflet map; light theme isn't useful here.
    MaterialTheme(
        colorScheme = DarkColors,
        content = content,
    )
}
