package com.mahfazty.smart.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.CornerSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlurEffect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.changedToUpIgnoreConsumed
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.unit.dp
import com.mahfazty.smart.ui.theme.Motion
import com.mahfazty.smart.ui.theme.rememberReduceMotion
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin

/**
 * 💫 ضغطة ناعمة عالمية: عند اللمس ينضغط العنصر بسرعة وبقوة واضحة،
 * يلمع وميض أبيض فوقه (تأكيد بصري) وينخفض 2px خفيفاً، وعند الإفلات
 * يرتدّ مكانه بنابض.
 *
 * معايرة 2026 (تحليل التصميم): ضغط أقوى (0.92) وأسرع (160ms) ووميض أوضح (0.48).
 *
 * لا تستهلك أي حدث لمس إطلاقاً (requireUnconsumed=false وبدون consume) —
 * فلا تتصادم مع clickable أو أي إيماءة أخرى على نفس العنصر.
 */
fun Modifier.bounceClick(): Modifier = composed {
    val reduce = rememberReduceMotion()
    val scale = remember { Animatable(1f) }
    val press = remember { Animatable(0f) }
    val pressY = remember { Animatable(0f) }
    val scope = rememberCoroutineScope()
    this
        .graphicsLayer {
            scaleX = scale.value
            scaleY = scale.value
            translationY = pressY.value
        }
        .drawWithContent {
            drawContent()
            if (press.value > 0f) {
                drawRect(Color.White.copy(alpha = press.value * 0.48f))
            }
        }
        .pointerInput(Unit) {
            awaitEachGesture {
                val down = awaitFirstDown(requireUnconsumed = false)
                if (!reduce) {
                    scope.launch { scale.animateTo(0.92f, tween(160, easing = FastOutSlowInEasing)) }
                    scope.launch { press.animateTo(1f, tween(140)) }
                    scope.launch { pressY.animateTo(2f, tween(120, easing = FastOutSlowInEasing)) }
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
                    scope.launch { pressY.animateTo(0f, Motion.springPress) }
                }
            }
        }
}

/**
 * 🕰️ تأرجح الساعة: البطاقة تدخل بدوران كعقرب ساعة يتأرجح، ثم ترتد في الاتجاه
 * المعاكس قليلاً بتأثير الجاذبية، ثم تستقر في مكانها بنعومة. مع تلاشٍ داخلي
 * وتقليص طفيف (0.95 → 1.0) أثناء الدوران.
 *
 * معايرة 2026: تأرجح أوضح (-12° → 8° → -3° → 0°) وأسرع (350ms + 250ms).
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
    val scale = remember { Animatable(1f) }
    LaunchedEffect(Unit) {
        if (reduce) {
            alpha.snapTo(1f)
            return@LaunchedEffect
        }
        delay((index * Motion.STAGGER_STEP_MS).toLong())
        launch { alpha.animateTo(1f, tween(380, easing = LinearOutSlowInEasing)) }
        // 1) يدخل مائلاً كعقرب ساعة ثم يتأرجح نحو الداخل
        rotation.snapTo(-12f)
        rotation.animateTo(8f, tween(350, easing = LinearOutSlowInEasing))
        // 2) ارتداد معاكس خفيف — تأثير الجاذبية
        rotation.animateTo(-3f, tween(250, easing = LinearOutSlowInEasing))
        // 3) الاستقرار المتدرج في الوضع الطبيعي
        rotation.animateTo(0f, Motion.springGentle)
        // 4) تقليص خفيف أثناء الدوران (عمق بصري)
        scale.snapTo(0.95f)
        launch { scale.animateTo(1f, Motion.springGentle) }
    }
    Box(
        modifier.graphicsLayer {
            rotationZ = rotation.value
            this.alpha = alpha.value
            scaleX = scale.value
            scaleY = scale.value
        },
    ) { content() }
}

/**
 * نظام 11 — مطاط يتجاوز ثم يستقر.
 * معايرة 2026: ارتداد أقوى (منحنى 1.8)، أسرع (480ms)، وبداية أصغر (0.5).
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
        t.animateTo(1f, tween(480, easing = Motion.elasticOutStrong))
    }
    val v = t.value
    Box(
        modifier.graphicsLayer {
            val s = 0.5f + 0.5f * v
            scaleX = s
            scaleY = s
            translationY = 18f * (1f - v.coerceAtMost(1.2f))
            alpha = v.coerceIn(0f, 1f)
        },
    ) { content() }
}

/**
 * 📋 تتابع خانات النافذة: كل خانة في النوافذ المنبثقة تظهر
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
 * 🎉 قصاصات احتفال — جسيمات ملونة تتساقط وتدور وتتلاشى.
 * تُعرض عند: وصول دخل جديد، إتمام هدف 100%، أو أي لحظة احتفال.
 *
 * معايرة 2026: 90 جسيماً (كان 60)، مدة أطول (1500ms)، الجسيمات البعيدة
 * تحاط بهالة ناعمة (عمق ميدان — يُحاكى برسم ممتد شفّاف خلفها على كل الإصدارات)،
 * وتلاشي تدريجي أسفل الشاشة.
 */
