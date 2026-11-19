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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
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
import com.mahfazty.smart.ui.components.AnimatedNumber
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
import com.mahfazty.smart.ui.dialogs.AdjustRealDialog
import com.mahfazty.smart.ui.dialogs.AuditLogDialog
import com.mahfazty.smart.ui.dialogs.StatementOptionsDialog
import com.mahfazty.smart.ui.theme.LocalAppColors
import com.mahfazty.smart.ui.util.shareViaWhatsApp
import com.mahfazty.smart.ui.viewmodels.ClientsUiState
import com.mahfazty.smart.ui.viewmodels.InsufficientRealData
import java.util.Calendar

/** لون شارات الفواتير غير المسلمة (برتقالي) — نظام «الفواتير وحالة التسليم» */
private val InvoiceOrange = Color(0xFFF57C00)

// =====================================================================
// 1) شاشة قائمة العملاء
// =====================================================================

@Composable
fun ClientsScreen(
    state: ClientsUiState,
    toast: kotlinx.coroutines.flow.SharedFlow<com.mahfazty.smart.ui.viewmodels.ToastMsg>,
    /** إضافة 3.5: عملة العرض في نافذة المستحقات */
    currency: String,
    onSetQuery: (String) -> Unit,
    onAddClient: (String, String?, String?, String) -> Unit,
    onOpenClient: (Long) -> Unit,
    /** إضافة 5.3 من تقرير الفحص: فلتر حالة العميل */
    onSetStatusFilter: (com.mahfazty.smart.ui.viewmodels.ClientStatusFilter) -> Unit,
) {
    var showDueDebts by remember { mutableStateOf(false) }
    val snackbar = remember { SnackbarHostState() }
    LaunchedEffect(Unit) {
        toast.collect { msg ->
            if (msg.actionLabel != null && msg.onAction != null) {
                val result = snackbar.showSnackbar(
                    message = msg.text,
                    actionLabel = msg.actionLabel,
                    duration = SnackbarDuration.Long,
                )
                if (result == SnackbarResult.ActionPerformed) msg.onAction?.invoke()
            } else snackbar.showSnackbar(msg.text)
        }
    }
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
            // ===== إضافة 5.3/5.4 من تقرير الفحص: فلتر الحالة مع عدادات كل حالة =====
            item {
                ElasticEntrance(3) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    StatusFilterChip("الكل", state.statusFilter == com.mahfazty.smart.ui.viewmodels.ClientStatusFilter.ALL) {
                        onSetStatusFilter(com.mahfazty.smart.ui.viewmodels.ClientStatusFilter.ALL)
                    }
                    StatusFilterChip("🟢 نشط (${state.activeCount})", state.statusFilter == com.mahfazty.smart.ui.viewmodels.ClientStatusFilter.ACTIVE) {
                        onSetStatusFilter(com.mahfazty.smart.ui.viewmodels.ClientStatusFilter.ACTIVE)
                    }
                    StatusFilterChip("🟠 موقوف (${state.stoppedCount})", state.statusFilter == com.mahfazty.smart.ui.viewmodels.ClientStatusFilter.STOPPED) {
                        onSetStatusFilter(com.mahfazty.smart.ui.viewmodels.ClientStatusFilter.STOPPED)
                    }
                    StatusFilterChip("🔴 قائمة سوداء (${state.blockedCount})", state.statusFilter == com.mahfazty.smart.ui.viewmodels.ClientStatusFilter.BLOCKED) {
                        onSetStatusFilter(com.mahfazty.smart.ui.viewmodels.ClientStatusFilter.BLOCKED)
                    }
                }
                }
            }
            // ===== إضافة 3.5 من تقرير الفحص: زر الديون المستحقة/المتأخرة عبر كل العملاء =====
            if (state.dueDebts.isNotEmpty()) {
                item {
                    ElasticEntrance(4) {
                    Row(
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp),
                    ) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(20.dp))
                                .background(LocalAppColors.current.red.copy(alpha = 0.12f))
                                .border(1.5.dp, LocalAppColors.current.red, RoundedCornerShape(20.dp))
                                .clickable { showDueDebts = true }
                                .padding(horizontal = 14.dp, vertical = 8.dp),
                        ) {
                            Text(
                                "⏰ ديون مستحقة/متأخرة (${state.dueDebts.size})",
                                style = MaterialTheme.typography.labelMedium,
                                color = LocalAppColors.current.red,
                            )
                        }
                    }
                    }
                }
            }
            if (state.clients.isEmpty()) {
                // تحسين بيانات: تمييز «لا نتائج للبحث/الفلتر» عن «لا يوجد عملاء أصلاً»
                val filtering = state.query.isNotBlank() ||
                    state.statusFilter != com.mahfazty.smart.ui.viewmodels.ClientStatusFilter.ALL
                item {
                    EmptyState(
                        if (filtering) "🔍" else "👥",
                        if (filtering) "لا توجد نتائج مطابقة لبحثك أو الفلتر الحالي"
                        else "لا يوجد عملاء — أضف عميلك الأول",
                    )
                }
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
            existing = state.clients.map { it.client },
            onOpenExisting = onOpenClient,
            onDismiss = { showAdd = false },
            onSave = { name, phone, photo, status ->
                showAdd = false
                onAddClient(name, phone, photo, status)
            },
        )
    }

    // ===== إضافة 3.5 من تقرير الفحص: نافذة الديون المستحقة/المتأخرة عبر كل العملاء =====
    if (showDueDebts) {
        DueDebtsDialog(debts = state.dueDebts, currency = currency, onDismiss = { showDueDebts = false })
    }
}

