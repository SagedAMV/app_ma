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

// ============ قائمة العملاء ============

data class ClientsUiState(
    val query: String = "",
    val clients: List<ClientWithData> = emptyList(),
    val totalOn: Double = 0.0,
    val totalFor: Double = 0.0,
)

class ClientsViewModel(private val repo: ClientsRepository) : ViewModel() {

    private val query = MutableStateFlow("")

    val state: StateFlow<ClientsUiState> = combine(repo.clientsWithData, query) { list, q ->
        val filtered = list.filter { c ->
            q.isBlank() || c.client.name.contains(q, ignoreCase = true) ||
                (c.client.phone?.contains(q) == true)
        }
        var totalOn = 0.0
        var totalFor = 0.0
        list.forEach { c ->
            val t = c.total
            if (t > 0) totalOn += t else totalFor += -t
        }
        ClientsUiState(q, filtered, totalOn, totalFor)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), ClientsUiState())

    private val _toast = MutableSharedFlow<String>(extraBufferCapacity = 8)
    val toast = _toast.asSharedFlow()

    fun setQuery(q: String) { query.value = q }

    fun addClient(name: String, phone: String?, photoPath: String?) = viewModelScope.launch {
        repo.addClient(name, phone, photoPath)
        _toast.emit("تم حفظ العميل ✅")
    }

    fun updateClient(client: Client) = viewModelScope.launch {
        repo.updateClient(client)
        _toast.emit("تم التعديل ✅")
    }

    fun deleteClient(clientId: Long) = viewModelScope.launch {
        repo.deleteClient(clientId)
        _toast.emit("تم حذف العميل")
    }
}

// ============ حسابات عميل واحد ============

