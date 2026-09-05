package com.mahfazty.smart.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColor
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.updateTransition
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mahfazty.smart.data.AccountWithOps
import com.mahfazty.smart.data.ClientWithData
import com.mahfazty.smart.data.TransferDisplay
import com.mahfazty.smart.domain.Dates
import com.mahfazty.smart.domain.Money
import com.mahfazty.smart.domain.model.AppSettings
import com.mahfazty.smart.domain.model.Client
import com.mahfazty.smart.domain.model.ClientAccount
import com.mahfazty.smart.domain.model.ClientOperation
import com.mahfazty.smart.domain.model.OpType
import com.mahfazty.smart.domain.model.Wallet
import com.mahfazty.smart.ui.components.AppCard
import com.mahfazty.smart.ui.components.ConfirmDialog
import com.mahfazty.smart.ui.components.EmptyState
import com.mahfazty.smart.ui.components.PhotoAvatar
import com.mahfazty.smart.ui.components.PhotoStore
import com.mahfazty.smart.ui.components.SoftDivider
import com.mahfazty.smart.ui.components.ElasticEntrance
import com.mahfazty.smart.ui.components.animatedGradient
import com.mahfazty.smart.ui.components.bounceClick
import com.mahfazty.smart.ui.dialogs.AccountDialog
import com.mahfazty.smart.ui.dialogs.ClientDialog
import com.mahfazty.smart.ui.dialogs.FundRealDialog
import com.mahfazty.smart.ui.dialogs.HistoryDialog
import com.mahfazty.smart.ui.dialogs.InsufficientRealSheet
import com.mahfazty.smart.ui.dialogs.OperationDialog
import com.mahfazty.smart.ui.dialogs.TransferRealDialog
import com.mahfazty.smart.ui.dialogs.WithdrawRealDialog
import com.mahfazty.smart.ui.theme.LocalAppColors
import com.mahfazty.smart.ui.util.shareViaWhatsApp
import com.mahfazty.smart.ui.viewmodels.ClientsUiState
import com.mahfazty.smart.ui.viewmodels.InsufficientRealData

/** لون شارات الفواتير غير المسلمة (برتقالي) — نظام «الفواتير وحالة التسليم» */
private val InvoiceOrange = Color(0xFFF57C00)

// =====================================================================
// 1) شاشة قائمة العملاء
// =====================================================================

@Composable
fun ClientsScreen(
    state: ClientsUiState,
    toast: kotlinx.coroutines.flow.SharedFlow<String>,
    onSetQuery: (String) -> Unit,
    onAddClient: (String, String?, String?) -> Unit,
    onOpenClient: (Long) -> Unit,
) {
    val snackbar = remember { SnackbarHostState() }
    LaunchedEffect(Unit) { toast.collect { snackbar.showSnackbar(it) } }
    var showAdd by remember { mutableStateOf(false) }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbar) },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAdd = true },
                containerColor = MaterialTheme.colorScheme.primary,
                modifier = Modifier.bounceClick(),
            ) { Text("＋", fontSize = 22.sp) }
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(bottom = 90.dp),
        ) {
            item {
                ElasticEntrance(0) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text("👥 العملاء", style = MaterialTheme.typography.titleLarge)
                    Text(
                        "${state.clients.size} عميل",
                        style = MaterialTheme.typography.bodySmall,
                        color = LocalAppColors.current.muted,
                    )
                }
                }
            }
            item {
                ElasticEntrance(1) {
                OutlinedTextField(
                    value = state.query,
                    onValueChange = onSetQuery,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp),
                    placeholder = { Text("🔍 ابحث...") },
                    shape = RoundedCornerShape(16.dp),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = LocalAppColors.current.border,
                    ),
                )
                }
            }
            item {
                ElasticEntrance(2) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    BalanceBox("إجمالي عليه (لك)", state.totalOn, LocalAppColors.current.red, Modifier.weight(1f))
                    BalanceBox("إجمالي له (عليك)", state.totalFor, LocalAppColors.current.green, Modifier.weight(1f))
                }
                }
            }
            if (state.clients.isEmpty()) {
                item { EmptyState("👥", "لا يوجد عملاء") }
            } else {
                itemsIndexed(state.clients, key = { _, c -> c.client.id }) { index, c ->
                    ElasticEntrance(index + 3) {
                        ClientRow(c) { onOpenClient(c.client.id) }
                    }
                }
            }
        }
    }

    if (showAdd) {
        ClientDialog(
            title = "إضافة عميل",
            onDismiss = { showAdd = false },
            onSave = { name, phone, photo ->
                showAdd = false
                onAddClient(name, phone, photo)
            },
        )
    }
}