/** إضافة 5.3: رقاقة فلتر حالة العميل */
@Composable
private fun StatusFilterChip(label: String, selected: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(
                if (selected) MaterialTheme.colorScheme.primary.copy(alpha = 0.14f)
                else MaterialTheme.colorScheme.surfaceVariant,
            )
            .border(
                width = if (selected) 1.5.dp else 1.dp,
                color = if (selected) MaterialTheme.colorScheme.primary else LocalAppColors.current.border,
                shape = RoundedCornerShape(20.dp),
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 6.dp),
    ) {
        Text(
            label,
            style = MaterialTheme.typography.labelSmall,
            color = if (selected) MaterialTheme.colorScheme.primary else LocalAppColors.current.muted,
        )
    }
}

/** إضافة 3.5: نافذة تجميعية لكل الديون المستحقة/المتأخرة (عميل • حساب • مبلغ • استحقاق) */
@Composable
private fun DueDebtsDialog(
    debts: List<com.mahfazty.smart.ui.viewmodels.DueDebtSummary>,
    currency: String,
    onDismiss: () -> Unit,
) {
    com.mahfazty.smart.ui.components.AppDialog(onDismiss = onDismiss) {
        Column(Modifier.padding(20.dp)) {
            Text("⏰ الديون المستحقة والمتأخرة", style = MaterialTheme.typography.titleMedium)
            Text(
                "${debts.size} دين — من كل العملاء، الأقرب استحقاقاً أولاً",
                style = MaterialTheme.typography.labelSmall,
                color = LocalAppColors.current.muted,
                modifier = Modifier.padding(vertical = 6.dp),
            )
            Spacer(Modifier.height(4.dp))
            Column(
                Modifier
                    .heightIn(max = 380.dp)
                    .verticalScroll(rememberScrollState()),
            ) {
                debts.forEach { d ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text(
                                "${d.clientName} • ${d.accountName}",
                                style = MaterialTheme.typography.labelMedium,
                                maxLines = 1,
                            )
                            Text(
                                if (d.overdue) "⚠️ متأخرة منذ ${com.mahfazty.smart.domain.Dates.short(d.dueDate)}"
                                else "تستحق ${com.mahfazty.smart.domain.Dates.short(d.dueDate)}",
                                style = MaterialTheme.typography.labelSmall,
                                color = if (d.overdue) LocalAppColors.current.red else LocalAppColors.current.muted,
                            )
                        }
                        Text(
                            "${Money.fmt(d.amount)} $currency",
                            style = MaterialTheme.typography.labelLarge,
                            color = if (d.overdue) LocalAppColors.current.red else MaterialTheme.colorScheme.primary,
                        )
                    }
                }
            }
            Spacer(Modifier.height(10.dp))
            Button(
                onClick = onDismiss,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
            ) { Text("إغلاق") }
        }
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

/**
 * 💠 بطاقة الرصيد العادي (كشف عليه/له) — كم بقي على العميل أو له الآن.
 * منفصلة تماماً عن «الرصيد الحقيقي» (الذي يأتي من الشحن/السحب/التحويل فقط).
 *
 * الرقم يعدّ تصاعدياً (AnimatedNumber) عند أي إضافة/حذف/تعديل عملية، ولون
 * الحالة يتبدل فورياً: أحمر «عليه» (دين) / أخضر «له» / محايد «متساوي».
 */
