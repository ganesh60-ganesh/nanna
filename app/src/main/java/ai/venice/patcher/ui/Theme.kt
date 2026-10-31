@file:OptIn(androidx.compose.material3.ExperimentalMaterial3ExpressiveApi::class)

package ai.venice.patcher.ui

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialExpressiveTheme
import androidx.compose.material3.MotionScheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val Light = lightColorScheme(
    primary = Color(0xFF4F2E8F),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFE8DDFF),
    onPrimaryContainer = Color(0xFF1B0A3D),
    secondary = Color(0xFF5F5378),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFE5DEFA),
    onSecondaryContainer = Color(0xFF1B1726),
    tertiary = Color(0xFF4E5F00),
    onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = Color(0xFFC6F36B),
    onTertiaryContainer = Color(0xFF151F00),
    background = Color(0xFFFDF8FF),
    onBackground = Color(0xFF1C1B20),
    surface = Color(0xFFFDF8FF),
    onSurface = Color(0xFF1C1B20),
    surfaceVariant = Color(0xFFE7E0EC),
    onSurfaceVariant = Color(0xFF48454E),
    surfaceContainerLowest = Color(0xFFFFFFFF),
    surfaceContainerLow = Color(0xFFF8F2FB),
    surfaceContainer = Color(0xFFF3EDFA),
    surfaceContainerHigh = Color(0xFFEDE6F5),
    surfaceContainerHighest = Color(0xFFE7E0EF),
    outline = Color(0xFF79747E),
    outlineVariant = Color(0xFFCAC4D0),
    error = Color(0xFFB3261E),
    onError = Color(0xFFFFFFFF),
    errorContainer = Color(0xFFF9DEDC),
    onErrorContainer = Color(0xFF410E0B),
    inverseSurface = Color(0xFF31303A),
    inverseOnSurface = Color(0xFFF4EFF7),
    inversePrimary = Color(0xFFD7B8FF),
    scrim = Color(0xFF000000),
)

private val Dark = darkColorScheme(
    primary = Color(0xFFD7B8FF),
    onPrimary = Color(0xFF2E0F5C),
    primaryContainer = Color(0xFF452A78),
    onPrimaryContainer = Color(0xFFE8DDFF),
    secondary = Color(0xFFC9BFDC),
    onSecondary = Color(0xFF322C42),
    secondaryContainer = Color(0xFF474159),
    onSecondaryContainer = Color(0xFFE5DEFA),
    tertiary = Color(0xFFAAD24F),
    onTertiary = Color(0xFF253300),
    tertiaryContainer = Color(0xFF3B4A00),
    onTertiaryContainer = Color(0xFFC6F36B),
    background = Color(0xFF141218),
    onBackground = Color(0xFFE7E1E9),
    surface = Color(0xFF141218),
    onSurface = Color(0xFFE7E1E9),
    surfaceVariant = Color(0xFF48454E),
    onSurfaceVariant = Color(0xFFCAC4D0),
    surfaceContainerLowest = Color(0xFF0F0D13),
    surfaceContainerLow = Color(0xFF1C1A21),
    surfaceContainer = Color(0xFF201E25),
    surfaceContainerHigh = Color(0xFF2A282F),
    surfaceContainerHighest = Color(0xFF35323A),
    outline = Color(0xFF948F99),
    outlineVariant = Color(0xFF48454E),
    error = Color(0xFFF2B8B5),
    onError = Color(0xFF601410),
    errorContainer = Color(0xFF8C1D18),
    onErrorContainer = Color(0xFFF9DEDC),
    inverseSurface = Color(0xFFE7E1E9),
    inverseOnSurface = Color(0xFF31303A),
    inversePrimary = Color(0xFF4F2E8F),
    scrim = Color(0xFF000000),
)

private val ShapeSet = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(14.dp),
    medium = RoundedCornerShape(22.dp),
    large = RoundedCornerShape(30.dp),
    largeIncreased = RoundedCornerShape(38.dp),
    extraLarge = RoundedCornerShape(44.dp),
    extraLargeIncreased = RoundedCornerShape(52.dp),
    extraExtraLarge = RoundedCornerShape(60.dp),
)

private val TypeSet = Typography().let { base ->
    base.copy(
        displayLarge = base.displayLarge.copy(
            fontFamily = FontFamily.SansSerif,
            fontWeight = FontWeight.Black,
            letterSpacing = (-1.6).sp,
        ),
        displayMedium = base.displayMedium.copy(
            fontFamily = FontFamily.SansSerif,
            fontWeight = FontWeight.Black,
            letterSpacing = (-1.2).sp,
        ),
        displaySmall = base.displaySmall.copy(
            fontWeight = FontWeight.ExtraBold,
            letterSpacing = (-0.8).sp,
        ),
        headlineLarge = base.headlineLarge.copy(
            fontFamily = FontFamily.SansSerif,
            fontWeight = FontWeight.Black,
            letterSpacing = (-0.8).sp,
        ),
        headlineMedium = base.headlineMedium.copy(
            fontWeight = FontWeight.ExtraBold,
            letterSpacing = (-0.5).sp,
        ),
        headlineSmall = base.headlineSmall.copy(fontWeight = FontWeight.Bold),
        titleLarge = base.titleLarge.copy(fontWeight = FontWeight.Bold),
        titleMedium = base.titleMedium.copy(fontWeight = FontWeight.SemiBold),
        labelLarge = TextStyle(
            fontFamily = FontFamily.SansSerif,
            fontWeight = FontWeight.SemiBold,
            fontSize = 14.sp,
            letterSpacing = 0.1.sp,
        ),
        labelMedium = TextStyle(
            fontFamily = FontFamily.SansSerif,
            fontWeight = FontWeight.Medium,
            fontSize = 12.sp,
            letterSpacing = 0.5.sp,
        ),
    )
}

@Composable
fun PatcherTheme(
    dark: Boolean = isSystemInDarkTheme(),
    dynamic: Boolean = true,
    content: @Composable () -> Unit,
) {
    val ctx = LocalContext.current
    val scheme: ColorScheme = when {
        dynamic && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && dark -> dynamicDarkColorScheme(ctx)
        dynamic && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> dynamicLightColorScheme(ctx)
        dark -> Dark
        else -> Light
    }

    MaterialExpressiveTheme(
        colorScheme = scheme,
        motionScheme = MotionScheme.expressive(),
        shapes = ShapeSet,
        typography = TypeSet,
        content = content,
    )
}
