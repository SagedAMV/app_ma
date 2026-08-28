package com.mahfazty.smart.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.changedToUpIgnoreConsumed
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.TextStyle
import com.mahfazty.smart.ui.theme.Motion
import com.mahfazty.smart.ui.theme.rememberReduceMotion
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.sin

/**
 * 💫 ضغطة ناعمة عالمية (الملحق 41+45): عند اللمس ينضغط العنصر ببطء مرئي،
 * يلمع وميض أبيض ناعم فوقه (تأكيد بصري)، وعند الإفلات يعود بنعومة متدرجة.
 *
 * إصلاح جذري: هذه النسخة لا تستهلك أي حدث لمس إطلاقاً (requireUnconsumed=false
 * وبدون consume) — فلا تتصادم مع clickable أو أي إيماءة أخرى على نفس العنصر.
 * النسخة السابقة كانت تستخدم detectTapGestures الذي يستهلك اللمسات ويبتلع النقرات.
 */
fun Modifier.bounceClick(): Modifier = composed {
    val reduce = rememberReduceMotion()
    val scale = remember { Animatable(1f) }
    val press = remember { Animatable(0f) }
    val scope = rememberCoroutineScope()
    this
        .graphicsLayer {
            scaleX = scale.value
            scaleY = scale.value
        }
        .drawWithContent {
            drawContent()
            if (press.value > 0f) {
                drawRect(Color.White.copy(alpha = press.value * 0.30f))
            }
        }
        .pointerInput(Unit) {
            awaitEachGesture {
                val down = awaitFirstDown(requireUnconsumed = false)
                if (!reduce) {
                    scope.launch { scale.animateTo(0.955f, tween(240, easing = FastOutSlowInEasing)) }
                    scope.launch { press.animateTo(1f, tween(200)) }
                }
                // انتظار الإفلات أو الإلغاء — بلا أي استهلاك للأحداث
                while (true) {
                    val event = awaitPointerEvent(PointerEventPass.Main)
                    val change = event.changes.firstOrNull { it.id == down.id } ?: break
                    if (change.changedToUpIgnoreConsumed() || !change.pressed) break
                }
                if (!reduce) {
                    scope.launch { scale.animateTo(1f, Motion.springPress) }
                    scope.launch { press.animateTo(0f, tween(380, easing = LinearOutSlowInEasing)) }
                }
            }
        }
}

/**
 * 🕰️ تأرجح الساعة (الملحق 44 — الفكرة المبتكرة): البطاقة تدخل بدوران
 * كعقرب ساعة يتأرجح، ثم ترتد في الاتجاه المعاكس قليلاً بتأثير الجاذبية،
 * ثم تستقر في مكانها بنعومة. مع تلاشٍ داخلي — كل ذلك مرئي وهادئ.
 */
@Composable
fun SwingCardEntrance(
    index: Int,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    val reduce = rememberReduceMotion()
    val rotation = remember { Animatable(0f) }
    val alpha = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        if (reduce) {
            alpha.snapTo(1f)
            return@LaunchedEffect
        }
        delay((index * Motion.STAGGER_STEP_MS).toLong())
        launch { alpha.animateTo(1f, tween(380, easing = LinearOutSlowInEasing)) }
        // 1) يدخل مائلاً كعقرب ساعة ثم يتأرجح نحو الداخل
        rotation.snapTo(-7f)
        rotation.animateTo(5f, tween(430, easing = LinearOutSlowInEasing))
        // 2) ارتداد معاكس خفيف — تأثير الجاذبية
        rotation.animateTo(-2.5f, tween(320, easing = LinearOutSlowInEasing))
        // 3) الاستقرار المتدرج في الوضع الطبيعي
        rotation.animateTo(0f, Motion.springGentle)
    }
    Box(
        modifier.graphicsLayer {
            rotationZ = rotation.value
            this.alpha = alpha.value
        },
    ) { content() }
}

/**
 * نظام 11 — مطاط يتجاوز ثم يستقر (حركة 7 + تنفيذ 6).
 * تتابع حسب الفهرس (حركة 10). تقليل الحركة = ظهور فوري (حركة 15).
 * graphicsLayer فقط (حركة 17).
 */
