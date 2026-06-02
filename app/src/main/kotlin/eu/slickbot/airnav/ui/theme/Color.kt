package eu.slickbot.airnav.ui.theme

import androidx.compose.ui.graphics.Color

// Backdrop / surfaces
val MapBackground = Color(0xFF0F0F18)
val SurfaceDark = Color(0xFF161622)
val SurfaceElevatedDark = Color(0xFF1E1E2D)
val OutlineDark = Color(0xFF3A3A4E)

// Foreground
val OnSurfaceDark = Color(0xFFE8E8F0)
val OnSurfaceVariantDark = Color(0xFFB6B6C8)

// Light surfaces
val BackgroundLight = Color(0xFFF7F7FB)
val SurfaceLight = Color(0xFFFFFFFF)
val SurfaceElevatedLight = Color(0xFFECECF3)
val OutlineLight = Color(0xFFC4C4D0)

// Light foreground
val OnSurfaceLight = Color(0xFF1A1A24)
val OnSurfaceVariantLight = Color(0xFF45454F)

// Accents (tuned for dark surfaces)
val AccentBlue = Color(0xFF6EC8FF)     // ADS-B / sky
val AccentAmber = Color(0xFFFFB74D)    // alerts / squawk
val AccentTeal = Color(0xFF4DD0E1)     // AIS / sea
val AccentRose = Color(0xFFFF8A95)
val AccentLavender = Color(0xFFBFA7FF)

// Accents (deepened for light surfaces, where the bright variants wash out)
val AccentBlueLight = Color(0xFF00629E)
val AccentTealLight = Color(0xFF006972)
val AccentAmberLight = Color(0xFF7A5300)

// Status indicators
val StatusConnected = Color(0xFF66DDB0)
val StatusConnecting = Color(0xFFFFC066)
val StatusOffline = Color(0xFF8B8BA0)
