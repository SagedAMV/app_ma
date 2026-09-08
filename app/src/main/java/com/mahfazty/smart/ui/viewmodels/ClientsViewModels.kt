package com.mahfazty.smart.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mahfazty.smart.data.AccountWithOps
import com.mahfazty.smart.data.ClientWithData
import com.mahfazty.smart.data.ClientsRepository
import com.mahfazty.smart.data.TransferDisplay
import com.mahfazty.smart.domain.WalletEngine
import com.mahfazty.smart.domain.model.Client
import com.mahfazty.smart.domain.model.ClientAccount
import com.mahfazty.smart.domain.model.ClientOperation
import com.mahfazty.smart.domain.model.MaterialItem
import com.mahfazty.smart.domain.model.OpType
import com.mahfazty.smart.domain.model.Wallet
import com.mahfazty.smart.domain.model.WalletError
import com.mahfazty.smart.ui.flow.PendingClientOp
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.Calendar

// ============ أدوات مشتركة ============

/**
 * توست مع إجراء اختياري — أساس زر «تراجع» (إصلاح act-2).
 * actionLabel != null → Snackbar بإجراء وفترة أطول.
 */
data class ToastMsg(
    val text: String,
    val actionLabel: String? = null,
    val onAction: (() -> Unit)? = null,
)

// ============ قائمة العملاء ============

/** إضافة 5.3 من تقرير الفحص: فلتر حالة العميل */
enum class ClientStatusFilter { ALL, ACTIVE, STOPPED, BLOCKED }

/** إضافة 3.5 من تقرير الفحص: دين مستحق مجمّع عبر كل العملاء (للشاشة التجميعية) */
data class DueDebtSummary(
    val clientName: String,
    val accountName: String,
    val amount: Double,
    val dueDate: Long,
    /** true = تجاوز تاريخ الاستحقاق فعلاً (متأخر) */
    val overdue: Boolean,
)

data class ClientsUiState(
    val query: String = "",
    val clients: List<ClientWithData> = emptyList(),
    val totalOn: Double = 0.0,
    val totalFor: Double = 0.0,
    /** إضافة 5.3/5.4: الفلتر الحالي وعدادات كل حالة (من كل العملاء لا من المفلتر) */
    val statusFilter: ClientStatusFilter = ClientStatusFilter.ALL,
    val activeCount: Int = 0,
    val stoppedCount: Int = 0,
    val blockedCount: Int = 0,
    /** إضافة 3.5: الديون المستحقة/المتأخرة من كل العملاء (الأقرب استحقاقاً أولاً) */
    val dueDebts: List<DueDebtSummary> = emptyList(),
)

class ClientsViewModel(private val repo: ClientsRepository) : ViewModel() {

    private val query = MutableStateFlow("")
    private val statusFilter = MutableStateFlow(ClientStatusFilter.ALL)

    fun setStatusFilter(f: ClientStatusFilter) { statusFilter.value = f }

    val state: StateFlow<ClientsUiState> = combine(repo.clientsWithData, query, statusFilter) { list, q, f ->
        val now = System.currentTimeMillis()
        val dayMs = 86_400_000L
        val endOfToday = Calendar.getInstance().apply {
            timeInMillis = now
            add(Calendar.DAY_OF_YEAR, 1)
            set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
        }.timeInMillis

        // إضافة 3.5: تجميع الديون المستحقة/المتأخرة من كل العملاء
        val dueDebts = list.flatMap { c ->
            c.accounts.flatMap { a ->
                a.operations
                    .filter { it.type == OpType.DEBT && it.dueDate != null && it.dueDate < endOfToday }
                    .map { DueDebtSummary(c.client.name, a.account.name, it.amount, it.dueDate!!, it.dueDate!! < now) }
            }
        }.sortedBy { it.dueDate }

        val filtered = list.filter { c ->
            val matchesQuery = q.isBlank() || c.client.name.contains(q, ignoreCase = true) ||
                (c.client.phone?.contains(q) == true)
            // إضافة 5.3: فلتر الحالة
            val matchesStatus = when (f) {
                ClientStatusFilter.ALL -> true
                ClientStatusFilter.ACTIVE -> c.client.status == com.mahfazty.smart.domain.model.ClientStatus.ACTIVE
                ClientStatusFilter.STOPPED -> c.client.status == com.mahfazty.smart.domain.model.ClientStatus.STOPPED
                ClientStatusFilter.BLOCKED -> c.client.status == com.mahfazty.smart.domain.model.ClientStatus.BLOCKED
            }
            matchesQuery && matchesStatus
        }
        var totalOn = 0.0
        var totalFor = 0.0
        list.forEach { c ->
            val t = c.total
            if (t > 0) totalOn += t else totalFor += -t
        }
        ClientsUiState(
            q, filtered, totalOn, totalFor,
            statusFilter = f,
            activeCount = list.count { it.client.status == com.mahfazty.smart.domain.model.ClientStatus.ACTIVE },
            stoppedCount = list.count { it.client.status == com.mahfazty.smart.domain.model.ClientStatus.STOPPED },
            blockedCount = list.count { it.client.status == com.mahfazty.smart.domain.model.ClientStatus.BLOCKED },
            dueDebts = dueDebts,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), ClientsUiState())

