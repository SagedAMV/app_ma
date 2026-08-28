package com.mahfazty.smart.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mahfazty.smart.data.ClientsRepository
import com.mahfazty.smart.data.SettingsRepository
import com.mahfazty.smart.data.WalletRepository
import com.mahfazty.smart.domain.Dates
import com.mahfazty.smart.domain.WalletEngine
import com.mahfazty.smart.domain.categoryName
import com.mahfazty.smart.domain.model.AppSettings
import com.mahfazty.smart.domain.model.DayBar
import com.mahfazty.smart.domain.model.Goal
import com.mahfazty.smart.domain.model.GoalWithSaved
import com.mahfazty.smart.domain.model.Transaction
import com.mahfazty.smart.domain.model.TxType
import com.mahfazty.smart.domain.model.Wallet
import com.mahfazty.smart.domain.model.WalletError
import com.mahfazty.smart.ui.flow.PendingWalletAction
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

// ============ أدوات مشتركة ============

/** رسالة عربية واضحة لكل خطأ محفظة */
fun WalletError.message(): String = when (this) {
    WalletError.InvalidAmount -> "أدخل مبلغاً أكبر من صفر"
    is WalletError.BoxShortage -> "رصيد ${if (wallet == Wallet.BANK) "البنك" else "الكاش"} لا يكفي"
    is WalletError.TotalShortage -> "رصيدك الإجمالي لا يكفي"
    is WalletError.OverWithdraw -> "المبلغ أكبر من المدخر"
    WalletError.EmptySavings -> "لا يوجد مدخرات للسحب"
    is WalletError.InsufficientReal -> "الرصيد الحقيقي غير كافٍ"
    WalletError.ProtectedCategory -> "هذه العملية تخص فئة نظام (أهداف/ادخار/عملاء) — تُدار من شاشتها المخصصة فقط"
    WalletError.UnsafeDelete -> "حذف هذه العملية يجعل رصيداً أو مدخراً سالباً — احذف العمليات الأحدث أولاً"
}

/** اقتراح سحب سريع (من هدف أو ادخار) عند نقص الرصيد */
data class Suggestion(
    val kind: String,          // "goal" أو "savings"
    val title: String,
    val available: Double,
    val amount: Double,
    val id: Long? = null,
    val name: String = "",
)

/** بيانات نافذة "الرصيد غير الكافي" */
data class InsufficientData(
    val operation: String,
    val current: Double,
    val needed: Double,
    val walletLabel: String?,
    val suggestions: List<Suggestion>,
)

/** قاعدة مشتركة: تحويل خطأ إلى حالة نافذة نقص الرصيد مع اقتراحات */
fun WalletError.toInsufficient(
    operation: String,
    goals: List<GoalWithSaved>,
    savingsTotal: Double,
): InsufficientData {
    val current = when (this) {
        is WalletError.BoxShortage -> have
        is WalletError.TotalShortage -> have
        else -> 0.0
    }
    val needed = when (this) {
        is WalletError.BoxShortage -> need
        is WalletError.TotalShortage -> need
        else -> 0.0
    }
    val shortage = (needed - current).coerceAtLeast(0.0)
    val walletLabel = (this as? WalletError.BoxShortage)?.wallet?.let { w ->
        if (w == Wallet.BANK) "البنك" else "الكاش"
    }
    val suggestions = buildList {
        goals.filter { it.saved > 0 }.forEach { g ->
            add(
                Suggestion(
                    kind = "goal", id = g.goal.id, title = "${g.goal.icon} ${g.goal.name}",
                    name = g.goal.name, available = g.saved, amount = minOf(g.saved, shortage),
                ),
            )
        }
        if (savingsTotal > 0) {
            add(
                Suggestion(
                    kind = "savings", title = "🐷 حصالة الادخار",
                    available = savingsTotal, amount = minOf(savingsTotal, shortage),
                ),
            )
        }
    }
    return InsufficientData(operation, current, needed, walletLabel, suggestions)
}

// ============ ViewModel الرئيسي (مشترك لكل الشاشات) ============