@Composable
fun ElasticEntrance(
    index: Int,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    val reduce = rememberReduceMotion()
    val t = remember { Animatable(if (reduce) 1f else 0f) }
    LaunchedEffect(Unit) {
        if (reduce) {
            t.snapTo(1f)
            return@LaunchedEffect
        }
        delay((index * Motion.STAGGER_STEP_MS).toLong())
        t.animateTo(1f, tween(600, easing = Motion.elasticOut))
    }
    val v = t.value
    Box(
        modifier.graphicsLayer {
            val s = 0.6f + 0.4f * v
            scaleX = s
            scaleY = s
            translationY = 18f * (1f - v.coerceAtMost(1.2f))
            alpha = v.coerceIn(0f, 1f)
        },
    ) { content() }
}

/**
 * 📋 تتابع خانات النافذة (الملحق 47): كل خانة في النوافذ المنبثقة تظهر
 * بعد سابقتها بفاصل مريح، منزلقةً من الأسفل بتلاشٍ — ترتيب بصري مرتب وناعم.
 */
@Composable
fun SheetFieldEntrance(
    index: Int,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    val reduce = rememberReduceMotion()
    val duration = if (reduce) 0 else 520
    val delayMs = if (reduce) 0 else 120 + index * Motion.FIELD_STAGGER_MS
    androidx.compose.animation.AnimatedVisibility(
        visible = true,
        enter = androidx.compose.animation.fadeIn(tween(duration, delayMillis = delayMs, easing = LinearOutSlowInEasing)) +
            androidx.compose.animation.slideInVertically(
                tween(duration, delayMillis = delayMs, easing = LinearOutSlowInEasing),
            ) { it / 5 },
        modifier = modifier,
    ) { content() }
}

/**
 * 🎉 قصاصات احتفال (Confetti) — جسيمات ملونة تتساقط وتدور وتتلاشى.
 * تُعرض عند: وصول دخل جديد، إتمام هدف 100%، أو أي لحظة احتفال.
 */
private data class ConfettiParticle(
    val startX: Float, val color: Color, val size: Float, val fallSpeed: Float,
    val swayAmp: Float, val swayFreq: Float, val phase: Float, val spinSpeed: Float,
)

@Composable
fun ConfettiOverlay(onFinished: () -> Unit) {
    val reduce = rememberReduceMotion()
    val progress = remember { Animatable(0f) }
    val particles = remember {
        val rnd = kotlin.random.Random(42)
        val colors = listOf(
            Color(0xFF6C5CE7), Color(0xFF00B894), Color(0xFFFF7675), Color(0xFFFDCB6E),
            Color(0xFF74B9FF), Color(0xFFFD79A8), Color(0xFF55EFC4), Color(0xFFA29BFE),
        )
        List(60) {
            ConfettiParticle(
                startX = rnd.nextFloat(),
                color = colors[rnd.nextInt(colors.size)],
                size = 6f + rnd.nextFloat() * 8f,
                fallSpeed = 0.7f + rnd.nextFloat() * 0.5f,
                swayAmp = 20f + rnd.nextFloat() * 60f,
                swayFreq = 2f + rnd.nextFloat() * 3f,
                phase = rnd.nextFloat() * 6.28f,
                spinSpeed = 3f + rnd.nextFloat() * 6f,
            )
        }
    }
    LaunchedEffect(Unit) {
        if (reduce) { onFinished(); return@LaunchedEffect }
        progress.snapTo(0f)
        progress.animateTo(1f, tween(1000, easing = LinearOutSlowInEasing))
        onFinished()
    }
    val p = progress.value
    Canvas(
        Modifier
            .fillMaxSize()
            // يبتلع اللمسات حتى لا تصل لما تحتها أثناء الاحتفال
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {},
    ) {
        val w = size.width
        val h = size.height
        particles.forEach { pt ->
            val alpha = if (p > 0.75f) (1f - (p - 0.75f) / 0.25f).coerceAtLeast(0f) else 1f
            val x = pt.startX * w + sin(p * pt.swayFreq * 2 * PI + pt.phase).toFloat() * pt.swayAmp
            val y = p * pt.fallSpeed * h - 40f
            rotate(pt.spinSpeed * p * 360f, pivot = Offset(x, y)) {
                drawRect(
                    color = pt.color.copy(alpha = alpha),
                    topLeft = Offset(x - pt.size / 2, y - pt.size / 2),
                    size = Size(pt.size, pt.size * 0.6f),
                )
            }
        }
    }
}

/**
 * 🌈 تدرج حي — يتنفس بين اللونين الأساسيين ببطء (بطاقات الرصيد).
 */
