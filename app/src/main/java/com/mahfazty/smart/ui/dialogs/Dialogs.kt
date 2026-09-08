package com.mahfazty.smart.ui.dialogs

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.keyframes
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mahfazty.smart.domain.Money
import com.mahfazty.smart.domain.allCategories
import com.mahfazty.smart.domain.categoryIcon
import com.mahfazty.smart.domain.categoryName
import com.mahfazty.smart.domain.model.AppSettings
import com.mahfazty.smart.domain.model.CategoryKind
import com.mahfazty.smart.domain.model.Transaction
import com.mahfazty.smart.domain.model.TxType
import com.mahfazty.smart.domain.model.Wallet
import com.mahfazty.smart.ui.components.AmountField
import com.mahfazty.smart.ui.components.AppSheet
import com.mahfazty.smart.ui.components.AppTextField
import com.mahfazty.smart.ui.components.SegmentedSwitch
import com.mahfazty.smart.ui.components.SheetFieldEntrance
import com.mahfazty.smart.ui.components.bounceClick
import com.mahfazty.smart.ui.theme.LocalAppColors
import com.mahfazty.smart.ui.theme.Motion
import com.mahfazty.smart.ui.theme.rememberReduceMotion
import com.mahfazty.smart.ui.viewmodels.InsufficientData
import com.mahfazty.smart.ui.viewmodels.Suggestion

