package com.mahfazty.smart.domain.model

/**
 * النماذج الأساسية للتطبيق — "محفظتي الذكية".
 * طبقة نقية (لا تعتمد على أندرويد إطلاقاً) لتكون قابلة لاختبار الوحدة.
 * ملاحظة: كل النماذج data class بخواص val فقط → ثبات كامل (المهارة 2).
 */

/** نوع العملية المالية */
enum class TxType { INCOME, EXPENSE, TRANSFER }

/** الصندوق: بنك أو كاش */
enum class Wallet { BANK, CASH }

/** نوع عملية العميل: دين (عليه) أو سداد (له) */
enum class OpType { DEBT, PAY }

/** وضع المظهر */
enum class ThemeMode { LIGHT, DARK, SYSTEM }

/** نوع الفئة */
enum class CategoryKind { EXPENSE, INCOME, TRANSFER }

/** المعرفات الثابتة للفئات الخاصة (أهداف، ادخار، تحويلات، عملاء) */
object CategoryIds {
    const val GOAL_ADD = "goal_add"
    const val GOAL_WITHDRAW = "goal_withdraw"
    const val SAVINGS_ADD = "savings_add"
    const val SAVINGS_WITHDRAW = "savings_withdraw"
    const val BANK_TO_CASH = "bank_to_cash"
    const val CASH_TO_BANK = "cash_to_bank"
    const val CLIENT_FUND = "client_fund"
    const val CLIENT_WITHDRAW = "client_withdraw"

    /** الفئات التي تخصم من الرصيد الحقيقي عند حساب الدخل/المصروف الفعلي */
    val nonRealExpense = setOf(GOAL_ADD, SAVINGS_ADD)
    val nonRealIncome = setOf(GOAL_WITHDRAW, SAVINGS_WITHDRAW)

    /**
     * فئات النظام: لا يجوز إنشاؤها/تعديلها من المسار العام (نافذة إضافة/تعديل/تكرار عملية)
     * لأن لكل واحدة مساراً مخصصاً يفحص السعة (الأهداف/الادخار/التحويل/شحن العملاء).
     * إصلاح A2: منع «خلق المال من العدم» عبر تكرار أو تعديل عمليات هذه الفئات.
     */
    val protectedCategories = setOf(
        GOAL_ADD, GOAL_WITHDRAW, SAVINGS_ADD, SAVINGS_WITHDRAW,
        BANK_TO_CASH, CASH_TO_BANK, CLIENT_FUND, CLIENT_WITHDRAW,
    )
}

/** فئة (تصنيف) للعمليات */
data class Category(
    val id: String,
    val icon: String,
    val name: String,
    val kind: CategoryKind,
)

/** عملية مالية على المحفظة (بنك/كاش) */
data class Transaction(
    val id: Long,
    val type: TxType,
    val amount: Double,
    val category: String,
    val note: String? = null,
    val date: Long,
    val goalId: Long? = null,
    val wallet: Wallet = Wallet.CASH,
)

/** هدف ادخاري (حفظ المدخر مشتق من العمليات) */
data class Goal(
    val id: Long,
    val name: String,
    val target: Double,
    val icon: String = "💻",
    val opening: Double = 0.0,
)

/** حصالة الادخار — الإجمالي مشتق من العمليات (لا مال من العدم) */
data class SavingsAccount(
    val opening: Double = 0.0,
    val goal: Double = 50_000.0,
)

/** إعدادات التطبيق الكاملة */
data class AppSettings(
    val name: String = "أحمد",
    val bankName: String = "البنك",
    val cashName: String = "الكاش",
    val currency: String = "ر.ي",
    val theme: ThemeMode = ThemeMode.LIGHT,
    val primaryColor: String = "#6C5CE7",
    val primary2: String = "#A29BFE",
    val hideBalance: Boolean = false,
    val hideSavings: Boolean = false,
    val savingsGoal: Double = 50_000.0,
    val budgets: Map<String, Double> = defaultBudgets(),
    val customExpense: List<Category> = emptyList(),
    val customIncome: List<Category> = emptyList(),
) {
    companion object {
        fun defaultBudgets(): Map<String, Double> = linkedMapOf(
            "food" to 0.0, "transport" to 0.0, "shopping" to 0.0, "bills" to 0.0,
            "home" to 0.0, "health" to 0.0, "fun" to 0.0, "other" to 0.0,
            "salary" to 0.0, "freelance" to 0.0, "gift" to 0.0,
        )
    }
}

/** عميل (مدين/دائن) */
data class Client(
    val id: Long,
    val name: String,
    val phone: String? = null,
    val photoPath: String? = null,
)

/**
 * حساب داخل ملف العميل (مثل: ماطور، كهرباء...).
 * الرصيد الحقيقي يتغيّر من عمليات له/عليه على الحساب نفسه،
 * ومن الشحن/السحب/التحويل (بنك، كاش، حسابات أخرى).
 */
data class ClientAccount(
    val id: Long,
    val clientId: Long,
    val name: String,
    val icon: String = "💰",
    val realBalance: Double = 0.0,
)

/** صنف داخل فاتورة العملية */
data class MaterialItem(
    val name: String = "",
    val qty: Double = 0.0,
    val unitPrice: Double = 0.0,
) {
    val total: Double get() = qty * unitPrice
}

/** عملية على حساب عميل: دين أو سداد */
data class ClientOperation(
    val id: Long,
    val accountId: Long,
    val type: OpType,
    val amount: Double,
    val note: String? = null,
    val date: Long,
    val materials: List<MaterialItem> = emptyList(),
    val receiptPath: String? = null,
)

/** تحويل رصيد حقيقي بين حسابات العملاء */
data class RealTransfer(
    val id: Long,
    val fromClientId: Long,
    val fromAccountId: Long,
    val toClientId: Long,
    val toAccountId: Long,
    val amount: Double,
    val date: Long,
)

/** أخطاء عمليات المحفظة — كل خطأ يحمل بيانات كافية لعرض "الرصيد غير الكافي" */
sealed interface WalletError {
    data object InvalidAmount : WalletError
    data class BoxShortage(val wallet: Wallet, val have: Double, val need: Double) : WalletError
    data class TotalShortage(val have: Double, val need: Double) : WalletError
    data class OverWithdraw(val have: Double, val need: Double) : WalletError
    data object EmptySavings : WalletError
    data class InsufficientReal(val have: Double, val need: Double) : WalletError

    /** إصلاح A2/B1: العملية تخص فئة نظام تُدار من شاشاتها المخصصة فقط */
    data object ProtectedCategory : WalletError

    /** إصلاح A3: الحذف سيجعل رصيداً أو مدخراً سالباً — يُرفض */
    data object UnsafeDelete : WalletError
}

/** هدف مع مدخره المشتق */
data class GoalWithSaved(val goal: Goal, val saved: Double) {
    val progress: Float get() = if (goal.target <= 0) 0f else (saved / goal.target).toFloat().coerceIn(0f, 1f)
}

/** شريط في مخطط الأسبوع */
data class DayBar(val label: String, val value: Double)