private data class ConfettiParticle(
    val startX: Float, val color: Color, val size: Float, val fallSpeed: Float,
    val swayAmp: Float, val swayFreq: Float, val phase: Float, val spinSpeed: Float,
    val isFar: Boolean,
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
        List(90) { i ->
            val far = i % 3 == 0
            ConfettiParticle(
                startX = rnd.nextFloat(),
                color = colors[rnd.nextInt(colors.size)],
                size = if (far) 5f + rnd.nextFloat() * 4f else 6f + rnd.nextFloat() * 8f,
                fallSpeed = 0.7f + rnd.nextFloat() * 0.5f,
                swayAmp = 20f + rnd.nextFloat() * 60f,
                swayFreq = 2f + rnd.nextFloat() * 3f,
                phase = rnd.nextFloat() * 6.28f,
                spinSpeed = 3f + rnd.nextFloat() * 6f,
                isFar = far,
            )
        }
    }
    LaunchedEffect(Unit) {
        if (reduce) { onFinished(); return@LaunchedEffect }
        progress.snapTo(0f)
        progress.animateTo(1f, tween(1500, easing = LinearOutSlowInEasing))
        onFinished()
    }
    val p = progress.value
    // قسم الجسيمات طبقتين مرة واحدة: بعيدة (ضبابية) وقريبة (حادة)
    val (farList, nearList) = remember {
        val far = particles.filter { it.isFar }
        val near = particles.filter { !it.isFar }
        far to near
    }
    Box(
        Modifier
            .fillMaxSize()
            // يبتلع اللمسات حتى لا تصل لما تحتها أثناء الاحتفال
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {},
    ) {
        // الطبقة البعيدة — ضبابية بعد 30% من السقوط (تُتجاهل تلقائياً قبل أندرويد 12)
        Canvas(
            Modifier.fillMaxSize().graphicsLayer {
                renderEffect = if (p > 0.3f) BlurEffect(4f, 4f) else null
            },
        ) { drawConfetti(p, farList, dim = 0.7f) }
        // الطبقة القريبة — حادة
        Canvas(Modifier.fillMaxSize()) { drawConfetti(p, nearList, dim = 1f) }
    }
}

/** رسم جسيمات القصاصات داخل Canvas (مشترك بين الطبقتين). */
private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawConfetti(
    p: Float,
    list: List<ConfettiParticle>,
    dim: Float,
) {
    val w = size.width
    val h = size.height
    list.forEach { pt ->
        val alpha = if (p > 0.75f) (1f - (p - 0.75f) / 0.25f).coerceAtLeast(0f) else 1f
        val x = pt.startX * w + sin(p * pt.swayFreq * 2 * PI + pt.phase).toFloat() * pt.swayAmp
        val y = p * pt.fallSpeed * h - 40f
        // تلاشٍ تدريجي في أسفل الشاشة (قناع تدرجي)
        val bottomFade = if (y > h * 0.7f) 1f - (y - h * 0.7f) / (h * 0.3f) else 1f
        val a = (alpha * bottomFade * dim).coerceIn(0f, 1f)
        if (a <= 0f) return@forEach
        rotate(pt.spinSpeed * p * 360f, pivot = Offset(x, y)) {
            drawRect(
                color = pt.color.copy(alpha = a),
                topLeft = Offset(x - pt.size / 2, y - pt.size / 2),
                size = Size(pt.size, pt.size * 0.6f),
            )
        }
    }
}

/**
 * 🌈 تدرج حي — يتنفس بين ثلاث درجات (أول، منتصف، ثاني) ببطء مع دوران
 * تدريجي لاتجاه التدرج (بطاقات الرصيد).
 *
 * معايرة 2026: أبطأ (9 ثوانٍ — كان 5) ولون ثالث في المنتصف.
 */
