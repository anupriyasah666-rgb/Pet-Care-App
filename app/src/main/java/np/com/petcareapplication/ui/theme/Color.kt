package np.com.petcareapplication.ui.theme

import androidx.compose.ui.graphics.Color

// The app's main blue. BlueDark is also used for the "Create an Account" and "Login" links
// so they stand out against the blue background
val BluePrimary = Color(0xFF2196F3)
val BlueDark = Color(0xFF1976D2)
val BlueLight = Color(0xFFBBDEFB)

// Pink is the second colour, used for highlights like the logo border and the Add Task button
val PinkHighlight = Color(0xFFE91E63)
val PinkLight = Color(0xFFF8BBD0)

// Plain colours for backgrounds and text
val White = Color(0xFFFFFFFF)
val BackgroundWhite = Color(0xFFF5F5F5)
val TextDark = Color(0xFF212121)
val TextGray = Color(0xFF757575)

// Light mode: these fill in the Material 3 colour slots used in Theme.kt.
// "on" colours are for text and icons sitting on top of the matching colour
val md_theme_light_primary = BluePrimary
val md_theme_light_onPrimary = Color.White
val md_theme_light_primaryContainer = BlueLight
val md_theme_light_onPrimaryContainer = Color(0xFF001D36)
val md_theme_light_secondary = PinkHighlight
val md_theme_light_onSecondary = Color.White
val md_theme_light_secondaryContainer = PinkLight
val md_theme_light_onSecondaryContainer = Color(0xFF31111D)
val md_theme_light_error = Color(0xFFBA1A1A)
val md_theme_light_background = BackgroundWhite
val md_theme_light_onBackground = TextDark
val md_theme_light_surface = White
val md_theme_light_onSurface = TextDark

// Dark mode: lighter, softer versions of the blue and pink so they don't glare on a dark background
val md_theme_dark_primary = Color(0xFF90CAF9) // lighter blue
val md_theme_dark_onPrimary = Color(0xFF003258)
val md_theme_dark_primaryContainer = Color(0xFF00497D)
val md_theme_dark_onPrimaryContainer = Color(0xFFD1E4FF)
val md_theme_dark_secondary = Color(0xFFFFB2C1) // lighter pink
val md_theme_dark_onSecondary = Color(0xFF5F122B)
val md_theme_dark_secondaryContainer = Color(0xFF7D2941)
val md_theme_dark_onSecondaryContainer = Color(0xFFFFD9DF)
val md_theme_dark_error = Color(0xFFFFB4AB)
val md_theme_dark_background = Color(0xFF1A1C1E) // very dark grey rather than pure black
val md_theme_dark_onBackground = Color(0xFFE2E2E6)
val md_theme_dark_surface = Color(0xFF1A1C1E)
val md_theme_dark_onSurface = Color(0xFFE2E2E6)