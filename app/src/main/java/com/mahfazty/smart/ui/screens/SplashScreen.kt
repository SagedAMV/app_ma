package com.mahfazty.smart.ui.screens

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.mahfazty.smart.ui.theme.Motion
import com.mahfazty.smart.ui.theme.rememberReduceMotion
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.sin

private val GoldLight = Color(0xFFF6D365)
private val GoldDeep = Color(0xFFE8A838)

/**
 * دخول التطبيق — العينة 1: امتلاء السائل الذهبي.
 * الرسالة: المال يملأ المحفظة ثم يستقر (تفكير 1–2).
 */
@Composable
fun SplashScreen() {
    val reduce = rememberReduceMotion()

    // مهارة تنفيذ 6: Animatable للتحكم الكامل بالتسلسل
    val liquid = remember { Animatable(if (reduce) 0.62f else 0f) }
    val wave = remember { Animatable(0f) }
    val logoA = remember { Animatable(if (reduce) 1f else 0f) }
    val logoS = remember { Animatable(if (reduce) 1f else 0.72f) }
    val titleA = remember { Animatable(if (reduce) 1f else 0f) }
    val titleY = remember { Animatable(if (reduce) 0f else 16f) }
    val subA = remember { Animatable(if (reduce) 1f else 0f) }
    val subY = remember { Animatable(if (reduce) 0f else 16f) }

    LaunchedEffect(reduce) {
        if (reduce) return@LaunchedEffect // مهارة تفكير 15: لا حركة إن طُلب تقليلها

        // مهارة 8 ترقّب: لحظة سكون قبل الاندفاع
        delay(80)

        // مهارة 4+5+6+9: 1200ms، يتجاوز 78% ثم يستقر 62% (متابعة سائل)
        // مهارة 6: LinearOutSlowInEasing = دخول سريع ثم هبوط ناعم
        launch {
            liquid.animateTo(
                targetValue = 0.62f,
                animationSpec = keyframes {
                    durationMillis = 1200
                    0f at 0
                    0.78f at 840 with LinearOutSlowInEasing
                    0.62f at 1200 with LinearOutSlowInEasing
                },
            )
        }

        // مهارة 10 تتابع: الشعار ثم العنوان ثم السطر — فواصل ~180ms
        launch {
            delay(500)
            launch { logoA.animateTo(1f, tween(450, easing = LinearOutSlowInEasing)) }
            launch { logoS.animateTo(1f, Motion.springGentle) } // مهارة 7 نابض استقرار
        }
        launch {
            delay(700)
            launch { titleA.animateTo(1f, tween(420, easing = LinearOutSlowInEasing)) }
            launch { titleY.animateTo(0f, tween(420, easing = LinearOutSlowInEasing)) }
        }
        launch {
            delay(880)
            launch { subA.animateTo(1f, tween(420, easing = LinearOutSlowInEasing)) }
            launch { subY.animateTo(0f, tween(420, easing = LinearOutSlowInEasing)) }
        }

        // مهارة 9 سطح السائل يبقى يتنفس بعد الامتلاء (متابعة) — رخيص: زاوية فقط
        launch {
            delay(920)
            wave.animateTo(1f, tween(1400, easing = LinearOutSlowInEasing))
        }
    }

    val primary = MaterialTheme.colorScheme.primary
    val secondary = MaterialTheme.colorScheme.secondary

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.linearGradient(listOf(primary, secondary)))
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {},
    ) {
        // السائل من الأسفل — مهارة 17: نحرّك الارتفاع فقط (رخيص)
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .fillMaxHeight(liquid.value)
                .background(Brush.verticalGradient(listOf(GoldLight, GoldDeep))),
        ) {
            // موجة السطح (تفكير 5: توزيع الإطارات يوحي بالكتلة السائلة)
            Canvas(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .fillMaxWidth()
                    .height(28.dp)
                    .graphicsLayer { translationY = -8f },
            ) {
                val amp = 9.dp.toPx()
                val phase = wave.value * (2f * PI.toFloat())
                val path = Path()
                path.moveTo(0f, size.height)
                var x = 0f
                while (x <= size.width) {
                    val y = amp + sin((x / size.width) * 2f * PI.toFloat() + phase) * amp
                    if (x == 0f) path.lineTo(0f, y) else path.lineTo(x, y)
                    x += 6f
                }
                path.lineTo(size.width, size.height)
                path.close()
                drawPath(path, GoldLight)
            }
        }

        // مهارة 11: الشعار فوق السائل هو العنصر الأساسي بعد الامتلاء
        Column(
            modifier = Modifier
                .align(Alignment.Center)
                .zIndex(2f),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                "💰",
                fontSize = 72.sp,
                modifier = Modifier.graphicsLayer {
                    alpha = logoA.value
                    scaleX = logoS.value
                    scaleY = logoS.value
                },
            )
            Spacer(Modifier.height(16.dp))
            Text(
                "محفظتي الذكية",
                color = Color.White,
                fontWeight = FontWeight.ExtraBold,
                fontSize = 24.sp,
                modifier = Modifier.graphicsLayer {
                    alpha = titleA.value
                    translationY = titleY.value
                },
            )
            Spacer(Modifier.height(8.dp))
            Text(
                "المحاسب الشخصي",
                fontSize = 13.sp,
                color = Color.White.copy(alpha = 0.88f),
                modifier = Modifier.graphicsLayer {
                    alpha = subA.value
                    translationY = subY.value
                },
            )
        }
    }
}