class MainViewModel(
    private val walletRepo: WalletRepository,
    private val settingsRepo: SettingsRepository,
) : ViewModel() {

    val settings: StateFlow<AppSettings> = settingsRepo.settings
        .stateIn(viewModelScope, SharingStarted.Eagerly, AppSettings())

    val bank: StateFlow<Double> = walletRepo.bankBalance
        .stateIn(viewModelScope, SharingStarted.Eagerly, 0.0)

    val cash: StateFlow<Double> = walletRepo.cashBalance
        .stateIn(viewModelScope, SharingStarted.Eagerly, 0.0)

    val total: StateFlow<Double> = walletRepo.totalBalance
        .stateIn(viewModelScope, SharingStarted.Eagerly, 0.0)

    val savingsTotal: StateFlow<Double> = walletRepo.savingsTotal
        .stateIn(viewModelScope, SharingStarted.Eagerly, 0.0)

    fun updateSetting(key: String, value: String) = viewModelScope.launch { settingsRepo.set(key, value) }
    fun setBudget(catId: String, value: Double) = viewModelScope.launch { settingsRepo.setBudget(catId, value) }
    fun setOpeningBank(value: Double) = viewModelScope.launch { settingsRepo.setOpeningBank(value) }
    fun setOpeningCash(value: Double) = viewModelScope.launch { settingsRepo.setOpeningCash(value) }
}

data class HomeUiState(
    val name: String = "أحمد",
    val bankName: String = "البنك",
    val cashName: String = "الكاش",
    val currency: String = "ر.ي",
    val hideBalance: Boolean = false,
    val hideSavings: Boolean = false,
    val bank: Double = 0.0,
    val cash: Double = 0.0,
    val total: Double = 0.0,
    val displayedTotal: Double = 0.0,
    val wealth: Double = 0.0,
    val monthIncome: Double = 0.0,
    val monthExpense: Double = 0.0,
    val monthSave: Double = 0.0,
    val week: List<DayBar> = emptyList(),
    val goals: List<GoalWithSaved> = emptyList(),
    val recent: List<Transaction> = emptyList(),
    val warnings: List<String> = emptyList(),
    val dateLabel: String = "",
)

