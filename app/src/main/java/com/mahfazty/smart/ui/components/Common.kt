package com.mahfazty.smart.ui.components

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.ui.window.Dialog
import com.mahfazty.smart.domain.Money
import com.mahfazty.smart.domain.model.DayBar
import com.mahfazty.smart.ui.theme.Motion
import com.mahfazty.smart.domain.model.Transaction
import com.mahfazty.smart.domain.model.TxType
import com.mahfazty.smart.domain.categoryIcon
import com.mahfazty.smart.domain.categoryName
import java.io.File
import kotlinx.coroutines.launch

// ============ نصوص مالية ============

/** نص مبلغ مع دعم إخفاء الأرصدة (وضع الخصوصية) */
@Composable
fun MoneyText(
    amount: Double,
    currency: String,
    hidden: Boolean,
    modifier: Modifier = Modifier,
    style: TextStyle? = null,
    color: Color = Color.Unspecified,
) {
    Text(
        text = if (hidden) "•••••" else "${Money.fmt(amount)} $currency",
        modifier = modifier,
        style = style ?: MaterialTheme.typography.headlineMedium,
        color = color,
    )
}

// ============ حاويات ============

@Composable
fun AppCard(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 2.dp,
    ) { content() }
}

@Composable
fun SectionHeader(title: String, action: String? = null, onAction: () -> Unit = {}) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(title, style = MaterialTheme.typography.titleMedium)
        if (action != null) {
            Text(
                action,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.clickable(onClick = onAction),
            )
        }
    }
}

@Composable
fun EmptyState(icon: String, message: String) {
    // الأيقونة تنبثق بنابض عند أول ظهور (لحظة بهجة حتى في الفراغ)
    val reduce = com.mahfazty.smart.ui.theme.rememberReduceMotion()
    val scale = androidx.compose.animation.core.Animatable(if (reduce) 1f else 0.3f)
    val alpha = androidx.compose.animation.core.Animatable(if (reduce) 1f else 0f)
    LaunchedEffect(Unit) {
        launch {
            scale.animateTo(
                1f,
                androidx.compose.animation.core.spring(
                    dampingRatio = androidx.compose.animation.core.Spring.DampingRatioMediumBouncy,
                    stiffness = androidx.compose.animation.core.Spring.StiffnessLow,
                ),
            )
        }
        launch { alpha.animateTo(1f, com.mahfazty.smart.ui.theme.Motion.enter) }
    }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            icon,
            fontSize = 40.sp,
            modifier = Modifier.graphicsLayer {
                scaleX = scale.value
                scaleY = scale.value
                this.alpha = alpha.value
            },
        )
        Spacer(Modifier.height(8.dp))
        Text(message, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

/** صف عملية مالية (يُستخدم في الرئيسية والسجل) */
@Composable
fun TxRow(
    tx: Transaction,
    currency: String,
    hidden: Boolean,
    onClick: () -> Unit,
    running: Double? = null,
    modifier: Modifier = Modifier,
) {
    val isIncome = tx.type == TxType.INCOME
    val amountColor = when {
        tx.type == TxType.TRANSFER -> MaterialTheme.colorScheme.primary
        isIncome -> com.mahfazty.smart.ui.theme.LocalAppColors.current.green
        else -> com.mahfazty.smart.ui.theme.LocalAppColors.current.red
    }
    val sign = when {
        tx.type == TxType.TRANSFER -> ""
        isIncome -> "+"
        else -> "-"
    }
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 10.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center,
            ) { Text(categoryIcon(tx.category), fontSize = 20.sp) }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(categoryName(tx.category), style = MaterialTheme.typography.labelLarge)
                if (!tx.note.isNullOrBlank()) {
                    Text(tx.note, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
                }
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    "$sign${if (hidden) "•••" else Money.fmt(tx.amount)}",
                    style = MaterialTheme.typography.labelLarge,
                    color = amountColor,
                )
                if (running != null) {
                    Text(
                        "الرصيد: ${if (hidden) "•••" else Money.fmt(running)} $currency",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

/** خط فاصل ناعم */
@Composable
fun SoftDivider() {
    HorizontalDivider(
        modifier = Modifier.padding(horizontal = 16.dp),
        color = com.mahfazty.smart.ui.theme.LocalAppColors.current.border,
        thickness = 0.5.dp,
    )
}

// ============ نوافذ ============

/** نافذة حوار علوية بتصميم موحد — تنبثق بنابض وتتلاشى داخلاً */
@Composable
fun AppDialog(onDismiss: () -> Unit, content: @Composable () -> Unit) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface,
            shadowElevation = 12.dp,
        ) {
            DialogContentTransition { content() }
        }
    }
}

