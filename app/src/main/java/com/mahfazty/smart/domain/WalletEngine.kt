package com.mahfazty.smart.domain

import com.mahfazty.smart.domain.model.CategoryIds
import com.mahfazty.smart.domain.model.ClientAccount
import com.mahfazty.smart.domain.model.ClientOperation
import com.mahfazty.smart.domain.model.DayBar
import com.mahfazty.smart.domain.model.OpType
import com.mahfazty.smart.domain.model.Transaction
import com.mahfazty.smart.domain.model.TxType
import com.mahfazty.smart.domain.model.Wallet
import com.mahfazty.smart.domain.model.WalletError
import java.util.Calendar
import java.util.Locale

/**
 * محرك القواعد المالية — دالة نقية لكل قاعدة (يسهل اختبارها).
 * القواعد (مطابقة لنسخة الويب 100%):
 *  1. لا يمكن أن يصبح أي صندوق (بنك/كاش) سالباً.
 *  2. الرصيد الكلي لا يمكن أن يصبح سالباً.
 *  3. الادخار والأهداف قيم مشتقة من العمليات — حذف العملية يعيد المال تلقائياً.
 *  4. الصرف اليومي من الكاش افتراضياً، والدخل للبنك افتراضياً.
 *  5. الرصيد الحقيقي لحساب العميل يأتي من عمليات الحساب نفسه:
 *     - له  (PAY)  يزيد الرصيد الحقيقي دائماً.
 *     - عليه (DEBT) تخصم من الرصيد الحقيقي فقط إذا كان الحقيقي ≥ المبلغ، وإلا تُرفض
 *       وتظهر مصادر الشحن: البنك، الكاش، أو حسابات أخرى فيها رصيد حقيقي.
 */
object WalletEngine {

    // ============ الأرصدة المشتقة ============

    fun bankBalance(opening: Double, txs: List<Transaction>): Double {
        var bal = opening
        txs.forEach { t ->
            when (t.category) {
                CategoryIds.BANK_TO_CASH -> bal -= t.amount
                CategoryIds.CASH_TO_BANK -> bal += t.amount
                else -> when {
                    t.type == TxType.INCOME && t.wallet == Wallet.BANK -> bal += t.amount
                    t.type == TxType.EXPENSE && t.wallet == Wallet.BANK -> bal -= t.amount
                }
            }
        }
        return bal
    }

    fun cashBalance(opening: Double, txs: List<Transaction>): Double {
        var bal = opening
        txs.forEach { t ->
            when (t.category) {
                CategoryIds.BANK_TO_CASH -> bal += t.amount
                CategoryIds.CASH_TO_BANK -> bal -= t.amount
                else -> when {
                    t.type == TxType.INCOME && t.wallet == Wallet.CASH -> bal += t.amount
                    t.type == TxType.EXPENSE && t.wallet == Wallet.CASH -> bal -= t.amount
                }
            }
        }
        return bal
    }

    fun balanceOf(wallet: Wallet, openingBank: Double, openingCash: Double, txs: List<Transaction>): Double =
        if (wallet == Wallet.BANK) bankBalance(openingBank, txs) else cashBalance(openingCash, txs)

    /** إجمالي الادخار = افتتاحي + (إضافات - سحوبات) — مشتق من العمليات */
    fun savingsTotal(opening: Double, txs: List<Transaction>): Double =
        opening + txs.sumOf { t ->
            when (t.category) {
                CategoryIds.SAVINGS_ADD -> t.amount
                CategoryIds.SAVINGS_WITHDRAW -> -t.amount
                else -> 0.0
            }
        }

    /** مدخر الهدف = افتتاحي + (إضافات - سحوبات) لذلك الهدف */
    fun goalSaved(opening: Double, goalId: Long, txs: List<Transaction>): Double =
        opening + txs.sumOf { t ->
            if (t.goalId != goalId) 0.0
            else when (t.category) {
                CategoryIds.GOAL_ADD -> t.amount
                CategoryIds.GOAL_WITHDRAW -> -t.amount
                else -> 0.0
            }
        }

    /** رصيد حساب العميل من العمليات (كشف عليه/له): موجب = عليه، سالب = له.
     * هذا مستقل عن الرصيد الحقيقي. */
    fun opsBalance(ops: List<ClientOperation>): Double =
        ops.sumOf { if (it.type == OpType.DEBT) it.amount else -it.amount }