class HomeViewModel(
    private val walletRepo: WalletRepository,
    private val settingsRepo: SettingsRepository,
) : ViewModel() {

    private data class Base(
        val s: AppSettings, val bank: Double, val cash: Double,
        val savingsTotal: Double, val txs: List<Transaction>,
    )

    private val base = combine(
        settingsRepo.settings, walletRepo.bankBalance, walletRepo.cashBalance,
        walletRepo.savingsTotal, walletRepo.transactions,
    ) { s, b, c, sv, txs -> Base(s, b, c, sv, txs) }

    val state: StateFlow<HomeUiState> = combine(base, walletRepo.goalsWithSaved) { (s, bank, cash, savingsTotal, txs), goals ->
        val now = System.currentTimeMillis()
        val total = bank + cash
        val wealth = total + savingsTotal + goals.sumOf { it.saved }
        HomeUiState(
            name = s.name,
            bankName = s.bankName,
            cashName = s.cashName,
            currency = s.currency,
            hideBalance = s.hideBalance,
            hideSavings = s.hideSavings,
            bank = bank,
            cash = cash,
            total = total,
            displayedTotal = if (s.hideSavings) total - savingsTotal else total,
            wealth = wealth,
            monthIncome = WalletEngine.monthIncome(txs, now),
            monthExpense = WalletEngine.monthExpense(txs, now),
            monthSave = WalletEngine.monthSave(txs, now),
            week = WalletEngine.weekBars(txs, now),
            goals = goals,
            recent = txs.sortedByDescending { it.date }.take(5),
            warnings = WalletEngine.budgetWarnings(txs, s.budgets, { categoryName(it, s) }, now),
            dateLabel = Dates.day(now),
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), HomeUiState())

    private val _insufficient = MutableStateFlow<InsufficientData?>(null)
    val insufficient: StateFlow<InsufficientData?> = _insufficient.asStateFlow()

    private val _toast = MutableSharedFlow<String>(extraBufferCapacity = 8)
    val toast: kotlinx.coroutines.flow.SharedFlow<String> = _toast.asSharedFlow()

    /** لحظة احتفال — timestamp جديد يطلق قصاصات الاحتفال عند كل دخل جديد */
    private val _celebrate = MutableStateFlow(0L)
    val celebrate: StateFlow<Long> = _celebrate.asStateFlow()

    fun celebrateDone() { _celebrate.value = 0L }

    private val _pending = MutableStateFlow<PendingWalletAction?>(null)
    val pending: StateFlow<PendingWalletAction?> = _pending.asStateFlow()
    fun clearPending() { _pending.value = null }

    fun dismissInsufficient() { _insufficient.value = null }

    fun addTransaction(type: TxType, amount: Double, category: String, note: String?, wallet: Wallet) =
        viewModelScope.launch {
            walletRepo.addTransaction(type, amount, category, note, wallet)?.let { err ->
                if (err is WalletError.BoxShortage || err is WalletError.TotalShortage) {
                    _pending.value = PendingWalletAction.Tx(type, amount, category, note, wallet)
                    _insufficient.value = err.toInsufficient(
                        operation = "صرف ${categoryName(category)} من ${if (wallet == Wallet.BANK) "البنك" else "الكاش"}",
                        goals = currentGoals(), savingsTotal = currentSavings(),
                    )
                } else _toast.emit(err.message())
            } ?: run {
                _pending.value = null
                if (type == TxType.INCOME) _celebrate.value = System.currentTimeMillis()
                _toast.emit("تم حفظ العملية ✅")
            }
        }

    fun transfer(direction: String, amount: Double, note: String?) = viewModelScope.launch {
        walletRepo.transfer(direction, amount, note)?.let { err ->
            if (err is WalletError.BoxShortage) {
                _pending.value = PendingWalletAction.Transfer(direction, amount, note)
                _insufficient.value = err.toInsufficient(
                    operation = if (direction == "bank_to_cash") "سحب من البنك للكاش" else "إيداع من الكاش للبنك",
                    goals = currentGoals(), savingsTotal = currentSavings(),
                )
            } else _toast.emit(err.message())
        } ?: run {
            _pending.value = null
            _toast.emit("تم التحويل ✅")
        }
    }

    fun quickWithdrawGoal(goalId: Long, goalName: String, amount: Double) = viewModelScope.launch {
        _insufficient.value = null
        walletRepo.contributeGoal(goalId, goalName, false, amount)?.let { _toast.emit(it.message()) }
            ?: _toast.emit("تم السحب من الهدف — يمكنك الآن تنفيذ العملية ✅")
    }

    fun quickWithdrawSavings(amount: Double) = viewModelScope.launch {
        _insufficient.value = null
        walletRepo.withdrawSavings(amount)?.let { _toast.emit(it.message()) }
            ?: _toast.emit("تم السحب من الادخار — يمكنك الآن تنفيذ العملية ✅")
    }

    /** تحديث رصيد البنك الافتتاحي (من كارت الرصيد) */
    fun setOpeningBank(value: Double) = viewModelScope.launch {
        settingsRepo.setOpeningBank(value)
        _toast.emit("تم حفظ رصيد البنك ✅")
    }

    private suspend fun currentGoals(): List<GoalWithSaved> {
        val goals = walletRepo.goals
        val txs = walletRepo.transactions
        // قراءة لمرة واحدة عبر first()
        val gs = goals.first()
        val t = txs.first()
        return gs.map { GoalWithSaved(it, WalletEngine.goalSaved(it.opening, it.id, t)) }
    }

    private suspend fun currentSavings(): Double = walletRepo.savingsTotal.first()
}

// ============ سجل العمليات ============

sealed interface TxFilter {
    data object All : TxFilter
    data object Expenses : TxFilter
    data object Incomes : TxFilter
    data object Goals : TxFilter
    data object Savings : TxFilter
    data class Category(val id: String) : TxFilter
}

data class TxGroup(val label: String, val items: List<Pair<Transaction, Double>>)

data class TxUiState(
    val query: String = "",
    val filter: TxFilter = TxFilter.All,
    val groups: List<TxGroup> = emptyList(),
    val allCount: Int = 0,
    val todayExpense: Double = 0.0,
    val weekExpense: Double = 0.0,
    val allExpense: Double = 0.0,
    val currency: String = "ر.ي",
    val hideBalance: Boolean = false,
    val bank: Double = 0.0,
    val cash: Double = 0.0,
)