@Composable
fun animatedGradient(primary: Color, secondary: Color): Brush {
    val reduce = rememberReduceMotion()
    if (reduce) return Brush.linearGradient(listOf(primary, secondary))
    val inf = rememberInfiniteTransition(label = "gradient")
    val phase by inf.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(9000, easing = LinearEasing), RepeatMode.Reverse),
        label = "gradientPhase",
    )
    val mid = lerp(primary, secondary, 0.5f)
    return Brush.linearGradient(
        colors = listOf(
            lerp(primary, mid, phase),
            lerp(mid, secondary, phase),
            lerp(secondary, primary, phase),
        ),
        start = Offset.Zero,
        // دوران تدريجي لاتجاه الخط: من عمودي نحو مائل ثم يعكس (موجة)
        end = Offset(900f * phase, 900f * (1f - phase)),
    )
}

/**
 * 🕊️ أيقونة طافية — تعلو وتهبط بهدوء مستمر (شعار حول التطبيق، الحصالة...).
 *
 * معايرة 2026: حركة أكبر (12px) + حركة أفقية (±4px) فتصبح المسار بيضاوياً
 * مع easing ناعم في الطرفين (اقتراب من موجة جيبية).
 */
@Composable
fun FloatingIcon(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    val reduce = rememberReduceMotion()
    val inf = rememberInfiniteTransition(label = "float")
    val offsetY by inf.animateFloat(
        initialValue = 0f,
        targetValue = 12f,
        animationSpec = infiniteRepeatable(tween(2200, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "floatY",
    )
    val offsetX by inf.animateFloat(
        initialValue = -4f,
        targetValue = 4f,
        animationSpec = infiniteRepeatable(tween(2800, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "floatX",
    )
    Box(modifier.graphicsLayer {
        translationY = if (reduce) 0f else offsetY
        translationX = if (reduce) 0f else offsetX
    }) { content() }
}

/**
 * 🔢 عدّاد أرقام متصاعد: الرصيد لا "يقفز" للقيمة الجديدة بل يعدّ حتى يصل إليها.
 *
 * معايرة 2026: المدة ديناميكية حسب حجم الفرق (200–500ms بدل 340 ثابتة)،
 * ووميض أخضر خفيف عند الزيادة (تأكيد إيجابي).
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
    val flash = remember { Animatable(0f) }
    var counting by remember { mutableStateOf(false) }
    var lastTarget by remember { mutableStateOf(target) }
    LaunchedEffect(target) {
        // abs دالة عليا وليست امتداداً — الاستدعاء الصحيح abs(x) لا x.abs()
        val diff = abs(target.toFloat() - animated.value)
        // مدة متغيرة: فرق صغير = عدّ سريع، فرق كبير = حتى 500ms
        val duration = (200 + (diff / 100f).coerceIn(0f, 300f)).toInt()
        if (!reduceMotion && diff > 0.001) counting = true
        animated.animateTo(
            targetValue = target.toFloat(),
            animationSpec = if (reduceMotion) snap() else tween(duration, easing = FastOutSlowInEasing),
        )
        counting = false
        if (!reduceMotion && target > lastTarget) {
            flash.snapTo(0.18f)
            flash.animateTo(0f, tween(700, easing = LinearOutSlowInEasing))
        }
        lastTarget = target
    }
    Box(modifier) {
        Text(
            text = if (hidden) "•••••" else format(animated.value.toDouble()),
            style = style ?: MaterialTheme.typography.headlineMedium,
            color = color,
            modifier = Modifier.graphicsLayer {
                // ضباب خفيف أثناء العدّ — إيحاء رقمي (تُتجاهل قبل أندرويد 12)
                renderEffect = if (counting) BlurEffect(2f, 2f) else null
            },
        )
        if (flash.value > 0f) {
            Box(
                Modifier
                    .matchParentSize()
                    .background(Color(0xFF00B894).copy(alpha = flash.value)),
            )
        }
    }
}

/**
 * 🚪 دخول متدرج للعناصر: العناصر تظهر بفواصل زمنية قصيرة حسب ترتيبها
 * فتقود العين بترتيب القراءة.
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
 *
 * معايرة 2026: انبثاق أقوى (0.85 → 1.05 → 1.0) بدل (0.92 → 1.0).
 */
@Composable
fun DialogContentTransition(content: @Composable () -> Unit) {
    val reduceMotion = rememberReduceMotion()
    val scale = remember { Animatable(if (reduceMotion) 1f else 0.85f) }
    val alpha = remember { Animatable(if (reduceMotion) 1f else 0f) }
    LaunchedEffect(Unit) {
        launch {
            scale.animateTo(1.05f, tween(250, easing = FastOutSlowInEasing))
            scale.animateTo(1f, Motion.springBounce)
        }
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

// =====================================================================
// أنميشنات جديدة (تحليل التصميم 2026)
// =====================================================================

/**
 * ⌨️ كتابة تدريجية (Typewriter) — الأحرف تظهر تباعاً بحجم كامل للنص في Text واحد،
 * باستخدام spans شفافية لكل حرف: تحافظ على الربط العربي (لا تفكيك الأحرف
 * إلى نصوص منفصلة يكسر اتصال الحروف).
 *
 * ملاحظة: يجب أن يكون [color] لوناً محدداً (لا Color.Unspecified) لأن
 * الشفافية تُطبق عليه.
 */
@Composable
fun TypewriterReveal(
    text: String,
    visible: Boolean,
    style: TextStyle,
    color: Color,
    modifier: Modifier = Modifier,
) {
    val reduce = rememberReduceMotion()
    val progress = remember { Animatable(if (reduce || visible) 1f else 0f) }
    LaunchedEffect(visible) {
        if (visible && !reduce) {
            progress.snapTo(0f)
            progress.animateTo(1f, tween(900, easing = LinearOutSlowInEasing))
        }
    }
    val p = if (reduce) 1f else progress.value
    val ann = remember(text, p) {
        // AnnotatedString ثابتة لا تحوي addStyle — البناء الصحيح عبر buildAnnotatedString
        buildAnnotatedString {
            append(text)
            if (p < 1f) {
                val len = text.length
                for (i in 0 until len) {
                    val chAlpha = (p * (len + 4f) - i).coerceIn(0f, 1f)
                    addStyle(SpanStyle(color = color.copy(alpha = chAlpha)), i, i + 1)
                }
            }
        }
    }
    Text(ann, style = style, color = color, modifier = modifier)
}

/**
 * 🫧 Morph — انتقال تدريجي بين شكلين (Shape).
 * إذا كان الشكلان كلاهما زوايا دائرية (RoundedCornerShape) يُستوفى نصف قطر
 * كل زاوية على حدة، وإلا يُبَدَّل الشكل عند نقطة المنتصف.
 */
@Composable
fun MorphTransition(
    from: Shape,
    to: Shape,
    progress: Float,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    val p = progress.coerceIn(0f, 1f)
    val density = LocalDensity.current
    val shape: Shape = remember(from, to, p, density) {
        val f = from as? RoundedCornerShape
        val t = to as? RoundedCornerShape
        if (f != null && t != null) {
            // زوايا RoundedCornerShape نوعها CornerSize لا CornerRadius — نُدرّج بالبكسل عبر Density
            // (مرجع 100px يؤثر فقط على الزوايا المئوية %؛ زوايا dp/px تُقيَّم بدقة تامة)
            val reference = Size(100f, 100f)
            val lerpCorner = { a: CornerSize, b: CornerSize ->
                val ra = a.toPx(reference, density)
                val rb = b.toPx(reference, density)
                CornerSize(ra + (rb - ra) * p)
            }
            RoundedCornerShape(
                topStart = lerpCorner(f.topStart, t.topStart),
                topEnd = lerpCorner(f.topEnd, t.topEnd),
                bottomStart = lerpCorner(f.bottomStart, t.bottomStart),
                bottomEnd = lerpCorner(f.bottomEnd, t.bottomEnd),
            )
        } else if (p < 0.5f) from else to
    }
    Box(modifier = modifier.clip(shape)) { content() }
}

/**
 * 🌊 ملء سائل (Liquid Fill) — موجتان متراكمتان ترتفعان/تنخفضان حسب [progress].
 * يُوضع داخل عنصر بقاعدة دائرية أو مستطيلة (مثل مؤشر تقدم دائري).
 */
@Composable
fun LiquidFillIndicator(
    progress: Float,
    color: Color,
    modifier: Modifier = Modifier,
) {
    val reduce = rememberReduceMotion()
    val inf = rememberInfiniteTransition(label = "liquid")
    val phase by inf.animateFloat(
        initialValue = 0f,
        targetValue = 2f * PI.toFloat(),
        animationSpec = infiniteRepeatable(tween(2600, easing = LinearEasing), RepeatMode.Restart),
        label = "liquidPhase",
    )
    val p = progress.coerceIn(0f, 1f)
    Canvas(modifier) {
        val ph = if (reduce) 0f else phase
        val level = size.height * (1f - p)
        fun wave(amp: Float, freq: Float, phaseOffset: Float, colorAlpha: Float) {
            val path = Path()
            path.moveTo(0f, size.height)
            var x = 0f
            while (x <= size.width) {
                val y = level + sin((x / size.width) * 2f * freq * PI.toFloat() + ph + phaseOffset) * amp
                path.lineTo(x, y)
                x += 4f
            }
            path.lineTo(size.width, size.height)
            path.close()
            drawPath(path, color.copy(alpha = colorAlpha))
        }
        wave(6.dp.toPx(), 2f, 0f, 0.55f)
        wave(5.dp.toPx(), 2.6f, PI.toFloat(), 0.9f)
    }
}

private data class BurstParticle(
    val angle: Float, val dist: Float, val speed: Float, val size: Float,
)

/**
 * 💥 انفجار جسيمات (Particle Burst) — جسيمات تنفجر من مركز العنصر وتتلاشى
 * ثم يستدعي [onFinished] (لحظات النجاح والتأكيد).
 */
@Composable
fun ParticleBurst(
    color: Color,
    count: Int = 20,
    onFinished: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val reduce = rememberReduceMotion()
    val progress = remember { Animatable(0f) }
    val burst = remember {
        val rnd = kotlin.random.Random(7)
        List(count) {
            BurstParticle(
                angle = rnd.nextFloat() * 2f * PI.toFloat(),
                dist = 34f + rnd.nextFloat() * 56f,
                speed = 0.75f + rnd.nextFloat() * 0.5f,
                size = 3f + rnd.nextFloat() * 3f,
            )
        }
    }
    LaunchedEffect(Unit) {
        if (reduce) { onFinished(); return@LaunchedEffect }
        progress.snapTo(0f)
        progress.animateTo(1f, tween(700, easing = LinearOutSlowInEasing))
        onFinished()
    }
    val p = progress.value
    Canvas(modifier) {
        val cx = size.width / 2f
        val cy = size.height / 2f
        burst.forEach { b ->
            val d = p * b.dist * b.speed
            val x = cx + cos(b.angle) * d
            val y = cy + sin(b.angle) * d + p * p * 14f // جاذبية خفيفة في النهاية
            val a = (1f - p).coerceAtLeast(0f)
            if (a > 0f) {
                drawCircle(color = color.copy(alpha = a), radius = b.size, center = Offset(x, y))
            }
        }
    }
}

/** أنماط كشف النص (Text Reveal). */
enum class RevealStyle { FADE_IN, SLIDE_UP, TYPEWRITER, GLITCH }

/**
 * 🎬 كشف نص بأنماط متعددة: تلاشٍ / صعود / كتابة تدريجية / طيف قصير (glitch).
 * [textStyle] و [color] ينطبقان على النص (TYPEWRITER يتطلب لوناً محدداً).
 */
@Composable
fun TextReveal(
    text: String,
    visible: Boolean,
    style: RevealStyle = RevealStyle.FADE_IN,
    textStyle: TextStyle = MaterialTheme.typography.bodyLarge,
    color: Color = MaterialTheme.colorScheme.onSurface,
    modifier: Modifier = Modifier,
) {
    when (style) {
        RevealStyle.TYPEWRITER ->
            TypewriterReveal(text, visible, textStyle, color, modifier)

        RevealStyle.GLITCH -> {
            val reduce = rememberReduceMotion()
            val jitter = remember { Animatable(0f) }
            var shown by remember { mutableStateOf(false) }
            LaunchedEffect(visible) {
                if (!visible) return@LaunchedEffect
                if (reduce) {
                    shown = true
                } else {
                    repeat(8) { i ->
                        jitter.snapTo(if (i % 2 == 0) 5f else -5f)
                        delay(28)
                    }
                    jitter.snapTo(0f)
                    shown = true
                }
            }
            if (shown) {
                Text(
                    text,
                    style = textStyle,
                    color = color,
                    modifier = modifier.graphicsLayer { translationX = jitter.value },
                )
            }
        }

        else -> {
            val enter = androidx.compose.animation.fadeIn(tween(340, easing = LinearOutSlowInEasing)) +
                (if (style == RevealStyle.SLIDE_UP) {
                    androidx.compose.animation.slideInVertically(tween(340, easing = LinearOutSlowInEasing)) { it / 2 }
                } else {
                    androidx.compose.animation.slideInVertically(tween(1)) { 0 }
                })
            androidx.compose.animation.AnimatedVisibility(
                visible = visible,
                enter = enter,
                modifier = modifier,
            ) {
                Text(text, style = textStyle, color = color)
            }
        }
    }
}