    /**
     * الرصيد العادي «الجاري» بعد كل عملية — يُظهر كيف يتنقل الرصيد بين العمليات.
     * يُرتَّب زمنياً (الأقدم أولاً) ويُراكَم: «عليه» يزيد، «له» ينقص.
     * موجب = عليه، سالب = له. مستقل تماماً عن الرصيد الحقيقي (كشف حسابات).
     *
     * ملاحظة: لا نستخدم تاريخ العملية وحده كمفتاح لأن عمليتين قد تتشاركان نفس
     * الطابع الزمني — نستخدم id العملية (فريد) ونرتب بالتاريخ ثم الـ id للاستقرار.
     */
    fun runningOpsBalance(ops: List<ClientOperation>): Map<Long, Double> {
        val map = mutableMapOf<Long, Double>()
        var running = 0.0
        ops.sortedWith(compareBy({ it.date }, { it.id })).forEach { op ->
            running += if (op.type == OpType.DEBT) op.amount else -op.amount
            map[op.id] = running
        }
        return map
    }

    /**
     * عملية عليه تتطلب أن الرصيد الحقيقي ≥ المبلغ.
     * يُستخدم قبل الخصم؛ القيمة الفارغة = مسموح.
     */
    fun checkDebtAgainstReal(realBalance: Double, amount: Double): WalletError? =
        if (realBalance - amount < 0) WalletError.InsufficientReal(realBalance, amount) else null

    /** أثر العملية على الرصيد الحقيقي: له +المبلغ، عليه −المبلغ */
    fun applyOpToReal(realBalance: Double, type: OpType, amount: Double): Double =
        if (type == OpType.DEBT) realBalance - amount else realBalance + amount

    /** عكس أثر عملية عند الحذف أو قبل التعديل */
    fun reverseOpFromReal(realBalance: Double, type: OpType, amount: Double): Double =
        if (type == OpType.DEBT) realBalance + amount else realBalance - amount

    /**
     * إصلاح fin-1: فحص أن عكس أثر العملية على الرصيد الحقيقي لن يجعله سالباً.
     * القيمة الفارغة = آمن، وإلا WalletError.UnsafeRealDelete.
     */
    fun checkReverseOpKeepsNonNegative(realBalance: Double, type: OpType, amount: Double): WalletError? =
        if (reverseOpFromReal(realBalance, type, amount) < -0.000001) WalletError.UnsafeRealDelete else null

    fun clientTotal(accounts: List<ClientAccount>, opsByAccount: (Long) -> List<ClientOperation>): Double =
        accounts.sumOf { opsBalance(opsByAccount(it.id)) }

    // ============ فحص كفاية الرصيد ============

    /**
     * فحص صرف مبلغ من صندوق:
     * يفشل إذا أصبح الصندوق المختار سالباً أو أصبح الإجمالي سالباً.
     * يرجع null عند النجاح (النمط: القيمة الفارغة = لا خطأ — المهارة 1).
     */
    fun checkExpense(amount: Double, wallet: Wallet, bank: Double, cash: Double): WalletError? {
        val box = if (wallet == Wallet.BANK) bank else cash
        if (box - amount < 0) return WalletError.BoxShortage(wallet, box, amount)
        if (bank + cash - amount < 0) return WalletError.TotalShortage(bank + cash, amount)
        return null
    }

    /** فحص تحويل بين الصناديق: الصندوق المصدر يجب أن يكفي */
    fun checkTransfer(direction: String, amount: Double, bank: Double, cash: Double): WalletError? {
        val source = if (direction == CategoryIds.BANK_TO_CASH) bank else cash
        return if (source - amount < 0) {
            WalletError.BoxShortage(
                if (direction == CategoryIds.BANK_TO_CASH) Wallet.BANK else Wallet.CASH,
                source, amount,
            )
        } else null
    }

    /** فحص سحب من الادخار/هدف: لا يمكن سحب أكثر من المدخر */
    fun checkWithdraw(amount: Double, have: Double): WalletError? =
        if (amount > have) WalletError.OverWithdraw(have, amount) else null

    // ============ الإحصائيات ============