class TransactionsViewModel(
    private val walletRepo: WalletRepository,
    private val settingsRepo: SettingsRepository,
) : ViewModel() {

    private val query = MutableStateFlow("")
    private val filter = MutableStateFlow<TxFilter>(TxFilter.All)

    private val _insufficient = MutableStateFlow<InsufficientData?>(null)
    val insufficient: StateFlow<InsufficientData?> = _insufficient.asStateFlow()

    private val _toast = MutableSharedFlow<String>(extraBufferCapacity = 8)
    val toast: kotlinx.coroutines.flow.SharedFlow<String> = _toast.asSharedFlow()

    private data class Base(
        val txs: List<Transaction>,
        val settings: AppSettings,
        val openingBank: Double,
        val openingCash: Double,
    )

    private val base = combine(
        walletRepo.transactions, settingsRepo.settings,
        settingsRepo.openingBank, settingsRepo.openingCash,
    ) { txs, settings, ob, oc -> Base(txs, settings, ob, oc) }

    val state: StateFlow<TxUiState> = combine(base, query, filter) { b, q, f ->
        buildState(b.txs, b.settings, b.openingBank, b.openingCash, q, f)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), TxUiState())

    private fun buildState(
        all: List<Transaction>, settings: AppSettings,
        openingBank: Double, openingCash: Double, q: String, f: TxFilter,
    ): TxUiState {
        val now = System.currentTimeMillis()
        val filtered = all.filter { tx ->
            val matchesFilter = when (f) {
                TxFilter.All -> true
                TxFilter.Expenses -> tx.type == TxType.EXPENSE
                TxFilter.Incomes -> tx.type == TxType.INCOME
                TxFilter.Goals -> tx.category == "goal_add" || tx.category == "goal_withdraw"
                TxFilter.Savings -> tx.category == "savings_add" || tx.category == "savings_withdraw"
                is TxFilter.Category -> tx.category == f.id
            }
            val matchesQuery = q.isBlank() ||
                (tx.note?.contains(q, ignoreCase = true) == true) ||
                categoryName(tx.category, settings).contains(q, ignoreCase = true)
            matchesFilter && matchesQuery
        }.sortedByDescending { it.date }

        val running = WalletEngine.runningBalanceMap(
            all.sortedBy { it.date }, openingBank, openingCash,
        )

        val dayMs = 86_400_000L
        val weekAgo = now - 7 * dayMs
        val todayStart = Calendar.getInstance().apply {
            timeInMillis = now
            set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
        }.timeInMillis

        fun realExpenseSum(list: List<Transaction>): Double = list.filter {
            it.type == TxType.EXPENSE && it.category !in setOf("goal_add", "savings_add")
        }.sumOf { it.amount }

        val groups = filtered.groupBy { Dates.groupKey(it.date, now) }.toSortedMap()
            .map { (_, items) ->
                TxGroup(
                    label = Dates.groupLabel(items.first().date, now),
                    items = items.map { it to (running[it.id] ?: 0.0) },
                )
            }

        return TxUiState(
            query = q, filter = f, groups = groups,
            allCount = filtered.size,
            todayExpense = realExpenseSum(filtered.filter { it.date >= todayStart }),
            weekExpense = realExpenseSum(filtered.filter { it.date >= weekAgo }),
            allExpense = realExpenseSum(filtered),
            currency = settings.currency,
            hideBalance = settings.hideBalance,
            bank = WalletEngine.bankBalance(openingBank, all),
            cash = WalletEngine.cashBalance(openingCash, all),
        )
    }

    fun setQuery(q: String) { query.value = q }
    fun setFilter(f: TxFilter) { filter.value = f }
    fun dismissInsufficient() { _insufficient.value = null }

    private val _pending = MutableStateFlow<PendingWalletAction?>(null)
    val pending: StateFlow<PendingWalletAction?> = _pending.asStateFlow()
    fun clearPending() { _pending.value = null }

    fun addTransaction(type: TxType, amount: Double, category: String, note: String?, wallet: Wallet) =
        viewModelScope.launch {
            walletRepo.addTransaction(type, amount, category, note, wallet)?.let { err ->
                if (err is WalletError.BoxShortage || err is WalletError.TotalShortage) {
                    _pending.value = PendingWalletAction.Tx(type, amount, category, note, wallet)
                    _insufficient.value = buildInsufficient(err, "صرف ${categoryName(category)}")
                } else _toast.emit(err.message())
            } ?: run {
                _pending.value = null
                _toast.emit("تم الحفظ ✅")
            }
        }

    fun updateTransaction(tx: Transaction) = viewModelScope.launch {
        walletRepo.updateTransaction(tx)?.let { err ->
            if (err is WalletError.BoxShortage || err is WalletError.TotalShortage) {
                _pending.value = PendingWalletAction.Tx(tx.type, tx.amount, tx.category, tx.note, tx.wallet, editing = tx)
                _insufficient.value = buildInsufficient(err, "تعديل ${categoryName(tx.category)}")
            } else _toast.emit(err.message())
        } ?: run {
            _pending.value = null
            _toast.emit("تم التعديل ✅")
        }
    }

    fun quickWithdrawGoal(goalId: Long, goalName: String, amount: Double) = viewModelScope.launch {
        _insufficient.value = null
        walletRepo.contributeGoal(goalId, goalName, false, amount)?.let { _toast.emit(it.message()) }
            ?: _toast.emit("تم السحب من الهدف — يمكنك الآن تنفيذ العملية ✅")
    }

    fun quickWithdrawSavings(amount: Double) = viewModelScope.launch {
        _insufficient.value = null
        walletRepo.withdrawSavings(amount)?.let { _toast.emit(it.message()) }
            ?: _toast.emit("تم السحب من الادخار — يمكنك الآن تنفيذ العملية ✅")
    }

    private suspend fun buildInsufficient(err: WalletError, operation: String): InsufficientData {
        val gs = walletRepo.goals.first()
        val t = walletRepo.transactions.first()
        val goals = gs.map { GoalWithSaved(it, WalletEngine.goalSaved(it.opening, it.id, t)) }
        val savingsTotal = walletRepo.savingsTotal.first()
        return err.toInsufficient(operation, goals, savingsTotal)
    }

    fun deleteTransaction(tx: Transaction) = viewModelScope.launch {
        // إصلاح A3: الحذف قد يُرفض إذا كسر ثابت «لا رصيد/مدخر سالب»
        walletRepo.deleteTransaction(tx)?.let { err ->
            _toast.emit(err.message())
        } ?: _toast.emit("تم حذف العملية")
    }

    fun duplicateTransaction(tx: Transaction) = viewModelScope.launch {
        walletRepo.addTransaction(tx.type, tx.amount, tx.category, tx.note, tx.wallet, tx.goalId)?.let { err ->
            _toast.emit(err.message())
        } ?: _toast.emit("تم تكرار العملية ✅")
    }
}