/** ورقة سفلية بنمط مودالات نسخة الويب */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppSheet(title: String, onDismiss: () -> Unit, content: @Composable () -> Unit) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
    ) {
        Column(Modifier.padding(bottom = 24.dp)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(title, style = MaterialTheme.typography.titleMedium)
                IconButton(onClick = onDismiss) {
                    Text("✕", style = MaterialTheme.typography.titleMedium)
                }
            }
            SoftDivider()
            content()
        }
    }
}

/**
 * حوار تأكيد عام.
 * extraText/onExtra: خيار ثالث اختياري (مثال: «فتح العميل الموجود» عند كشف التكرار — إصلاح act-5).
 */
@Composable
fun ConfirmDialog(
    title: String,
    message: String,
    confirmText: String = "حذف",
    danger: Boolean = true,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    extraText: String? = null,
    onExtra: (() -> Unit)? = null,
) {
    AppDialog(onDismiss = onDismiss) {
        Column(Modifier.padding(20.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(8.dp))
            Text(message, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(16.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                TextButton(onClick = onDismiss) { Text("إلغاء") }
                if (extraText != null) {
                    TextButton(onClick = { onExtra?.invoke() }) { Text(extraText) }
                    Spacer(Modifier.width(4.dp))
                }
                Spacer(Modifier.width(8.dp))
                Button(
                    onClick = onConfirm,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (danger) com.mahfazty.smart.ui.theme.LocalAppColors.current.red
                        else MaterialTheme.colorScheme.primary,
                    ),
                ) { Text(confirmText) }
            }
        }
    }
}

// ============ حقول إدخال ============

@Composable
fun AmountField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    currency: String,
    modifier: Modifier = Modifier,
) {
    OutlinedTextField(
        value = value,
        onValueChange = { text -> onValueChange(text.filter { it.isDigit() || it == '.' }) },
        modifier = modifier.fillMaxWidth(),
        label = { Text(label) },
        suffix = { Text(currency, style = MaterialTheme.typography.labelMedium) },
        shape = RoundedCornerShape(14.dp),
        singleLine = true,
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = MaterialTheme.colorScheme.primary,
            unfocusedBorderColor = com.mahfazty.smart.ui.theme.LocalAppColors.current.border,
        ),
    )
}

@Composable
fun AppTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    /** إصلاح sec-5: حد أقصى لعدد الأحرف (يمنع تشوه العرض والتصدير) */
    maxLength: Int = Int.MAX_VALUE,
    /** إصلاح sec-4: لوحة مفاتيح رقمية لحقول الهاتف */
    phoneKeypad: Boolean = false,
) {
    OutlinedTextField(
        value = value,
        onValueChange = { if (it.length <= maxLength) onValueChange(it) },
        modifier = modifier.fillMaxWidth(),
        label = { Text(label) },
        shape = RoundedCornerShape(14.dp),
        singleLine = true,
        keyboardOptions = KeyboardOptions(
            keyboardType = if (phoneKeypad) KeyboardType.Phone else KeyboardType.Text,
        ),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = MaterialTheme.colorScheme.primary,
            unfocusedBorderColor = com.mahfazty.smart.ui.theme.LocalAppColors.current.border,
        ),
    )
}

