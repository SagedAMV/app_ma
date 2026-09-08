package com.mahfazty.smart.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.animation.core.animateFloatAsState
import com.mahfazty.smart.domain.Money
import com.mahfazty.smart.domain.model.Goal
import com.mahfazty.smart.domain.model.GoalWithSaved
import com.mahfazty.smart.ui.components.AppCard
import com.mahfazty.smart.ui.components.ConfirmDialog
import com.mahfazty.smart.ui.components.EmptyState
import com.mahfazty.smart.ui.components.ConfettiOverlay
import com.mahfazty.smart.ui.components.ElasticEntrance
import com.mahfazty.smart.ui.components.ShimmerBand
import com.mahfazty.smart.ui.components.bounceClick
import com.mahfazty.smart.ui.dialogs.ContributeDialog
import com.mahfazty.smart.ui.dialogs.GoalDialog
import com.mahfazty.smart.ui.dialogs.InsufficientSheet
import com.mahfazty.smart.ui.theme.LocalAppColors
import com.mahfazty.smart.ui.theme.Motion
import com.mahfazty.smart.ui.theme.rememberReduceMotion
import com.mahfazty.smart.ui.viewmodels.GoalsUiState
import com.mahfazty.smart.ui.viewmodels.InsufficientData

@Composable
fun GoalsScreen(
    state: GoalsUiState,
    insufficient: InsufficientData?,
    pending: com.mahfazty.smart.ui.flow.PendingWalletAction?,
    toast: kotlinx.coroutines.flow.SharedFlow<String>,
    onAddGoal: (String, Double, Double, String) -> Unit,
    onContribute: (Goal, Boolean, Double) -> Unit,
    onDeleteGoal: (Goal) -> Unit,
    onDismissInsufficient: () -> Unit,
    onQuickWithdrawGoal: (Long, String, Double) -> Unit,
    onQuickWithdrawSavings: (Double) -> Unit,
    onClearPending: () -> Unit,
) {
    val snackbar = remember { SnackbarHostState() }
    LaunchedEffect(Unit) { toast.collect { snackbar.showSnackbar(it) } }

    var showAdd by remember { mutableStateOf(false) }
    var contributing by remember { mutableStateOf<Goal?>(null) }
    var contributeAdd by remember { mutableStateOf(true) }
    var deleting by remember { mutableStateOf<Goal?>(null) }

    // اهتزاز القائمة عند فشل المساهمة (معايرة 2026): تنبيه جسدي ناعم بدل التجاهل
    val reduceMotion = rememberReduceMotion()
    val failShake = remember { androidx.compose.animation.core.Animatable(0f) }
    LaunchedEffect(insufficient) {
        if (insufficient != null && !reduceMotion) {
            failShake.snapTo(0f)
            kotlinx.coroutines.delay(250)
            failShake.animateTo(targetValue = 1f, animationSpec = Motion.softShake)
        }
    }

    // 🎉 احتفال عند إتمام هدف (عبور 100%) — أول ظهور لا يحتسب حتى لا نحتفل عند فتح الشاشة
    var celebrateKey by remember { mutableStateOf(0L) }
    val completionSeen = remember { mutableStateMapOf<Long, Boolean>() }
    state.goals.forEach { g ->
        val done = g.progress >= 1f
        LaunchedEffect(g.goal.id) {
            completionSeen.putIfAbsent(g.goal.id, done)
        }
        LaunchedEffect(g.goal.id, done) {
            val seen = completionSeen[g.goal.id]
            if (seen == false && done) {
                completionSeen[g.goal.id] = true
                celebrateKey = System.currentTimeMillis()
            }
        }
    }

    androidx.compose.foundation.layout.Box {
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
                .padding(padding)
                .graphicsLayer { translationX = failShake.value },
            contentPadding = androidx.compose.foundation.layout.PaddingValues(bottom = 90.dp),
        ) {
            item {
                ElasticEntrance(0) {
                    Text(
                        "أهدافك 🎯",
                        style = MaterialTheme.typography.titleLarge,
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp),
                    )
                }
            }
            if (state.goals.isEmpty()) {
                item { ElasticEntrance(1) { EmptyState("🎯", "لا توجد أهداف — أضف هدفك الأول") } }
            } else {
                itemsIndexed(state.goals, key = { _, g -> g.goal.id }) { index, g ->
                    ElasticEntrance(index + 1) {
                        GoalCard(
                            goal = g,
                            currency = state.currency,
                            hidden = state.hideBalance,
                            onAdd = { contributing = g.goal; contributeAdd = true },
                            onWithdraw = { contributing = g.goal; contributeAdd = false },
                            onDelete = { deleting = g.goal },
                        )
                    }
                }
            }
            item {
                ElasticEntrance(state.goals.size + 1) {
                AppCard(Modifier.padding(horizontal = 20.dp, vertical = 8.dp)) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text("💡", fontSize = 28.sp)
                        Spacer(Modifier.width(12.dp))
                        Column {
                            Text("نصيحة ذكية", style = MaterialTheme.typography.labelMedium)
                            Text(
                                "حاول ادخار 20% من راتبك كل شهر لتحقيق أهدافك أسرع!",
                                style = MaterialTheme.typography.bodySmall,
                                color = LocalAppColors.current.muted,
                            )
                        }
                    }
                }
                }
            }
        }
        }
        if (celebrateKey != 0L) {
            key(celebrateKey) { ConfettiOverlay(onFinished = { celebrateKey = 0L }) }
        }
    }

    if (showAdd) {
        GoalDialog(
            onDismiss = { showAdd = false },
            onSave = { name, target, opening, icon ->
                showAdd = false
                onAddGoal(name, target, opening, icon)
            },
        )
    }
    val pendingContribute = pending as? com.mahfazty.smart.ui.flow.PendingWalletAction.Contribute
    val contributeGoal = contributing ?: pendingContribute?.let { p ->
        state.goals.firstOrNull { it.goal.id == p.goalId }?.goal
    }
    if (insufficient == null && contributeGoal != null) {
        ContributeDialog(
            goalName = contributeGoal.name,
            add = pendingContribute?.add ?: contributeAdd,
            currency = state.currency,
            initialAmount = pendingContribute?.amount,
            onDismiss = { contributing = null; onClearPending() },
            onSave = { add, amount ->
                contributing = null
                onContribute(contributeGoal, add, amount)
            },
        )
    }
    deleting?.let { goal ->
        ConfirmDialog(
            title = "حذف الهدف؟",
            message = "سيُحذف الهدف وتبقى عملياته السابقة في السجل.",
            onConfirm = { deleting = null; onDeleteGoal(goal) },
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
private fun GoalCard(
    goal: GoalWithSaved,
    currency: String,
    hidden: Boolean,
    onAdd: () -> Unit,
    onWithdraw: () -> Unit,
    onDelete: () -> Unit,
) {
    AppCard(Modifier.padding(horizontal = 20.dp, vertical = 6.dp)) {
        Column(Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(goal.goal.icon, fontSize = 28.sp)
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(goal.goal.name, style = MaterialTheme.typography.titleMedium)
                    Text(
                        "${if (hidden) "•••" else Money.fmt(goal.saved)} من ${Money.fmt(goal.goal.target)} $currency",
                        style = MaterialTheme.typography.bodySmall,
                        color = LocalAppColors.current.muted,
                    )
                }
                IconButton(onClick = onDelete) {
                    Text("🗑️", fontSize = 18.sp)
                }
            }
            Spacer(Modifier.height(10.dp))
            val progress by animateFloatAsState(
                targetValue = goal.progress,
                animationSpec = Motion.springSmooth,
                label = "goalProgress",
            )
            Box {
                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(10.dp)
                        .clip(RoundedCornerShape(5.dp)),
                    color = MaterialTheme.colorScheme.primary,
                    trackColor = LocalAppColors.current.chipBg,
                )
                ShimmerBand()
                // توهج أخضر عند الاكتمال 100% (معايرة 2026)
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
            Spacer(Modifier.height(6.dp))
            Text(
                "${(goal.progress * 100).toInt()}% مكتمل",
                style = MaterialTheme.typography.labelSmall,
                color = LocalAppColors.current.muted,
            )
            Spacer(Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = onAdd,
                    modifier = Modifier.weight(1f).bounceClick(),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                    shape = RoundedCornerShape(12.dp),
                ) { Text("➕ إضافة", fontSize = 12.sp) }
                Button(
                    onClick = onWithdraw,
                    modifier = Modifier.weight(1f).bounceClick(),
                    colors = ButtonDefaults.buttonColors(containerColor = LocalAppColors.current.chipBg),
                    shape = RoundedCornerShape(12.dp),
                ) { Text("💸 سحب", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant) }
            }
        }
    }
}
