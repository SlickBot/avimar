package eu.slickbot.avimar.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

object AvimarAccents {

  val plane: Color
    @Composable get() {
      return if (isSystemInDarkTheme()) AccentBlue else AccentBlueLight
    }

  val ship: Color
    @Composable get() {
      return if (isSystemInDarkTheme()) AccentTeal else AccentTealLight
    }

  val alert: Color
    @Composable get() {
      return if (isSystemInDarkTheme()) AccentAmber else AccentAmberLight
    }
}

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

private val LightColors = lightColorScheme(
  primary = AccentBlueLight,
  onPrimary = Color(0xFFFFFFFF),
  secondary = AccentTealLight,
  onSecondary = Color(0xFFFFFFFF),
  tertiary = AccentAmberLight,
  onTertiary = Color(0xFFFFFFFF),
  background = BackgroundLight,
  onBackground = OnSurfaceLight,
  surface = SurfaceLight,
  onSurface = OnSurfaceLight,
  surfaceVariant = SurfaceElevatedLight,
  onSurfaceVariant = OnSurfaceVariantLight,
  outline = OutlineLight,
)

@Composable
fun AvimarTheme(
  darkTheme: Boolean = isSystemInDarkTheme(),
  content: @Composable () -> Unit,
) {
  MaterialTheme(
    colorScheme = if (darkTheme) DarkColors else LightColors,
    content = content,
  )
}
