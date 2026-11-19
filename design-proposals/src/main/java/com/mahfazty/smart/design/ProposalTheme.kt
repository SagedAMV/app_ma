package com.mahfazty.smart.design

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

/**
 * لوحة التصاميم المقترحة «محفظتي الذكية 2027» — تطوير هوية التطبيق الحالية
 * (بنفسجي #6C5CE7) نحو نظام Material 3 كامل بألوان أوضح تباينًا ومساحات أرحب.
 */
object ProposalPalette {
    // ===== الأساس (مشتق من هوية التطبيق) =====
    val Primary = Color(0xFF6C5CE7)          // البنفسجي الأصلي — أثبت الهوية
    val PrimaryDeep = Color(0xFF5A4BD1)
    val PrimaryContainer = Color(0xFFE8E4FF)
    val OnPrimaryContainer = Color(0xFF221A5C)

    // ===== ثانوي: أخضر مالي (دخل) =====
    val Secondary = Color(0xFF00B894)
    val SecondaryContainer = Color(0xFFD8F5EE)

    // ===== ثالثي: ذهبي (ادخار/إنجاز) =====
    val Tertiary = Color(0xFFE8A820)
    val TertiaryContainer = Color(0xFFFFF3D6)

    // ===== خطأ/خطر =====
    val Error = Color(0xFFE5484D)
    val ErrorContainer = Color(0xFFFFE9E9)

    // ===== محايد — فاتح =====
    val BackgroundL = Color(0xFFF6F5FB)
    val SurfaceL = Color(0xFFFFFFFF)
    val SurfaceVariantL = Color(0xFFEDEBFA)
    val OutlineL = Color(0xFFD9D6EF)
    val MutedL = Color(0xFF6E6A8F)

    // ===== محايد — داكن =====
    val BackgroundD = Color(0xFF14131F)
    val SurfaceD = Color(0xFF1E1D2E)
    val SurfaceVariantD = Color(0xFF2A2942)
    val OutlineD = Color(0xFF3A3956)
    val MutedD = Color(0xFFA5A1C4)
}

/** ألوان دلالية موحدة عبر الدارات (تُستخدم مباشرة، لا عبر MaterialTheme) */
object ProposalColors {
    val income = ProposalPalette.Secondary
    val expense = ProposalPalette.Error
    val transfer = ProposalPalette.Primary
    val gold = ProposalPalette.Tertiary
}

private val LightScheme = lightColorScheme(
    primary = ProposalPalette.Primary,
    onPrimary = Color.White,
    primaryContainer = ProposalPalette.PrimaryContainer,
    onPrimaryContainer = ProposalPalette.OnPrimaryContainer,
    secondary = ProposalPalette.Secondary,
    secondaryContainer = ProposalPalette.SecondaryContainer,
    tertiary = ProposalPalette.Tertiary,
    tertiaryContainer = ProposalPalette.TertiaryContainer,
    error = ProposalPalette.Error,
    errorContainer = ProposalPalette.ErrorContainer,
    background = ProposalPalette.BackgroundL,
    onBackground = Color(0xFF1D1B2E),
    surface = ProposalPalette.SurfaceL,
    onSurface = Color(0xFF1D1B2E),
    surfaceVariant = ProposalPalette.SurfaceVariantL,
    onSurfaceVariant = ProposalPalette.MutedL,
    outline = ProposalPalette.OutlineL,
)

private val DarkScheme = darkColorScheme(
    primary = Color(0xFFB9AFFF),
    onPrimary = Color(0xFF251E63),
    primaryContainer = ProposalPalette.OnPrimaryContainer,
    onPrimaryContainer = Color(0xFFE8E4FF),
    secondary = Color(0xFF3DDDB8),
    secondaryContainer = Color(0xFF0A4A3C),
    tertiary = Color(0xFFF5C542),
    tertiaryContainer = Color(0xFF5A430C),
    error = Color(0xFFFF8A8C),
    errorContainer = Color(0xFF5C1F22),
    background = ProposalPalette.BackgroundD,
    onBackground = Color(0xFFEFEEF7),
    surface = ProposalPalette.SurfaceD,
    onSurface = Color(0xFFEFEEF7),
    surfaceVariant = ProposalPalette.SurfaceVariantD,
    onSurfaceVariant = ProposalPalette.MutedD,
    outline = ProposalPalette.OutlineD,
)

@Composable
fun ProposalTheme(darkTheme: Boolean = isSystemInDarkTheme(), content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkScheme else LightScheme,
        typography = ProposalTypography,
        content = content,
    )
}