@Composable
private fun ClientOpsBalanceCard(balance: Double, currency: String) {
    val colors = LocalAppColors.current
    val isDebt = balance > 0.0001
    val isPay = balance < -0.0001
    val accent = when {
        isDebt -> colors.red
        isPay -> colors.green
        else -> colors.muted
    }
    val stateText = when {
        isDebt -> "🔴 عليه (دين)"
        isPay -> "🟢 له"
        else -> "⚖️ متساوي"
    }
    AppCard(Modifier.padding(horizontal = 20.dp, vertical = 4.dp)) {
        Column(Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("💠 الرصيد الحالي (عليه / له)", style = MaterialTheme.typography.labelMedium)
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .background(accent.copy(alpha = 0.14f))
                        .padding(horizontal = 10.dp, vertical = 4.dp),
                ) {
                    Text(stateText, style = MaterialTheme.typography.labelSmall, color = accent)
                }
            }
            Spacer(Modifier.height(8.dp))
            AnimatedNumber(
                target = kotlin.math.abs(balance),
                format = { "${Money.fmt(it)} $currency" },
                style = MaterialTheme.typography.headlineMedium.copy(color = accent),
            )
            Spacer(Modifier.height(4.dp))
            Text(
                "الرصيد العادي من عمليات الدين/السداد — منفصل عن الرصيد الحقيقي 🔐",
                style = MaterialTheme.typography.bodySmall,
                color = colors.muted,
            )
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
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        c.client.name,
                        style = MaterialTheme.typography.labelLarge,
                        modifier = Modifier.weight(1f, fill = false),
                        maxLines = 1,
                    )
                    // إصلاح data-3: شارة حالة العميل (نشط/موقوف/قائمة سوداء)
                    if (c.client.status != com.mahfazty.smart.domain.model.ClientStatus.ACTIVE) {
                        Spacer(Modifier.width(6.dp))
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(
                                    if (c.client.status == com.mahfazty.smart.domain.model.ClientStatus.BLOCKED)
                                        LocalAppColors.current.red.copy(alpha = 0.15f)
                                    else Color(0xFFE17055).copy(alpha = 0.18f),
                                )
                                .padding(horizontal = 6.dp, vertical = 2.dp),
                        ) {
                            Text(
                                "${com.mahfazty.smart.domain.model.ClientStatus.icon(c.client.status)}" +
                                    " ${com.mahfazty.smart.domain.model.ClientStatus.label(c.client.status)}",
                                style = MaterialTheme.typography.labelSmall,
                                color = if (c.client.status == com.mahfazty.smart.domain.model.ClientStatus.BLOCKED)
                                    LocalAppColors.current.red
                                else Color(0xFFE17055),
                            )
                        }
                    }
                }
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
    toast: kotlinx.coroutines.flow.SharedFlow<com.mahfazty.smart.ui.viewmodels.ToastMsg>,
    /** سجل التحويلات — لإظهار أثر الحذف في تأكيد الحذف (إصلاح fin-3) */
    transfers: List<TransferDisplay>,
    /** نتيجة حذف العميل (لوحة تراجع/عودة — إصلاح act-2) */
    deleted: com.mahfazty.smart.ui.viewmodels.ClientAccountsViewModel.DeletedClientInfo?,
    onUndoDeleteClient: () -> Unit,
    onCloseDeleteResult: () -> Unit,
    onBack: () -> Unit,
    onAddAccount: (String, String) -> Unit,
    onUpdateClient: (Client) -> Unit,
    onDeleteClient: (Long) -> Unit,
    onUpdateAccount: (ClientAccount) -> Unit,
    onDeleteAccount: (Long) -> Unit,
    /** إضافة 12.2 من تقرير الفحص: سحب الرصيد الحقيقي ثم حذف الحساب */
    onWithdrawAndDeleteAccount: (Long) -> Unit,
    onOpenAccount: (Long) -> Unit,
) {
    val snackbar = remember { SnackbarHostState() }
    LaunchedEffect(Unit) {
        toast.collect { msg ->
            if (msg.actionLabel != null && msg.onAction != null) {
                val result = snackbar.showSnackbar(
                    message = msg.text,
                    actionLabel = msg.actionLabel,
                    duration = SnackbarDuration.Long,
                )
                if (result == SnackbarResult.ActionPerformed) msg.onAction?.invoke()
            } else snackbar.showSnackbar(msg.text)
        }
    }
    var showAddAccount by remember { mutableStateOf(false) }
    var editingClient by remember { mutableStateOf(false) }
    var editingAccount by remember { mutableStateOf<ClientAccount?>(null) }
    var deletingClient by remember { mutableStateOf(false) }
    var deletingAccount by remember { mutableStateOf<ClientAccount?>(null) }

    val client = data?.client

    // ===== إصلاح act-2: لوحة نتيجة حذف العميل مع تراجع حقيقي =====
    if (deleted != null) {
        Scaffold(snackbarHost = { SnackbarHost(snackbar) }) { _ ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                Text("🗑️", fontSize = 48.sp)
                Spacer(Modifier.height(12.dp))
                Text("تم حذف «${deleted.name}»", style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(6.dp))
                Text(
                    "حُذف مع كل حساباته وعملياته. يمكنك التراجع خلال ثوانٍ.",
                    style = MaterialTheme.typography.bodySmall,
                    color = LocalAppColors.current.muted,
                )
                Spacer(Modifier.height(20.dp))
                Button(
                    onClick = onUndoDeleteClient,
                    modifier = Modifier.fillMaxWidth().bounceClick(),
                    shape = RoundedCornerShape(14.dp),
                ) { Text("↩️ تراجع — استعادة العميل") }
                Spacer(Modifier.height(10.dp))
                Button(
                    onClick = {
                        onCloseDeleteResult()
                        onBack()
                    },
                    modifier = Modifier.fillMaxWidth().bounceClick(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = LocalAppColors.current.red,
                    ),
                    shape = RoundedCornerShape(14.dp),
                ) { Text("العودة إلى قائمة العملاء") }
            }
        }
        return
    }

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
            onSave = { name, phone, photo, status ->
                editingClient = false
                onUpdateClient(client.copy(name = name, phone = phone, photoPath = photo, status = status))
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
    // إصلاح fin-2/fin-3: تحذير صريح بالمبلغ الحقيقي المتبقي + أثر الحذف على سجل التحويلات
    if (deletingClient && client != null) {
        val realTotal = data?.accounts?.sumOf { it.account.realBalance } ?: 0.0
        val myAccountIds = data?.accounts?.map { it.account.id }?.toSet() ?: emptySet()
        val transferCount = transfers.count {
            it.transfer.fromAccountId in myAccountIds || it.transfer.toAccountId in myAccountIds
        }
        val msg = buildString {
            append("سيُحذف العميل وكل حساباته وعملياته نهائياً.")
            if (realTotal > 0) {
                append("\n\n⚠️ ⚠️ تحذير: يحتوي حساباته رصيدًا حقيقيًا متبقيًا قدره ${Money.fmt(realTotal)}.")
                append("\nسيختفي هذا المبلغ من التتبع نهائيًا دون عودة تلقائية للبنك أو الكاش.")
                append("\nاسحبه أولاً من شاشة الحساب إن أردت استرداده.")
            }
            if (transferCount > 0) {
                append("\n\nلديه $transferCount تحويل رصيد سابق — ستبقى في سجل التحويلات باسم «عميل محذوف».")
            }
        }
        ConfirmDialog(
            title = "حذف العميل؟",
            message = msg,
            onConfirm = { deletingClient = false; onDeleteClient(client.id) },
            onDismiss = { deletingClient = false },
        )
    }
    deletingAccount?.let { acc ->
        val transferCount = transfers.count {
            it.transfer.fromAccountId == acc.id || it.transfer.toAccountId == acc.id
        }
        val msg = buildString {
            append("سيُحذف الحساب وكل عملياته.")
            if (acc.realBalance > 0) {
                append("\n\n⚠️ ⚠️ تحذير: يحتوي رصيدًا حقيقيًا متبقيًا قدره ${Money.fmt(acc.realBalance)}.")
                append("\nسيختفي هذا المبلغ من التتبع نهائيًا دون عودة للبنك أو الكاش.")
                append("\nاسحبه أولاً إن أردت استرداده.")
            }
            if (transferCount > 0) {
                append("\n\nلديه $transferCount تحويل سابق — ستبقى في سجل التحويلات باسم «حساب محذوف».")
            }
        }
        ConfirmDialog(
            title = "حذف الحساب؟",
            message = msg,
            onConfirm = { deletingAccount = null; onDeleteAccount(acc.id) },
            onDismiss = { deletingAccount = null },
            // إضافة 12.2 من تقرير الفحص: سحب الرصيد إلى البنك ثم الحذف — دفعة واحدة
            extraText = if (acc.realBalance > 0) "💵 سحب ثم حذف" else null,
            onExtra = if (acc.realBalance > 0) {
                { deletingAccount = null; onWithdrawAndDeleteAccount(acc.id) }
            } else null,
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

/**
 * تأكيد تنفيذ عملية رصيد حقيقي (إصلاح act-1).
 * الشحن/السحب/التحويل لم يعد يُنفذ بضغطة واحدة.
 */
private sealed class RealBalanceConfirm {
    data class Fund(val amount: Double, val from: Wallet) : RealBalanceConfirm()
    data class Withdraw(val amount: Double, val to: Wallet) : RealBalanceConfirm()
    data class Transfer(
        val fromClientId: Long,
        val fromAccountId: Long,
        val fromAccountName: String,
        val amount: Double,
    ) : RealBalanceConfirm()
}

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
    /** سجل التدقيق (إصلاح sec-6) */
    audit: List<com.mahfazty.smart.data.AuditLogEntry>,
    toast: kotlinx.coroutines.flow.SharedFlow<com.mahfazty.smart.ui.viewmodels.ToastMsg>,
    // فلاتر البحث في العمليات (إصلاح act-3)
    filterQuery: String,
    filterType: com.mahfazty.smart.ui.viewmodels.OpTypeFilter,
    filterPeriod: com.mahfazty.smart.ui.viewmodels.OpPeriod,
    onSetFilterQuery: (String) -> Unit,
    onSetFilterType: (com.mahfazty.smart.ui.viewmodels.OpTypeFilter) -> Unit,
    onSetFilterPeriod: (com.mahfazty.smart.ui.viewmodels.OpPeriod) -> Unit,
    /** إضافة 3.1 من تقرير الفحص: فلتر الديون المستحقة/المتأخرة */
    dueOnly: Boolean,
    onSetDueOnly: (Boolean) -> Unit,
    /** إضافة 15.3 من تقرير الفحص: ترتيب عمليات الحساب */
    sortMode: com.mahfazty.smart.ui.viewmodels.OpSortMode,
    onSetSortMode: (com.mahfazty.smart.ui.viewmodels.OpSortMode) -> Unit,
    onBack: () -> Unit,
    onToggleSelect: (Long) -> Unit,
    onClearSelection: () -> Unit,
    onAddOperation: (OpType, Double, String?, List<com.mahfazty.smart.domain.model.MaterialItem>, String?, Boolean, String?, Long?, String) -> Unit,
    onUpdateOperation: (ClientOperation) -> Unit,
    onDeleteOperation: (ClientOperation) -> Unit,
    onDeleteSelected: () -> Unit,
    onMarkInvoiceDelivered: (ClientOperation) -> Unit,
    onUpdateAccount: (ClientAccount) -> Unit,
    onDeleteAccount: (Long) -> Unit,
    /** إضافة 12.2 من تقرير الفحص: سحب الرصيد الحقيقي ثم حذف الحساب */
    onWithdrawAndDeleteAccount: (Long) -> Unit,
    /** إصلاح fin-4: تسوية رصيد حقيقي (مبلغ، سبب) */
    onAdjustReal: (Double, String) -> Unit,
    /** إصلاح act-4: تصدير كشف كامل CSV */
    onExportCsv: () -> Unit,
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
    LaunchedEffect(Unit) {
        toast.collect { msg ->
            if (msg.actionLabel != null && msg.onAction != null) {
                val result = snackbar.showSnackbar(
                    message = msg.text,
                    actionLabel = msg.actionLabel,
                    duration = SnackbarDuration.Long,
                )
                if (result == SnackbarResult.ActionPerformed) msg.onAction?.invoke()
            } else snackbar.showSnackbar(msg.text)
        }
    }

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
    // إصلاحات v2.7.0
    var realConfirm by remember { mutableStateOf<RealBalanceConfirm?>(null) } // act-1
    var showAdjust by remember { mutableStateOf(false) }                      // fin-4
    var showAudit by remember { mutableStateOf(false) }                        // sec-6
    var showStatement by remember { mutableStateOf(false) }                    // act-4

    val client = clientData?.client
    val acc = account?.account
    val ops = account?.operations ?: emptyList()
    // عداد حي للفواتير غير المسلمة (يُعرض في الفلتر)
    val pendingInvoicesCount = ops.count { it.isInvoice && !it.invoiceDelivered }
    // 💠 الرصيد العادي الجاري بعد كل عملية — يُظهر كيف يتنقل الرصيد بين العمليات
    // (مرتّب زمنياً بصرف النظر عن ترتيب العرض/الفلاتر، لأنه حالة الرصيد بعد كل عملية)
    val runningAfterMap = remember(ops) {
        com.mahfazty.smart.domain.WalletEngine.runningOpsBalance(ops)
    }

    // ===== إصلاح act-3: بحث + فلتر نوع + فلتر مدى زمني =====
    val now = System.currentTimeMillis()

    // ===== إضافة 3.1 من تقرير الفحص: عداد الديون المستحقة/المتأخرة =====
    val endOfToday = Calendar.getInstance().apply {
        timeInMillis = now
        add(Calendar.DAY_OF_YEAR, 1)
        set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
    }.timeInMillis
    val dueCount = ops.count { it.type == OpType.DEBT && it.dueDate != null && it.dueDate < endOfToday }

    val dayMs = 86_400_000L
    val periodCutoff: Long? = when (filterPeriod) {
        com.mahfazty.smart.ui.viewmodels.OpPeriod.ALL -> null
        com.mahfazty.smart.ui.viewmodels.OpPeriod.MONTH -> Calendar.getInstance().apply {
            timeInMillis = now
            set(Calendar.DAY_OF_MONTH, 1)
            set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
        }.timeInMillis
        com.mahfazty.smart.ui.viewmodels.OpPeriod.THREE_MONTHS -> now - 90 * dayMs
        com.mahfazty.smart.ui.viewmodels.OpPeriod.YEAR -> now - 365 * dayMs
    }
    val queryFiltered = ops.filter { op ->
        val q = filterQuery.trim()
        val matchesQuery = q.isEmpty() ||
            op.note?.contains(q, ignoreCase = true) == true ||
            op.invoiceRef?.contains(q, ignoreCase = true) == true ||
            op.materials.any { it.name.contains(q, ignoreCase = true) }
        val matchesType = when (filterType) {
            com.mahfazty.smart.ui.viewmodels.OpTypeFilter.All -> true
            com.mahfazty.smart.ui.viewmodels.OpTypeFilter.Debt -> op.type == OpType.DEBT
            com.mahfazty.smart.ui.viewmodels.OpTypeFilter.Pay -> op.type == OpType.PAY
        }
        val matchesPeriod = periodCutoff == null || op.date >= periodCutoff
        matchesQuery && matchesType && matchesPeriod
    }
    // ===== إضافة 3.1/3.2/15.3 من تقرير الفحص: فلتر الاستحقاق + الترتيب =====
    // فلتر «مستحق»: ديون عليه بتاريخ استحقاق حان أو تجاوز (الأقرب استحقاقاً أولاً — 3.2)
    val dueFiltered = if (dueOnly) {
        queryFiltered.filter { it.type == OpType.DEBT && it.dueDate != null && it.dueDate < endOfToday }.sortedBy { it.dueDate }
    } else queryFiltered
    // ترتيب عام (15.3): الافتراضي «الأحدث أولاً» — العملية الجديدة تظهر في الأعلى
    // والقديمة تنزل تحتها بعد كل عملية تُضاف (إصلاح: كان يعتمد على ترتيب الاستعلام
    // وهو ترتيب الإدخال = الأقدم أولاً، فتظهر العمليات الجديدة في الأسفل).
    // عند تساوي التاريخ (عمليتان في اللحظة نفسها) نحتكم إلى id: الأحدث (الأكبر) أولاً.
    val sortedOps = when {
        dueOnly -> dueFiltered
        sortMode == com.mahfazty.smart.ui.viewmodels.OpSortMode.AMOUNT_DESC -> dueFiltered.sortedByDescending { it.amount }
        else -> dueFiltered.sortedWith(
            compareByDescending<ClientOperation> { it.date }.thenByDescending { it.id },
        )
    }
    // فواتير معلقة فقط (فلتر أصلي) فوق نتائج البحث
    val shownOps = if (pendingInvoicesOnly) sortedOps.filter { it.isInvoice && !it.invoiceDelivered } else sortedOps

    /** إصلاح act-4: بناء نص الكشف — آخر 10 أو كامل */
    fun buildStatement(limit: Int): String {
        val a = account ?: return ""
        val c = clientData?.client ?: return ""
        val bal = a.opsBalance
        val real = a.account.realBalance
        val sb = StringBuilder()
        sb.append("🧾 كشف حساب: ${a.account.name} ${a.account.icon}\n")
        sb.append("👤 العميل: ${c.name}\n")
        sb.append("💰 الرصيد الحقيقي: ${Money.fmt(real)}\n")
        if (bal > 0) sb.append("🔴 عليه: ${Money.fmt(bal)}\n")
        else if (bal < 0) sb.append("🟢 له: ${Money.fmt(-bal)}\n")
        sb.append(if (limit == Int.MAX_VALUE) "\n📋 كل العمليات (${a.operations.size}):\n" else "\n📋 آخر ${limit} عمليات:\n")
        a.operations.sortedByDescending { it.date }.take(limit).forEachIndexed { i, op ->
            sb.append("${i + 1}. ${if (op.type == OpType.DEBT) "عليه" else "له"} ${Money.fmt(op.amount)} ${op.currency ?: ""} - ${op.note ?: ""}".trim())
            if (op.isInvoice && !op.invoiceRef.isNullOrBlank()) sb.append(" (🧾 ${op.invoiceRef})")
            if (op.materials.isNotEmpty()) sb.append(" [${op.materials.joinToString(", ") { it.name }}]")
            sb.append("\n")
        }
        sb.append("\nمحفظتي الذكية 💰")
        return sb.toString()
    }

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
                        // إصلاح sec-6: سجل التعديلات
                        IconButton(onClick = { showAudit = true }) {
                            Text("📝", fontSize = 18.sp)
                        }
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
                            RealAction("➖ سحب") { showWithdraw = true }
                            RealAction("🔄 تحويل") { showTransfer = true }
                            // إصلاح fin-4: تسوية محمية بسبب إلزامي
                            RealAction("⚖️ تسوية") { showAdjust = true }
                            RealAction("📋 سجل") { showHistory = true }
                        }
                    }
                }
                }
            }
            // ===== 💠 الرصيد العادي (كشف عليه/له) — كم بقي على العميل أو له الآن =====
            // مطلب: واجهة الحساب كانت تعرض «الرصيد الحقيقي» فقط دون الرصيد العادي.
            item {
                ElasticEntrance(1) {
                    ClientOpsBalanceCard(
                        balance = account?.opsBalance ?: 0.0,
                        currency = currency,
                    )
                }
            }
            // ===== إضافة 5.1/5.2 من تقرير الفحص: لافتة تحذير للعميل الموقوف/القائمة السوداء =====
            val clientStatus = client?.status
            if (clientStatus == com.mahfazty.smart.domain.model.ClientStatus.STOPPED ||
                clientStatus == com.mahfazty.smart.domain.model.ClientStatus.BLOCKED
            ) {
                item {
                    val blocked = clientStatus == com.mahfazty.smart.domain.model.ClientStatus.BLOCKED
                    val bannerColor = if (blocked) LocalAppColors.current.red else Color(0xFFFF9800)
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 6.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(bannerColor.copy(alpha = 0.12f))
                            .border(1.5.dp, bannerColor, RoundedCornerShape(14.dp))
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                    ) {
                        Text(
                            if (blocked) "⛔ هذا العميل في القائمة السوداء — التعامل معه خطر. إضافة وتعديل العمليات ممنوعان حتى تغيير حالته."
                            else "⚠️ هذا العميل موقوف — إضافة وتعديل العمليات ممنوعان حتى تغيير حالته من ملفه.",
                            style = MaterialTheme.typography.labelMedium,
                            color = bannerColor,
                        )
                    }
                }
            }
            // ===== زر واتساب =====
            // ===== إصلاح act-4: كشف الحساب — آخر 10 / كامل / تصدير CSV =====
            item {
                ElasticEntrance(2) {
                Button(
                    onClick = { showStatement = true },
                    modifier = Modifier
                        .fillMaxWidth()
                        .bounceClick()
                        .padding(horizontal = 20.dp, vertical = 8.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF25D366)),
                    shape = RoundedCornerShape(14.dp),
                ) { Text("💬 كشف الحساب (واتساب / CSV)") }
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
                // ===== إصلاح act-3: البحث + فلتر النوع + فلتر المدى الزمني =====
                item {
                    OutlinedTextField(
                        value = filterQuery,
                        onValueChange = onSetFilterQuery,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 4.dp),
                        placeholder = { Text("🔍 ابحث في العمليات (بيان / رقم فاتورة / صنف)...") },
                        shape = RoundedCornerShape(14.dp),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = LocalAppColors.current.border,
                        ),
                    )
                }
                item {
                    Row(
                        modifier = Modifier
                            .padding(horizontal = 20.dp, vertical = 2.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        FilterChip("الكل", filterType == com.mahfazty.smart.ui.viewmodels.OpTypeFilter.All) {
                            onSetFilterType(com.mahfazty.smart.ui.viewmodels.OpTypeFilter.All)
                        }
                        FilterChip("عليه", filterType == com.mahfazty.smart.ui.viewmodels.OpTypeFilter.Debt) {
                            onSetFilterType(com.mahfazty.smart.ui.viewmodels.OpTypeFilter.Debt)
                        }
                        FilterChip("له", filterType == com.mahfazty.smart.ui.viewmodels.OpTypeFilter.Pay) {
                            onSetFilterType(com.mahfazty.smart.ui.viewmodels.OpTypeFilter.Pay)
                        }
                    }
                }
                item {
                    Row(
                        modifier = Modifier
                            .padding(horizontal = 20.dp, vertical = 2.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        FilterChip("كل الأوقات", filterPeriod == com.mahfazty.smart.ui.viewmodels.OpPeriod.ALL) {
                            onSetFilterPeriod(com.mahfazty.smart.ui.viewmodels.OpPeriod.ALL)
                        }
                        FilterChip("هذا الشهر", filterPeriod == com.mahfazty.smart.ui.viewmodels.OpPeriod.MONTH) {
                            onSetFilterPeriod(com.mahfazty.smart.ui.viewmodels.OpPeriod.MONTH)
                        }
                        FilterChip("3 أشهر", filterPeriod == com.mahfazty.smart.ui.viewmodels.OpPeriod.THREE_MONTHS) {
                            onSetFilterPeriod(com.mahfazty.smart.ui.viewmodels.OpPeriod.THREE_MONTHS)
                        }
                        FilterChip("سنة", filterPeriod == com.mahfazty.smart.ui.viewmodels.OpPeriod.YEAR) {
                            onSetFilterPeriod(com.mahfazty.smart.ui.viewmodels.OpPeriod.YEAR)
                        }
                    }
                }
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
                // ===== إضافة 3.1 + 15.3 من تقرير الفحص: فلتر الاستحقاق + رقاقة الترتيب =====
                item {
                    Row(
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        // فلتر الديون المستحقة/المتأخرة
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(20.dp))
                                .background(
                                    if (dueOnly) LocalAppColors.current.red.copy(alpha = 0.14f)
                                    else MaterialTheme.colorScheme.surfaceVariant,
                                )
                                .border(
                                    width = if (dueOnly) 1.5.dp else 1.dp,
                                    color = if (dueOnly) LocalAppColors.current.red else LocalAppColors.current.border,
                                    shape = RoundedCornerShape(20.dp),
                                )
                                .clickable { onSetDueOnly(!dueOnly) }
                                .padding(horizontal = 12.dp, vertical = 6.dp),
                        ) {
                            Text(
                                "⏰ مستحق ($dueCount)",
                                style = MaterialTheme.typography.labelMedium,
                                color = if (dueOnly) LocalAppColors.current.red else LocalAppColors.current.muted,
                            )
                        }
                        // ترتيب القائمة: الأحدث / الأكبر مبلغاً
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(20.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant)
                                .border(1.dp, LocalAppColors.current.border, RoundedCornerShape(20.dp))
                                .clickable {
                                    onSetSortMode(
                                        if (sortMode == com.mahfazty.smart.ui.viewmodels.OpSortMode.LATEST)
                                            com.mahfazty.smart.ui.viewmodels.OpSortMode.AMOUNT_DESC
                                        else com.mahfazty.smart.ui.viewmodels.OpSortMode.LATEST,
                                    )
                                }
                                .padding(horizontal = 12.dp, vertical = 6.dp),
                        ) {
                            Text(
                                if (sortMode == com.mahfazty.smart.ui.viewmodels.OpSortMode.LATEST) "↕️ الأحدث أولاً" else "↕️ الأكبر مبلغاً",
                                style = MaterialTheme.typography.labelMedium,
                                color = LocalAppColors.current.muted,
                            )
                        }
                    }
                }
                if (shownOps.isEmpty() && pendingInvoicesOnly) {
                    item { EmptyState("🧾", "لا توجد فواتير معلقة — كلها سُلّمت 🎉") }
                } else if (shownOps.isEmpty()) {
                    // تحسين بيانات: الفلاتر/البحث أخفى كل العمليات — رسالة توضح السبب بدل قائمة فارغة صامتة
                    item { EmptyState("🔍", "لا توجد نتائج مطابقة للفلاتر الحالية — جرّب توسيع البحث أو المدى الزمني") }
                } else {
                    items(shownOps, key = { it.id }) { op ->
                        ElasticEntrance(3) {
                        Box(Modifier.animateItem()) {
                            OpRow(
                                op = op,
                                currency = currency,
                                selected = op.id in selection,
                                runningAfter = runningAfterMap[op.id],
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
            // إصلاح data-1 + data-4: الاستحقاق (للديون) والعملة المثبتة تمران مع العملية
            onSave = { type, amount, note, materials, receipt, isInvoice, invoiceRef, dueDate, currency ->
                showAddOp = false
                editingOp = null
                if (editing != null) {
                    onUpdateOperation(
                        editing.copy(
                            type = type, amount = amount, note = note,
                            materials = materials, receiptPath = receipt,
                            isInvoice = isInvoice, invoiceRef = invoiceRef,
                            dueDate = dueDate, currency = currency,
                            // عند إلغاء صفة «فاتورة» تُصفَّر حالة التسليم أيضاً
                            invoiceDelivered = if (isInvoice) editing.invoiceDelivered else false,
                        ),
                    )
                } else {
                    onAddOperation(type, amount, note, materials, receipt, isInvoice, invoiceRef, dueDate, currency)
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
            onSave = { amount, from ->
                // إصلاح act-1: لا تنفيذ فوري — نافذة تأكيد صريحة أولاً
                showFund = false
                realConfirm = RealBalanceConfirm.Fund(amount, from)
            },
        )
    }
    if (showWithdraw) {
        WithdrawRealDialog(
            currency = currency,
            realBalance = acc?.realBalance ?: 0.0,
            onDismiss = { showWithdraw = false },
            onSave = { amount, to ->
                // إصلاح act-1: لا تنفيذ فوري — نافذة تأكيد صريحة أولاً
                showWithdraw = false
                realConfirm = RealBalanceConfirm.Withdraw(amount, to)
            },
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
                // إصلاح act-1: لا تنفيذ فوري — نافذة تأكيد صريحة أولاً (باسم الحساب المصْدَر)
                val name = allAccounts.firstOrNull { it.second.id == fromAccountId }
                    ?.let { "${it.first.name} • ${it.second.name}" } ?: "حساب آخر"
                showTransfer = false
                realConfirm = RealBalanceConfirm.Transfer(fromClientId, fromAccountId, name, amount)
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
    // ===== إصلاح fin-4: تسوية محمية بسبب إلزامي (تُسجل في سجل التدقيق) =====
    if (showAdjust) {
        AdjustRealDialog(
            currency = currency,
            currentReal = acc?.realBalance ?: 0.0,
            onDismiss = { showAdjust = false },
            onSave = { amount, reason ->
                showAdjust = false
                onAdjustReal(amount, reason)
            },
        )
    }
    // ===== إصلاح sec-6: سجل التعديلات — من فعل ماذا ومتى =====
    if (showAudit) {
        AuditLogDialog(entries = audit, onDismiss = { showAudit = false })
    }
    // ===== إصلاح act-4: خيارات كشف الحساب (آخر 10 / كامل / CSV) =====
    if (showStatement) {
        StatementOptionsDialog(
            opCount = account?.operations?.size ?: 0,
            onDismiss = { showStatement = false },
            onShareLast10 = {
                showStatement = false
                shareViaWhatsApp(context, buildStatement(10), client?.phone)
            },
            onShareFull = {
                showStatement = false
                shareViaWhatsApp(context, buildStatement(Int.MAX_VALUE), client?.phone)
            },
            onExportCsv = {
                showStatement = false
                onExportCsv()
            },
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
    // إصلاح fin-2/fin-3: تحذير بالمبلغ الحقيقي المتبقي + أثر الحذف على سجل التحويلات
    if (deletingAccount && acc != null) {
        val transferCount = transfers.count {
            it.transfer.fromAccountId == acc.id || it.transfer.toAccountId == acc.id
        }
        val msg = buildString {
            append("سيُحذف الحساب وكل عملياته.")
            if (acc.realBalance > 0) {
                append("\n\n⚠️ ⚠️ تحذير: يحتوي رصيدًا حقيقيًا متبقيًا قدره ${Money.fmt(acc.realBalance)}.")
                append("\nسيختفي هذا المبلغ من التتبع نهائيًا دون عودة للبنك أو الكاش.")
                append("\nاسحبه أولاً إن أردت استرداده.")
            }
            if (transferCount > 0) {
                append("\n\nلديه $transferCount تحويل سابق — ستبقى في سجل التحويلات باسم «حساب محذوف».")
            }
        }
        ConfirmDialog(
            title = "حذف الحساب؟",
            message = msg,
            onConfirm = { deletingAccount = false; onDeleteAccount(acc.id); onBack() },
            onDismiss = { deletingAccount = false },
            // إضافة 12.2 من تقرير الفحص: سحب الرصيد إلى البنك ثم الحذف
            extraText = if (acc.realBalance > 0) "💵 سحب ثم حذف" else null,
            onExtra = if (acc.realBalance > 0) {
                { deletingAccount = false; onWithdrawAndDeleteAccount(acc.id); onBack() }
            } else null,
        )
    }
    insufficientReal?.let { data ->
        InsufficientRealSheet(
            data = data,
            currency = currency,
            onClose = onDismissInsufficientReal,
            onFund = onQuickFundReal,
            onTransfer = onQuickTransferReal,
            // إصلاح fin-6: صفّا «الادخار» و«الأهداف» كانا يعرضان أزرار شحن معطلة (دوال فارغة افتراضياً)
            // — الآن موصولان فعلياً بمساري الشحن السريع، فيعمل كل زر ظاهر في النافذة.
            onFundSavings = onQuickFundSavings,
            onFundGoal = onQuickFundGoal,
        )
    }

    // ===== إصلاح act-1: تأكيد صريح قبل تنفيذ أي حركة رصيد حقيقي =====
    realConfirm?.let { rc ->
        val msg = when (rc) {
            is RealBalanceConfirm.Fund ->
                "سيُشحن ${Money.fmt(rc.amount)} إلى الرصيد الحقيقي من ${if (rc.from == Wallet.BANK) settings.bankName else settings.cashName}."
            is RealBalanceConfirm.Withdraw ->
                "سيُسحب ${Money.fmt(rc.amount)} من الرصيد الحقيقي إلى ${if (rc.to == Wallet.BANK) settings.bankName else settings.cashName}."
            is RealBalanceConfirm.Transfer ->
                "سيُحوَّل ${Money.fmt(rc.amount)} من «${rc.fromAccountName}» إلى «${acc?.name ?: ""}»."
        }
        ConfirmDialog(
            title = "تأكيد الحركة المالية؟",
            message = msg,
            confirmText = "تأكيد",
            danger = false,
            onConfirm = {
                when (rc) {
                    is RealBalanceConfirm.Fund -> onFundReal(rc.amount, rc.from)
                    is RealBalanceConfirm.Withdraw -> onWithdrawReal(rc.amount, rc.to)
                    is RealBalanceConfirm.Transfer ->
                        onTransferReal(rc.fromClientId, rc.fromAccountId, client?.id ?: -1, acc?.id ?: -1, rc.amount)
                }
                realConfirm = null
            },
            onDismiss = { realConfirm = null },
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
    /** 💠 الرصيد العادي الجاري بعد هذه العملية (موجب = عليه، سالب = له) — اختياري */
    runningAfter: Double? = null,
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
                // إصلاح data-1: تاريخ استحقاق الدين — يُحمَّر عند التأخر ليوم واحد على الأقل
                op.dueDate?.let { dueMs ->
                    val endOfToday = Calendar.getInstance().apply {
                        timeInMillis = System.currentTimeMillis()
                        add(Calendar.DAY_OF_YEAR, 1)
                        set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0)
                        set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
                    }.timeInMillis
                    val overdue = dueMs < endOfToday
                    Text(
                        "⏰ الاستحقاق: ${Dates.dateTime(dueMs)}" + (if (overdue) " — متأخر!" else ""),
                        style = MaterialTheme.typography.labelSmall,
                        color = if (overdue) LocalAppColors.current.red else LocalAppColors.current.muted,
                    )
                }
                Text(
                    Dates.dateTime(op.date),
                    style = MaterialTheme.typography.labelSmall,
                    color = LocalAppColors.current.muted,
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    Money.fmt(op.amount),
                    style = MaterialTheme.typography.titleMedium,
                    color = if (op.type == OpType.DEBT) LocalAppColors.current.red else LocalAppColors.current.green,
                )
                // 💠 الرصيد العادي الجاري بعد هذه العملية — «كيف يتنقل الرصيد بين العمليات»:
                // كل صف يكشف الرصيد الناتج بعده، فيقرأ المستخدم كشف الحساب كعدّاد متحرك.
                runningAfter?.let { rb ->
                    if (kotlin.math.abs(rb) < 0.0001) {
                        Text(
                            "بعدها: ⚖️ متساوي",
                            style = MaterialTheme.typography.labelSmall,
                            color = LocalAppColors.current.muted,
                        )
                    } else {
                        val rbDebt = rb > 0.0
                        val rbColor =
                            if (rbDebt) LocalAppColors.current.red else LocalAppColors.current.green
                        AnimatedNumber(
                            target = kotlin.math.abs(rb),
                            format = { "بعدها: ${if (rbDebt) "عليه" else "له"} ${Money.fmt(it)}" },
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = rbColor,
                                fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                            ),
                        )
                    }
                }
            }
        }
    }
}

/** رقاقة فلتر صغيرة (نوع العملية / المدى الزمني — إصلاح act-3) */
@Composable
private fun FilterChip(label: String, selected: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(
                if (selected) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.surfaceVariant,
            )
            .clickable(onClick = onClick)
            .bounceClick()
            .padding(horizontal = 12.dp, vertical = 6.dp),
    ) {
        Text(
            label,
            style = MaterialTheme.typography.labelSmall,
            color = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
        )
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