// ============ الأهداف ============

data class GoalsUiState(
    val goals: List<GoalWithSaved> = emptyList(),
    val currency: String = "ر.ي",
    val hideBalance: Boolean = false,
)

class GoalsViewModel(
    private val walletRepo: WalletRepository,
    private val settingsRepo: SettingsRepository,
) : ViewModel() {

    val state: StateFlow<GoalsUiState> = combine(walletRepo.goalsWithSaved, settingsRepo.settings) { goals, s ->
        GoalsUiState(goals, s.currency, s.hideBalance)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), GoalsUiState())

    private val _insufficient = MutableStateFlow<InsufficientData?>(null)
    val insufficient: StateFlow<InsufficientData?> = _insufficient.asStateFlow()

    private val _toast = MutableSharedFlow<String>(extraBufferCapacity = 8)
    val toast: kotlinx.coroutines.flow.SharedFlow<String> = _toast.asSharedFlow()

    fun dismissInsufficient() { _insufficient.value = null }

    private val _pending = MutableStateFlow<PendingWalletAction?>(null)
    val pending: StateFlow<PendingWalletAction?> = _pending.asStateFlow()
    fun clearPending() { _pending.value = null }

    fun addGoal(name: String, target: Double, opening: Double, icon: String) = viewModelScope.launch {
        walletRepo.addGoal(name, target, opening, icon)
        _toast.emit("تم إضافة الهدف 🚀")
    }

    fun contribute(goal: Goal, add: Boolean, amount: Double) = viewModelScope.launch {
        walletRepo.contributeGoal(goal.id, goal.name, add, amount)?.let { err ->
            if (err is WalletError.BoxShortage || err is WalletError.TotalShortage) {
                _pending.value = PendingWalletAction.Contribute(goal.id, goal.name, add, amount)
                _insufficient.value = err.toInsufficient(
                    operation = "إضافة لهدف ${goal.name}",
                    goals = currentGoals().filterNot { it.goal.id == goal.id },
                    savingsTotal = currentSavings(),
                )
            } else _toast.emit(err.message())
        } ?: run {
            _pending.value = null
            _toast.emit(if (add) "تمت الإضافة للهدف ✅" else "تم السحب من الهدف ✅")
        }
    }

    fun deleteGoal(goal: Goal) = viewModelScope.launch {
        walletRepo.deleteGoal(goal)
        _toast.emit("تم حذف الهدف")
    }

    fun quickWithdrawGoal(goalId: Long, goalName: String, amount: Double) = viewModelScope.launch {
        _insufficient.value = null
        walletRepo.contributeGoal(goalId, goalName, false, amount)?.let { _toast.emit(it.message()) }
            ?: _toast.emit("تم السحب من الهدف ✅")
    }

    fun quickWithdrawSavings(amount: Double) = viewModelScope.launch {
        _insufficient.value = null
        walletRepo.withdrawSavings(amount)?.let { _toast.emit(it.message()) }
            ?: _toast.emit("تم السحب من الادخار ✅")
    }

    private suspend fun currentGoals(): List<GoalWithSaved> {
        val gs = walletRepo.goals.first()
        val t = walletRepo.transactions.first()
        return gs.map { GoalWithSaved(it, WalletEngine.goalSaved(it.opening, it.id, t)) }
    }

    private suspend fun currentSavings(): Double = walletRepo.savingsTotal.first()
}