    private val _toast = MutableSharedFlow<ToastMsg>(extraBufferCapacity = 8)
    val toast = _toast.asSharedFlow()

    fun setQuery(q: String) { query.value = q }

    fun addClient(name: String, phone: String?, photoPath: String?, status: String = com.mahfazty.smart.domain.model.ClientStatus.ACTIVE) = viewModelScope.launch {
        repo.addClient(name, phone, photoPath, status)
        _toast.emit(ToastMsg("تم حفظ العميل ✅"))
    }

    fun updateClient(client: Client) = viewModelScope.launch {
        repo.updateClient(client)
        _toast.emit(ToastMsg("تم التعديل ✅"))
    }

    /** حذف عميل مع لقطة كاملة — التوفر عبر Snackbar «تراجع» (إصلاح act-2) */
    fun deleteClient(clientId: Long) = viewModelScope.launch {
        val name = state.value.clients.firstOrNull { it.client.id == clientId }?.client?.name ?: "العميل"
        val snapshot = repo.snapshotClient(clientId)
        repo.deleteClient(clientId)
        _toast.emit(
            ToastMsg("تم حذف «$name»", "تراجع") {
                viewModelScope.launch { snapshot?.let { repo.restoreClient(it) } }
            },
        )
    }
}

// ============ حسابات عميل واحد ============

