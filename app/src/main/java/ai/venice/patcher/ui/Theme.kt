package ai.venice.patcher.ui

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
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
    primary = Color(0xFF4F2E8F), onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFE8DDFF), onPrimaryContainer = Color(0xFF1B0A3D),
    secondaryContainer = Color(0xFFE5DEFA),
    tertiary = Color(0xFF4E5F00), tertiaryContainer = Color(0xFFC6F36B),
    onTertiaryContainer = Color(0xFF151F00),
    background = Color(0xFFFDF8FF), surface = Color(0xFFFDF8FF),
    surfaceContainer = Color(0xFFF3EDFA), surfaceContainerHigh = Color(0xFFEDE6F5),
    onSurface = Color(0xFF1C1B20), onSurfaceVariant = Color(0xFF48454E),
    error = Color(0xFFB3261E),
)

private val Dark = darkColorScheme(
    primary = Color(0xFFD7B8FF), onPrimary = Color(0xFF2E0F5C),
    primaryContainer = Color(0xFF452A78), onPrimaryContainer = Color(0xFFE8DDFF),
    secondaryContainer = Color(0xFF474159),
    tertiaryContainer = Color(0xFF3B4A00), onTertiaryContainer = Color(0xFFC6F36B),
    background = Color(0xFF141218), surface = Color(0xFF141218),
    surfaceContainer = Color(0xFF201E25), surfaceContainerHigh = Color(0xFF2A282F),
    onSurface = Color(0xFFE7E1E9), onSurfaceVariant = Color(0xFFCAC4D0),
)

private val ExpressiveShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(14.dp),
    medium = RoundedCornerShape(22.dp),
    large = RoundedCornerShape(30.dp),
    largeIncreased = RoundedCornerShape(38.dp),
    extraLarge = RoundedCornerShape(44.dp),
    extraLargeIncreased = RoundedCornerShape(52.dp),
)

private val ExpressiveType = Typography().let { b ->
    b.copy(
        displayLarge = b.displayLarge.copy(
            fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Black, letterSpacing = (-1.6).sp,
        ),
        headlineLarge = b.headlineLarge.copy(
            fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Black, letterSpacing = (-0.8).sp,
        ),
        headlineMedium = b.headlineMedium.copy(fontWeight = FontWeight.ExtraBold, letterSpacing = (-0.5).sp),
        titleLarge = b.titleLarge.copy(fontWeight = FontWeight.Bold),
        labelLarge = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.SemiBold, fontSize = 14.sp),
    )
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun PatcherTheme(
    dark: Boolean = isSystemInDarkTheme(),
    dynamic: Boolean = true,
    content: @Composable () -> Unit,
) {
    val ctx = LocalContext.current
    val scheme: ColorScheme = when {
        dynamic && Build.VERSION.SDK_INT >= 31 && dark -> dynamicDarkColorScheme(ctx)
        dynamic && Build.VERSION.SDK_INT >= 31 -> dynamicLightColorScheme(ctx)
        dark -> Dark
        else -> Light
    }
    MaterialExpressiveTheme(
        colorScheme = scheme,
        motionScheme = MotionScheme.expressive(),
        shapes = ExpressiveShapes,
        typography = ExpressiveType,
        content = content,
    )
}