@Composable
private fun BalanceBox(label: String, value: Double, color: Color, modifier: Modifier = Modifier) {
    AppCard(modifier) {
        Column(Modifier.padding(12.dp)) {
            Text(label, style = MaterialTheme.typography.labelSmall, color = LocalAppColors.current.muted)
            Spacer(Modifier.height(4.dp))
            Text(Money.fmt(value), style = MaterialTheme.typography.titleMedium, color = color)
        }
    }
}

@Composable
private fun ClientRow(c: ClientWithData, onClick: () -> Unit) {
    AppCard(Modifier.padding(horizontal = 20.dp, vertical = 5.dp)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onClick)
                .bounceClick()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            PhotoAvatar(c.client.photoPath, c.client.name.firstOrNull()?.toString() ?: "ع")
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(c.client.name, style = MaterialTheme.typography.labelLarge)
                Text(
                    "${c.client.phone ?: "بدون هاتف"} • ${c.accounts.size} حساب",
                    style = MaterialTheme.typography.bodySmall,
                    color = LocalAppColors.current.muted,
                )
            }
            val total = c.total
            Text(
                when {
                    total > 0 -> "عليه ${Money.fmt(total)}"
                    total < 0 -> "له ${Money.fmt(-total)}"
                    else -> "متساوي"
                },
                style = MaterialTheme.typography.labelMedium,
                color = when {
                    total > 0 -> LocalAppColors.current.red
                    total < 0 -> LocalAppColors.current.green
                    else -> LocalAppColors.current.muted
                },
            )
        }
    }
}

// =====================================================================
// 2) شاشة حسابات العميل
// =====================================================================

