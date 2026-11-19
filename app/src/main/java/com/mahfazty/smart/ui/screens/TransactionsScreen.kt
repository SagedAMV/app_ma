package com.mahfazty.smart.ui.screens

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
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
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mahfazty.smart.domain.Money
import com.mahfazty.smart.domain.model.AppSettings
import com.mahfazty.smart.domain.model.Transaction
import com.mahfazty.smart.domain.model.TxType
import com.mahfazty.smart.domain.model.Wallet
import com.mahfazty.smart.ui.components.AnimatedNumber
import com.mahfazty.smart.ui.components.bounceClick
import com.mahfazty.smart.ui.components.ElasticEntrance
import com.mahfazty.smart.ui.components.AppCard
import com.mahfazty.smart.ui.components.ConfirmDialog
import com.mahfazty.smart.ui.components.EmptyState
import com.mahfazty.smart.ui.components.TxRow
import com.mahfazty.smart.ui.dialogs.AddTransactionDialog
import com.mahfazty.smart.ui.dialogs.EditTransactionDialog
import com.mahfazty.smart.ui.dialogs.InsufficientSheet
import com.mahfazty.smart.ui.dialogs.TxDetailDialog
import com.mahfazty.smart.ui.theme.LocalAppColors
import com.mahfazty.smart.ui.theme.Motion
import com.mahfazty.smart.ui.viewmodels.InsufficientData
import com.mahfazty.smart.ui.viewmodels.TxFilter
import com.mahfazty.smart.ui.viewmodels.TxUiState

/**
 * عنصر قائمة السجل — رأس مجموعة تاريخ أو صف عملية.
 * تحويل المجموعات إلى قائمة مسطحة بمفاتيح ثابتة شرطٌ لعمل animateItem (مهارة التنفيذ 15).
 */
private sealed interface TxListItem {
    val key: String

    data class Header(val label: String) : TxListItem {
        override val key: String get() = "h_$label"
    }

    data class RowItem(val tx: Transaction, val running: Double) : TxListItem {
        override val key: String get() = "t_${tx.id}"
    }
}