// =====================================================================
// إضافة / تعديل عملية مالية
// =====================================================================

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AddTransactionDialog(
    settings: AppSettings,
    bank: Double,
    cash: Double,
    initialType: TxType,
    initialAmount: Double? = null,
    initialNote: String? = null,
    initialCategory: String? = null,
    initialWallet: Wallet? = null,
    onDismiss: () -> Unit,
    onSave: (TxType, Double, String, String?, Wallet) -> Unit,
) {
    var type by remember { mutableStateOf(initialType) }
    var wallet by remember { mutableStateOf(initialWallet ?: if (initialType == TxType.EXPENSE) Wallet.CASH else Wallet.BANK) }
    var amount by remember { mutableStateOf(if (initialAmount != null && initialAmount > 0) Money.input(initialAmount) else "") }
    var note by remember { mutableStateOf(initialNote.orEmpty()) }
    val categories = allCategories(settings)
    val cats = if (type == TxType.EXPENSE) categories[CategoryKind.EXPENSE] else categories[CategoryKind.INCOME]
    var categoryId by remember { mutableStateOf(initialCategory ?: cats?.firstOrNull()?.id ?: "other") }

    AppSheet(title = "إضافة عملية جديدة", onDismiss = onDismiss) {
        Column(Modifier.padding(horizontal = 20.dp)) {
            SheetFieldEntrance(0) {
                SegmentedSwitch(
                    options = listOf(TxType.EXPENSE to "⬆️ مصروف", TxType.INCOME to "⬇️ دخل"),
                    selected = type,
                    onSelect = {
                        type = it
                        wallet = if (it == TxType.EXPENSE) Wallet.CASH else Wallet.BANK
                        val newCats = if (it == TxType.EXPENSE) categories[CategoryKind.EXPENSE] else categories[CategoryKind.INCOME]
                        categoryId = newCats?.firstOrNull()?.id ?: "other"
                    },
                    selectedColor = if (type == TxType.EXPENSE) LocalAppColors.current.red else LocalAppColors.current.green,
                )
            }
            Spacer(Modifier.height(12.dp))
            SheetFieldEntrance(1) {
                Text("من أي صندوق؟", style = MaterialTheme.typography.labelMedium)
                Spacer(Modifier.height(6.dp))
                SegmentedSwitch(
                    options = listOf(Wallet.CASH to "💵 ${settings.cashName}", Wallet.BANK to "🏦 ${settings.bankName}"),
                    selected = wallet,
                    onSelect = { wallet = it },
                    selectedColor = MaterialTheme.colorScheme.primary,
                )
                Text(
                    "🏦 ${settings.bankName}: ${if (settings.hideBalance) "••••" else Money.fmt(bank)}   •   " +
                        "💵 ${settings.cashName}: ${if (settings.hideBalance) "••••" else Money.fmt(cash)}",
                    style = MaterialTheme.typography.labelSmall,
                    color = LocalAppColors.current.muted,
                    modifier = Modifier.padding(top = 6.dp),
                )
            }
            Spacer(Modifier.height(12.dp))
            SheetFieldEntrance(2) { AmountField(amount, { amount = it }, "المبلغ", settings.currency) }
            Spacer(Modifier.height(12.dp))
            SheetFieldEntrance(3) {
                Text("الفئة", style = MaterialTheme.typography.labelMedium)
                Spacer(Modifier.height(6.dp))
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    cats?.forEach { c ->
                        val selected = c.id == categoryId
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .background(
                                    if (selected) MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                                    else MaterialTheme.colorScheme.surfaceVariant,
                                )
                                .clickable { categoryId = c.id }
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(c.icon, fontSize = 18.sp)
                                Text(
                                    c.name,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                    }
                }
            }
            Spacer(Modifier.height(12.dp))
            SheetFieldEntrance(4) { AppTextField(note, { note = it }, "ملاحظة (اختياري)", maxLength = 200) }
            Spacer(Modifier.height(16.dp))
            SheetFieldEntrance(5) {
                Button(
                    onClick = {
                        val amt = Money.parse(amount)
                        if (amt > 0) onSave(type, amt, categoryId, note.ifBlank { null }, wallet)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .bounceClick(),
                    shape = RoundedCornerShape(14.dp),
                ) { Text("حفظ العملية ✅") }
            }
            Spacer(Modifier.height(8.dp))
        }
    }
}

// =====================================================================
// تعديل عملية موجودة
// =====================================================================

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun EditTransactionDialog(
    settings: AppSettings,
    tx: Transaction,
    onDismiss: () -> Unit,
    onSave: (Transaction) -> Unit,
) {
    var type by remember { mutableStateOf(tx.type) }
    var wallet by remember { mutableStateOf(tx.wallet) }
    var amount by remember { mutableStateOf(Money.input(tx.amount)) }
    var note by remember { mutableStateOf(tx.note.orEmpty()) }
    val categories = allCategories(settings)
    val cats = if (type == TxType.INCOME) categories[CategoryKind.INCOME] else categories[CategoryKind.EXPENSE]
    var categoryId by remember { mutableStateOf(tx.category) }

    AppSheet(title = "✏️ تعديل العملية", onDismiss = onDismiss) {
        Column(Modifier.padding(horizontal = 20.dp)) {
            SegmentedSwitch(
                options = listOf(TxType.EXPENSE to "⬆️ مصروف", TxType.INCOME to "⬇️ دخل"),
                selected = type,
                onSelect = {
                    type = it
                    val newCats = if (it == TxType.INCOME) categories[CategoryKind.INCOME] else categories[CategoryKind.EXPENSE]
                    categoryId = newCats?.firstOrNull()?.id ?: "other"
                },
                selectedColor = if (type == TxType.EXPENSE) LocalAppColors.current.red else LocalAppColors.current.green,
            )
            Spacer(Modifier.height(12.dp))
            SegmentedSwitch(
                options = listOf(Wallet.CASH to "💵 ${settings.cashName}", Wallet.BANK to "🏦 ${settings.bankName}"),
                selected = wallet,
                onSelect = { wallet = it },
                selectedColor = MaterialTheme.colorScheme.primary,
            )
            Spacer(Modifier.height(12.dp))
            AmountField(amount, { amount = it }, "المبلغ", settings.currency)
            Spacer(Modifier.height(12.dp))
            Text("الفئة", style = MaterialTheme.typography.labelMedium)
            Spacer(Modifier.height(6.dp))
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                cats?.forEach { c ->
                    val selected = c.id == categoryId
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(
                                if (selected) MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                                else MaterialTheme.colorScheme.surfaceVariant,
                            )
                            .clickable { categoryId = c.id }
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(c.icon, fontSize = 18.sp)
                            Text(
                                c.name,
                                style = MaterialTheme.typography.labelSmall,
                                color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            }
            Spacer(Modifier.height(12.dp))
            AppTextField(note, { note = it }, "ملاحظة", maxLength = 200)
            Spacer(Modifier.height(16.dp))
            Button(
                onClick = {
                    val amt = Money.parse(amount)
                    if (amt > 0) onSave(tx.copy(type = type, amount = amt, category = categoryId, note = note.ifBlank { null }, wallet = wallet))
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .bounceClick(),
                shape = RoundedCornerShape(14.dp),
            ) { Text("حفظ التعديل ✅") }
            Spacer(Modifier.height(8.dp))
        }
    }
}

// =====================================================================
// تفاصيل عملية (من السجل)
// =====================================================================

@Composable
fun TxDetailDialog(
    tx: Transaction,
    currency: String,
    onDismiss: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onDuplicate: () -> Unit,
) {
    androidx.compose.ui.window.Dialog(onDismissRequest = onDismiss) {
        androidx.compose.material3.Surface(
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface,
            shadowElevation = 12.dp,
        ) {
            Column(Modifier.padding(20.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(14.dp)),
                        contentAlignment = Alignment.Center,
                    ) { Text(categoryIcon(tx.category), fontSize = 24.sp) }
                    Spacer(Modifier.width(12.dp))
                    Column {
                        Text(categoryName(tx.category), style = MaterialTheme.typography.titleMedium)
                        Text(
                            com.mahfazty.smart.domain.Dates.dateTime(tx.date),
                            style = MaterialTheme.typography.bodySmall,
                            color = LocalAppColors.current.muted,
                        )
                    }
                }
                Spacer(Modifier.height(14.dp))
                val sign = if (tx.type == TxType.INCOME) "+" else if (tx.type == TxType.EXPENSE) "-" else ""
                Text(
                    "$sign${Money.fmt(tx.amount)} $currency",
                    style = MaterialTheme.typography.headlineMedium,
                    color = when {
                        tx.type == TxType.INCOME -> LocalAppColors.current.green
                        tx.type == TxType.EXPENSE -> LocalAppColors.current.red
                        else -> MaterialTheme.colorScheme.primary
                    },
                )
                if (!tx.note.isNullOrBlank()) {
                    Spacer(Modifier.height(6.dp))
                    Text("📝 ${tx.note}", style = MaterialTheme.typography.bodyMedium)
                }
                Text(
                    "الصندوق: ${if (tx.wallet == Wallet.BANK) "🏦 البنك" else "💵 الكاش"}",
                    style = MaterialTheme.typography.bodySmall,
                    color = LocalAppColors.current.muted,
                    modifier = Modifier.padding(top = 4.dp),
                )
                Spacer(Modifier.height(16.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = onDelete,
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = LocalAppColors.current.red),
                        shape = RoundedCornerShape(12.dp),
                    ) { Text("🗑️ حذف", fontSize = 12.sp) }
                    Button(
                        onClick = onDuplicate,
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                        shape = RoundedCornerShape(12.dp),
                    ) { Text("📋 تكرار", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant) }
                    Button(
                        onClick = onEdit,
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                        shape = RoundedCornerShape(12.dp),
                    ) { Text("✏️ تعديل", fontSize = 12.sp) }
                }
            }
        }
    }
}