@Composable
fun ClientAccountsScreen(
    data: ClientWithData?,
    toast: kotlinx.coroutines.flow.SharedFlow<String>,
    onBack: () -> Unit,
    onAddAccount: (String, String) -> Unit,
    onUpdateClient: (Client) -> Unit,
    onDeleteClient: (Long) -> Unit,
    onUpdateAccount: (ClientAccount) -> Unit,
    onDeleteAccount: (Long) -> Unit,
    onOpenAccount: (Long) -> Unit,
) {
    val snackbar = remember { SnackbarHostState() }
    LaunchedEffect(Unit) { toast.collect { snackbar.showSnackbar(it) } }
    var showAddAccount by remember { mutableStateOf(false) }
    var editingClient by remember { mutableStateOf(false) }
    var editingAccount by remember { mutableStateOf<ClientAccount?>(null) }
    var deletingClient by remember { mutableStateOf(false) }
    var deletingAccount by remember { mutableStateOf<ClientAccount?>(null) }

    val client = data?.client

    Scaffold(
        snackbarHost = { SnackbarHost(snackbar) },
        floatingActionButton = {
            if (client != null) {
                FloatingActionButton(
                    onClick = { showAddAccount = true },
                    containerColor = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.bounceClick(),
                ) { Text("＋", fontSize = 22.sp) }
            }
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(bottom = 90.dp),
        ) {
            item { ElasticEntrance(0) { ScreenTopBar("حسابات", onBack) } }
            item {
                ElasticEntrance(1) {
                if (client != null) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        AppCard(Modifier.weight(1f)) {
                            Row(
                                modifier = Modifier.padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                PhotoAvatar(client.photoPath, client.name.firstOrNull()?.toString() ?: "ع", 46)
                                Spacer(Modifier.width(12.dp))
                                Column(Modifier.weight(1f)) {
                                    Text(client.name, style = MaterialTheme.typography.titleMedium)
                                    if (!client.phone.isNullOrBlank()) {
                                        Text(client.phone, style = MaterialTheme.typography.bodySmall, color = LocalAppColors.current.muted)
                                    }
                                }
                                val total = data.total
                                Text(
                                    when {
                                        total > 0 -> "عليه ${Money.fmt(total)}"
                                        total < 0 -> "له ${Money.fmt(-total)}"
                                        else -> "متساوي"
                                    },
                                    style = MaterialTheme.typography.labelMedium,
                                    color = when {
                                        total > 0 -> LocalAppColors.current.red
                                        total < 0 -> LocalAppColors.current.green
                                        else -> LocalAppColors.current.muted
                                    },
                                )
                            }
                        }
                        IconButton(onClick = { editingClient = true }) {
                            Text("✏️", fontSize = 18.sp)
                        }
                        IconButton(onClick = { deletingClient = true }) {
                            Text("🗑️", fontSize = 18.sp)
                        }
                    }
                }
                }
            }
            item {
                ElasticEntrance(1) {
                Text(
                    "الحسابات (مثل: ماطور، كهرباء، عهدة مشتريات...)",
                    style = MaterialTheme.typography.bodySmall,
                    color = LocalAppColors.current.muted,
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 6.dp),
                )
                }
            }
            if (data?.accounts.isNullOrEmpty()) {
                item { EmptyState("📂", "لا يوجد حسابات — أضف حسابك الأول") }
            } else {
                itemsIndexed(data.accounts, key = { _, a -> a.account.id }) { index, acc ->
                    ElasticEntrance(index + 2) {
                        AccountRow(acc) { onOpenAccount(acc.account.id) }
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        Spacer(Modifier.weight(1f))
                        IconButton(onClick = { editingAccount = acc.account }) {
                            Text("✏️", fontSize = 16.sp)
                        }
                        IconButton(onClick = { deletingAccount = acc.account }) {
                            Text("🗑️", fontSize = 16.sp)
                        }
                        Spacer(Modifier.width(16.dp))
                    }
                }
            }
        }
    }

    if (showAddAccount) {
        AccountDialog(
            title = "إضافة حساب",
            onDismiss = { showAddAccount = false },
            onSave = { name, icon ->
                showAddAccount = false
                onAddAccount(name, icon)
            },
        )
    }
    if (editingClient && client != null) {
        ClientDialog(
            title = "تعديل العميل",
            initial = client,
            onDismiss = { editingClient = false },
            onSave = { name, phone, photo ->
                editingClient = false
                onUpdateClient(client.copy(name = name, phone = phone, photoPath = photo))
            },
        )
    }
    editingAccount?.let { acc ->
        AccountDialog(
            title = "تعديل الحساب",
            initial = acc,
            onDismiss = { editingAccount = null },
            onSave = { name, icon ->
                editingAccount = null
                onUpdateAccount(acc.copy(name = name, icon = icon))
            },
        )
    }
    if (deletingClient && client != null) {
        ConfirmDialog(
            title = "حذف العميل؟",
            message = "سيُحذف العميل وكل حساباته وعملياته نهائياً.",
            onConfirm = { deletingClient = false; onDeleteClient(client.id); onBack() },
            onDismiss = { deletingClient = false },
        )
    }
    deletingAccount?.let { acc ->
        ConfirmDialog(
            title = "حذف الحساب؟",
            message = "سيُحذف الحساب وكل عملياته.",
            onConfirm = { deletingAccount = null; onDeleteAccount(acc.id) },
            onDismiss = { deletingAccount = null },
        )
    }
}

@Composable
private fun AccountRow(acc: AccountWithOps, onClick: () -> Unit) {
    AppCard(Modifier.padding(horizontal = 20.dp, vertical = 5.dp)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onClick)
                .bounceClick()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center,
            ) { Text(acc.account.icon, fontSize = 22.sp) }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(acc.account.name, style = MaterialTheme.typography.labelLarge)
                Text(
                    "حقيقي: ${Money.fmt(acc.account.realBalance)} • ${acc.operations.size} عملية",
                    style = MaterialTheme.typography.bodySmall,
                    color = LocalAppColors.current.muted,
                )
            }
            val bal = acc.opsBalance
            Text(
                when {
                    bal > 0 -> "عليه ${Money.fmt(bal)}"
                    bal < 0 -> "له ${Money.fmt(-bal)}"
                    else -> "متساوي"
                },
                style = MaterialTheme.typography.labelMedium,
                color = when {
                    bal > 0 -> LocalAppColors.current.red
                    bal < 0 -> LocalAppColors.current.green
                    else -> LocalAppColors.current.muted
                },
            )
        }
    }
}

