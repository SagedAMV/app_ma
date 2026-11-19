package com.mahfazty.smart.design

/**
 * بيانات نموذجية ثابتة للتصاميم المقترحة — بلا ViewModels ولا قواعد بيانات
 * (مأخوذة من واقع التطبيق: عملات، فئات، عملاء ماطور/حسابات يمنية).
 */
object ProposalData {

    // ===== الرئيسية =====
    val totalWealth = "١٢٣٬٤٥٦ ر.س"          // ثابت عرض فقط
    const val bankBalance = 45_230
    const val cashBalance = 8_120
    const val monthlyIncome = 38_500
    const val monthlyExpense = 21_300
    const val monthlySavings = 6_000
    const val savingsGoalMonthly = 10_000
    const val savingsTotal = 68_400
    val weekly = listOf(35 to 12, 22 to 6, 41 to 18, 28 to 9, 48 to 25, 31 to 14, 19 to 5)

    // ===== العمليات =====
    data class Tx(
        val icon: String,
        val title: String,
        val category: String,
        val amount: String,
        val sign: Int,          // +1 دخل / -1 مصروف / 0 تحويل
        val date: String,
        val balanceAfter: String,
    )

    val recentTxs = listOf(
        Tx("💼", "راتب شهر نوفمبر", "دخل", "+٣٨٬٥٠٠", 1, "اليوم 09:41", "١٢٣٬٤٥٦"),
        Tx("🍔", "مشاوير ومطاعم", "مصروف", "−١٬٢٥٠", -1, "اليوم 12:20", "١٢٢٬٢٠٦"),
        Tx("💻", "عمل حر — تصميم", "دخل", "+٢٬٠٠٠", 1, "أمس 18:02", "١٢٠٬٤٥٦"),
        Tx("🧾", "فاتورة كهرباء", "مصروف", "−٩٥٠", -1, "أمس 10:15", "١١٨٬٤٥٦"),
        Tx("🎯", "إضافة لهدف السيارة", "تحويل", "−٣٬٠٠٠", 0, "أمس 09:00", "١١٥٬٤٥٦"),
    )

    // ===== الأهداف =====
    data class Goal(
        val icon: String,
        val name: String,
        val saved: String,
        val target: String,
        val pct: Float,
        val daysLeft: Int,
    )

    val goals = listOf(
        Goal("🚗", "سيارة العائلة", "٢٬٤٠٠٬٠٠٠", "٣٬٠٠٠٬٠٠٠", 0.80f, 165),
        Goal("🏠", "بيت المستقبل", "٦٠٠٬٠٠٠", "٢٬٥٠٠٬٠٠٠", 0.24f, 730),
        Goal("✈️", "رحلة عمرة", "١٨٠٬٠٠٠", "٢٥٠٬٠٠٠", 0.72f, 45),
    )

    // ===== العملاء =====
    data class ClientRowModel(
        val name: String,
        val sub: String,
        val due: String,
        val status: Int,       // 0 نشط، 1 موقوف، 2 محظور
    )

    val clients = listOf(
        ClientRowModel("محمد ماطور الحي", "٣ حسابات · آخر حركة أمس", "عليه ٤٥٬٠٠٠", 0),
        ClientRowModel("أحمد صالح النجار", "١ حساب · آخر حركة قبل ٣ أيام", "له ١٢٬٥٠٠", 0),
        ClientRowModel("مؤسسة الأمانة", "٢ حسابات · فواتير شهريًا", "عليه ٢٣٠٬٠٠٠", 1),
        ClientRowModel("سعيد قاسم", "١ حساب · متأخر عن السداد", "عليه ٨٬٥٠٠", 2),
    )

    // ===== عمليات كشف حساب =====
    data class OpRowModel(
        val icon: String,
        val title: String,
        val amount: String,
        val sign: Int,
        val date: String,
        val runningBalance: String,   // بعدها
        val badge: String?,           // فاتورة 🧾 / مستحق
        val invoiceUndelivered: Boolean = false,
    )

    val accountOps = listOf(
        OpRowModel("🛢️", "بيع دبب بترول", "+٢٣٬٠٠٠", 1, "١٤ نوفمبر 09:12", "٤٥٬٠٠٠", "🧾"),
        OpRowModel("⚡", "دفعة صيانة ماطور", "−١٢٬٥٠٠", -1, "١٢ نوفمبر 14:30", "٢٢٬٠٠٠", null),
        OpRowModel("🧾", "فاتورة مواد كهربائية", "+٨٬٤٠٠", 1, "١٠ نوفمبر 11:05", "٣٤٬٥٠٠", "🧾 مستحقة"),
        OpRowModel("💧", "تركيب طابلوت", "+٥٬٢٠٠", 1, "٨ نوفمبر 16:40", "٢٦٬١٠٠", null),
    )

    // ===== سجل التدقيق =====
    val auditLog = listOf(
        "تعديل رصيد بنكي افتتاحي: ١٢٬٠٠٠ ← ١٥٬٠٠٠" to "اليوم 14:22",
        "تسوية رصيد «ماطور الحي»: ٤٥٬٠٠٠ ← ٤٥٬٢٠٠ (سبب: فرق قيد)" to "أمس 10:01",
        "شحن رصيد حقيقي +٥٠٬٠٠٠ إلى «مؤسسة الأمانة»" to "١٢ نوفمبر",
    )

    // ===== الفئات =====
    val expenseCats = listOf("🍔 مطاعم" to "١٬٢٥٠", "🚗 مواصلات" to "٨٥٠", "🛒 تسوق" to "٢٬٤٠٠", "💡 فواتير" to "٩٥٠", "💊 صحة" to "٣٢٠", "🎬 ترفيه" to "١٥٠")
    val incomeCats = listOf("💼 راتب" to "٣٨٬٥٠٠", "💻 عمل حر" to "٦٬٢٠٠", "🎁 هدية" to "٥٠٠")

    // ===== أرصدة الأهداف/الادخار للاقتراح الذكي =====
    val smartSources = listOf(
        Triple("🐷 الادخار", "٦٨٬٤٠٠ متاح", 0.62f),
        Triple("🎯 هدف السيارة", "٢٫٤ مليون متاح", 0.31f),
        Triple("🏠 هدف البيت", "٦٠٠ ألف متاح", 0.07f),
    )
}
