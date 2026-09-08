package com.mahfazty.smart.ui.screens

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.rememberInfiniteTransition
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
import androidx.compose.material3.ripple
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.mahfazty.smart.ui.components.LiquidFillIndicator
import com.mahfazty.smart.ui.components.ParticleBurst
import com.mahfazty.smart.ui.components.TypewriterReveal
import com.mahfazty.smart.ui.theme.Motion
import com.mahfazty.smart.ui.theme.rememberReduceMotion
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.sin

private val GoldLight = Color(0xFFF6D365)
private val GoldDeep = Color(0xFFE8A838)

/**
 * دخول التطبيق — العينة 1: امتلاء السائل الذهبي.
 * الرسالة: المال يملأ المحفظة ثم يستقر.
 *
 * معايرة 2026 (تحليل التصميم): امتلاء أسرع (850ms بدل 1200)، الموجة تبدأ
 * مع الامتلاء مباشرة، اللوجو ينبثق بدوران خفيف (±3°)، العنوان يُكتب
 * حرفاً حرفاً (Typewriter) دون كسر الربط العربي، جسيمات ذهبية تطفو،
 * وripple عند اللمس.
 */
@Composable
fun SplashScreen() {
    val reduce = rememberReduceMotion()

    // Animatable للتحكم الكامل بالتسلسل
    val liquid = remember { Animatable(if (reduce) 0.62f else 0f) }
    val wave = remember { Animatable(0f) }
    val logoA = remember { Animatable(if (reduce) 1f else 0f) }
    val logoS = remember { Animatable(if (reduce) 1f else 0.72f) }
    val logoR = remember { Animatable(0f) }
    val titleY = remember { Animatable(if (reduce) 0f else 16f) }
    val subA = remember { Animatable(if (reduce) 1f else 0f) }
    val subY = remember { Animatable(if (reduce) 0f else 16f) }
    var titleVisible by remember { mutableStateOf(if (reduce) true else false) }

    LaunchedEffect(reduce) {
        if (reduce) return@LaunchedEffect // لا حركة إن طُلب تقليلها

        // لحظة سكون قبل الاندفاع
        delay(80)

        // امتلاء أسرع (معايرة 2026: 850ms): يتجاوز 78% ثم يستقر عند 62%
        launch {
            liquid.animateTo(
                targetValue = 0.62f,
                animationSpec = keyframes {
                    durationMillis = 850
                    0f at 0
                    0.78f at 600 with LinearOutSlowInEasing
                    0.62f at 850 with LinearOutSlowInEasing
                },
            )
        }

        // الموجة تنطلق مع الامتلاء مباشرة (معايرة 2026: بلا انتظار 920ms)
        launch {
            wave.animateTo(1f, tween(1400, easing = LinearOutSlowInEasing))
        }

        // تتابع: الشعار ثم العنوان ثم السطر — فواصل ~180ms
        launch {
            delay(500)
            launch { logoA.animateTo(1f, tween(450, easing = LinearOutSlowInEasing)) }
            launch { logoS.animateTo(1f, Motion.springGentle) }
            // دوران خفيف يجعل اللوجو أكثر حيوية (معايرة 2026: ±3°)
            launch {
                logoR.animateTo(3f, tween(200, easing = FastOutSlowInEasing))
                logoR.animateTo(-2f, Motion.springGentle)
                logoR.animateTo(0f, Motion.springGentle)
            }
        }
        launch {
            delay(700)
            titleVisible = true
            titleY.animateTo(0f, tween(420, easing = LinearOutSlowInEasing))
        }
        launch {
            delay(880)
            launch { subA.animateTo(1f, tween(420, easing = LinearOutSlowInEasing)) }
            launch { subY.animateTo(0f, tween(420, easing = LinearOutSlowInEasing)) }
        }
    }

    val primary = MaterialTheme.colorScheme.primary
    val secondary = MaterialTheme.colorScheme.secondary

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.linearGradient(listOf(primary, secondary)))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = ripple(bounded = false),
            ) {},
    ) {
        // جسيمات ذهبية تطفو (معايرة 2026)
        if (!reduce) {
            val goldInf = rememberInfiniteTransition(label = "splashGold")
            val gPhase by goldInf.animateFloat(
                initialValue = 0f,
                targetValue = 1f,
                animationSpec = infiniteRepeatable(tween(9000, easing = LinearEasing), RepeatMode.Restart),
                label = "splashGoldPhase",
            )
            Canvas(Modifier.fillMaxSize().zIndex(1f)) {
                val w = size.width
                val h = size.height
                repeat(9) { i ->
                    val fi = (gPhase + i / 9f) % 1f
                    val x = w * (0.08f + 0.84f * i / 8f) + sin(gPhase * 2f * PI.toFloat() + i) * 14f
                    val y = h * (1.05f - fi * 1.15f)
                    val a = (1f - abs(fi - 0.5f) * 2f) * 0.4f
                    drawCircle(
                        color = GoldLight.copy(alpha = a),
                        radius = 2.5f + (i % 3) * 1.2f,
                        center = Offset(x, y),
                    )
                }
            }
        }

        // السائل من الأسفل — نحرّك الارتفاع فقط (رخيص)
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .fillMaxHeight(liquid.value)
                .background(Brush.verticalGradient(listOf(GoldLight, GoldDeep))),
        ) {
            // موجة السطح — توزيع الإطارات يوحي بالكتلة السائلة
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

        // الشعار والعنوان فوق السائل
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
                    rotationZ = logoR.value
                },
            )
            Spacer(Modifier.height(16.dp))
            // كتابة تدريجية (معايرة 2026) — أحرف كاملة في Text واحد فلا ينكسر الربط العربي
            TypewriterReveal(
                text = "محفظتي الذكية",
                visible = titleVisible,
                style = TextStyle(fontWeight = FontWeight.ExtraBold, fontSize = 24.sp),
                color = Color.White,
                modifier = Modifier.graphicsLayer {
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