/** زر تبديل خيارين (مصروف/دخل، بنك/كاش...) */
@Composable
fun <T> SegmentedSwitch(
    options: List<Pair<T, String>>,
    selected: T,
    onSelect: (T) -> Unit,
    modifier: Modifier = Modifier,
    selectedColor: Color? = null,
) {
    val resolvedSelected = selectedColor ?: MaterialTheme.colorScheme.primary
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(14.dp))
            .padding(4.dp),
    ) {
        options.forEach { (value, label) ->
            val isSelected = value == selected
            val segScale by animateFloatAsState(
                targetValue = if (isSelected) 1.05f else 1f,
                animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
                label = "segScale",
            )
            Box(
                modifier = Modifier
                    .weight(1f)
                    .graphicsLayer {
                        scaleX = segScale
                        scaleY = segScale
                    }
                    .background(
                        if (isSelected) resolvedSelected else Color.Transparent,
                        RoundedCornerShape(11.dp),
                    )
                    .clickable { onSelect(value) }
                    .padding(vertical = 10.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    label,
                    style = MaterialTheme.typography.labelMedium,
                    color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

// ============ المخططات ============

/** مخطط أعمدة بسيط بدون مكتبات خارجية — الأعمدة تنمو من الأسفل بتتابع متدرج + نبض عمود اليوم */
@Composable
fun BarChart(bars: List<DayBar>, color: Color, height: Int = 130, highlightLast: Boolean = false) {
    if (bars.isEmpty()) {
        EmptyState("📊", "لا توجد بيانات بعد")
        return
    }
    // التقاط القيم قبل Canvas: نطاق DrawScope ليس سياق Composable
    val reduceMotion = com.mahfazty.smart.ui.theme.rememberReduceMotion()
    val trackColor = MaterialTheme.colorScheme.surfaceVariant
    val maxValue = bars.maxOf { it.value }.coerceAtLeast(1.0)
    // إصلاح انعكاس الأيام: الرسم داخل Canvas لا يراعي اتجاه RTL إطلاقاً (إحداثيات فيزيائية دائماً)،
    // بينما صف التسميات تحته يُرتَّب RTL — فكان كل عمود يقف فوق يوم معكوس. نلتقط الاتجاه هنا لنعاكس المواضع.
    val rtl = LocalLayoutDirection.current == LayoutDirection.Rtl
    // نبض عمود "اليوم" (آخر عمود) — يجذب العين لأحدث البيانات
    val pulseAlpha = if (highlightLast && !reduceMotion) {
        val inf = androidx.compose.animation.core.rememberInfiniteTransition(label = "todayPulse")
        inf.animateFloat(
            initialValue = 0.55f,
            targetValue = 1f,
            animationSpec = androidx.compose.animation.core.infiniteRepeatable(
                androidx.compose.animation.core.tween(
                    700,
                    easing = androidx.compose.animation.core.FastOutSlowInEasing,
                ),
                androidx.compose.animation.core.RepeatMode.Reverse,
            ),
            label = "todayAlpha",
        ).value
    } else 1f
    // مهارة التنفيذ 4 + مهارة التفكير 10: كل عمود ينمو بتأخير 80ms عن سابقه
    val fractions = bars.mapIndexed { index, bar ->
        androidx.compose.animation.core.animateFloatAsState(
            targetValue = if (bar.value > 0) (bar.value / maxValue).toFloat() else 0f,
            animationSpec = if (reduceMotion) androidx.compose.animation.core.snap()
            else androidx.compose.animation.core.tween(
                durationMillis = 600,
                delayMillis = index * Motion.STAGGER_STEP_MS,
                easing = Motion.elasticOut,
            ),
            label = "bar$index",
        ).value
    }
    Column(Modifier.fillMaxWidth().padding(horizontal = 8.dp)) {
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(height.dp),
        ) {
            val barWidth = size.width / (bars.size * 1.6f)
            val gap = (size.width - barWidth * bars.size) / (bars.size + 1)
            val chartHeight = size.height - 24.dp.toPx()
            bars.forEachIndexed { index, bar ->
                val h = (fractions[index] * chartHeight)
                    .coerceAtLeast(if (bar.value > 0) 6.dp.toPx() else 2.dp.toPx())
                // إصلاح: في RTL يُرسم أقدم عمود في أقصى اليمين و«اليوم» في أقصى اليسار —
                // مطابقاً لترتيب صف التسميات تحته، فيقف كل عمود فوق يومه الصحيح
                val left = if (rtl) size.width - gap - barWidth - index * (barWidth + gap)
                else gap + index * (barWidth + gap)
                val top = chartHeight - h
                val isToday = highlightLast && index == bars.lastIndex
                drawRoundRect(
                    color = if (bar.value > 0) {
                        if (isToday) color.copy(alpha = pulseAlpha) else color
                    } else trackColor,
                    topLeft = Offset(left, top),
                    size = Size(barWidth, h),
                    cornerRadius = CornerRadius(barWidth / 2, barWidth / 2),
                )
            }
        }
        Row(Modifier.fillMaxWidth()) {
            bars.forEachIndexed { index, bar ->
                Text(
                    bar.label,
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

// ============ الصور ============

/**
 * إصلاح sec-5: حفظ الصور بأمان —
 *  1) حد أقصى لحجم الملف المصدر (20 م.ب) قبل أي نسخ.
 *  2) تصغير (downsampling) لأي شيء يتجاوز 1600px + ضغط JPEG —
 *     فلا تخزين ملفات ضخمة ولا decode كامل لاحقاً يستهلك الذاكرة (OutOfMemoryError).
 *  3) فشل الحفظ لم يعد صامتاً: يُرجع رسالة واضحة تُعرض للمستخدم (إصلاح act-6).
 */
object PhotoStore {
    const val MAX_SOURCE_BYTES = 20L * 1024 * 1024
    const val MAX_DIMENSION = 1600

    data class PhotoResult(val path: String?, val error: String?)

    fun save(context: Context, uri: Uri, prefix: String): PhotoResult = runCatching {
        val size = context.contentResolver.openInputStream(uri)?.use { it.length() } ?: 0L
        if (size > MAX_SOURCE_BYTES) {
            return@runCatching PhotoResult(null, "حجم الصورة كبير جداً (الحد الأقصى 20 م.ب) — اختر صورة أصغر")
        }
        // 1) قراءة الأبعاد فقط
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        context.contentResolver.openInputStream(uri)?.use {
            BitmapFactory.decodeStream(it, null, bounds)
        }
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) {
            return@runCatching PhotoResult(null, "تعذر قراءة الصورة — صيغة غير مدعومة")
        }
        // 2) تصغير قبل الفتح الكامل
        var sample = 1
        while (maxOf(bounds.outWidth, bounds.outHeight) / sample > MAX_DIMENSION) sample *= 2
        val opts = BitmapFactory.Options().apply { inSampleSize = sample }
        val decoded = context.contentResolver.openInputStream(uri)?.use {
            BitmapFactory.decodeStream(it, null, opts)
        } ?: return@runCatching PhotoResult(null, "تعذر قراءة الصورة")
        val bitmap = if (decoded.width > MAX_DIMENSION || decoded.height > MAX_DIMENSION) {
            val scale = maxOf(
                decoded.width.toFloat() / MAX_DIMENSION,
                decoded.height.toFloat() / MAX_DIMENSION,
            )
            Bitmap.createScaledBitmap(
                decoded,
                (decoded.width / scale).toInt(),
                (decoded.height / scale).toInt(),
                true,
            )
        } else decoded
        val dir = File(context.filesDir, "photos").apply { mkdirs() }
        val file = File(dir, "${prefix}_${System.currentTimeMillis()}.jpg")
        file.outputStream().use { bitmap.compress(Bitmap.CompressFormat.JPEG, 82, it) }
        PhotoResult(file.absolutePath, null)
    }.getOrElse { e -> PhotoResult(null, "تعذر حفظ الصورة: ${e.message}") }

    /**
     * فتح صورة للتصغير (أفاتار/معاينة) بحجم هدف محدد — لا تفكيك كامل للصورة الضخمة.
     */
    fun load(path: String?, targetPx: Int = 512): androidx.compose.ui.graphics.ImageBitmap? {
        if (path.isNullOrBlank()) return null
        return runCatching {
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeFile(path, bounds)
            if (bounds.outWidth <= 0) return@runCatching null
            var sample = 1
            while (maxOf(bounds.outWidth, bounds.outHeight) / sample > targetPx) sample *= 2
            BitmapFactory.decodeFile(path, BitmapFactory.Options().apply { inSampleSize = sample })
                ?.asImageBitmap()
        }.getOrNull()
    }
}

/** صورة دائرية من ملف */
@Composable
fun PhotoAvatar(path: String?, fallback: String, sizeDp: Int = 46) {
    val bmp = remember(path) { PhotoStore.load(path) }
    Box(
        modifier = Modifier
            .size(sizeDp.dp)
            .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(14.dp)),
        contentAlignment = Alignment.Center,
    ) {
        if (bmp != null) {
            Image(
                bitmap = bmp,
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
            )
        } else {
            Text(fallback, style = MaterialTheme.typography.titleMedium)
        }
    }
}