@Composable
fun animatedGradient(primary: Color, secondary: Color): Brush {
    val reduce = rememberReduceMotion()
    if (reduce) return Brush.linearGradient(listOf(primary, secondary))
    val inf = rememberInfiniteTransition(label = "gradient")
    val phase by inf.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(5000, easing = LinearEasing), RepeatMode.Reverse),
        label = "gradientPhase",
    )
    return Brush.linearGradient(
        colors = listOf(lerp(primary, secondary, phase), lerp(secondary, primary, phase)),
        start = Offset.Zero,
        end = Offset.Infinite,
    )
}

/**
 * 🕊️ أيقونة طافية — تعلو وتهبط بهدوء مستمر (شعار حول التطبيق، الحصالة...).
 */
@Composable
fun FloatingIcon(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    val reduce = rememberReduceMotion()
    val inf = rememberInfiniteTransition(label = "float")
    val offset by inf.animateFloat(
        initialValue = 0f,
        targetValue = 6f,
        animationSpec = infiniteRepeatable(tween(2200, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "floatY",
    )
    Box(modifier.graphicsLayer { translationY = if (reduce) 0f else offset }) { content() }
}

/**
 * 🔢 عدّاد أرقام متصاعد (مهارة التفكير 14: السرعة المُدركة).
 * الرصيد لا "يقفز" للقيمة الجديدة بل يعدّ حتى يصل إليها.
 */
@Composable
fun AnimatedNumber(
    target: Double,
    format: (Double) -> String,
    modifier: Modifier = Modifier,
    style: TextStyle? = null,
    color: Color = Color.Unspecified,
    hidden: Boolean = false,
) {
    val reduceMotion = rememberReduceMotion()
    val animated = remember { Animatable(target.toFloat()) }
    LaunchedEffect(target) {
        animated.animateTo(
            targetValue = target.toFloat(),
            animationSpec = if (reduceMotion) androidx.compose.animation.core.snap() else Motion.standard,
        )
    }
    Text(
        text = if (hidden) "•••••" else format(animated.value.toDouble()),
        modifier = modifier,
        style = style ?: MaterialTheme.typography.headlineMedium,
        color = color,
    )
}

/**
 * 🚪 دخول متدرج للعناصر (مهارة التفكير 10: Staggering).
 * العناصر تظهر بفواصل زمنية قصيرة حسب ترتيبها فتقود العين بترتيب القراءة.
 */
@Composable
fun StaggeredEntrance(
    index: Int,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    val reduceMotion = rememberReduceMotion()
    val duration = if (reduceMotion) 0 else 360
    val delay = if (reduceMotion) 0 else (index * Motion.STAGGER_STEP_MS).coerceAtMost(400)
    androidx.compose.animation.AnimatedVisibility(
        visible = true,
        enter = androidx.compose.animation.fadeIn(tween(duration, delayMillis = delay)) +
            androidx.compose.animation.slideInVertically(tween(duration, delayMillis = delay)) { it / 4 },
        modifier = modifier,
    ) { content() }
}

/**
 * 🪟 انتقال موحد لمحتوى النوافذ الحوارية — تنبثق بنابض وتتلاشى داخلاً.
 */
@Composable
fun DialogContentTransition(content: @Composable () -> Unit) {
    val reduceMotion = rememberReduceMotion()
    val scale = remember { Animatable(if (reduceMotion) 1f else 0.92f) }
    val alpha = remember { Animatable(if (reduceMotion) 1f else 0f) }
    LaunchedEffect(Unit) {
        launch { scale.animateTo(1f, Motion.springBounce) }
        launch { alpha.animateTo(1f, Motion.quick) }
    }
    Box(
        Modifier.graphicsLayer {
            scaleX = scale.value
            scaleY = scale.value
            this.alpha = alpha.value
        },
    ) { content() }
}

/**
 * 🔤 ظهور متدرج لنص حرفاً حرفاً (شاشة الافتتاح).
 */
@Composable
fun StaggeredLetters(text: String, visible: Boolean, style: TextStyle, color: Color) {
    val reduce = rememberReduceMotion()
    Row {
        text.forEachIndexed { index, ch ->
            androidx.compose.animation.AnimatedVisibility(
                visible = visible,
                enter = androidx.compose.animation.fadeIn(
                    tween(320, delayMillis = if (reduce) 0 else index * 45),
                ) + androidx.compose.animation.slideInVertically(
                    tween(320, delayMillis = if (reduce) 0 else index * 45),
                ) { it / 2 },
            ) {
                Text(ch.toString(), style = style, color = color)
            }
        }
    }
}