// =====================================================================
// 3) شاشة عمليات الحساب
// =====================================================================

@Composable
fun AccountOpsScreen(
    clientData: ClientWithData?,
    account: AccountWithOps?,
    allAccounts: List<Pair<Client, ClientAccount>>,
    transfers: List<TransferDisplay>,
    selection: Set<Long>,
    insufficientReal: InsufficientRealData?,
    pendingOp: com.mahfazty.smart.ui.flow.PendingClientOp?,
    settings: AppSettings,
    bank: Double,
    cash: Double,
    savings: Double,
    goalSources: List<Triple<Long, String, Double>>,
    toast: kotlinx.coroutines.flow.SharedFlow<String>,
    onBack: () -> Unit,
    onToggleSelect: (Long) -> Unit,
    onClearSelection: () -> Unit,
    onAddOperation: (OpType, Double, String?, List<com.mahfazty.smart.domain.model.MaterialItem>, String?, Boolean, String?) -> Unit,
    onUpdateOperation: (ClientOperation) -> Unit,
    onDeleteOperation: (ClientOperation) -> Unit,
    onDeleteSelected: () -> Unit,
    onMarkInvoiceDelivered: (ClientOperation) -> Unit,
    onUpdateAccount: (ClientAccount) -> Unit,
    onDeleteAccount: (Long) -> Unit,
    onFundReal: (Double, Wallet) -> Unit,
    onWithdrawReal: (Double, Wallet) -> Unit,
    onTransferReal: (Long, Long, Long, Long, Double) -> Unit,
    onQuickFundReal: (Wallet, Double) -> Unit,
    onQuickTransferReal: (Long, Double) -> Unit,
    onQuickFundSavings: (Double) -> Unit,
    onQuickFundGoal: (Long, String, Double) -> Unit,
    onDismissInsufficientReal: () -> Unit,
    onClearPendingOp: () -> Unit,
) {
    val context = LocalContext.current
    val currency = settings.currency
    val snackbar = remember { SnackbarHostState() }
    LaunchedEffect(Unit) { toast.collect { snackbar.showSnackbar(it) } }

    var showAddOp by remember { mutableStateOf(false) }
    var editingOp by remember { mutableStateOf<ClientOperation?>(null) }
    var deletingOp by remember { mutableStateOf<ClientOperation?>(null) }
    var showFund by remember { mutableStateOf(false) }
    var showWithdraw by remember { mutableStateOf(false) }
    var showTransfer by remember { mutableStateOf(false) }
    var showHistory by remember { mutableStateOf(false) }
    var editingAccount by remember { mutableStateOf(false) }
    var deletingAccount by remember { mutableStateOf(false) }
    // نظام «الفواتير وحالة التسليم»: قائمة تسليم الفاتورة + فلتر الفواتير المعلقة
    var invoiceMenuFor by remember { mutableStateOf<ClientOperation?>(null) }
    var pendingInvoicesOnly by remember { mutableStateOf(false) }

    val client = clientData?.client
    val acc = account?.account
    val ops = account?.operations ?: emptyList()
    // عداد حي للفواتير غير المسلمة (يُعرض في الفلتر)
    val pendingInvoicesCount = ops.count { it.isInvoice && !it.invoiceDelivered }
    // الفلتر يعرض الفواتير المعلقة فقط
    val shownOps = if (pendingInvoicesOnly) ops.filter { it.isInvoice && !it.invoiceDelivered } else ops

    Scaffold(
        snackbarHost = { SnackbarHost(snackbar) },
        floatingActionButton = {
            if (acc != null) {
                FloatingActionButton(
                    onClick = { showAddOp = true },
                    containerColor = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.bounceClick(),
                ) { Text("＋", fontSize = 22.sp) }
            }
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(bottom = 100.dp),
        ) {
            item {
                ElasticEntrance(0) {
                ScreenTopBar(
                    title = acc?.name ?: "تفاصيل",
                    onBack = onBack,
                    actions = {
                        IconButton(onClick = { editingAccount = true }) {
                            Text("✏️", fontSize = 18.sp)
                        }
                        IconButton(onClick = { deletingAccount = true }) {
                            Text("🗑️", fontSize = 18.sp)
                        }
                    },
                )
                }
            }
            // ===== كارت الرصيد الحقيقي =====
            item {
                ElasticEntrance(1) {
                AppCard(Modifier.padding(horizontal = 20.dp, vertical = 4.dp)) {
                    Column(Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            Column {
                                Text("💰 الرصيد الحقيقي لهذا الحساب", style = MaterialTheme.typography.labelMedium)
                                Text(
                                    "منفصل عن كل حساب آخر",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = LocalAppColors.current.muted,
                                )
                            }
                            Text("🔐", fontSize = 24.sp)
                        }
                        Spacer(Modifier.height(10.dp))
                        Text(
                            "${Money.fmt(acc?.realBalance ?: 0.0)} $currency",
                            style = MaterialTheme.typography.headlineMedium,
                            color = MaterialTheme.colorScheme.primary,
                        )
                        Text(
                            "يأتي من الشحن والسحب والتحويلات فقط",
                            style = MaterialTheme.typography.bodySmall,
                            color = LocalAppColors.current.muted,
                        )
                        Spacer(Modifier.height(12.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            RealAction("➕ شحن") { showFund = true }
                            RealAction("🔄 تحويل") { showTransfer = true }
                            RealAction("📋 سجل") { showHistory = true }
                            RealAction("➖ سحب") { showWithdraw = true }
                        }
                    }
                }
                }
            }
            // ===== زر واتساب =====
            item {
                ElasticEntrance(2) {
                Button(
                    onClick = {
                        val text = buildString {
                            append("🧾 كشف حساب: ${acc?.name ?: ""} ${acc?.icon ?: ""}\n")
                            append("👤 العميل: ${client?.name ?: ""}\n")
                            append("💰 الرصيد الحقيقي: ${Money.fmt(acc?.realBalance ?: 0.0)}\n")
                            val bal = account?.opsBalance ?: 0.0
                            if (bal > 0) append("🔴 عليه: ${Money.fmt(bal)}\n")
                            else if (bal < 0) append("🟢 له: ${Money.fmt(-bal)}\n")
                            append("\n📋 آخر العمليات:\n")
                            ops.sortedByDescending { it.date }.take(10).forEachIndexed { i, op ->
                                append("${i + 1}. ${if (op.type == OpType.DEBT) "عليه" else "له"} ${Money.fmt(op.amount)} - ${op.note ?: ""}\n")
                            }
                            append("\nمحفظتي الذكية 💰")
                        }
                        shareViaWhatsApp(context, text, client?.phone)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .bounceClick()
                        .padding(horizontal = 20.dp, vertical = 8.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF25D366)),
                    shape = RoundedCornerShape(14.dp),
                ) { Text("💬 مشاركة كشف الحساب عبر واتساب") }
                }
            }
            // ===== شريط الاختيار المتعدد =====
            item {
                AnimatedVisibility(
                    visible = selection.isNotEmpty(),
                    enter = expandVertically(tween(220)) + fadeIn(tween(220)),
                    exit = shrinkVertically(tween(180)) + fadeOut(tween(180)),
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            "${selection.size} عملية محددة",
                            style = MaterialTheme.typography.labelMedium,
                            modifier = Modifier.weight(1f),
                        )
                        Button(
                            onClick = {
                                val text = buildString {
                                    append("🧾 عمليات مختارة: ${acc?.name ?: ""}\n👤 ${client?.name ?: ""}\n\n")
                                    ops.filter { it.id in selection }.sortedByDescending { it.date }
                                        .forEachIndexed { i, op ->
                                            append("${i + 1}. ${if (op.type == OpType.DEBT) "عليه" else "له"} ${Money.fmt(op.amount)} - ${op.note ?: ""}\n")
                                        }
                                    append("\nمحفظتي الذكية 💰")
                                }
                                shareViaWhatsApp(context, text, client?.phone)
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF25D366)),
                            shape = RoundedCornerShape(12.dp),
                        ) { Text("💬 إرسال", fontSize = 12.sp) }
                        Button(
                            onClick = onDeleteSelected,
                            colors = ButtonDefaults.buttonColors(containerColor = LocalAppColors.current.red),
                            shape = RoundedCornerShape(12.dp),
                        ) { Text("🗑️ حذف", fontSize = 12.sp) }
                        Button(
                            onClick = onClearSelection,
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                            shape = RoundedCornerShape(12.dp),
                        ) { Text("إلغاء", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant) }
                    }
                }
            }
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 20.dp, end = 20.dp, top = 8.dp, bottom = 2.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        "العمليات (دين / سداد)",
                        style = MaterialTheme.typography.labelMedium,
                        color = LocalAppColors.current.muted,
                        modifier = Modifier.weight(1f),
                    )
                }
            }
            if (ops.isEmpty()) {
                item { EmptyState("📋", "لا توجد عمليات بعد") }
            } else {
                // ===== فلتر «الفواتير غير المسلمة» مع عداد حي =====
                item {
                    Row(
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        val active = pendingInvoicesOnly
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(20.dp))
                                .background(
                                    if (active) InvoiceOrange.copy(alpha = 0.18f)
                                    else MaterialTheme.colorScheme.surfaceVariant,
                                )
                                .border(
                                    width = if (active) 1.5.dp else 1.dp,
                                    color = if (active) InvoiceOrange
                                    else LocalAppColors.current.border,
                                    shape = RoundedCornerShape(20.dp),
                                )
                                .clickable { pendingInvoicesOnly = !active }
                                .padding(horizontal = 12.dp, vertical = 6.dp),
                        ) {
                            Text(
                                "🧾 فواتير غير مسلمة ($pendingInvoicesCount)",
                                style = MaterialTheme.typography.labelMedium,
                                color = if (active) InvoiceOrange else LocalAppColors.current.muted,
                            )
                        }
                    }
                }
                if (shownOps.isEmpty() && pendingInvoicesOnly) {
                    item { EmptyState("🧾", "لا توجد فواتير معلقة — كلها سُلّمت 🎉") }
                } else {
                    items(shownOps, key = { it.id }) { op ->
                        ElasticEntrance(3) {
                        Box(Modifier.animateItem()) {
                            OpRow(
                                op = op,
                                currency = currency,
                                selected = op.id in selection,
                                onClick = {
                                    if (selection.isNotEmpty()) onToggleSelect(op.id)
                                    else editingOp = op
                                },
                                onLongClick = {
                                    // ضغطة مطولة على فاتورة غير مسلمة: قائمة تسليم الفاتورة
                                    if (op.isInvoice && !op.invoiceDelivered) invoiceMenuFor = op
                                    else onToggleSelect(op.id)
                                },
                            )
                            DropdownMenu(
                                expanded = invoiceMenuFor?.id == op.id,
                                onDismissRequest = { invoiceMenuFor = null },
                            ) {
                                DropdownMenuItem(
                                    text = { Text("✅ تسليم الفاتورة") },
                                    onClick = {
                                        invoiceMenuFor = null
                                        onMarkInvoiceDelivered(op)
                                    },
                                )
                                DropdownMenuItem(
                                    text = { Text("🔀 تحديد العملية") },
                                    onClick = {
                                        invoiceMenuFor = null
                                        onToggleSelect(op.id)
                                    },
                                )
                                DropdownMenuItem(
                                    text = { Text("✏️ تعديل العملية") },
                                    onClick = {
                                        invoiceMenuFor = null
                                        editingOp = op
                                    },
                                )
                            }
                        }
                        }
                    }
                }
            }
        }
    }

    // ===== النوافذ =====
    val showOpForm = insufficientReal == null && (showAddOp || editingOp != null || pendingOp != null)
    if (showOpForm) {
        val editing = pendingOp?.editing ?: editingOp
        val initial = pendingOp?.toInitial() ?: editing
        OperationDialog(
            title = if (editing != null) "تعديل العملية" else "عملية له/عليه",
            currency = currency,
            realBalance = if (editing != null) {
                (acc?.realBalance ?: 0.0) + (if (editing.type == OpType.DEBT) editing.amount else -editing.amount)
            } else (acc?.realBalance ?: 0.0),
            initial = initial,
            onDismiss = {
                showAddOp = false
                editingOp = null
                onClearPendingOp()
            },
            onSave = { type, amount, note, materials, receipt, isInvoice, invoiceRef ->
                showAddOp = false
                editingOp = null
                if (editing != null) {
                    onUpdateOperation(
                        editing.copy(
                            type = type, amount = amount, note = note,
                            materials = materials, receiptPath = receipt,
                            isInvoice = isInvoice, invoiceRef = invoiceRef,
                            // عند إلغاء صفة «فاتورة» تُصفَّر حالة التسليم أيضاً
                            invoiceDelivered = if (isInvoice) editing.invoiceDelivered else false,
                        ),
                    )
                } else {
                    onAddOperation(type, amount, note, materials, receipt, isInvoice, invoiceRef)
                }
            },
        )
    }
    deletingOp?.let { op ->
        ConfirmDialog(
            title = "حذف العملية؟",
            message = "سيُعكس أثر العملية على الرصيد الحقيقي.",
            onConfirm = { deletingOp = null; onDeleteOperation(op) },
            onDismiss = { deletingOp = null },
        )
    }
    if (showFund) {
        FundRealDialog(
            currency = currency,
            bank = bank,
            cash = cash,
            bankName = settings.bankName,
            cashName = settings.cashName,
            onDismiss = { showFund = false },
            onSave = { amount, from -> showFund = false; onFundReal(amount, from) },
        )
    }
    if (showWithdraw) {
        WithdrawRealDialog(
            currency = currency,
            realBalance = acc?.realBalance ?: 0.0,
            onDismiss = { showWithdraw = false },
            onSave = { amount, to -> showWithdraw = false; onWithdrawReal(amount, to) },
        )
    }
    if (showTransfer) {
        TransferRealDialog(
            currency = currency,
            sources = allAccounts.filter { it.second.id != (acc?.id ?: -1) && it.second.realBalance > 0 },
            savings = savings,
            goals = goalSources,
            onDismiss = { showTransfer = false },
            onSave = { fromClientId, fromAccountId, amount ->
                showTransfer = false
                onTransferReal(fromClientId, fromAccountId, client?.id ?: -1, acc?.id ?: -1, amount)
            },
            onFromSavings = { amount ->
                showTransfer = false
                onQuickFundSavings(amount)
            },
            onFromGoal = { id, name, amount ->
                showTransfer = false
                onQuickFundGoal(id, name, amount)
            },
        )
    }
    if (showHistory) {
        HistoryDialog(
            transfers = transfers,
            currency = currency,
            onDismiss = { showHistory = false },
        )
    }
    if (editingAccount && acc != null) {
        AccountDialog(
            title = "تعديل الحساب",
            initial = acc,
            onDismiss = { editingAccount = false },
            onSave = { name, icon ->
                editingAccount = false
                onUpdateAccount(acc.copy(name = name, icon = icon))
            },
        )
    }
    if (deletingAccount && acc != null) {
        ConfirmDialog(
            title = "حذف الحساب؟",
            message = "سيُحذف الحساب وكل عملياته.",
            onConfirm = { deletingAccount = false; onDeleteAccount(acc.id); onBack() },
            onDismiss = { deletingAccount = false },
        )
    }
    insufficientReal?.let { data ->
        InsufficientRealSheet(
            data = data,
            currency = currency,
            onClose = onDismissInsufficientReal,
            onFund = onQuickFundReal,
            onTransfer = onQuickTransferReal,
        )
    }
}