class ClientAccountsViewModel(
    private val repo: ClientsRepository,
    private val clientId: Long,
) : ViewModel() {

    val state: StateFlow<ClientWithData?> = repo.clientWithData(clientId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    /** سجل التحويلات — لإظهار أثر الحذف في تأكيد حذف الحساب/العميل (إصلاح fin-3) */
    val transfers: StateFlow<List<TransferDisplay>> = repo.transfersDisplay
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _toast = MutableSharedFlow<ToastMsg>(extraBufferCapacity = 8)
    val toast = _toast.asSharedFlow()

    /** نتيجة حذف العميل — لوحة «تراجع/عودة» بدل توست عابر لأن الشاشة تُغلق بعده (إصلاح act-2) */
    data class DeletedClientInfo(val name: String)

    private val _deletedClient = MutableStateFlow<DeletedClientInfo?>(null)
    val deletedClient: StateFlow<DeletedClientInfo?> = _deletedClient.asStateFlow()

    private var lastClientSnapshot: com.mahfazty.smart.data.ClientSnapshot? = null

    fun addAccount(name: String, icon: String) = viewModelScope.launch {
        repo.addAccount(clientId, name, icon)
        _toast.emit(ToastMsg("تمت إضافة الحساب ✅"))
    }

    fun updateAccount(account: ClientAccount) = viewModelScope.launch {
        repo.updateAccount(account)
        _toast.emit(ToastMsg("تم التعديل ✅"))
    }

    /** حذف حساب مع لقطة — تراجع عبر Snackbar (إصلاح act-2) */
    fun deleteAccount(accountId: Long) = viewModelScope.launch {
        val snap = repo.snapshotAccount(accountId)
        val name = snap?.account?.name ?: "الحساب"
        repo.deleteAccount(accountId)
        _toast.emit(
            ToastMsg("تم حذف «$name»", "تراجع") {
                viewModelScope.launch { snap?.let { repo.restoreAccount(it) } }
            },
        )
    }

    /**
     * إضافة 12.2 من تقرير الفحص: سحب الرصيد الحقيقي إلى البنك ثم حذف الحساب —
     * دفعة واحدة (السحب عبر المسار المفحوص الذرّي ثم الحذف) حتى لا يضيع المال من التتبع.
     */
    fun withdrawAndDeleteAccount(accountId: Long) = viewModelScope.launch {
        val snap = repo.snapshotAccount(accountId)
        val acc = snap?.account
        if (acc == null) {
            _toast.emit(ToastMsg("تعذر العثور على الحساب"))
            return@launch
        }
        if (acc.realBalance > 0) {
            val err = repo.withdrawReal(accountId, acc.realBalance, Wallet.BANK)
            if (err != null) {
                _toast.emit(ToastMsg(err.message()))
                return@launch
            }
        }
        repo.deleteAccount(accountId)
        _toast.emit(
            ToastMsg("تم سحب ${com.mahfazty.smart.domain.Money.fmt(acc.realBalance)} إلى البنك وحذف «${acc.name}» ✅"),
        )
    }

    fun updateClient(client: Client) = viewModelScope.launch {
        repo.updateClient(client)
        _toast.emit(ToastMsg("تم التعديل ✅"))
    }

    /** حذف العميل كاملًا — لقطة ثم حذف ثم لوحة نتيجة مع «تراجع» حقيقي */
    fun deleteClient(clientId: Long) = viewModelScope.launch {
        val name = state.value?.client?.name ?: "العميل"
        lastClientSnapshot = repo.snapshotClient(clientId)
        repo.deleteClient(clientId)
        _deletedClient.value = DeletedClientInfo(name)
    }

    /** التراجع عن حذف العميل (من لوحة النتيجة) */
    fun undoDeleteClient() {
        val snap = lastClientSnapshot
        lastClientSnapshot = null
        _deletedClient.value = null
        if (snap != null) viewModelScope.launch { repo.restoreClient(snap) }
    }

    /** إغلاق لوحة النتيجة (عودة للقائمة) */
    fun closeDeleteResult() {
        lastClientSnapshot = null
        _deletedClient.value = null
    }
}

// ============ عمليات حساب واحد ============

/** بيانات نافذة "الرصيد الحقيقي غير الكافي" مع اقتراحات التمويل */
data class InsufficientRealData(
    val have: Double,
    val needed: Double,
    val bank: Double,
    val cash: Double,
    val transferSources: List<Triple<String, Long, Double>>, // (العنوان، معرف الحساب، المتاح)
    val savings: Double = 0.0,
    val goals: List<Triple<Long, String, Double>> = emptyList(), // id, عنوان، المتاح
)

/** فلترة نوع العملية (إصلاح act-3) */
sealed interface OpTypeFilter {
    data object All : OpTypeFilter
    data object Debt : OpTypeFilter
    data object Pay : OpTypeFilter
}

/** مدى زمني للفلترة (إصلاح act-3) */
enum class OpPeriod { ALL, MONTH, THREE_MONTHS, YEAR }

/** إضافة 15.3 من تقرير الفحص: ترتيب عمليات الحساب */
enum class OpSortMode { LATEST, AMOUNT_DESC }

class AccountOpsViewModel(
    private val repo: ClientsRepository,
    private val walletRepo: com.mahfazty.smart.data.WalletRepository,
    private val accountId: Long,
) : ViewModel() {

    // ---------- فلاتر البحث (إصلاح act-3) ----------

    private val _query = MutableStateFlow("")
    val query: StateFlow<String> = _query.asStateFlow()
    fun setQuery(q: String) { _query.value = q }

    private val _opTypeFilter = MutableStateFlow<OpTypeFilter>(OpTypeFilter.All)
    val opTypeFilter: StateFlow<OpTypeFilter> = _opTypeFilter.asStateFlow()
    fun setOpTypeFilter(f: OpTypeFilter) { _opTypeFilter.value = f }

    private val _opPeriod = MutableStateFlow(OpPeriod.ALL)
    val opPeriod: StateFlow<OpPeriod> = _opPeriod.asStateFlow()
    fun setOpPeriod(p: OpPeriod) { _opPeriod.value = p }

    // ===== إضافة 3.1 من تقرير الفحص: فلتر الديون المستحقة/المتأخرة =====
    private val _dueOnly = MutableStateFlow(false)
    val dueOnly: StateFlow<Boolean> = _dueOnly.asStateFlow()
    fun setDueOnly(b: Boolean) { _dueOnly.value = b }

    // ===== إضافة 15.3 من تقرير الفحص: ترتيب عمليات الحساب =====
    private val _sortMode = MutableStateFlow(OpSortMode.LATEST)
    val sortMode: StateFlow<OpSortMode> = _sortMode.asStateFlow()
    fun setSortMode(m: OpSortMode) { _sortMode.value = m }

    /** العميل الذي يملك هذا الحساب (للعرض والمشاركة) */
    val client: StateFlow<ClientWithData?> = repo.clientsWithData
        .map { list -> list.firstOrNull { c -> c.accounts.any { it.account.id == accountId } } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val account: StateFlow<AccountWithOps?> = repo.accountWithOps(accountId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val allAccounts: StateFlow<List<Pair<Client, ClientAccount>>> = repo.allAccounts
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val transfers: StateFlow<List<TransferDisplay>> = repo.transfersDisplay
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val bank: StateFlow<Double> = walletRepo.bankBalance
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    val cash: StateFlow<Double> = walletRepo.cashBalance
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    val savings: StateFlow<Double> = walletRepo.savingsTotal
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    val goalSources: StateFlow<List<Triple<Long, String, Double>>> = walletRepo.goalsWithSaved
        .map { list ->
            list.filter { it.saved > 0 }
                .map { Triple(it.goal.id, "${it.goal.icon} ${it.goal.name}", it.saved) }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    /** وضع اختيار العمليات المتعدد */
    private val _selection = MutableStateFlow<Set<Long>>(emptySet())
    val selection: StateFlow<Set<Long>> = _selection.asStateFlow()

    private val _insufficientReal = MutableStateFlow<InsufficientRealData?>(null)
    val insufficientReal: StateFlow<InsufficientRealData?> = _insufficientReal.asStateFlow()

    private val _pendingOp = MutableStateFlow<PendingClientOp?>(null)
    val pendingOp: StateFlow<PendingClientOp?> = _pendingOp.asStateFlow()
    fun clearPendingOp() { _pendingOp.value = null }

    /** سجل التدقيق — أحدث 100 قيد (إصلاح sec-6) */
    val audit: StateFlow<List<com.mahfazty.smart.data.AuditLogEntry>> = repo.auditLog
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _toast = MutableSharedFlow<ToastMsg>(extraBufferCapacity = 8)
    val toast = _toast.asSharedFlow()

    fun dismissInsufficientReal() { _insufficientReal.value = null }

    fun toggleSelect(opId: Long) {
        _selection.value = _selection.value.let { if (opId in it) it - opId else it + opId }
    }

    fun clearSelection() { _selection.value = emptySet() }

    fun addOperation(
        type: OpType, amount: Double, note: String?,
        materials: List<MaterialItem>, receiptPath: String?,
        isInvoice: Boolean = false, invoiceRef: String? = null,
        dueDate: Long? = null, currency: String? = null,
    ) = viewModelScope.launch {
        // إضافة 5.1 من تقرير الفحص: منع العمليات على العملاء الموقوفين/القائمة السوداء
        val status = client.value?.client?.status
        if (status == com.mahfazty.smart.domain.model.ClientStatus.STOPPED ||
            status == com.mahfazty.smart.domain.model.ClientStatus.BLOCKED
        ) {
            _toast.emit(
                ToastMsg(
                    if (status == com.mahfazty.smart.domain.model.ClientStatus.BLOCKED)
                        "⛔ هذا العميل في القائمة السوداء — غيّر حالته من ملفه أولاً"
                    else "⚠️ هذا العميل موقوف — غيّر حالته من ملفه أولاً",
                ),
            )
            return@launch
        }
        repo.addOperation(accountId, type, amount, note, materials, receiptPath, isInvoice, invoiceRef, dueDate, currency)?.let { err ->
            if (err is WalletError.InsufficientReal) {
                _pendingOp.value = PendingClientOp(
                    type, amount, note, materials, receiptPath,
                    isInvoice = isInvoice, invoiceRef = invoiceRef,
                    dueDate = dueDate, currency = currency,
                )
            }
            handleError(err, amount)
        } ?: run {
            _pendingOp.value = null
            _toast.emit(ToastMsg("تم حفظ العملية ✅"))
        }
    }

    fun updateOperation(op: ClientOperation) = viewModelScope.launch {
            // إضافة 5.1: نفس المنع عند التعديل
            val status = client.value?.client?.status
            if (status == com.mahfazty.smart.domain.model.ClientStatus.STOPPED ||
                status == com.mahfazty.smart.domain.model.ClientStatus.BLOCKED
            ) {
                _toast.emit(ToastMsg("⚠️ غيّر حالة العميل أولاً — التعديل ممنوع"))
                return@launch
            }
            repo.updateOperation(op)?.let { err ->
                if (err is WalletError.InsufficientReal) {
                    _pendingOp.value = PendingClientOp(
                        op.type, op.amount, op.note, op.materials, op.receiptPath,
                        isInvoice = op.isInvoice, invoiceRef = op.invoiceRef,
                        dueDate = op.dueDate, currency = op.currency, editing = op,
                    )
                }
                handleError(err, op.amount)
            } ?: run {
            _pendingOp.value = null
            _toast.emit(ToastMsg("تم التعديل ✅"))
        }
    }

    /** تسليم فاتورة غير مسلمة (تُستدعى من Long Press في قائمة العمليات) */
    fun markInvoiceDelivered(op: ClientOperation) = viewModelScope.launch {
        repo.markInvoiceDelivered(op.id)
        _toast.emit(ToastMsg("تم تسليم الفاتورة ✅"))
    }

    /**
     * حذف عملية مع لقطة حسابها قبل الحذف (أساس «تراجع» — إصلاح act-2).
     * قد يُرفض الحذف نفسه إن كان سيكسر قاعدة «لا رصيد حقيقي سالب» (إصلاح fin-1).
     */
    fun deleteOperation(op: ClientOperation) = viewModelScope.launch {
        val snap = repo.snapshotAccount(op.accountId)
        repo.deleteOperation(op)?.let { err ->
            _toast.emit(ToastMsg(err.message()))
            return@launch
        }
        _toast.emit(
            ToastMsg("تم حذف العملية", "تراجع") {
                viewModelScope.launch {
                    snap?.let { repo.restoreAccount(it) }
                }
            },
        )
    }

    /** حذف المحدد دفعة واحدة — لقطة الحسابات المتأثرة ثم تراجع جماعي (act-2 + fin-1) */
    fun deleteSelected() = viewModelScope.launch {
        val ops = account.value?.operations?.filter { it.id in _selection.value } ?: emptyList()
        if (ops.isEmpty()) return@launch
        val affectedAccounts = ops.map { it.accountId }.distinct()
            .mapNotNull { id -> repo.snapshotAccount(id)?.account }
        // إصلاح B4: دفعة ذرّية واحدة (مع تحقق fin-1 داخلياً)
        repo.deleteOperations(ops)?.let { err ->
            _toast.emit(ToastMsg(err.message()))
            return@launch
        }
        _selection.value = emptySet()
        _toast.emit(
            ToastMsg("تم حذف ${ops.size} عملية", "تراجع") {
                viewModelScope.launch {
                    repo.restoreOperations(ops, affectedAccounts)
                }
            },
        )
    }

    /** إصلاح fin-4: تسوية يدوية للرصيد الحقيقي مع سبب إلزامي (تُسجَّل في سجل التدقيق) */
    fun adjustReal(newAmount: Double, reason: String) = viewModelScope.launch {
        repo.adjustReal(accountId, newAmount, reason)?.let { err ->
            _toast.emit(ToastMsg(err.message()))
        } ?: _toast.emit(ToastMsg("تمت التسوية ✅ — سُجلت في سجل التعديلات"))
    }

    /** إصلاح act-4: تصدير كشف حساب كامل (CSV) للحساب الحالي ومشاركته */
    fun exportAccountCsv(context: android.content.Context) = viewModelScope.launch {
        val c = client.value ?: return@launch
        val acc = account.value ?: return@launch
        val sb = StringBuilder("\uFEFF")
        sb.append("التاريخ,النوع,المبلغ,العملة,البيان,المواد,الفاتورة\n")
        acc.operations.sortedBy { it.date }.forEach { op ->
            val label = if (op.type == OpType.DEBT) "عليه" else "له"
            val materials = op.materials.joinToString("؛ ") { m -> "${m.name} (${m.qty}×${m.unitPrice})" }
            sb.append(
                "${com.mahfazty.smart.domain.Dates.fileStamp(op.date)},$label," +
                    "${com.mahfazty.smart.domain.Money.fmtLat(op.amount)},${op.currency ?: ""}," +
                    "${(op.note ?: "").replace(",", "،")}," +
                    "${materials.replace(",", "،")},${if (op.isInvoice) (op.invoiceRef ?: "") else ""}\n"
            )
        }
        runCatching {
            val dir = java.io.File(context.cacheDir, "exports").apply { mkdirs() }
            val file = java.io.File(dir, "mahfazty-account-${acc.account.name}-${com.mahfazty.smart.domain.Dates.fileStamp(System.currentTimeMillis())}.csv")
            file.writeText(sb.toString(), Charsets.UTF_8)
            val uri = androidx.core.content.FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
            com.mahfazty.smart.ui.util.shareFile(context, uri, "text/csv", "كشف حساب: ${acc.account.name}")
            _toast.emit(ToastMsg("تم تصدير الكشف كاملًا ✅"))
        }.onFailure { _toast.emit(ToastMsg("تعذر تصدير الكشف")) }
    }

    private suspend fun handleError(err: WalletError, amount: Double) {
        when (err) {
            is WalletError.InsufficientReal -> {
                val bank = walletRepo.bankBalance.first()
                val cash = walletRepo.cashBalance.first()
                val sources = allAccounts.value
                    .filter { (_, acc) -> acc.id != accountId && acc.realBalance > 0 }
                    .map { (c, acc) -> Triple("${c.name} • ${acc.name}", acc.id, acc.realBalance) }
                val savings = walletRepo.savingsTotal.first()
                val txs = walletRepo.transactions.first()
                val goals = walletRepo.goals.first()
                    .map { g -> Triple(g.id, "${g.icon} ${g.name}", WalletEngine.goalSaved(g.opening, g.id, txs)) }
                    .filter { it.third > 0 }
                _insufficientReal.value = InsufficientRealData(
                    err.have, err.need, bank, cash, sources, savings, goals,
                )
            }
            else -> _toast.emit(ToastMsg(err.message()))
        }
    }

    fun quickFundReal(from: Wallet, amount: Double) = viewModelScope.launch {
        _insufficientReal.value = null
        if (amount <= 0) {
            _toast.emit(ToastMsg("لا يوجد رصيد كافٍ في هذا الصندوق"))
            return@launch
        }
        repo.fundReal(accountId, amount, from)?.let { _toast.emit(ToastMsg(it.message())) }
            ?: _toast.emit(ToastMsg("تم الشحن ✅ — أكمل العملية السابقة"))
    }

    fun quickTransferReal(fromAccountId: Long, amount: Double) = viewModelScope.launch {
        _insufficientReal.value = null
        val from = allAccounts.value.firstOrNull { (_, a) -> a.id == fromAccountId } ?: return@launch
        val to = allAccounts.value.firstOrNull { (_, a) -> a.id == accountId } ?: return@launch
        repo.transferReal(from.first.id, fromAccountId, to.first.id, accountId, amount)?.let {
            _toast.emit(ToastMsg(it.message()))
        } ?: _toast.emit(ToastMsg("تم التحويل ✅ — أكمل العملية السابقة"))
    }

    fun quickFundFromSavings(amount: Double) = viewModelScope.launch {
        _insufficientReal.value = null
        if (amount <= 0) return@launch
        walletRepo.withdrawSavings(amount)?.let { err ->
            _toast.emit(ToastMsg(err.message()))
            return@launch
        }
        repo.fundReal(accountId, amount, Wallet.BANK)?.let { _toast.emit(ToastMsg(it.message())) }
            ?: _toast.emit(ToastMsg("تم الشحن من الادخار ✅ — أكمل العملية السابقة"))
    }

    fun quickFundFromGoal(goalId: Long, goalName: String, amount: Double) = viewModelScope.launch {
        _insufficientReal.value = null
        if (amount <= 0) return@launch
        walletRepo.contributeGoal(goalId, goalName, false, amount)?.let { err ->
            _toast.emit(ToastMsg(err.message()))
            return@launch
        }
        repo.fundReal(accountId, amount, Wallet.BANK)?.let { _toast.emit(ToastMsg(it.message())) }
            ?: _toast.emit(ToastMsg("تم الشحن من الهدف ✅ — أكمل العملية السابقة"))
    }

    fun fundReal(amount: Double, from: Wallet) = viewModelScope.launch {
        repo.fundReal(accountId, amount, from)?.let { _toast.emit(ToastMsg(it.message())) }
            ?: _toast.emit(ToastMsg("تم شحن الرصيد الحقيقي ✅"))
    }

    fun withdrawReal(amount: Double, to: Wallet) = viewModelScope.launch {
        repo.withdrawReal(accountId, amount, to)?.let { err ->
            if (err is WalletError.InsufficientReal) {
                _toast.emit(ToastMsg("الرصيد الحقيقي لا يكفي"))
            } else _toast.emit(ToastMsg(err.message()))
        } ?: _toast.emit(ToastMsg("تم السحب ✅"))
    }

    fun transferReal(fromClientId: Long, fromAccountId: Long, toClientId: Long, toAccountId: Long, amount: Double) =
        viewModelScope.launch {
            repo.transferReal(fromClientId, fromAccountId, toClientId, toAccountId, amount)?.let {
                _toast.emit(ToastMsg(it.message()))
            } ?: _toast.emit(ToastMsg("تم التحويل ✅"))
        }

    fun updateAccount(account: ClientAccount) = viewModelScope.launch {
        repo.updateAccount(account)
        _toast.emit(ToastMsg("تم التعديل ✅"))
    }

    /** حذف حساب مع لقطة — تراجع عبر Snackbar (إصلاح act-2) */
    fun deleteAccount(accountId: Long) = viewModelScope.launch {
        val snap = repo.snapshotAccount(accountId)
        val name = snap?.account?.name ?: "الحساب"
        repo.deleteAccount(accountId)
        _toast.emit(
            ToastMsg("تم حذف «$name»", "تراجع") {
                viewModelScope.launch { snap?.let { repo.restoreAccount(it) } }
            },
        )
    }

    /** إضافة 12.2 من تقرير الفحص: سحب الرصيد الحقيقي إلى البنك ثم حذف الحساب */
    fun withdrawAndDeleteAccount(accountId: Long) = viewModelScope.launch {
        val snap = repo.snapshotAccount(accountId)
        val acc = snap?.account
        if (acc == null) {
            _toast.emit(ToastMsg("تعذر العثور على الحساب"))
            return@launch
        }
        if (acc.realBalance > 0) {
            val err = repo.withdrawReal(accountId, acc.realBalance, Wallet.BANK)
            if (err != null) {
                _toast.emit(ToastMsg(err.message()))
                return@launch
            }
        }
        repo.deleteAccount(accountId)
        _toast.emit(
            ToastMsg("تم سحب ${com.mahfazty.smart.domain.Money.fmt(acc.realBalance)} إلى البنك وحذف «${acc.name}» ✅"),
        )
    }

    /** نص مشاركة واتساب لكشف الحساب (إصلاح act-4: آخر 10 أو كامل) */
    fun shareText(limit: Int = 10): String? {
        val c = client.value ?: return null
        val acc = account.value ?: return null
        return repo.whatsAppText(c.client, acc, limit)
    }

    /** نص مشاركة عمليات محددة */
    fun shareSelectedText(): String? {
        val c = client.value ?: return null
        val acc = account.value ?: return null
        val selectedOps = acc.operations.filter { it.id in _selection.value }
        if (selectedOps.isEmpty()) return null
        val sb = StringBuilder("🧾 عمليات مختارة: ${acc.account.name} ${acc.account.icon}\n👤 العميل: ${c.client.name}\n\n")
        selectedOps.sortedByDescending { it.date }.forEachIndexed { i, op ->
            val label = if (op.type == OpType.DEBT) "عليه" else "له"
            sb.append("${i + 1}. $label ${com.mahfazty.smart.domain.Money.fmt(op.amount)} ${op.currency ?: ""} - ${op.note ?: ""}\n".trim())
        }
        sb.append("\nمحفظتي الذكية 💰")
        return sb.toString()
    }
}