// =====================================================================
// تحويل بين الصناديق
// =====================================================================

@Composable
fun TransferDialog(
    settings: AppSettings,
    bank: Double,
    cash: Double,
    initialDirection: String? = null,
    initialAmount: Double? = null,
    initialNote: String? = null,
    onDismiss: () -> Unit,
    onSave: (String, Double, String?) -> Unit,
) {
    var direction by remember { mutableStateOf(initialDirection ?: "bank_to_cash") }
    var amount by remember { mutableStateOf(if (initialAmount != null && initialAmount > 0) Money.input(initialAmount) else "") }
    var note by remember { mutableStateOf(initialNote.orEmpty()) }

    AppSheet(title = "🔄 تحويل بين البنك والكاش", onDismiss = onDismiss) {
        Column(Modifier.padding(horizontal = 20.dp)) {
            SheetFieldEntrance(0) {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(16.dp))
                            .background(LocalAppColors.current.chipBg)
                            .padding(14.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("🏦", fontSize = 24.sp)
                            Text(settings.bankName, style = MaterialTheme.typography.labelSmall)
                            Text(Money.fmt(bank), style = MaterialTheme.typography.labelLarge)
                        }
                    }
                    Box(contentAlignment = Alignment.Center) { Text("↔️", fontSize = 20.sp) }
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(16.dp))
                            .background(LocalAppColors.current.chipBg)
                            .padding(14.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("💵", fontSize = 24.sp)
                            Text(settings.cashName, style = MaterialTheme.typography.labelSmall)
                            Text(Money.fmt(cash), style = MaterialTheme.typography.labelLarge)
                        }
                    }
                }
            }
            Spacer(Modifier.height(14.dp))
            SheetFieldEntrance(1) {
                SegmentedSwitch(
                    options = listOf(
                        "bank_to_cash" to "🏦→💵 سحب للكاش",
                        "cash_to_bank" to "💵→🏦 إيداع للبنك",
                    ),
                    selected = direction,
                    onSelect = { direction = it },
                    selectedColor = MaterialTheme.colorScheme.primary,
                )
            }
            Spacer(Modifier.height(14.dp))
            AmountField(amount, { amount = it }, "المبلغ", settings.currency)
            Spacer(Modifier.height(12.dp))
            AppTextField(note, { note = it }, "ملاحظة (اختياري)", maxLength = 200)
            Spacer(Modifier.height(16.dp))
            // إضافة 13.2 من تقرير الفحص: تأكيد صريح قبل تنفيذ التحويل بين الصناديق
            var confirmStep by remember { mutableStateOf(false) }
            Button(
                onClick = {
                    val amt = Money.parse(amount)
                    if (amt > 0) confirmStep = true
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .bounceClick(),
                shape = RoundedCornerShape(14.dp),
            ) { Text("تأكيد التحويل ✅") }
            if (confirmStep) {
                val amt = Money.parse(amount)
                com.mahfazty.smart.ui.components.ConfirmDialog(
                    title = "تنفيذ التحويل؟",
                    message = "سيتم ${if (direction == "bank_to_cash") "سحب ${Money.fmt(amt)} ${settings.currency} من ${settings.bankName} إلى ${settings.cashName}" else "إيداع ${Money.fmt(amt)} ${settings.currency} من ${settings.cashName} إلى ${settings.bankName}"}.",
                    confirmText = "تنفيذ",
                    danger = false,
                    onConfirm = {
                        confirmStep = false
                        onSave(direction, amt, note.ifBlank { null })
                    },
                    onDismiss = { confirmStep = false },
                )
            }
            Text(
                "قاعدة: لا يمكن أن يصبح أي صندوق سالباً",
                style = MaterialTheme.typography.labelSmall,
                color = LocalAppColors.current.muted,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 10.dp),
            )
            Spacer(Modifier.height(8.dp))
        }
    }
}

