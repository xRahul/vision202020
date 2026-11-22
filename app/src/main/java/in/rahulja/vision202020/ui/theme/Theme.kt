package `in`.rahulja.vision202020.ui.theme

import android.app.Activity
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import androidx.glance.material3.ColorProviders
import `in`.rahulja.vision202020.data.AppThemeMode

internal val LightColorScheme = lightColorScheme(
    primary = SkyBlueDark,
    onPrimary = LightSurface,
    background = LightBackground,
    onBackground = LightOnBackground,
    surface = LightSurface,
    onSurface = LightOnSurface,
    surfaceVariant = LightSurfaceVariant,
    onSurfaceVariant = LightOnSurfaceVariant,
    outline = LightOutline
)

internal val DarkColorScheme = darkColorScheme(
    primary = SkyBluePrimary,
    onPrimary = DarkBackground,
    background = DarkBackground,
    onBackground = DarkOnBackground,
    surface = DarkSurface,
    onSurface = DarkOnSurface,
    surfaceVariant = DarkSurfaceVariant,
    onSurfaceVariant = DarkOnSurfaceVariant,
    outline = DarkOutline
)

private val OledColorScheme = darkColorScheme(
    primary = SkyBluePrimary,
    onPrimary = OledBackground,
    background = OledBackground,
    onBackground = OledOnBackground,
    surface = OledSurface,
    onSurface = OledOnSurface,
    surfaceVariant = OledSurfaceVariant,
    onSurfaceVariant = OledOnSurfaceVariant,
    outline = OledOutline
)

val VisionGlanceColorProviders = ColorProviders(
    light = LightColorScheme,
    dark = DarkColorScheme
)

@Composable
fun VisionTheme(
    appTheme: AppThemeMode = AppThemeMode.OLED_BLACK,
    content: @Composable () -> Unit
) {
    val colorScheme = when (appTheme) {
        AppThemeMode.LIGHT -> LightColorScheme
        AppThemeMode.DARK -> DarkColorScheme
        AppThemeMode.OLED_BLACK -> OledColorScheme
    }

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window
            if (window != null) {
                val insetsController = WindowCompat.getInsetsController(window, view)
                val isLight = appTheme == AppThemeMode.LIGHT
                insetsController.isAppearanceLightStatusBars = isLight
                insetsController.isAppearanceLightNavigationBars = isLight
            }
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        content = content
    )
}