@Composable
private fun RealAction(label: String, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(10.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .clickable(onClick = onClick)
            .bounceClick()
            .padding(horizontal = 10.dp, vertical = 8.dp),
    ) {
        Text(label, style = MaterialTheme.typography.labelSmall)
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun OpRow(
    op: ClientOperation,
    currency: String,
    selected: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
) {
    val primary = MaterialTheme.colorScheme.primary
    val transition = updateTransition(targetState = selected, label = "opRow")
    val rowBg by transition.animateColor(label = "opRowBg") { if (it) primary.copy(alpha = 0.08f) else Color.Transparent }
    val rowScale by transition.animateFloat(label = "opRowScale") { if (it) 1.012f else 1f }
    AppCard(Modifier.padding(horizontal = 20.dp, vertical = 4.dp)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .graphicsLayer {
                    scaleX = rowScale
                    scaleY = rowScale
                }
                .background(rowBg)
                .combinedClickable(onClick = onClick, onLongClick = onLongClick)
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (selected) {
                Text("✓", color = MaterialTheme.colorScheme.primary, fontSize = 18.sp)
                Spacer(Modifier.width(8.dp))
            }
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(
                                if (op.type == OpType.DEBT) LocalAppColors.current.red.copy(alpha = 0.12f)
                                else LocalAppColors.current.green.copy(alpha = 0.12f),
                            )
                            .padding(horizontal = 8.dp, vertical = 3.dp),
                    ) {
                        Text(
                            if (op.type == OpType.DEBT) "🔴 عليه (دين)" else "🟢 له (سداد)",
                            style = MaterialTheme.typography.labelSmall,
                            color = if (op.type == OpType.DEBT) LocalAppColors.current.red else LocalAppColors.current.green,
                        )
                    }
                    // ===== شارة الفاتورة وحالة التسليم (نظام «الفواتير») =====
                    if (op.isInvoice) {
                        Spacer(Modifier.width(8.dp))
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(
                                    if (op.invoiceDelivered) LocalAppColors.current.green.copy(alpha = 0.15f)
                                    else InvoiceOrange.copy(alpha = 0.15f),
                                )
                                .padding(horizontal = 8.dp, vertical = 3.dp),
                        ) {
                            Text(
                                if (op.invoiceDelivered) "✅📑 تم التسليم" else "🧾 غير مسلمة",
                                style = MaterialTheme.typography.labelSmall,
                                color = if (op.invoiceDelivered) LocalAppColors.current.green else InvoiceOrange,
                            )
                        }
                    }
                    Spacer(Modifier.width(8.dp))
                    if (op.materials.isNotEmpty()) {
                        Text("📦 ${op.materials.size} صنف", style = MaterialTheme.typography.labelSmall, color = LocalAppColors.current.muted)
                    }
                    if (op.receiptPath != null) {
                        Spacer(Modifier.width(6.dp))
                        Text("📷", fontSize = 12.sp)
                    }
                }
                Spacer(Modifier.height(4.dp))
                if (!op.note.isNullOrBlank()) {
                    Text(op.note, style = MaterialTheme.typography.bodySmall, color = LocalAppColors.current.muted, maxLines = 1)
                }
                // رقم/وصف الفاتورة — يظهر تحت البيان بلون الفاتورة
                if (op.isInvoice && !op.invoiceRef.isNullOrBlank()) {
                    Text(
                        "🧾 ${op.invoiceRef}",
                        style = MaterialTheme.typography.labelSmall,
                        color = if (op.invoiceDelivered) LocalAppColors.current.green else InvoiceOrange,
                        maxLines = 1,
                    )
                }
                Text(
                    Dates.dateTime(op.date),
                    style = MaterialTheme.typography.labelSmall,
                    color = LocalAppColors.current.muted,
                )
            }
            Text(
                Money.fmt(op.amount),
                style = MaterialTheme.typography.titleMedium,
                color = if (op.type == OpType.DEBT) LocalAppColors.current.red else LocalAppColors.current.green,
            )
        }
    }
}

@Composable
private fun ScreenTopBar(
    title: String,
    onBack: () -> Unit,
    actions: @Composable () -> Unit = {},
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(onClick = onBack) {
            Text("🔙", fontSize = 18.sp)
        }
        Text(title, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
        actions()
    }
}