// =====================================================================
// تعديل رصيد البنك الافتتاحي
// =====================================================================

@Composable
fun EditBankDialog(
    currency: String,
    current: Double,
    onDismiss: () -> Unit,
    onSave: (Double) -> Unit,
) {
    var amount by remember { mutableStateOf(if (current > 0) Money.input(current) else "") }
    AppSheet(title = "🏦 رصيد البنك", onDismiss = onDismiss) {
        Column(Modifier.padding(horizontal = 20.dp)) {
            AmountField(amount, { amount = it }, "كم رصيدك الحالي في البنك؟", currency)
            Text(
                "هذا الرصيد هو نقطة البداية، وكل العمليات ستضاف أو تخصم منه.",
                style = MaterialTheme.typography.bodySmall,
                color = LocalAppColors.current.muted,
                modifier = Modifier.padding(vertical = 10.dp),
            )
            Button(
                onClick = { onSave(Money.parse(amount)) },
                modifier = Modifier
                    .fillMaxWidth()
                    .bounceClick(),
                shape = RoundedCornerShape(14.dp),
            ) { Text("حفظ الرصيد") }
            Spacer(Modifier.height(8.dp))
        }
    }
}

// =====================================================================
// هدف جديد
// =====================================================================

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun GoalDialog(
    onDismiss: () -> Unit,
    onSave: (String, Double, Double, String) -> Unit,
) {
    var name by remember { mutableStateOf("") }
    var target by remember { mutableStateOf("") }
    var opening by remember { mutableStateOf("") }
    var icon by remember { mutableStateOf("💻") }
    val icons = listOf("💻" to "تقنية", "🚗" to "سيارة", "🏠" to "منزل", "✈️" to "سفر", "📱" to "جوال", "🎓" to "دراسة", "💍" to "زواج", "🎮" to "ترفيه")

    AppSheet(title = "🎯 هدف جديد", onDismiss = onDismiss) {
        Column(Modifier.padding(horizontal = 20.dp)) {
            SheetFieldEntrance(0) { AppTextField(name, { name = it }, "ما الذي تريد شراءه؟") }
            Spacer(Modifier.height(10.dp))
            SheetFieldEntrance(1) { AmountField(target, { target = it }, "المبلغ المطلوب", "") }
            Spacer(Modifier.height(10.dp))
            SheetFieldEntrance(2) { AmountField(opening, { opening = it }, "المبلغ المدخر حالياً (اختياري)", "") }
            Spacer(Modifier.height(10.dp))
            SheetFieldEntrance(3) {
                Text("اختر أيقونة", style = MaterialTheme.typography.labelMedium)
                Spacer(Modifier.height(6.dp))
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                icons.forEach { (ic, label) ->
                    val selected = ic == icon
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(
                                if (selected) MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                                else MaterialTheme.colorScheme.surfaceVariant,
                            )
                            .clickable { icon = ic }
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(ic, fontSize = 18.sp)
                            Text(
                                label,
                                style = MaterialTheme.typography.labelSmall,
                                color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
                }
            }
            Spacer(Modifier.height(16.dp))
            SheetFieldEntrance(4) {
                Button(
                    onClick = {
                        val t = Money.parse(target)
                        if (name.isNotBlank() && t > 0) onSave(name.trim(), t, Money.parse(opening), icon)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .bounceClick(),
                    shape = RoundedCornerShape(14.dp),
                ) { Text("إضافة الهدف 🚀") }
            }
            Spacer(Modifier.height(8.dp))
        }
    }
}

// =====================================================================
// مساهمة في هدف (إضافة/سحب)
// =====================================================================

@Composable
fun ContributeDialog(
    goalName: String,
    add: Boolean,
    currency: String,
    initialAmount: Double? = null,
    onDismiss: () -> Unit,
    onSave: (Boolean, Double) -> Unit,
) {
    var mode by remember { mutableStateOf(add) }
    var amount by remember { mutableStateOf(if (initialAmount != null && initialAmount > 0) Money.input(initialAmount) else "") }
    // إضافة 13.1 من تقرير الفحص: تأكيد قبل تنفيذ إضافة/سحب الأهداف
    var confirmStep by remember { mutableStateOf(false) }

    AppSheet(title = if (mode) "إضافة للهدف" else "سحب من الهدف", onDismiss = onDismiss) {
        Column(Modifier.padding(horizontal = 20.dp)) {
            Text("🎯 $goalName", style = MaterialTheme.typography.labelMedium, color = LocalAppColors.current.muted)
            Spacer(Modifier.height(12.dp))
            SegmentedSwitch(
                options = listOf(true to "➕ إضافة", false to "💸 سحب"),
                selected = mode,
                onSelect = { mode = it },
                selectedColor = if (mode) LocalAppColors.current.green else MaterialTheme.colorScheme.onSurface,
            )
            Spacer(Modifier.height(12.dp))
            AmountField(amount, { amount = it }, "المبلغ", currency)
            Text(
                if (mode) "سيُخصم المبلغ من البنك ويُضاف للهدف" else "سيُسحب من الهدف ويعود للبنك",
                style = MaterialTheme.typography.labelSmall,
                color = LocalAppColors.current.muted,
                modifier = Modifier.padding(vertical = 8.dp),
            )
            Button(
                onClick = {
                    val amt = Money.parse(amount)
                    if (amt > 0) confirmStep = true
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .bounceClick(),
                shape = RoundedCornerShape(14.dp),
            ) { Text(if (mode) "إضافة الآن ✅" else "تأكيد السحب") }
            if (confirmStep) {
                val amt = Money.parse(amount)
                com.mahfazty.smart.ui.components.ConfirmDialog(
                    title = if (mode) "إضافة للهدف؟" else "سحب من الهدف؟",
                    message = "🎯 $goalName\nالمبلغ: ${Money.fmt(amt)} $currency\n\n" +
                        if (mode) "سيُخصم من البنك ويُضاف لمدخر الهدف" else "سيُسحب من مدخر الهدف ويعود إلى البنك",
                    confirmText = "تنفيذ",
                    danger = false,
                    onConfirm = {
                        confirmStep = false
                        onSave(mode, amt)
                    },
                    onDismiss = { confirmStep = false },
                )
            }
            Spacer(Modifier.height(8.dp))
        }
    }
}

// =====================================================================
// إضافة/سحب من الادخار
// =====================================================================

@Composable
fun SavingsAmountDialog(
    title: String,
    currency: String,
    note: String,
    initialAmount: Double? = null,
    onDismiss: () -> Unit,
    onSave: (Double) -> Unit,
) {
    var amount by remember { mutableStateOf(if (initialAmount != null && initialAmount > 0) Money.input(initialAmount) else "") }
    var confirmStep by remember { mutableStateOf(false) }
    AppSheet(title = title, onDismiss = onDismiss) {
        Column(Modifier.padding(horizontal = 20.dp)) {
            AmountField(amount, { amount = it }, "المبلغ", currency)
            Text(
                note,
                style = MaterialTheme.typography.labelSmall,
                color = LocalAppColors.current.muted,
                modifier = Modifier.padding(vertical = 8.dp),
            )
            Button(
                onClick = {
                    val amt = Money.parse(amount)
                    if (amt > 0) confirmStep = true
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .bounceClick(),
                shape = RoundedCornerShape(14.dp),
            ) { Text("تأكيد ✅") }
            // إضافة 13.1 من تقرير الفحص: تأكيد المبلغ قبل تنفيذ حركات الادخار
            if (confirmStep) {
                val amt = Money.parse(amount)
                com.mahfazty.smart.ui.components.ConfirmDialog(
                    title = "تنفيذ العملية؟",
                    message = "$title\nالمبلغ: ${Money.fmt(amt)} $currency" + if (note.isBlank()) "" else "\n\n$note",
                    confirmText = "تنفيذ",
                    danger = false,
                    onConfirm = {
                        confirmStep = false
                        onSave(amt)
                    },
                    onDismiss = { confirmStep = false },
                )
            }
            Spacer(Modifier.height(8.dp))
        }
    }
}

// =====================================================================
// نافذة الرصيد غير الكافي (مع اقتراحات السحب الذكية)
// =====================================================================

@Composable
fun InsufficientSheet(
    data: InsufficientData,
    currency: String,
    onClose: () -> Unit,
    onWithdrawGoal: (Long, String, Double) -> Unit,
    onWithdrawSavings: (Double) -> Unit,
) {
    val shortage = (data.needed - data.current).coerceAtLeast(0.0)
    // اهتزاز هادئ (الملحق 46): موجات بطيئة بسعة صغيرة — تنبيه ناعم لا إنذار
    val reduceMotion = rememberReduceMotion()
    val shake = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        if (!reduceMotion) {
            kotlinx.coroutines.delay(320)
            shake.animateTo(targetValue = 1f, animationSpec = Motion.softShake)
        }
    }
    AppSheet(title = "❌ رصيد غير كافٍ", onDismiss = onClose) {
        Column(Modifier.padding(horizontal = 20.dp)) {
            Text("🚫", fontSize = 36.sp, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
            Spacer(Modifier.height(8.dp))
            Text(
                "لا يمكن تنفيذ العملية",
                style = MaterialTheme.typography.titleMedium,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )
            Text(
                "لا يمكنك ${data.operation} بمبلغ ${Money.fmt(data.needed)} $currency لأن رصيدك الحالي${data.walletLabel?.let { " في $it" } ?: ""} ${Money.fmt(data.current)} $currency لا يكفي.",
                style = MaterialTheme.typography.bodySmall,
                color = LocalAppColors.current.muted,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(vertical = 8.dp),
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .graphicsLayer { translationX = shake.value }
                    .clip(RoundedCornerShape(14.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .padding(12.dp),
                horizontalArrangement = Arrangement.SpaceAround,
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("رصيدك الحالي", style = MaterialTheme.typography.labelSmall, color = LocalAppColors.current.muted)
                    Text(Money.fmt(data.current), style = MaterialTheme.typography.labelLarge)
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("المطلوب", style = MaterialTheme.typography.labelSmall, color = LocalAppColors.current.muted)
                    Text(Money.fmt(data.needed), style = MaterialTheme.typography.labelLarge)
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("النقص", style = MaterialTheme.typography.labelSmall, color = LocalAppColors.current.muted)
                    val inf = androidx.compose.animation.core.rememberInfiniteTransition(label = "shortagePulse")
                    val pulse by inf.animateFloat(
                        initialValue = 0.45f,
                        targetValue = 1f,
                        animationSpec = androidx.compose.animation.core.infiniteRepeatable(
                            androidx.compose.animation.core.tween(450, easing = androidx.compose.animation.core.FastOutSlowInEasing),
                            androidx.compose.animation.core.RepeatMode.Reverse,
                        ),
                        label = "shortageAlpha",
                    )
                    Text(
                        Money.fmt(shortage),
                        style = MaterialTheme.typography.labelLarge,
                        color = LocalAppColors.current.red.copy(alpha = pulse),
                    )
                }
            }
            Spacer(Modifier.height(14.dp))
            Text("💡 يمكنك السحب من:", style = MaterialTheme.typography.labelMedium)
            Spacer(Modifier.height(8.dp))
            if (data.suggestions.isEmpty()) {
                Text(
                    "لا يوجد أهداف أو ادخار للسحب منه. أضف دخل أولاً 💰",
                    style = MaterialTheme.typography.bodySmall,
                    color = LocalAppColors.current.red,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(LocalAppColors.current.red.copy(alpha = 0.08f))
                        .padding(10.dp),
                )
            } else {
                data.suggestions.forEach { s ->
                    SuggestionRow(s, currency) {
                        if (s.kind == "goal" && s.id != null) onWithdrawGoal(s.id, s.name, s.amount)
                        else onWithdrawSavings(s.amount)
                    }
                    Spacer(Modifier.height(6.dp))
                }
            }
            Spacer(Modifier.height(10.dp))
            Button(
                onClick = onClose,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.onSurface),
                shape = RoundedCornerShape(14.dp),
            ) { Text("حسناً، فهمت") }
            Spacer(Modifier.height(8.dp))
        }
    }
}

@Composable
private fun SuggestionRow(s: Suggestion, currency: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(s.title, style = MaterialTheme.typography.labelMedium)
            Text("متاح: ${Money.fmt(s.available)} $currency", style = MaterialTheme.typography.labelSmall, color = LocalAppColors.current.muted)
        }
        Button(
            onClick = onClick,
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
            shape = RoundedCornerShape(10.dp),
        ) { Text("سحب ${Money.fmt(s.amount)}", fontSize = 11.sp) }
    }
}