@Composable
fun TransactionsScreen(
    state: TxUiState,
    settings: AppSettings,
    insufficient: InsufficientData?,
    pending: com.mahfazty.smart.ui.flow.PendingWalletAction?,
    toast: kotlinx.coroutines.flow.SharedFlow<String>,
    onSetQuery: (String) -> Unit,
    onSetFilter: (TxFilter) -> Unit,
    onAdd: (TxType, Double, String, String?, Wallet) -> Unit,
    onUpdate: (Transaction) -> Unit,
    onDelete: (Transaction) -> Unit,
    onDuplicate: (Transaction) -> Unit,
    onDismissInsufficient: () -> Unit,
    onQuickWithdrawGoal: (Long, String, Double) -> Unit,
    onQuickWithdrawSavings: (Double) -> Unit,
    onClearPending: () -> Unit,
) {
    val snackbar = remember { SnackbarHostState() }
    LaunchedEffect(Unit) { toast.collect { snackbar.showSnackbar(it) } }

    var showAdd by remember { mutableStateOf(false) }
    var selected by remember { mutableStateOf<Transaction?>(null) }
    var editing by remember { mutableStateOf<Transaction?>(null) }
    var deleting by remember { mutableStateOf<Transaction?>(null) }

    // تحويل المجموعات إلى عناصر مسطحة بمفاتيح ثابتة — أساس حركة القوائم
    val flatItems = remember(state.groups) {
        state.groups.flatMap { g ->
            listOf<TxListItem>(TxListItem.Header(g.label)) +
                g.items.map { (tx, running) -> TxListItem.RowItem(tx, running) }
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbar) },
        floatingActionButton = {
            val fabRotation by animateFloatAsState(
                targetValue = if (showAdd) 45f else 0f,
                animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
                label = "fabRotation",
            )
            FloatingActionButton(
                onClick = { showAdd = true },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = Color.White,
                modifier = Modifier.bounceClick(),
            ) {
                Text(
                    "＋",
                    fontSize = 22.sp,
                    modifier = Modifier.graphicsLayer { rotationZ = fabRotation },
                )
            }
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
                    Text("سجل العمليات", style = MaterialTheme.typography.titleLarge)
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(MaterialTheme.colorScheme.onSurface)
                            .clickable { showAdd = true }
                            .bounceClick(),
                        contentAlignment = Alignment.Center,
                    ) { Text("＋", color = MaterialTheme.colorScheme.surface, fontSize = 18.sp) }
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
                    placeholder = { Text("🔍 ابحث في العمليات (مثلاً: غداء، راتب...)") },
                    shape = RoundedCornerShape(16.dp),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = LocalAppColors.current.border,
                    ),
                )
                }
            }
            // ===== إحصائيات سريعة (أرقام متصاعدة) =====
            item {
                ElasticEntrance(2) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    QuickStat("اليوم", state.todayExpense, state.currency, state.hideBalance, Modifier.weight(1f))
                    QuickStat("الأسبوع", state.weekExpense, state.currency, state.hideBalance, Modifier.weight(1f))
                    QuickStat("الكل", state.allExpense, state.currency, state.hideBalance, Modifier.weight(1f), "${state.allCount} عملية")
                }
                }
            }
            // ===== شرائح الفلاتر =====
            item {
                ElasticEntrance(3) {
                val chips = listOf(
                    "الكل" to TxFilter.All,
                    "مصاريف" to TxFilter.Expenses,
                    "دخل" to TxFilter.Incomes,
                    "🎯 أهداف" to TxFilter.Goals,
                    "🐷 ادخار" to TxFilter.Savings,
                    "🍔 طعام" to TxFilter.Category("food"),
                    "🚕 مواصلات" to TxFilter.Category("transport"),
                    "🛍️ تسوق" to TxFilter.Category("shopping"),
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                        .padding(horizontal = 20.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    chips.forEach { (label, filter) ->
                        val selectedFilter = state.filter == filter
                        val chipScale by animateFloatAsState(
                            targetValue = if (selectedFilter) 1.08f else 1f,
                            animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
                            label = "chip_$label",
                        )
                        Box(
                            modifier = Modifier
                                .graphicsLayer {
                                    scaleX = chipScale
                                    scaleY = chipScale
                                }
                                .clip(RoundedCornerShape(20.dp))
                                .background(
                                    if (selectedFilter) MaterialTheme.colorScheme.primary
                                    else MaterialTheme.colorScheme.surface,
                                )
                                .clickable { onSetFilter(filter) }
                                .padding(horizontal = 14.dp, vertical = 8.dp),
                        ) {
                            Text(
                                label,
                                style = MaterialTheme.typography.labelSmall,
                                color = if (selectedFilter) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
                Spacer(Modifier.height(10.dp))
                }
            }
            // ===== القائمة المسطحة بحركة كاملة =====
            if (flatItems.isEmpty()) {
                item { ElasticEntrance(4) { EmptyState("📭", "لا توجد عمليات مطابقة") } }
            } else {
                items(flatItems, key = { it.key }) { item ->
                    when (item) {
                        is TxListItem.Header -> {
                            Text(
                                item.label,
                                style = MaterialTheme.typography.labelMedium,
                                color = LocalAppColors.current.muted,
                                modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp),
                            )
                        }
                        is TxListItem.RowItem -> {
                            ElasticEntrance(4) {
                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 20.dp, vertical = 3.dp)
                                    .animateItem(
                                        fadeInSpec = Motion.enter,
                                        placementSpec = spring(
                                            dampingRatio = Spring.DampingRatioMediumBouncy,
                                            stiffness = Spring.StiffnessLow,
                                        ),
                                        fadeOutSpec = Motion.exit,
                                    ),
                                shape = RoundedCornerShape(16.dp),
                                color = MaterialTheme.colorScheme.surface,
                                shadowElevation = 1.dp,
                            ) {
                                TxRow(
                                    tx = item.tx,
                                    currency = state.currency,
                                    hidden = state.hideBalance,
                                    onClick = { selected = item.tx },
                                    running = item.running,
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
    val pendingTx = pending as? com.mahfazty.smart.ui.flow.PendingWalletAction.Tx
    if (insufficient == null && (showAdd || (pendingTx != null && pendingTx.editing == null))) {
        AddTransactionDialog(
            settings = settings,
            bank = state.bank, cash = state.cash,
            initialType = pendingTx?.type ?: TxType.EXPENSE,
            initialAmount = pendingTx?.amount,
            initialNote = pendingTx?.note,
            initialCategory = pendingTx?.category,
            initialWallet = pendingTx?.wallet,
            onDismiss = { showAdd = false; onClearPending() },
            onSave = { type, amount, cat, note, wallet ->
                showAdd = false
                onAdd(type, amount, cat, note, wallet)
            },
        )
    }
    selected?.let { tx ->
        TxDetailDialog(
            tx = tx,
            currency = state.currency,
            onDismiss = { selected = null },
            onEdit = { selected = null; editing = tx },
            onDelete = { selected = null; deleting = tx },
            onDuplicate = { selected = null; onDuplicate(tx) },
        )
    }
    val editingTx = pendingTx?.editing ?: editing
    if (insufficient == null) {
        editingTx?.let { tx ->
            val draft = pendingTx?.takeIf { it.editing != null }
            EditTransactionDialog(
                settings = settings,
                tx = if (draft != null) tx.copy(
                    type = draft.type, amount = draft.amount, category = draft.category,
                    note = draft.note, wallet = draft.wallet,
                ) else tx,
                onDismiss = { editing = null; onClearPending() },
                onSave = { updated ->
                    editing = null
                    onUpdate(updated)
                },
            )
        }
    }
    deleting?.let { tx ->
        ConfirmDialog(
            title = "حذف العملية؟",
            message = "سيُعاد المبلغ إلى الصندوق تلقائياً (إن كان مصروفاً).",
            onConfirm = { deleting = null; onDelete(tx) },
            onDismiss = { deleting = null },
        )
    }
    insufficient?.let { data ->
        InsufficientSheet(
            data = data,
            currency = state.currency,
            onClose = onDismissInsufficient,
            onWithdrawGoal = onQuickWithdrawGoal,
            onWithdrawSavings = onQuickWithdrawSavings,
        )
    }
}

@Composable
private fun QuickStat(
    label: String,
    value: Double,
    currency: String,
    hidden: Boolean,
    modifier: Modifier = Modifier,
    extra: String? = null,
) {
    AppCard(modifier) {
        Column(Modifier.padding(10.dp)) {
            Text(label, style = MaterialTheme.typography.labelSmall, color = LocalAppColors.current.muted)
            Spacer(Modifier.height(4.dp))
            AnimatedNumber(
                target = value,
                hidden = hidden,
                format = { Money.fmt(it) },
                style = MaterialTheme.typography.titleMedium,
            )
            if (extra != null) {
                Text(extra, style = MaterialTheme.typography.labelSmall, color = LocalAppColors.current.muted)
            }
        }
    }
}