class ClientAccountsViewModel(
    private val repo: ClientsRepository,
    private val clientId: Long,
) : ViewModel() {

    val state: StateFlow<ClientWithData?> = repo.clientWithData(clientId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    private val _toast = MutableSharedFlow<String>(extraBufferCapacity = 8)
    val toast = _toast.asSharedFlow()

    fun addAccount(name: String, icon: String) = viewModelScope.launch {
        repo.addAccount(clientId, name, icon)
        _toast.emit("تمت إضافة الحساب ✅")
    }

    fun updateAccount(account: ClientAccount) = viewModelScope.launch {
        repo.updateAccount(account)
        _toast.emit("تم التعديل ✅")
    }

    fun deleteAccount(accountId: Long) = viewModelScope.launch {
        repo.deleteAccount(accountId)
        _toast.emit("تم حذف الحساب")
    }

    fun updateClient(client: Client) = viewModelScope.launch {
        repo.updateClient(client)
        _toast.emit("تم التعديل ✅")
    }

    fun deleteClient(clientId: Long) = viewModelScope.launch {
        repo.deleteClient(clientId)
        _toast.emit("تم حذف العميل")
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

class AccountOpsViewModel(
    private val repo: ClientsRepository,
    private val walletRepo: com.mahfazty.smart.data.WalletRepository,
    private val accountId: Long,
) : ViewModel() {

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

    private val _toast = MutableSharedFlow<String>(extraBufferCapacity = 8)
    val toast = _toast.asSharedFlow()

    fun dismissInsufficientReal() { _insufficientReal.value = null }

    fun toggleSelect(opId: Long) {
        _selection.value = _selection.value.let { if (opId in it) it - opId else it + opId }
    }

    fun clearSelection() { _selection.value = emptySet() }

    fun addOperation(type: OpType, amount: Double, note: String?, materials: List<MaterialItem>, receiptPath: String?) =
        viewModelScope.launch {
            repo.addOperation(accountId, type, amount, note, materials, receiptPath)?.let { err ->
                if (err is WalletError.InsufficientReal) {
                    _pendingOp.value = PendingClientOp(type, amount, note, materials, receiptPath)
                }
                handleError(err, amount)
            } ?: run {
                _pendingOp.value = null
                _toast.emit("تم حفظ العملية ✅")
            }
        }

    fun updateOperation(op: ClientOperation) = viewModelScope.launch {
            repo.updateOperation(op)?.let { err ->
                if (err is WalletError.InsufficientReal) {
                    _pendingOp.value = PendingClientOp(op.type, op.amount, op.note, op.materials, op.receiptPath, editing = op)
                }
                handleError(err, op.amount)
            } ?: run {
            _pendingOp.value = null
            _toast.emit("تم التعديل ✅")
        }
    }

    fun deleteOperation(op: ClientOperation) = viewModelScope.launch {
        repo.deleteOperation(op)
        _toast.emit("تم حذف العملية")
    }

    fun deleteSelected() = viewModelScope.launch {
        val ops = account.value?.operations?.filter { it.id in _selection.value } ?: emptyList()
        // إصلاح B4: دفعة ذرّية واحدة بدل حلقة معاملات منفصلة
        repo.deleteOperations(ops)
        _selection.value = emptySet()
        _toast.emit("تم حذف ${ops.size} عملية")
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
            else -> _toast.emit(err.message())
        }
    }

    fun quickFundReal(from: Wallet, amount: Double) = viewModelScope.launch {
        _insufficientReal.value = null
        if (amount <= 0) {
            _toast.emit("لا يوجد رصيد كافٍ في هذا الصندوق")
            return@launch
        }
        repo.fundReal(accountId, amount, from)?.let { _toast.emit(it.message()) }
            ?: _toast.emit("تم الشحن ✅ — أكمل العملية السابقة")
    }

    fun quickTransferReal(fromAccountId: Long, amount: Double) = viewModelScope.launch {
        _insufficientReal.value = null
        val from = allAccounts.value.firstOrNull { (_, a) -> a.id == fromAccountId } ?: return@launch
        val to = allAccounts.value.firstOrNull { (_, a) -> a.id == accountId } ?: return@launch
        repo.transferReal(from.first.id, fromAccountId, to.first.id, accountId, amount)?.let {
            _toast.emit(it.message())
        } ?: _toast.emit("تم التحويل ✅ — أكمل العملية السابقة")
    }

    fun quickFundFromSavings(amount: Double) = viewModelScope.launch {
        _insufficientReal.value = null
        if (amount <= 0) return@launch
        walletRepo.withdrawSavings(amount)?.let { err ->
            _toast.emit(err.message())
            return@launch
        }
        repo.fundReal(accountId, amount, Wallet.BANK)?.let { _toast.emit(it.message()) }
            ?: _toast.emit("تم الشحن من الادخار ✅ — أكمل العملية السابقة")
    }

    fun quickFundFromGoal(goalId: Long, goalName: String, amount: Double) = viewModelScope.launch {
        _insufficientReal.value = null
        if (amount <= 0) return@launch
        walletRepo.contributeGoal(goalId, goalName, false, amount)?.let { err ->
            _toast.emit(err.message())
            return@launch
        }
        repo.fundReal(accountId, amount, Wallet.BANK)?.let { _toast.emit(it.message()) }
            ?: _toast.emit("تم الشحن من الهدف ✅ — أكمل العملية السابقة")
    }

    fun fundReal(amount: Double, from: Wallet) = viewModelScope.launch {
        repo.fundReal(accountId, amount, from)?.let { _toast.emit(it.message()) }
            ?: _toast.emit("تم شحن الرصيد الحقيقي ✅")
    }

    fun withdrawReal(amount: Double, to: Wallet) = viewModelScope.launch {
        repo.withdrawReal(accountId, amount, to)?.let { err ->
            if (err is WalletError.InsufficientReal) {
                _toast.emit("الرصيد الحقيقي لا يكفي")
            } else _toast.emit(err.message())
        } ?: _toast.emit("تم السحب ✅")
    }

    fun transferReal(fromClientId: Long, fromAccountId: Long, toClientId: Long, toAccountId: Long, amount: Double) =
        viewModelScope.launch {
            repo.transferReal(fromClientId, fromAccountId, toClientId, toAccountId, amount)?.let {
                _toast.emit(it.message())
            } ?: _toast.emit("تم التحويل ✅")
        }

    fun updateAccount(account: ClientAccount) = viewModelScope.launch {
        repo.updateAccount(account)
        _toast.emit("تم التعديل ✅")
    }

    fun deleteAccount(accountId: Long) = viewModelScope.launch {
        repo.deleteAccount(accountId)
        _toast.emit("تم حذف الحساب")
    }

    /** نص مشاركة واتساب */
    fun shareText(): String? {
        val c = client.value ?: return null
        val acc = account.value ?: return null
        return repo.whatsAppText(c.client, acc)
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
            sb.append("${i + 1}. $label ${com.mahfazty.smart.domain.Money.fmt(op.amount)} - ${op.note ?: ""}\n")
        }
        sb.append("\nمحفظتي الذكية 💰")
        return sb.toString()
    }
}