// ============ الادخار ============

data class SavingsUiState(
    val total: Double = 0.0,
    val goal: Double = 50_000.0,
    val percent: Float = 0f,
    val currency: String = "ر.ي",
    val hideBalance: Boolean = false,
    val history: List<Double> = emptyList(),
    val historyLabels: List<String> = emptyList(),
)

class SavingsViewModel(
    private val walletRepo: WalletRepository,
    private val settingsRepo: SettingsRepository,
) : ViewModel() {

    val state: StateFlow<SavingsUiState> = combine(
        walletRepo.savingsTotal, walletRepo.savings, walletRepo.transactions, settingsRepo.settings,
    ) { total, savings, txs, s ->
        val goal = if (s.savingsGoal > 0) s.savingsGoal else 50_000.0
        SavingsUiState(
            total = total,
            goal = goal,
            percent = if (goal > 0) (total / goal).toFloat().coerceIn(0f, 1f) else 0f,
            currency = s.currency,
            hideBalance = s.hideBalance,
            history = WalletEngine.savingsHistory(savings.opening, txs, System.currentTimeMillis()),
            historyLabels = lastMonthsLabels(),
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), SavingsUiState())

    private val _toast = MutableSharedFlow<String>(extraBufferCapacity = 8)
    val toast: kotlinx.coroutines.flow.SharedFlow<String> = _toast.asSharedFlow()

    private val _insufficient = MutableStateFlow<InsufficientData?>(null)
    val insufficient: StateFlow<InsufficientData?> = _insufficient.asStateFlow()
    fun dismissInsufficient() { _insufficient.value = null }

    private val _pending = MutableStateFlow<PendingWalletAction?>(null)
    val pending: StateFlow<PendingWalletAction?> = _pending.asStateFlow()
    fun clearPending() { _pending.value = null }

    fun addToSavings(amount: Double) = viewModelScope.launch {
        walletRepo.addToSavings(amount)?.let { err ->
            if (err is WalletError.BoxShortage || err is WalletError.TotalShortage) {
                _pending.value = PendingWalletAction.SavingsAdd(amount)
                val gs = walletRepo.goals.first()
                val t = walletRepo.transactions.first()
                val goals = gs.map { GoalWithSaved(it, WalletEngine.goalSaved(it.opening, it.id, t)) }
                _insufficient.value = err.toInsufficient(
                    operation = "إضافة للادخار من البنك",
                    goals = goals,
                    savingsTotal = 0.0,
                )
            } else _toast.emit(err.message())
        } ?: run {
            _pending.value = null
            _toast.emit("تمت الإضافة للادخار 🐷")
        }
    }

    fun quickWithdrawGoal(goalId: Long, goalName: String, amount: Double) = viewModelScope.launch {
        _insufficient.value = null
        walletRepo.contributeGoal(goalId, goalName, false, amount)?.let { _toast.emit(it.message()) }
            ?: _toast.emit("تم السحب من الهدف — يمكنك الآن تنفيذ العملية ✅")
    }

    fun withdrawSavings(amount: Double) = viewModelScope.launch {
        walletRepo.withdrawSavings(amount)?.let { _toast.emit(it.message()) }
            ?: _toast.emit("تم السحب من الادخار ✅")
    }

    fun setSavingsGoal(value: Double) = viewModelScope.launch { walletRepo.setSavingsGoal(value) }

    private fun lastMonthsLabels(): List<String> {
        val fmt = SimpleDateFormat("MMM", Locale("ar"))
        val cal = Calendar.getInstance()
        return (5 downTo 0).map { i ->
            val c = Calendar.getInstance().apply { timeInMillis = cal.timeInMillis; add(Calendar.MONTH, -i) }
            fmt.format(c.time)
        }
    }
}
