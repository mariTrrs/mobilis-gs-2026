package br.com.fiap.mobilis.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

private val LightColors = lightColorScheme(
    primary = Teal,
    onPrimary = Color.White,
    primaryContainer = TealContainer,
    onPrimaryContainer = TealDarkText,
    secondary = InkMuted,
    onSecondary = Color.White,
    tertiary = AmberText,
    onTertiary = Color.White,
    background = SurfaceSoft,
    onBackground = Ink,
    surface = Color.White,
    onSurface = Ink,
    surfaceVariant = Trilho,
    onSurfaceVariant = InkMuted,
    error = ErrorRed,
    onError = Color.White,
    outline = Color(0xFF6F7977),
    outlineVariant = LinhaSuave
)

private val DarkColors = darkColorScheme(
    primary = TealBright,
    onPrimary = TealDarkText,
    primaryContainer = Teal,
    onPrimaryContainer = TealContainer,
    secondary = Color(0xFFB7CCC9),
    onSecondary = Color(0xFF1A2A28),
    tertiary = Color(0xFFFFD267),
    onTertiary = Color(0xFF2A1C00),
    background = Night,
    onBackground = Color(0xFFE6EEEC),
    surface = NightSurface,
    onSurface = Color(0xFFE6EEEC),
    surfaceVariant = Color(0xFF2A3332),
    onSurfaceVariant = Color(0xFFC5D0CD),
    error = Color(0xFFFFB4AB),
    onError = Color(0xFF690005),
    outlineVariant = Color(0xFF3A4543)
)

private val Formas = Shapes(
    extraSmall = RoundedCornerShape(10.dp),
    small = RoundedCornerShape(14.dp),
    medium = RoundedCornerShape(20.dp),
    large = RoundedCornerShape(28.dp),
    extraLarge = RoundedCornerShape(32.dp)
)

@Composable
fun MobilisTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        typography = Typography,
        shapes = Formas,
        content = content
    )
}