    fun monthBounds(now: Long): Pair<Long, Long> {
        val cal = Calendar.getInstance().apply { timeInMillis = now }
        val start = Calendar.getInstance().apply {
            timeInMillis = now
            set(Calendar.DAY_OF_MONTH, 1)
            set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
        }.timeInMillis
        val end = Calendar.getInstance().apply {
            timeInMillis = now
            set(Calendar.DAY_OF_MONTH, cal.getActualMaximum(Calendar.DAY_OF_MONTH))
            set(Calendar.HOUR_OF_DAY, 23); set(Calendar.MINUTE, 59)
            set(Calendar.SECOND, 59); set(Calendar.MILLISECOND, 999)
        }.timeInMillis
        return start to end
    }

    private fun inMonth(ts: Long, bounds: Pair<Long, Long>) = ts in bounds.first..bounds.second

    fun monthIncome(txs: List<Transaction>, now: Long): Double {
        val b = monthBounds(now)
        return txs.filter { it.type == TxType.INCOME && it.category !in CategoryIds.nonRealIncome && inMonth(it.date, b) }
            .sumOf { it.amount }
    }

    fun monthExpense(txs: List<Transaction>, now: Long): Double {
        val b = monthBounds(now)
        return txs.filter { it.type == TxType.EXPENSE && it.category !in CategoryIds.nonRealExpense && inMonth(it.date, b) }
            .sumOf { it.amount }
    }

    /** ادخار هذا الشهر = ما ذهب للأهداف والادخار */
    fun monthSave(txs: List<Transaction>, now: Long): Double {
        val b = monthBounds(now)
        return txs.filter {
            (it.category == CategoryIds.GOAL_ADD || it.category == CategoryIds.SAVINGS_ADD) && inMonth(it.date, b)
        }.sumOf { it.amount }
    }

    /** مصاريف آخر 7 أيام (بدون تحويلات الأهداف/الادخار) */
    fun weekBars(txs: List<Transaction>, now: Long): List<DayBar> {
        val result = mutableListOf<DayBar>()
        val dayMs = 86_400_000L
        val cal = Calendar.getInstance().apply { timeInMillis = now }
        for (i in 6 downTo 0) {
            val start = Calendar.getInstance().apply {
                timeInMillis = now
                add(Calendar.DAY_OF_YEAR, -i)
                set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
            }.timeInMillis
            val end = start + dayMs - 1
            val total = txs.filter {
                it.type == TxType.EXPENSE &&
                    it.category !in CategoryIds.nonRealExpense &&
                    it.date in start..end
            }.sumOf { it.amount }
            val label = if (i == 0) "اليوم" else formatDayLabel(start)
            result += DayBar(label, total)
        }
        return result
    }

    private fun formatDayLabel(ts: Long): String {
        val cal = Calendar.getInstance().apply { timeInMillis = ts }
        val days = arrayOf("أحد", "اثنين", "ثلاثاء", "أربعاء", "خميس", "جمعة", "سبت")
        val idx = cal.get(Calendar.DAY_OF_WEEK) - 1
        return days.getOrElse(idx) { "" }
    }

    /** صافي الادخار لكل شهر من آخر 6 أشهر — يبدأ من الرصيد الافتتاحي ويراكم شهرياً */
    fun savingsHistory(opening: Double, txs: List<Transaction>, now: Long): List<Double> {
        val months = mutableListOf<Pair<Long, Long>>()
        for (i in 5 downTo 0) {
            val cal = Calendar.getInstance().apply { timeInMillis = now }
            cal.add(Calendar.MONTH, -i)
            val start = Calendar.getInstance().apply {
                timeInMillis = cal.timeInMillis
                set(Calendar.DAY_OF_MONTH, 1)
                set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
            }.timeInMillis
            val end = Calendar.getInstance().apply {
                timeInMillis = cal.timeInMillis
                set(Calendar.DAY_OF_MONTH, getActualMaximum(Calendar.DAY_OF_MONTH))
                set(Calendar.HOUR_OF_DAY, 23); set(Calendar.MINUTE, 59)
                set(Calendar.SECOND, 59); set(Calendar.MILLISECOND, 999)
            }.timeInMillis
            months += start to end
        }
        val cumulative = mutableListOf<Double>()
        var running = opening
        months.forEach { (start, end) ->
            val net = txs.filter { it.date in start..end }.sumOf { t ->
                when (t.category) {
                    CategoryIds.SAVINGS_ADD, CategoryIds.GOAL_ADD -> t.amount
                    CategoryIds.SAVINGS_WITHDRAW, CategoryIds.GOAL_WITHDRAW -> -t.amount
                    else -> 0.0
                }
            }
            running += net
            cumulative += running.coerceAtLeast(0.0)
        }
        return cumulative
    }

