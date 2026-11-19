package com.mahfazty.smart.ui.theme

import android.graphics.Color as AndroidColor
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import com.mahfazty.smart.domain.model.AppSettings
import com.mahfazty.smart.domain.model.ThemeMode

/** ألوان إضافية للتطبيق (نفس لوحة نسخة الويب) */
data class AppColors(
    val muted: Color,
    val border: Color,
    val green: Color,
    val red: Color,
    val gold: Color,
    val chipBg: Color,
    val cardTop: Color,
)

val LightAppColors = AppColors(
    muted = Color(0xFF636E72),
    border = Color(0xFFDFE6E9),
    green = Color(0xFF00B894),
    red = Color(0xFFFF7675),
    gold = Color(0xFFFDCB6E),
    chipBg = Color(0xFFEEF0FF),
    cardTop = Color(0xFFFFFFFF),
)

val DarkAppColors = AppColors(
    muted = Color(0xFF9A9AB0),
    border = Color(0xFF34344A),
    green = Color(0xFF00D1A7),
    red = Color(0xFFFF8A88),
    gold = Color(0xFFFDCB6E),
    chipBg = Color(0xFF2E2E45),
    cardTop = Color(0xFF262636),
)

val LocalAppColors = staticCompositionLocalOf { LightAppColors }

/** قراءة لون سداسي عشري بأمان (المهارة 1: لا تعطل عند قيمة تالفة) */
fun parseHex(hex: String, fallback: Color = Color(0xFF6C5CE7)): Color = runCatching {
    Color(AndroidColor.parseColor(hex))
}.getOrDefault(fallback)

private fun lightScheme(primary: Color, primary2: Color) = lightColorScheme(
    primary = primary,
    onPrimary = Color.White,
    secondary = primary2,
    background = Color(0xFFF5F6FB),
    onBackground = Color(0xFF2D3436),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF2D3436),
    surfaceVariant = Color(0xFFEEF0FF),
    onSurfaceVariant = Color(0xFF636E72),
    error = Color(0xFFFF7675),
    outline = Color(0xFFDFE6E9),
)

private fun darkScheme(primary: Color, primary2: Color) = darkColorScheme(
    primary = primary,
    onPrimary = Color.White,
    secondary = primary2,
    background = Color(0xFF1B1B28),
    onBackground = Color(0xFFF0F0F5),
    surface = Color(0xFF262636),
    onSurface = Color(0xFFF0F0F5),
    surfaceVariant = Color(0xFF2E2E45),
    onSurfaceVariant = Color(0xFF9A9AB0),
    error = Color(0xFFFF8A88),
    outline = Color(0xFF34344A),
)

@Composable
fun MahfaztyTheme(settings: AppSettings, content: @Composable () -> Unit) {
    val dark = when (settings.theme) {
        ThemeMode.DARK -> true
        ThemeMode.LIGHT -> false
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
    }
    val primary = parseHex(settings.primaryColor)
    val primary2 = parseHex(settings.primary2)
    val scheme = if (dark) darkScheme(primary, primary2) else lightScheme(primary, primary2)
    val appColors = if (dark) DarkAppColors else LightAppColors

    CompositionLocalProvider(LocalAppColors provides appColors) {
        MaterialTheme(
            colorScheme = scheme,
            typography = MahfaztyTypography,
            content = content,
        )
    }
}
