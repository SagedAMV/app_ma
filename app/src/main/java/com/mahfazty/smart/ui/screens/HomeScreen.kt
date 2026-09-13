package com.mahfazty.smart.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mahfazty.smart.domain.Money
import com.mahfazty.smart.domain.model.AppSettings
import com.mahfazty.smart.domain.model.GoalWithSaved
import com.mahfazty.smart.domain.model.Transaction
import com.mahfazty.smart.domain.model.TxType
import com.mahfazty.smart.domain.model.Wallet
import com.mahfazty.smart.ui.components.AnimatedNumber
import com.mahfazty.smart.ui.components.AppCard
import com.mahfazty.smart.ui.components.BarChart
import com.mahfazty.smart.ui.components.ConfettiOverlay
import com.mahfazty.smart.ui.components.animatedGradient
import com.mahfazty.smart.ui.components.bounceClick
import com.mahfazty.smart.ui.components.ElasticEntrance
import com.mahfazty.smart.ui.components.EmptyState
import com.mahfazty.smart.ui.components.SectionHeader
import com.mahfazty.smart.ui.components.ShimmerBand
import com.mahfazty.smart.ui.components.SoftDivider
import com.mahfazty.smart.ui.components.TxRow
import com.mahfazty.smart.ui.dialogs.AddTransactionDialog
import com.mahfazty.smart.ui.dialogs.EditBankDialog
import com.mahfazty.smart.ui.dialogs.InsufficientSheet
import com.mahfazty.smart.ui.dialogs.TransferDialog
import com.mahfazty.smart.ui.theme.LocalAppColors
import com.mahfazty.smart.ui.theme.Motion
import com.mahfazty.smart.ui.theme.rememberReduceMotion
import com.mahfazty.smart.ui.viewmodels.HomeUiState
import com.mahfazty.smart.ui.viewmodels.InsufficientData
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.scaleIn
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.ui.graphics.graphicsLayer