    /** تحذيرات تجاوز الحدود الشهرية للفئات */
    fun budgetWarnings(
        txs: List<Transaction>,
        budgets: Map<String, Double>,
        categoryNames: (String) -> String,
        now: Long,
    ): List<String> {
        val b = monthBounds(now)
        return budgets.filterValues { it > 0 }.mapNotNull { (catId, limit) ->
            val spent = txs.filter {
                it.type == TxType.EXPENSE && it.category == catId && inMonth(it.date, b)
            }.sumOf { it.amount }
            val name = categoryNames(catId)
            when {
                spent >= limit * 2 -> "🚨 تجاوزت حد ${name} بشكل كبير: أنفقت ${Money.fmt(spent)} من حد ${Money.fmt(limit)}"
                spent >= limit -> "⚠️ وصلت حد ${name}: أنفقت ${Money.fmt(spent)} من حد ${Money.fmt(limit)}"
                spent >= limit * 0.75 -> "🔔 اقتربت من حد ${name}: أنفقت ${Money.fmt(spent)} من حد ${Money.fmt(limit)}"
                else -> null
            }
        }
    }

    /** الرصيد الجاري بعد كل عملية (مرتبة تصاعدياً) لعرضه في السجل */
    fun runningBalanceMap(
        sortedTxs: List<Transaction>,
        openingBank: Double,
        openingCash: Double,
    ): Map<Long, Double> {
        val map = mutableMapOf<Long, Double>()
        var bank = openingBank
        var cash = openingCash
        sortedTxs.forEach { t ->
            when {
                t.category == CategoryIds.BANK_TO_CASH -> { bank -= t.amount; cash += t.amount }
                t.category == CategoryIds.CASH_TO_BANK -> { bank += t.amount; cash -= t.amount }
                t.type == TxType.INCOME -> if (t.wallet == Wallet.BANK) bank += t.amount else cash += t.amount
                t.type == TxType.EXPENSE -> if (t.wallet == Wallet.BANK) bank -= t.amount else cash -= t.amount
            }
            map[t.id] = bank + cash
        }
        return map
    }
}

/** تنسيق مالي موحد (يتوافق مع Intl.NumberFormat('ar-YE') في نسخة الويب) */
object Money {
    private val arabicFmt = java.text.NumberFormat.getNumberInstance(Locale("ar"))
    private val latinFmt = java.text.NumberFormat.getNumberInstance(Locale.US)

    init {
        arabicFmt.maximumFractionDigits = 0
        latinFmt.maximumFractionDigits = 0
    }

    fun fmt(n: Double): String {
        if (n.isNaN() || n.isInfinite()) return "٠"
        return arabicFmt.format(kotlin.math.round(n))
    }

    /** أرقام لاتينية (للتصدير CSV وواتساب) */
    fun fmtLat(n: Double): String {
        if (n.isNaN() || n.isInfinite()) return "0"
        return latinFmt.format(kotlin.math.round(n))
    }

    /** نص حقل إدخال بلا فواصل تجميع — حتى يُقبل عند الحفظ */
    fun input(n: Double): String {
        if (n.isNaN() || n.isInfinite() || n == 0.0) return ""
        val r = kotlin.math.round(n)
        return if (kotlin.math.abs(n - r) < 0.0001) r.toLong().toString() else n.toString()
    }

    /** قراءة مبلغ من حقل: يتجاهل الفواصل ويحوّل الأرقام العربية */
    fun parse(s: String): Double {
        if (s.isBlank()) return 0.0
        val mapped = buildString {
            for (ch in s.trim()) {
                when (ch) {
                    in '0'..'9' -> append(ch)
                    in '٠'..'٩' -> append('0' + (ch - '٠'))
                    in '۰'..'۹' -> append('0' + (ch - '۰'))
                    '.', '٫' -> append('.')
                    else -> { /* فواصل تجميع ومسافات تُتجاهل */ }
                }
            }
        }
        return mapped.toDoubleOrNull() ?: 0.0
    }
}
