package com.careerlens.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.careerlens.app.domain.model.JobStage

// --- Executive Obsidian Dark & Clean Daylight Palette ---
val ObsidianBackground = Color(0xFF0A0E17)
val ObsidianSurface = Color(0xFF121826)
val ObsidianCard = Color(0xFF1A2234)
val ObsidianBorder = Color(0xFF28324B)

val DaylightBackground = Color(0xFFF4F7FB)
val DaylightSurface = Color(0xFFFFFFFF)
val DaylightCard = Color(0xFFFFFFFF)
val DaylightBorder = Color(0xFFE2E8F0)

// Brand & Semantic Accents
val ElectricIndigo = Color(0xFF6366F1)
val IndigoSoft = Color(0xFF818CF8)
val EmeraldOffer = Color(0xFF10B981)
val EmeraldSoft = Color(0xFF34D399)
val CyanInterview = Color(0xFF06B6D4)
val AmberAssessment = Color(0xFFF59E0B)
val RoseRejected = Color(0xFFF43F5E)
val VioletWishlist = Color(0xFFA855F7)
val SlateNeutral = Color(0xFF94A3B8)

private val DarkColors = darkColorScheme(
    primary = ElectricIndigo,
    onPrimary = Color.White,
    primaryContainer = Color(0xFF252A52),
    onPrimaryContainer = Color(0xFFE0E7FF),
    secondary = EmeraldOffer,
    onSecondary = Color(0xFF042F2E),
    secondaryContainer = Color(0xFF064E3B),
    onSecondaryContainer = Color(0xFFD1FAE5),
    tertiary = AmberAssessment,
    background = ObsidianBackground,
    onBackground = Color(0xFFF1F5F9),
    surface = ObsidianSurface,
    onSurface = Color(0xFFF8FAFC),
    surfaceVariant = ObsidianCard,
    onSurfaceVariant = Color(0xFF94A3B8),
    outline = ObsidianBorder
)

private val LightColors = lightColorScheme(
    primary = Color(0xFF4F46E5),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFE0E7FF),
    onPrimaryContainer = Color(0xFF1E1B4B),
    secondary = Color(0xFF059669),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFD1FAE5),
    onSecondaryContainer = Color(0xFF064E3B),
    tertiary = Color(0xFFD97706),
    background = DaylightBackground,
    onBackground = Color(0xFF0F172A),
    surface = DaylightSurface,
    onSurface = Color(0xFF0F172A),
    surfaceVariant = Color(0xFFEEF2F6),
    onSurfaceVariant = Color(0xFF475569),
    outline = DaylightBorder
)

val CareerLensTypography = Typography(
    headlineLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.ExtraBold,
        fontSize = 28.sp,
        lineHeight = 34.sp,
        letterSpacing = (-0.5).sp
    ),
    headlineMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Bold,
        fontSize = 22.sp,
        lineHeight = 28.sp,
        letterSpacing = (-0.3).sp
    ),
    titleLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Bold,
        fontSize = 18.sp,
        lineHeight = 24.sp
    ),
    titleMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.SemiBold,
        fontSize = 15.sp,
        lineHeight = 21.sp
    ),
    bodyLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Normal,
        fontSize = 15.sp,
        lineHeight = 22.sp
    ),
    bodyMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Normal,
        fontSize = 13.sp,
        lineHeight = 19.sp
    ),
    labelMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.SemiBold,
        fontSize = 11.sp,
        lineHeight = 15.sp,
        letterSpacing = 0.4.sp
    )
)

fun JobStage.accentColor(): Color = when (this) {
    JobStage.WISHLIST -> VioletWishlist
    JobStage.APPLIED -> IndigoSoft
    JobStage.ASSESSMENT -> AmberAssessment
    JobStage.INTERVIEW -> CyanInterview
    JobStage.OFFER -> EmeraldOffer
    JobStage.ACCEPTED -> EmeraldSoft
    JobStage.REJECTED -> RoseRejected
    JobStage.GHOSTED -> SlateNeutral
}

@Composable
fun CareerLensTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColors else LightColors
    MaterialTheme(
        colorScheme = colorScheme,
        typography = CareerLensTypography,
        content = content
    )
}