@Composable
fun HomeScreen(
    state: HomeUiState,
    settings: AppSettings,
    insufficient: InsufficientData?,
    pending: com.mahfazty.smart.ui.flow.PendingWalletAction?,
    celebrate: Long,
    onCelebrateDone: () -> Unit,
    toast: kotlinx.coroutines.flow.SharedFlow<String>,
    onDismissInsufficient: () -> Unit,
    onQuickWithdrawGoal: (Long, String, Double) -> Unit,
    onQuickWithdrawSavings: (Double) -> Unit,
    onAddTransaction: (TxType, Double, String, String?, Wallet) -> Unit,
    onTransfer: (String, Double, String?) -> Unit,
    onSaveBankOpening: (Double) -> Unit,
    onClearPending: () -> Unit,
    onTxClick: (Transaction) -> Unit,
    onOpenGoals: () -> Unit,
    onOpenTransactions: () -> Unit,
) {
    val snackbar = remember { SnackbarHostState() }
    LaunchedEffect(Unit) { toast.collect { snackbar.showSnackbar(it) } }

    // نبضة احتفال عند ارتفاع رصيد الكاش (الرقم الكبير — كما في نسخة الويب: الكاش وليس الإجمالي)
    val reduceMotion = rememberReduceMotion()
    val pulseScale = remember { Animatable(1f) }
    var previousTotal by remember { mutableStateOf(state.cash) }
    LaunchedEffect(state.cash) {
        if (state.cash > previousTotal && !reduceMotion) {
            pulseScale.animateTo(1.03f, Motion.quick)
            pulseScale.animateTo(1f, Motion.springBounce)
        }
        previousTotal = state.cash
    }

    var showAddTx by remember { mutableStateOf(false) }
    var addTxType by remember { mutableStateOf(TxType.EXPENSE) }
    var showTransfer by remember { mutableStateOf(false) }
    var showBankEdit by remember { mutableStateOf(false) }

    Box {
        Scaffold(snackbarHost = { SnackbarHost(snackbar) }) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(bottom = 20.dp),
        ) {
            // ===== الترويسة =====
            item {
                ElasticEntrance(0) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column {
                        Text("أهلاً، ${state.name} 👋", style = MaterialTheme.typography.titleLarge)
                        Text(
                            "اليوم • ${state.dateLabel}",
                            style = MaterialTheme.typography.bodySmall,
                            color = LocalAppColors.current.muted,
                        )
                    }
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .background(MaterialTheme.colorScheme.primary, CircleShape),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            state.name.firstOrNull()?.toString() ?: "أ",
                            color = Color.White,
                            style = MaterialTheme.typography.titleMedium,
                        )
                    }
                }
                }
            }

            // ===== إضافة 3.3 من تقرير الفحص: شارة الديون المستحقة/المتأخرة =====
            if (state.overdueCount > 0) {
                item {
                    HomeWarningBanner(
                        text = "⏰ لديك ${state.overdueCount} دين مستحق أو متأخرة لدى عملائك — راجع تبويب العملاء",
                        color = LocalAppColors.current.red,
                    )
                }
            }
            // ===== إضافة 10.2 من تقرير الفحص: شريط تذكير النسخ الاحتياطي في الرئيسية =====
            if (state.backupWarning != null) {
                item {
                    var dismissed by remember { mutableStateOf(false) }
                    if (!dismissed) {
                        val w = state.backupWarning
                        HomeWarningBanner(
                            text = if (w.never) "💾 لم تأخذ نسخة احتياطية بعد — صدّرها من الإعدادات قبل فوات الأوان"
                            else if (w.newOps >= 50) "💾 تراكمت ${w.newOps} عملية منذ آخر نسخة احتياطية — حان وقت التصدير"
                            else "💾 آخر نسخة احتياطية قبل ${w.daysAgo} يوم — حان وقت التصدير",
                            color = LocalAppColors.current.gold,
                            onDismiss = { dismissed = true },
                        )
                    }
                }
            }

            // ===== كارت الرصيد =====
            item {
                val appColors = LocalAppColors.current
                // لمعان متحرك (معايرة 2026): نوار ضوء يعبر البطاقة
                val cardInf = if (!reduceMotion) rememberInfiniteTransition(label = "cardShimmer") else null
                val shimmerX = cardInf?.let {
                    it.animateFloat(
                        initialValue = 0f,
                        targetValue = 1f,
                        animationSpec = infiniteRepeatable(tween(2400, easing = LinearEasing), RepeatMode.Restart),
                        label = "cardShimmerX",
                    ).value
                } ?: 0f
                ElasticEntrance(1) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp)
                        .graphicsLayer {
                            scaleX = pulseScale.value
                            scaleY = pulseScale.value
                        }
                        .shadow(10.dp, RoundedCornerShape(24.dp))
                        .clip(RoundedCornerShape(24.dp))
                        .background(animatedGradient(MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.secondary))
                        .padding(18.dp),
                ) {
                    // إضاءة شعاعية (معايرة 2026): وميض زجاجي من أعلى البطاقة
                    androidx.compose.foundation.Canvas(Modifier.matchParentSize()) {
                        drawCircle(
                            brush = Brush.radialGradient(
                                colors = listOf(Color.White.copy(alpha = 0.16f), Color.Transparent),
                                center = Offset(size.width * 0.22f, size.height * 0.18f),
                                radius = 700f,
                            ),
                            radius = 700f,
                            center = Offset(size.width * 0.22f, size.height * 0.18f),
                        )
                    }
                    if (!reduceMotion) {
                        Box(
                            Modifier
                                .matchParentSize()
                                .graphicsLayer { translationX = shimmerX * 1400f - 700f }
                                .background(
                                    Brush.linearGradient(
                                        colors = listOf(Color.Transparent, Motion.shimmerColor, Color.Transparent),
                                        start = Offset(0f, 0f),
                                        end = Offset(1400f, 500f),
                                    ),
                                ),
                        )
                    }
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                "💵 ${state.cashName} الحالي",
                                style = MaterialTheme.typography.labelMedium,
                                color = Color.White.copy(alpha = 0.95f),
                            )
                            Row(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(20.dp))
                                    .background(Color.Black.copy(alpha = 0.28f))
                                    .clickable { showBankEdit = true }
                                    .padding(horizontal = 12.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Text("🏦", fontSize = 12.sp)
                                Spacer(Modifier.width(6.dp))
                                Text(state.bankName, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                Spacer(Modifier.width(6.dp))
                                Text(
                                    if (state.hideBalance) "•••" else Money.fmt(state.bank),
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White,
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(Color.Black.copy(alpha = 0.3f))
                                        .padding(horizontal = 8.dp, vertical = 3.dp),
                                )
                            }
                        }
                        Spacer(Modifier.height(12.dp))
                        AnimatedNumber(
                            target = state.cash,
                            hidden = state.hideBalance,
                            format = { "${Money.fmt(it)} ${state.currency}" },
                            style = MaterialTheme.typography.headlineMedium.copy(color = Color.White),
                        )
                        Spacer(Modifier.height(8.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(28.dp)
                                    .background(Color.White.copy(alpha = 0.18f), RoundedCornerShape(8.dp)),
                                contentAlignment = Alignment.Center,
                            ) { Text("💳", fontSize = 14.sp) }
                            Spacer(Modifier.width(8.dp))
                            Column {
                                Text("إجمالي ثروتك", fontSize = 12.sp, color = Color.White.copy(alpha = 0.95f))
                                AnimatedNumber(
                                    target = state.wealth,
                                    hidden = state.hideBalance,
                                    format = { "${Money.fmt(it)} ${state.currency}" },
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp,
                                    ),
                                )
                            }
                            Spacer(Modifier.weight(1f))
                            Text(
                                "💵 كاش",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(Color.Black.copy(alpha = 0.22f))
                                    .padding(horizontal = 10.dp, vertical = 6.dp),
                            )
                        }
                        Spacer(Modifier.height(12.dp))
                        Text(
                            "الرصيد المتاح (بنك + كاش): ${if (state.hideBalance) "•••" else "${Money.fmt(state.total)} ${state.currency}"}",
                            fontSize = 12.sp, color = Color.White.copy(alpha = 0.9f),
                        )
                        Spacer(Modifier.height(14.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(
                                onClick = { showTransfer = true },
                                modifier = Modifier.weight(1f).bounceClick(),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color.White.copy(alpha = 0.16f),
                                    contentColor = Color.White,
                                ),
                                shape = RoundedCornerShape(14.dp),
                            ) { Text("🔄 تحويل بنك ↔ كاش", fontSize = 11.sp) }
                            Button(
                                onClick = { addTxType = TxType.INCOME; showAddTx = true },
                                modifier = Modifier.weight(1.2f).bounceClick(),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color.Black.copy(alpha = 0.34f),
                                    contentColor = Color.White,
                                ),
                                shape = RoundedCornerShape(14.dp),
                            ) { Text("💰 إضافة دخل", fontSize = 11.sp) }
                        }
                    }
                }
                }
            }

            // ===== تحذيرات الحدود (انزلاق + ارتداد — معايرة 2026: برتقالي + أيقونة) =====
            item {
                AnimatedVisibility(
                    visible = state.warnings.isNotEmpty(),
                    enter = if (reduceMotion) {
                        fadeIn(snap())
                    } else {
                        slideInVertically(Motion.enterOffset) { -it } +
                            fadeIn(Motion.enter) +
                            scaleIn(initialScale = 0.8f, animationSpec = Motion.springBounce)
                    },
                    exit = slideOutVertically(if (reduceMotion) snap() else Motion.exitOffset) { -it } +
                        fadeOut(if (reduceMotion) snap() else Motion.exit),
                ) {
                    Column(Modifier.padding(horizontal = 20.dp, vertical = 4.dp)) {
                        state.warnings.forEach { w ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(Color(0xFFFF9800).copy(alpha = 0.15f))
                                    .padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Text("⚠️", fontSize = 16.sp)
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    w,
                                    modifier = Modifier.weight(1f),
                                    style = MaterialTheme.typography.bodySmall,
                                )
                            }
                        }
                    }
                }
            }

            // ===== إحصائيات الشهر =====
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    ElasticEntrance(2, Modifier.weight(1f)) { StatCard("↓", "الدخل هذا الشهر", state.monthIncome, state.currency, state.hideBalance, LocalAppColors.current.green) }
                    ElasticEntrance(3, Modifier.weight(1f)) { StatCard("↑", "المصروف", state.monthExpense, state.currency, state.hideBalance, LocalAppColors.current.red) }
                    ElasticEntrance(4, Modifier.weight(1f)) { StatCard("◍", "الادخار", state.monthSave, state.currency, state.hideBalance, MaterialTheme.colorScheme.primary) }
                }
            }

            // ===== أزرار سريعة =====
            item {
                ElasticEntrance(5) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Button(
                        onClick = { addTxType = TxType.EXPENSE; showAddTx = true },
                        modifier = Modifier.weight(1f).bounceClick(),
                        colors = ButtonDefaults.buttonColors(containerColor = LocalAppColors.current.red),
                        shape = RoundedCornerShape(16.dp),
                    ) { Text("➖ مصروف جديد") }
                    Button(
                        onClick = { addTxType = TxType.INCOME; showAddTx = true },
                        modifier = Modifier.weight(1f).bounceClick(),
                        colors = ButtonDefaults.buttonColors(containerColor = LocalAppColors.current.green),
                        shape = RoundedCornerShape(16.dp),
                    ) { Text("➕ إضافة دخل") }
                }
                }
            }

            // ===== مصاريف الأسبوع =====
            item {
                ElasticEntrance(6) {
                Column {
                SectionHeader("📊 مصاريفك هذا الأسبوع", action = "التفاصيل", onAction = onOpenTransactions)
                AppCard(Modifier.padding(horizontal = 20.dp)) {
                    Box(Modifier.padding(14.dp)) {
                        BarChart(state.week, MaterialTheme.colorScheme.primary, highlightLast = true)
                    }
                }
                }
                }
            }

            // ===== الأهداف =====
            item {
                ElasticEntrance(7) {
                Column {
                SectionHeader("🎯 أهدافك", action = "عرض الكل", onAction = onOpenGoals)
                if (state.goals.isEmpty()) {
                    AppCard(Modifier.padding(horizontal = 20.dp)) { EmptyState("🎯", "لا توجد أهداف بعد") }
                } else {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState())
                            .padding(horizontal = 20.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        state.goals.take(3).forEach { g -> GoalMiniCard(g, state.currency, state.hideBalance) }
                    }
                }
                }
                }
            }

            // ===== آخر العمليات =====
            item {
                ElasticEntrance(8) {
                Column {
                SectionHeader("🕒 آخر العمليات", action = "الكل", onAction = onOpenTransactions)
                AppCard(Modifier.padding(horizontal = 20.dp)) {
                    if (state.recent.isEmpty()) {
                        EmptyState("📭", "لا يوجد عمليات بعد")
                    } else {
                        Column {
                            state.recent.forEachIndexed { index, tx ->
                                TxRow(tx, state.currency, state.hideBalance, onClick = { onTxClick(tx) })
                                if (index < state.recent.lastIndex) SoftDivider()
                            }
                        }
                    }
                }
                }
                }
            }
        }
        }
        // 🎉 قصاصات الاحتفال عند وصول دخل جديد
        if (celebrate != 0L) {
            key(celebrate) { ConfettiOverlay(onFinished = onCelebrateDone) }
        }
    }

    // ===== النوافذ =====
    val pendingTx = pending as? com.mahfazty.smart.ui.flow.PendingWalletAction.Tx
    val pendingTransfer = pending as? com.mahfazty.smart.ui.flow.PendingWalletAction.Transfer
    if (insufficient == null && (showAddTx || (pendingTx != null && pendingTx.editing == null))) {
        AddTransactionDialog(
            settings = settings,
            bank = state.bank,
            cash = state.cash,
            initialType = pendingTx?.type ?: addTxType,
            initialAmount = pendingTx?.amount,
            initialNote = pendingTx?.note,
            initialCategory = pendingTx?.category,
            initialWallet = pendingTx?.wallet,
            onDismiss = { showAddTx = false; onClearPending() },
            onSave = { type, amount, cat, note, wallet ->
                showAddTx = false
                onAddTransaction(type, amount, cat, note, wallet)
            },
        )
    }
    if (insufficient == null && (showTransfer || pendingTransfer != null)) {
        TransferDialog(
            settings = settings,
            bank = state.bank,
            cash = state.cash,
            initialDirection = pendingTransfer?.direction,
            initialAmount = pendingTransfer?.amount,
            initialNote = pendingTransfer?.note,
            onDismiss = { showTransfer = false; onClearPending() },
            onSave = { dir, amount, note ->
                showTransfer = false
                onTransfer(dir, amount, note)
            },
        )
    }
    if (showBankEdit) {
        EditBankDialog(
            currency = state.currency,
            current = state.bank,
            onDismiss = { showBankEdit = false },
            onSave = { v ->
                showBankEdit = false
                onSaveBankOpening(v)
            },
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
private fun StatCard(
    icon: String,
    label: String,
    value: Double,
    currency: String,
    hidden: Boolean,
    color: Color,
    modifier: Modifier = Modifier,
) {
    AppCard(modifier) {
        Column(Modifier.padding(12.dp)) {
            Box(
                modifier = Modifier
                    .size(30.dp)
                    .background(color.copy(alpha = 0.15f), RoundedCornerShape(9.dp)),
                contentAlignment = Alignment.Center,
            ) { Text(icon, fontSize = 14.sp, color = color, fontWeight = FontWeight.Bold) }
            Spacer(Modifier.height(8.dp))
            Text(label, style = MaterialTheme.typography.labelSmall, color = LocalAppColors.current.muted)
            Spacer(Modifier.height(4.dp))
            AnimatedNumber(
                target = value,
                hidden = hidden,
                format = { Money.fmt(it) },
                style = MaterialTheme.typography.titleMedium,
            )
        }
    }
}

@Composable
private fun GoalMiniCard(goal: GoalWithSaved, currency: String, hidden: Boolean) {
    androidx.compose.material3.Surface(
        modifier = Modifier.width(160.dp),
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 2.dp,
    ) {
        Column(Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(goal.goal.icon, fontSize = 22.sp)
                Spacer(Modifier.width(8.dp))
                Text(goal.goal.name, style = MaterialTheme.typography.labelMedium, maxLines = 1)
            }
            Spacer(Modifier.height(8.dp))
            Text(
                "${if (hidden) "•••" else Money.fmt(goal.saved)} / ${Money.fmt(goal.goal.target)}",
                style = MaterialTheme.typography.labelSmall,
                color = LocalAppColors.current.muted,
            )
            Spacer(Modifier.height(6.dp))
            val progress by animateFloatAsState(
                targetValue = goal.progress,
                animationSpec = Motion.springSmooth,
                label = "miniGoalProgress",
            )
            Box {
                androidx.compose.material3.LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .clip(RoundedCornerShape(4.dp)),
                    color = MaterialTheme.colorScheme.primary,
                    trackColor = LocalAppColors.current.chipBg,
                )
                ShimmerBand()
                // توهج عند الاكتمال 100% (معايرة 2026)
                if (goal.progress >= 1f) {
                    Box(
                        Modifier
                            .matchParentSize()
                            .background(
                                Brush.radialGradient(
                                    colors = listOf(Color(0xFF4CAF50).copy(alpha = 0.3f), Color.Transparent),
                                ),
                            ),
                    )
                }
            }
        }
    }
}

/** لافتة تحذير علوية للرئيسية (إضافة 3.3 + 10.2 من تقرير الفحص) */
@Composable
private fun HomeWarningBanner(text: String, color: Color, onDismiss: (() -> Unit)? = null) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 4.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(color.copy(alpha = 0.12f))
            .border(1.dp, color.copy(alpha = 0.5f), RoundedCornerShape(14.dp))
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(text, style = MaterialTheme.typography.labelMedium, color = color, modifier = Modifier.weight(1f))
        if (onDismiss != null) {
            Text(
                "✕",
                style = MaterialTheme.typography.labelMedium,
                color = color,
                modifier = Modifier.clickable(onClick = onDismiss),
            )
        }
    }
}
