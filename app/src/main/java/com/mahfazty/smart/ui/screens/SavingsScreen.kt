package com.mahfazty.smart.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.ui.graphics.graphicsLayer
import com.mahfazty.smart.domain.Money
import com.mahfazty.smart.domain.model.DayBar
import com.mahfazty.smart.ui.components.AppCard
import com.mahfazty.smart.ui.components.AnimatedNumber
import com.mahfazty.smart.ui.components.ElasticEntrance
import com.mahfazty.smart.ui.components.FloatingIcon
import com.mahfazty.smart.ui.components.animatedGradient
import com.mahfazty.smart.ui.components.bounceClick
import com.mahfazty.smart.ui.components.BarChart
import com.mahfazty.smart.ui.components.SectionHeader
import com.mahfazty.smart.ui.dialogs.SavingsAmountDialog
import com.mahfazty.smart.ui.theme.LocalAppColors
import com.mahfazty.smart.ui.theme.Motion
import com.mahfazty.smart.ui.theme.rememberReduceMotion
import com.mahfazty.smart.ui.viewmodels.SavingsUiState

@Composable
fun SavingsScreen(
    state: SavingsUiState,
    insufficient: com.mahfazty.smart.ui.viewmodels.InsufficientData?,
    pending: com.mahfazty.smart.ui.flow.PendingWalletAction?,
    toast: kotlinx.coroutines.flow.SharedFlow<String>,
    onAddToSavings: (Double) -> Unit,
    onWithdrawSavings: (Double) -> Unit,
    onDismissInsufficient: () -> Unit,
    onQuickWithdrawGoal: (Long, String, Double) -> Unit,
    onClearPending: () -> Unit,
) {
    val snackbar = remember { SnackbarHostState() }
    LaunchedEffect(Unit) { toast.collect { snackbar.showSnackbar(it) } }

    // الحصالة تتمايل احتفالاً عند ارتفاع المدخرات (مهارة التفكير 13)
    val reduceMotion = rememberReduceMotion()
    val pigRotation = remember { Animatable(0f) }
    var previousTotal by remember { mutableStateOf(state.total) }
    LaunchedEffect(state.total) {
        if (state.total > previousTotal && !reduceMotion) {
            pigRotation.animateTo(-10f, tween(110))
            pigRotation.animateTo(10f, tween(150))
            pigRotation.animateTo(-6f, tween(120))
            pigRotation.animateTo(0f, tween(100))
        }
        previousTotal = state.total
    }

    var showAdd by remember { mutableStateOf(false) }
    var showWithdraw by remember { mutableStateOf(false) }
    var calcTarget by remember { mutableStateOf("") }
    var calcMonths by remember { mutableStateOf("") }

    Scaffold(snackbarHost = { SnackbarHost(snackbar) }) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(bottom = 24.dp),
        ) {
            ElasticEntrance(0) {
            Text(
                "مدخراتك 🏦",
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp),
            )
            }

            // ===== كارت الحصالة =====
            ElasticEntrance(1) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
                    .clip(RoundedCornerShape(24.dp))
                    .background(animatedGradient(MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.secondary))
                    .padding(18.dp),
            ) {
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Column {
                            Text("صندوق الادخار", style = MaterialTheme.typography.titleMedium, color = Color.White)
                            Text(
                                "مدخراتك الآمنة للمستقبل",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.White.copy(alpha = 0.85f),
                            )
                        }
                        FloatingIcon {
                            Text("🐷", fontSize = 36.sp, modifier = Modifier.graphicsLayer { rotationZ = pigRotation.value })
                        }
                    }
                    Spacer(Modifier.height(12.dp))
                    AnimatedNumber(
                        target = state.total,
                        hidden = state.hideBalance,
                        format = { "${Money.fmt(it)} ${state.currency}" },
                        style = MaterialTheme.typography.headlineMedium.copy(color = Color.White),
                    )
                    Text(
                        "${(state.percent * 100).toInt()}% من هدفك الشهري",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(alpha = 0.9f),
                    )
                    Spacer(Modifier.height(12.dp))
                    val animatedPercent by animateFloatAsState(
                        targetValue = state.percent,
                        animationSpec = Motion.springSmooth,
                        label = "savingsPercent",
                    )
                    LinearProgressIndicator(
                        progress = { animatedPercent },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(10.dp)
                            .clip(RoundedCornerShape(5.dp)),
                        color = Color.White,
                        trackColor = Color.White.copy(alpha = 0.2f),
                    )
                    Spacer(Modifier.height(16.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            onClick = { showAdd = true },
                            modifier = Modifier.weight(1f).bounceClick(),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color.White,
                                contentColor = Color(0xFF2D3436),
                            ),
                            shape = RoundedCornerShape(14.dp),
                        ) { Text("➕ إضافة للادخار", fontSize = 12.sp) }
                        Button(
                            onClick = { showWithdraw = true },
                            modifier = Modifier.weight(1f).bounceClick(),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color.White.copy(alpha = 0.2f),
                                contentColor = Color.White,
                            ),
                            shape = RoundedCornerShape(14.dp),
                        ) { Text("سحب", fontSize = 12.sp) }
                    }
                }
            }

            // ===== حاسبة الادخار =====
            }

            ElasticEntrance(2) {
            SectionHeader("🧮 حاسبة الادخار")
            AppCard(Modifier.padding(horizontal = 20.dp)) {
                Column(Modifier.padding(16.dp)) {
                    Text(
                        "احسب مبلغ الادخار الشهري لتحقيق هدفك",
                        style = MaterialTheme.typography.bodySmall,
                        color = LocalAppColors.current.muted,
                    )
                    Spacer(Modifier.height(10.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        OutlinedTextField(
                            value = calcTarget,
                            onValueChange = { calcTarget = it.filter { c -> c.isDigit() || c == '.' } },
                            modifier = Modifier.weight(1f),
                            label = { Text("المبلغ المطلوب") },
                            shape = RoundedCornerShape(14.dp),
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = MaterialTheme.colorScheme.primary,
                                unfocusedBorderColor = LocalAppColors.current.border,
                            ),
                        )
                        OutlinedTextField(
                            value = calcMonths,
                            onValueChange = { calcMonths = it.filter { c -> c.isDigit() } },
                            modifier = Modifier.weight(1f),
                            label = { Text("خلال (شهر)") },
                            shape = RoundedCornerShape(14.dp),
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = MaterialTheme.colorScheme.primary,
                                unfocusedBorderColor = LocalAppColors.current.border,
                            ),
                        )
                    }
                    Spacer(Modifier.height(10.dp))
                    val target = calcTarget.toDoubleOrNull()
                    val months = calcMonths.toDoubleOrNull()
                    val hasResult = target != null && months != null && months > 0
                    // قيم آمنة محسوبة خارج AnimatedContent (بلا تأكيدات قسرية !!)
                    val targetValue = target ?: 0.0
                    val monthsValue = months ?: 0.0
                    val monthly = if (hasResult) targetValue / monthsValue else 0.0
                    val weekly = if (hasResult) targetValue / (monthsValue * 4.3) else 0.0
                    val monthCount = monthsValue.toInt()
                    AnimatedContent(
                        targetState = hasResult,
                        label = "calcResult",
                    ) { has ->
                        if (has) {
                            Column {
                                Text(
                                    "تحتاج تدخر ${Money.fmt(monthly)} ${state.currency} شهرياً",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = MaterialTheme.colorScheme.primary,
                                )
                                Text(
                                    "حوالي ${Money.fmt(weekly)} ${state.currency} أسبوعياً لتحقيق ${Money.fmt(targetValue)} ${state.currency} خلال $monthCount شهر",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = LocalAppColors.current.muted,
                                )
                            }
                        } else {
                            Text(
                                "ادخل المبلغ والمدة لمعرفة الخطة",
                                style = MaterialTheme.typography.bodySmall,
                                color = LocalAppColors.current.muted,
                            )
                        }
                    }
                }
            }
            }

            // ===== تطور المدخرات =====
            ElasticEntrance(3) {
            SectionHeader("📈 تطور مدخراتك")
            AppCard(Modifier.padding(horizontal = 20.dp)) {
                Box(Modifier.padding(14.dp)) {
                    BarChart(
                        state.history.mapIndexed { i, v ->
                            DayBar(state.historyLabels.getOrElse(i) { "" }, v)
                        },
                        MaterialTheme.colorScheme.primary,
                    )
                }
            }
            }
        }
    }

    val pendingAdd = pending as? com.mahfazty.smart.ui.flow.PendingWalletAction.SavingsAdd
    if (insufficient == null && (showAdd || pendingAdd != null)) {
        SavingsAmountDialog(
            title = "إضافة للادخار 🐷",
            currency = state.currency,
            note = "المبلغ سيُخصم من البنك",
            initialAmount = pendingAdd?.amount,
            onDismiss = { showAdd = false; onClearPending() },
            onSave = { amount -> showAdd = false; onAddToSavings(amount) },
        )
    }
    if (showWithdraw) {
        SavingsAmountDialog(
            title = "سحب من الادخار",
            currency = state.currency,
            note = "المبلغ سيعود إلى البنك",
            // تحسين بيانات ناقصة: عرض المتاح ومنع طلب سحب أكبر من المدخر قبل وصوله للمحرك
            available = state.total,
            onDismiss = { showWithdraw = false },
            onSave = { amount -> showWithdraw = false; onWithdrawSavings(amount) },
        )
    }
    insufficient?.let { data ->
        com.mahfazty.smart.ui.dialogs.InsufficientSheet(
            data = data,
            currency = state.currency,
            onClose = onDismissInsufficient,
            onWithdrawGoal = onQuickWithdrawGoal,
            onWithdrawSavings = { /* لا نسحب من الادخار لتمويل إضافة الادخار */ },
        )
    }
}
