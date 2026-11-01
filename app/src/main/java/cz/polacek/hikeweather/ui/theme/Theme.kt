package cz.polacek.hikeweather.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp

private val LightColors = lightColorScheme(
    primary = Forest,
    onPrimary = Sand,
    primaryContainer = ForestContainer,
    onPrimaryContainer = OnForestContainer,
    secondary = Sunset,
    onSecondary = Sand,
    secondaryContainer = SunsetContainer,
    onSecondaryContainer = OnSunsetContainer,
    background = Sand,
    onBackground = Bark,
    surface = Sand,
    onSurface = Bark,
    surfaceVariant = SandVariant,
    onSurfaceVariant = Moss,
    surfaceContainer = SandVariant,
    surfaceContainerLow = androidx.compose.ui.graphics.Color.White,
    surfaceContainerHigh = SandVariant,
    error = AlertRed
)

private val DarkColors = darkColorScheme(
    primary = ForestLight,
    onPrimary = Night,
    primaryContainer = ForestDarkContainer,
    onPrimaryContainer = ForestContainer,
    secondary = SunsetLight,
    onSecondary = Night,
    secondaryContainer = SunsetDarkContainer,
    onSecondaryContainer = SunsetContainer,
    background = Night,
    onBackground = Mist,
    surface = Night,
    onSurface = Mist,
    surfaceVariant = NightVariant,
    onSurfaceVariant = MistVariant,
    surfaceContainer = NightSurface,
    surfaceContainerLow = NightSurface,
    surfaceContainerHigh = NightVariant
)

private val AppShapes = Shapes(
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(18.dp),
    large = RoundedCornerShape(28.dp)
)

/** Motiv aplikace - svetly nebo tmavy podle nastaveni systemu */
@Composable
fun HikeWeatherTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        typography = Typography,
        shapes = AppShapes,
        content = content
    )
}
