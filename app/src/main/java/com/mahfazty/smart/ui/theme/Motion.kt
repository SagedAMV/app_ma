package com.mahfazty.smart.ui.theme

import android.provider.Settings
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.IntOffset

/**
 * نظام الحركة الموحد لـ"محفظتي الذكية" — مهارة التفكير 16: رموز Motion موحدة.
 * كل أنيميشن في التطبيق يستمد مواصفاته من هنا، فيبقى الإحساس متسقاً في كل الشاشات،
 * وأي تعديل مستقبلي للنظام يتم من مكان واحد.
 */
object Motion {

    // ===== المدد الزمنية (مهارة الملحق 42: سلم المدد الناعمة — مرئية للعين لا عابرة) =====
    const val QUICK_MS = 220        // تفاعلات دقيقة (ضغطة — يظهر وميضها للعين)
    const val STANDARD_MS = 340     // انتقالات عادية (بطاقات، عدّادات)
    const val SLOW_MS = 460         // نوافذ ولحظات كبيرة
    const val PAGE_MS = 340         // انتقالات الصفحات — تُرى بوضوح وتُتابع

    // ===== فاصل التتابع المتدرج (مهارة التفكير 10 + الملحق 47) =====
    const val STAGGER_STEP_MS = 50
    const val FIELD_STAGGER_MS = 70   // تتابع خانات النوافذ

    // ===== مواصفات Float (شفافية، حجم، تقدم) =====
    val quick = tween<Float>(QUICK_MS, easing = FastOutSlowInEasing)
    val standard = tween<Float>(STANDARD_MS, easing = FastOutSlowInEasing)
    val slow = tween<Float>(SLOW_MS, easing = FastOutSlowInEasing)
    val enter = tween<Float>(340, easing = LinearOutSlowInEasing)      // دخول: سريع ثم هبوط ناعم
    val exit = tween<Float>(240, easing = FastOutLinearInEasing)       // خروج: هادئ ثم تسارع لطيف
    val settle = tween<Float>(460, easing = LinearOutSlowInEasing)     // الاستقرار المتدرج (الملحق 43)

    // ===== مواصفات IntOffset (انزلاق) =====
    val enterOffset = tween<IntOffset>(340, easing = LinearOutSlowInEasing)
    val exitOffset = tween<IntOffset>(240, easing = FastOutLinearInEasing)

    // ===== النوابض الفيزيائية =====
    val springBounce = spring<Float>(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium)
    val springSmooth = spring<Float>(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessMediumLow)
    val springLively = spring<Float>(dampingRatio = Spring.DampingRatioHighBouncy, stiffness = Spring.StiffnessMedium)
    val springGentle = spring<Float>(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessLow)
    val springPress = spring<Float>(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMediumLow)

    /** نظام 11 للرئيسية: منحنى مطاطي يتجاوز الهدف ثم يستقر (تفكير 7) */
    val elasticOut = CubicBezierEasing(0.34f, 1.56f, 0.64f, 1f)

    // ===== الاهتزاز الهادئ (الملحق 46): سعة صغيرة + موجات بطيئة + هبوط ناعم (معايرة 2.5.4: 720→460ms) =====
    val softShake = keyframes<Float> {
        durationMillis = 460
        0f at 0
        -7f at 70 with LinearOutSlowInEasing
        6f at 150 with LinearOutSlowInEasing
        -4f at 230 with LinearOutSlowInEasing
        3f at 310 with LinearOutSlowInEasing
        -1.5f at 390 with LinearOutSlowInEasing
        0f at 460 with LinearOutSlowInEasing
    }
}

/**
 * كشف تفضيل "تقليل الحركة" من إعدادات نظام أندرويد (مهارة التفكير 15: الإتاحة).
 * المستخدم الذي فعّل "إزالة الحركات" في إعدادات النظام لا يجب أن يشاهد حركاتنا.
 */
@Composable
fun rememberReduceMotion(): Boolean {
    val context = LocalContext.current
    return remember(context) {
        runCatching {
            val cr = context.contentResolver
            val transition = Settings.Global.getString(cr, Settings.Global.TRANSITION_ANIMATION_SCALE)
            val animator = Settings.Global.getString(cr, Settings.Global.ANIMATOR_DURATION_SCALE)
            val off = setOf("0", "0.0", "0.00")
            (transition in off) || (animator in off)
        }.getOrDefault(false)
    }
}
