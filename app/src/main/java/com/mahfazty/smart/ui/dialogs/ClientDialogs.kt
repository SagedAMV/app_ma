package com.mahfazty.smart.ui.dialogs

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
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
import com.mahfazty.smart.data.TransferDisplay
import com.mahfazty.smart.domain.Dates
import com.mahfazty.smart.domain.Ids
import com.mahfazty.smart.domain.Money
import com.mahfazty.smart.domain.model.Client
import com.mahfazty.smart.domain.model.ClientAccount
import com.mahfazty.smart.domain.model.ClientOperation
import com.mahfazty.smart.domain.model.MaterialItem
import com.mahfazty.smart.domain.model.OpType
import com.mahfazty.smart.domain.model.Wallet
import com.mahfazty.smart.ui.components.AmountField
import com.mahfazty.smart.ui.components.AppSheet
import com.mahfazty.smart.ui.components.AppTextField
import com.mahfazty.smart.ui.components.bounceClick
import com.mahfazty.smart.ui.components.PhotoAvatar
import com.mahfazty.smart.ui.components.PhotoStore
import com.mahfazty.smart.ui.components.SegmentedSwitch
import com.mahfazty.smart.ui.theme.LocalAppColors
import com.mahfazty.smart.ui.viewmodels.InsufficientRealData

// =====================================================================
// عميل (إضافة/تعديل) مع صورة
// إصلاحات: sec-4 (تحقق من الهاتف) • sec-5 (حدود الحقول) • act-5 (كشف التكرار)
//          act-6 (خطأ حفظ الصورة) • data-2 (جهات الاتصال) • data-3 (الحالة)
// =====================================================================

@Composable
fun ClientDialog(
    title: String,
    initial: Client? = null,
    /** عملاء قائمون — لكشف التكرار (إصلاح act-5) */
    existing: List<Client> = emptyList(),
    /** فتح عميل موجود بدل إنشاء مكرر (إصلاح act-5) */
    onOpenExisting: (Long) -> Unit = {},
    onDismiss: () -> Unit,
    onSave: (String, String?, String?, String) -> Unit, // name, phone, photo, status
) {
    val context = LocalContext.current
    var name by remember { mutableStateOf(initial?.name ?: "") }
    var phone by remember { mutableStateOf(initial?.phone ?: "") }
    var photoPath by remember { mutableStateOf(initial?.photoPath) }
    var photoError by remember { mutableStateOf<String?>(null) } // إصلاح act-6
    var status by remember {
        mutableStateOf(initial?.status ?: com.mahfazty.smart.domain.model.ClientStatus.ACTIVE)
    }
    var saveHint by remember { mutableStateOf<String?>(null) }
    var duplicate by remember { mutableStateOf<Client?>(null) }

    // إصلاح act-6: فشل حفظ الصورة لم يعد صامتاً — رسالة واضحة للمستخدم
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let {
            val r = PhotoStore.save(context, it, "client")
            if (r.path != null) {
                photoPath = r.path
                photoError = null
            } else {
                photoError = r.error
            }
        }
    }

    // إصلاح data-2: استيراد من جهات الاتصال — اختيار مفرد (ACTION_PICK) بلا إذن READ_CONTACTS
    val contactPicker = rememberLauncherForActivityResult(ActivityResultContracts.PickContact()) { uri ->
        uri?.let { picked ->
            runCatching {
                var contactId = -1L
                var contactName = ""
                context.contentResolver.query(
                    picked,
                    arrayOf(android.provider.ContactsContract.Contacts._ID, android.provider.ContactsContract.Contacts.DISPLAY_NAME),
                    null, null, null,
                )?.use { c ->
                    if (c.moveToFirst()) {
                        contactId = c.getLong(0)
                        contactName = c.getString(1)
                    }
                }
                if (contactId > 0) {
                    name = contactName
                    // أول رقم هاتف مسجل للعميل
                    context.contentResolver.query(
                        android.provider.ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                        arrayOf(android.provider.ContactsContract.CommonDataKinds.Phone.NUMBER),
                        "${android.provider.ContactsContract.CommonDataKinds.Phone.CONTACT_ID} = ?",
                        arrayOf(contactId.toString()),
                        null,
                    )?.use { c ->
                        if (c.moveToFirst()) phone = c.getString(0)
                    }
                }
            }
        }
    }

    AppSheet(title = title, onDismiss = onDismiss) {
        Column(Modifier.padding(horizontal = 20.dp)) {
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .clickable { picker.launch("image/*") },
                contentAlignment = Alignment.Center,
            ) {
                if (photoPath != null) {
                    PhotoAvatar(photoPath, "👤", 80)
                } else {
                    Text("👤", fontSize = 32.sp)
                }
            }
            Text(
                "📷 اضغط لاختيار صورة العميل",
                style = MaterialTheme.typography.labelSmall,
                color = LocalAppColors.current.muted,
                modifier = Modifier.padding(top = 6.dp),
            )
            photoError?.let { err ->
                Text(
                    "⚠️ $err",
                    style = MaterialTheme.typography.labelSmall,
                    color = LocalAppColors.current.red,
                    modifier = Modifier.padding(top = 4.dp),
                )
            }
            Spacer(Modifier.height(12.dp))
            AppTextField(name, { name = it }, "الاسم *", maxLength = 50)
            Spacer(Modifier.height(10.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                // إصلاح sec-5: حد 20 حرفاً • إصلاح sec-4: لوحة رقمية + تحقق عند الحفظ
                AppTextField(
                    phone, { phone = it }, "هاتف (اختياري)",
                    modifier = Modifier.weight(1f),
                    maxLength = 20,
                    phoneKeypad = true,
                )
                Spacer(Modifier.width(8.dp))
                androidx.compose.material3.TextButton(
                    onClick = { contactPicker.launch(null) },
                ) { Text("👤 جهاتي", fontSize = 12.sp) }
            }
            Spacer(Modifier.height(12.dp))
            Text("حالة العميل", style = MaterialTheme.typography.labelMedium)
            Spacer(Modifier.height(6.dp))
            SegmentedSwitch(
                options = listOf(
                    com.mahfazty.smart.domain.model.ClientStatus.ACTIVE to "🟢 نشط",
                    com.mahfazty.smart.domain.model.ClientStatus.STOPPED to "🟠 موقوف",
                    com.mahfazty.smart.domain.model.ClientStatus.BLOCKED to "🔴 قائمة سوداء",
                ),
                selected = status,
                onSelect = { status = it },
                selectedColor = when (status) {
                    com.mahfazty.smart.domain.model.ClientStatus.STOPPED -> Color(0xFFE17055)
                    com.mahfazty.smart.domain.model.ClientStatus.BLOCKED -> LocalAppColors.current.red
                    else -> LocalAppColors.current.green
                },
            )
            Spacer(Modifier.height(14.dp))
            if (saveHint != null) {
                Text(saveHint!!, color = LocalAppColors.current.red, style = MaterialTheme.typography.labelSmall)
                Spacer(Modifier.height(6.dp))
            }
            Button(
                onClick = {
                    // إصلاح sec-4: تحقق من صيغة الهاتف قبل الحفظ
                    val digits = phone.filter { it.isDigit() }
                    when {
                        name.isBlank() -> saveHint = "اكتب اسم العميل"
                        phone.isNotBlank() &&
                            !(phone.all { it.isDigit() || it in " +()-/#" } && digits.length in 6..15) ->
                            saveHint = "رقم الهاتف غير صالح: أرقام فقط (6-15 رقماً) مع رمز دولة اختياري"
                        else -> {
                            saveHint = null
                            // إصلاح act-5: تنبيه التكرار (اسم أو هاتف مطابق) بدل منع صارم
                            val dup = existing.firstOrNull { c ->
                                c.id != (initial?.id ?: -1L) &&
                                    (c.name.trim().equals(name.trim(), ignoreCase = true) ||
                                        (digits.isNotBlank() && c.phone?.filter { d -> d.isDigit() } == digits))
                            }
                            if (dup != null) duplicate = dup
                            else onSave(name.trim(), phone.ifBlank { null }, photoPath, status)
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .bounceClick(),
                shape = RoundedCornerShape(14.dp),
            ) { Text("حفظ العميل ✅") }
            Spacer(Modifier.height(8.dp))
        }
    }

    duplicate?.let { dup ->
        ConfirmDialog(
            title = "عميل مكرر؟",
            message = "يوجد عميل بالفعل: ${dup.name}${dup.phone?.takeIf { it.isNotBlank() }?.let { " ($it)" } ?: ""}\nإن أنشأت سجلاً جديداً ستتوزع الديون على أكثر من حساب.",
            confirmText = "إنشاء جديد رغم ذلك",
            danger = false,
            extraText = "فتح العميل الموجود",
            onExtra = { duplicate = null; onOpenExisting(dup.id) },
            onConfirm = {
                duplicate = null
                onSave(name.trim(), phone.ifBlank { null }, photoPath, status)
            },
            onDismiss = { duplicate = null },
        )
    }
}

// =====================================================================
// حساب (إضافة/تعديل) مع أيقونة
// =====================================================================

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AccountDialog(
    title: String,
    initial: ClientAccount? = null,
    onDismiss: () -> Unit,
    onSave: (String, String) -> Unit,
) {
    var name by remember { mutableStateOf(initial?.name ?: "") }
    var icon by remember { mutableStateOf(initial?.icon ?: "🏍️") }
    val icons = listOf("🏍️", "💡", "🛒", "🏪", "🚗", "💰")

    AppSheet(title = title, onDismiss = onDismiss) {
        Column(Modifier.padding(horizontal = 20.dp)) {
            AppTextField(name, { name = it }, "اسم الحساب (ماطور، كهرباء...)")
            Spacer(Modifier.height(10.dp))
            Text("أيقونة", style = MaterialTheme.typography.labelMedium)
            Spacer(Modifier.height(6.dp))
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                icons.forEach { ic ->
                    val selected = ic == icon
                    Box(
                        modifier = Modifier
                            .size(46.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(
                                if (selected) MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                                else MaterialTheme.colorScheme.surfaceVariant,
                            )
                            .clickable { icon = ic },
                        contentAlignment = Alignment.Center,
                    ) { Text(ic, fontSize = 22.sp) }
                }
            }
            Spacer(Modifier.height(16.dp))
            Button(
                onClick = { if (name.isNotBlank()) onSave(name.trim(), icon) },
                modifier = Modifier
                    .fillMaxWidth()
                    .bounceClick(),
                shape = RoundedCornerShape(14.dp),
            ) { Text("إضافة ✅") }
            Spacer(Modifier.height(8.dp))
        }
    }
}

// =====================================================================
// عملية له/عليه مع مواد الفاتورة وصورة الإيصال
// =====================================================================

@Composable
fun OperationDialog(
    title: String,
    currency: String,
    realBalance: Double,
    initial: ClientOperation? = null,
    onDismiss: () -> Unit,
    /**
     * onSave: type, amount, note, materials, receipt, isInvoice, invoiceRef,
     *         dueDate (إصلاح data-1), pinnedCurrency (إصلاح data-4)
     */
    onSave: (OpType, Double, String?, List<MaterialItem>, String?, Boolean, String?, Long?, String) -> Unit,
) {
    val context = LocalContext.current
    var type by remember { mutableStateOf(initial?.type ?: OpType.DEBT) }
    var amount by remember { mutableStateOf(if (initial != null) Money.input(initial.amount) else "") }
    var note by remember { mutableStateOf(initial?.note ?: "") }
    val materials = remember {
        mutableStateListOf<MaterialItem>().also { list ->
            initial?.materials?.let { list.addAll(it) }
        }
    }
    var showMaterials by remember { mutableStateOf(initial?.materials?.isNotEmpty() == true) }
    var receiptPath by remember { mutableStateOf(initial?.receiptPath) }
    var receiptError by remember { mutableStateOf<String?>(null) } // إصلاح act-6
    // نظام «الفواتير وحالة التسليم»
    var isInvoice by remember { mutableStateOf(initial?.isInvoice ?: false) }
    var invoiceRef by remember { mutableStateOf(initial?.invoiceRef ?: "") }
    // إصلاح data-1: تاريخ استحقاق (ديون فقط) • إصلاح data-4: عملة مثبتة وقت التسجيل
    var dueDate by remember { mutableStateOf(initial?.dueDate) }
    var showDatePicker by remember { mutableStateOf(false) }
    val pinnedCurrency = initial?.currency ?: currency
    var saveHint by remember { mutableStateOf<String?>(null) }
    val receiptPicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let {
            val r = PhotoStore.save(context, it, "receipt")
            if (r.path != null) {
                receiptPath = r.path
                receiptError = null
            } else {
                receiptError = r.error
            }
        }
    }

    AppSheet(title = title, onDismiss = onDismiss) {
        Column(
            Modifier
                .padding(horizontal = 20.dp)
                .verticalScroll(rememberScrollState()),
        ) {
            SegmentedSwitch(
                options = listOf(OpType.DEBT to "🔴 عليه (دين)", OpType.PAY to "🟢 له (سداد)"),
                selected = type,
                onSelect = { type = it },
                selectedColor = if (type == OpType.DEBT) LocalAppColors.current.red else LocalAppColors.current.green,
            )
            Spacer(Modifier.height(12.dp))
            AmountField(amount, { amount = it }, "المبلغ الإجمالي", currency)
            Spacer(Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = { showMaterials = !showMaterials },
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = 48.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (showMaterials) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.secondaryContainer,
                    ),
                    shape = RoundedCornerShape(12.dp),
                ) {
                    Text(
                        if (showMaterials) "📦 إخفاء المواد" else "📦 إضافة مواد (فاتورة)",
                        fontSize = 12.sp,
                        color = if (showMaterials) Color.White else MaterialTheme.colorScheme.onSecondaryContainer,
                    )
                }
                if (materials.isNotEmpty()) {
                    Button(
                        onClick = { materials.clear() },
                        modifier = Modifier.heightIn(min = 48.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = LocalAppColors.current.red.copy(alpha = 0.12f)),
                        shape = RoundedCornerShape(12.dp),
                    ) { Text("🗑️ مسح", fontSize = 12.sp, color = LocalAppColors.current.red) }
                }
            }
            if (showMaterials) {
                Spacer(Modifier.height(10.dp))
                Column {
                    materials.forEachIndexed { index, m ->
                        MaterialRow(
                            m = m,
                            currency = currency,
                            onRemove = { materials.removeAt(index) },
                            onChange = { updated -> materials[index] = updated },
                        )
                        Spacer(Modifier.height(6.dp))
                    }
                    Button(
                        onClick = { materials.add(MaterialItem()) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 48.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                        shape = RoundedCornerShape(12.dp),
                    ) { Text("➕ إضافة صنف جديد", fontSize = 12.sp, color = Color.White) }
                    if (materials.isNotEmpty()) {
                        Spacer(Modifier.height(6.dp))
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("الإجمالي من المواد:", style = MaterialTheme.typography.labelMedium)
                            Text(
                                "${Money.fmt(materials.sumOf { it.total })} $currency",
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.primary,
                            )
                        }
                    }
                }
            }
            Spacer(Modifier.height(10.dp))
            AppTextField(note, { note = it }, "البيان / الملاحظة", maxLength = 200)
            Spacer(Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                Button(
                    onClick = { receiptPicker.launch("image/*") },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    shape = RoundedCornerShape(12.dp),
                ) {
                    Text(
                        if (receiptPath != null) "📷 تم إرفاق الفاتورة ✓" else "📷 إرفاق فاتورة",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                if (receiptPath != null) {
                    Button(
                        onClick = { receiptPath = null },
                        colors = ButtonDefaults.buttonColors(containerColor = LocalAppColors.current.red.copy(alpha = 0.1f)),
                        shape = RoundedCornerShape(12.dp),
                    ) { Text("إزالة", fontSize = 12.sp, color = LocalAppColors.current.red) }
                }
            }
            receiptError?.let { err ->
                Text(
                    "⚠️ $err",
                    style = MaterialTheme.typography.labelSmall,
                    color = LocalAppColors.current.red,
                    modifier = Modifier.padding(top = 6.dp),
                )
            }
            Spacer(Modifier.height(12.dp))
            // ===== نظام «الفواتير وحالة التسليم» =====
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(
                        if (isInvoice) MaterialTheme.colorScheme.primary.copy(alpha = 0.08f)
                        else MaterialTheme.colorScheme.surfaceVariant,
                    )
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    "🧾 هذه العملية عبارة عن فاتورة",
                    style = MaterialTheme.typography.labelMedium,
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(8.dp))
                        .clickable { isInvoice = !isInvoice }
                        .padding(vertical = 4.dp),
                )
                Switch(
                    checked = isInvoice,
                    onCheckedChange = { isInvoice = it },
                    colors = SwitchDefaults.colors(
                        checkedTrackColor = MaterialTheme.colorScheme.primary,
                        checkedThumbColor = Color.White,
                    ),
                )
            }
            if (isInvoice) {
                Spacer(Modifier.height(8.dp))
                AppTextField(invoiceRef, { invoiceRef = it }, "رقم / وصف الفاتورة *", maxLength = 100)
                Text(
                    "تُظهر العملية شارة 🧾 برتقالية حتى تُسلَّم (بالضغط المطول) فتصبح ✅📑.",
                    style = MaterialTheme.typography.labelSmall,
                    color = LocalAppColors.current.muted,
                    modifier = Modifier.padding(top = 4.dp),
                )
            }
            // ===== إصلاح data-1: تاريخ استحقاق الدين (اختياري) =====
            if (type == OpType.DEBT) {
                Spacer(Modifier.height(10.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .padding(horizontal = 12.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        "📅 تاريخ الاستحقاق",
                        style = MaterialTheme.typography.labelMedium,
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { showDatePicker = true }
                            .padding(vertical = 6.dp),
                    )
                    Text(
                        if (dueDate != null) com.mahfazty.smart.domain.Dates.short(dueDate!!) else "اختياري",
                        style = MaterialTheme.typography.labelMedium,
                        color = if (dueDate != null) MaterialTheme.colorScheme.primary else LocalAppColors.current.muted,
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { showDatePicker = true }
                            .padding(horizontal = 6.dp, vertical = 6.dp),
                    )
                    if (dueDate != null) {
                        Text(
                            "✕",
                            color = LocalAppColors.current.red,
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .clickable { dueDate = null }
                                .padding(horizontal = 8.dp, vertical = 6.dp),
                        )
                    }
                }
            }
            Spacer(Modifier.height(10.dp))
            Text(
                "الرصيد الحقيقي الحالي: ${Money.fmt(realBalance)} $currency\n" +
                    if (type == OpType.PAY) "🟢 له: يزيد الرصيد الحقيقي بهذا المبلغ"
                    else "🔴 عليه: تُخصم من الرصيد الحقيقي إن كان ≥ المبلغ، وإلا تُرفض",
                style = MaterialTheme.typography.labelSmall,
                color = LocalAppColors.current.muted,
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .padding(8.dp),
            )
            Spacer(Modifier.height(14.dp))
            if (saveHint != null) {
                Text(saveHint!!, color = LocalAppColors.current.red, style = MaterialTheme.typography.labelSmall)
                Spacer(Modifier.height(6.dp))
            }
            Button(
                onClick = {
                    val amt = Money.parse(amount)
                    when {
                        amt <= 0 -> saveHint = "أدخل مبلغاً أكبر من صفر"
                        isInvoice && invoiceRef.isBlank() -> saveHint = "اكتب رقم أو وصف الفاتورة (إجباري) قبل الحفظ"
                        else -> {
                            saveHint = null
                            // إصلاح data-1: الاستحقاق للديون فقط • data-4: تثبيت عملة التسجيل
                            onSave(
                                type, amt, note.ifBlank { null }, materials.toList(), receiptPath,
                                isInvoice, invoiceRef.trim().ifBlank { null },
                                if (type == OpType.DEBT) dueDate else null,
                                pinnedCurrency,
                            )
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 52.dp),
                shape = RoundedCornerShape(14.dp),
            ) { Text("حفظ العملية ✅") }
            Spacer(Modifier.height(8.dp))
        }
    }

    // ===== إصلاح data-1: منتقي تاريخ الاستحقاق =====
    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(initialSelectedDateMillis = dueDate)
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                androidx.compose.material3.TextButton(
                    onClick = {
                        datePickerState.selectedDateMillis?.let { dueDate = it }
                        showDatePicker = false
                    },
                ) { Text("حفظ") }
            },
            dismissButton = {
                androidx.compose.material3.TextButton(
                    onClick = { showDatePicker = false },
                ) { Text("إلغاء") }
            },
        ) {
            DatePicker(state = datePickerState)
        }
    }
}

@Composable
private fun MaterialRow(
    m: MaterialItem,
    currency: String,
    onRemove: () -> Unit,
    onChange: (MaterialItem) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        AppTextField(
            value = m.name,
            onValueChange = { onChange(m.copy(name = it)) },
            label = "الصنف",
            modifier = Modifier.weight(1.4f),
        )
        AppTextField(
            value = if (m.qty == 0.0) "" else Money.input(m.qty),
            onValueChange = { onChange(m.copy(qty = Money.parse(it))) },
            label = "الكمية",
            modifier = Modifier.weight(0.9f),
        )
        AppTextField(
            value = if (m.unitPrice == 0.0) "" else Money.input(m.unitPrice),
            onValueChange = { onChange(m.copy(unitPrice = Money.parse(it))) },
            label = "سعر الوحدة",
            modifier = Modifier.weight(1f),
        )
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("${Money.fmt(m.total)} $currency", style = MaterialTheme.typography.labelSmall)
            Text("✕", style = MaterialTheme.typography.labelMedium, color = LocalAppColors.current.red, modifier = Modifier.clickable(onClick = onRemove))
        }
    }
}

// =====================================================================
// شحن الرصيد الحقيقي
// =====================================================================

@Composable
fun FundRealDialog(
    currency: String,
    bank: Double,
    cash: Double,
    bankName: String,
    cashName: String,
    onDismiss: () -> Unit,
    onSave: (Double, Wallet) -> Unit,
) {
    var from by remember { mutableStateOf(Wallet.BANK) }
    var amount by remember { mutableStateOf("") }
    AppSheet(title = "➕ شحن الرصيد الحقيقي", onDismiss = onDismiss) {
        Column(Modifier.padding(horizontal = 20.dp)) {
            SegmentedSwitch(
                options = listOf(Wallet.BANK to "🏦 من $bankName", Wallet.CASH to "💵 من $cashName"),
                selected = from,
                onSelect = { from = it },
                selectedColor = MaterialTheme.colorScheme.primary,
            )
            Text(
                "البنك: ${Money.fmt(bank)} • الكاش: ${Money.fmt(cash)}",
                style = MaterialTheme.typography.labelSmall,
                color = LocalAppColors.current.muted,
                modifier = Modifier.padding(vertical = 8.dp),
            )
            AmountField(amount, { amount = it }, "المبلغ للشحن", currency)
            Spacer(Modifier.height(14.dp))
            Button(
                onClick = { (Money.parse(amount)).let { if (it > 0) onSave(it, from) } },
                modifier = Modifier
                    .fillMaxWidth()
                    .bounceClick(),
                shape = RoundedCornerShape(14.dp),
            ) { Text("شحن الرصيد الحقيقي ✅") }
            Spacer(Modifier.height(8.dp))
        }
    }
}

// =====================================================================
// سحب من الرصيد الحقيقي
// =====================================================================

@Composable
fun WithdrawRealDialog(
    currency: String,
    realBalance: Double,
    onDismiss: () -> Unit,
    onSave: (Double, Wallet) -> Unit,
) {
    var to by remember { mutableStateOf(Wallet.BANK) }
    var amount by remember { mutableStateOf("") }
    AppSheet(title = "➖ سحب الرصيد الحقيقي", onDismiss = onDismiss) {
        Column(Modifier.padding(horizontal = 20.dp)) {
            SegmentedSwitch(
                options = listOf(Wallet.BANK to "🏦 إلى البنك", Wallet.CASH to "💵 إلى الكاش"),
                selected = to,
                onSelect = { to = it },
                selectedColor = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                "الرصيد الحقيقي: ${Money.fmt(realBalance)}",
                style = MaterialTheme.typography.labelSmall,
                color = LocalAppColors.current.muted,
                modifier = Modifier.padding(vertical = 8.dp),
            )
            AmountField(amount, { amount = it }, "المبلغ للسحب", currency)
            Spacer(Modifier.height(14.dp))
            Button(
                onClick = { (Money.parse(amount)).let { if (it > 0) onSave(it, to) } },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.onSurface),
                shape = RoundedCornerShape(14.dp),
            ) { Text("سحب للبنك/الكاش ✅") }
            Spacer(Modifier.height(8.dp))
        }
    }
}

// =====================================================================
// تحويل رصيد حقيقي بين الحسابات
// =====================================================================

@Composable
fun TransferRealDialog(
    currency: String,
    sources: List<Pair<Client, ClientAccount>>,
    savings: Double = 0.0,
    goals: List<Triple<Long, String, Double>> = emptyList(),
    onDismiss: () -> Unit,
    onSave: (Long, Long, Double) -> Unit,
    onFromSavings: (Double) -> Unit = {},
    onFromGoal: (Long, String, Double) -> Unit = { _, _, _ -> },
) {
    var selectedAcc by remember { mutableStateOf<Pair<Client, ClientAccount>?>(null) }
    var selectedSavings by remember { mutableStateOf(false) }
    var selectedGoal by remember { mutableStateOf<Triple<Long, String, Double>?>(null) }
    var amount by remember { mutableStateOf("") }
    fun pickAcc(pair: Pair<Client, ClientAccount>) {
        selectedAcc = pair; selectedSavings = false; selectedGoal = null
    }
    fun pickSavings() {
        selectedAcc = null; selectedSavings = true; selectedGoal = null
    }
    fun pickGoal(g: Triple<Long, String, Double>) {
        selectedAcc = null; selectedSavings = false; selectedGoal = g
    }
    AppSheet(title = "🔄 تحويل رصيد حقيقي", onDismiss = onDismiss) {
        Column(Modifier.padding(horizontal = 20.dp)) {
            Text(
                "اختر المصدر: حساب عميل أو ادخار أو هدف، ثم حوّل إلى الحساب الحالي",
                style = MaterialTheme.typography.bodySmall,
                color = LocalAppColors.current.muted,
            )
            Spacer(Modifier.height(8.dp))
            Column(
                Modifier
                    .heightIn(max = 280.dp)
                    .verticalScroll(rememberScrollState()),
            ) {
                if (savings > 0) {
                    TransferSourceRow(
                        title = "🐷 صندوق الادخار",
                        subtitle = "متاح: ${Money.fmt(savings)} $currency",
                        selected = selectedSavings,
                        onClick = { pickSavings() },
                    )
                    Spacer(Modifier.height(8.dp))
                }
                goals.forEach { g ->
                    TransferSourceRow(
                        title = g.second,
                        subtitle = "متاح: ${Money.fmt(g.third)} $currency",
                        selected = selectedGoal?.first == g.first,
                        onClick = { pickGoal(g) },
                    )
                    Spacer(Modifier.height(8.dp))
                }
                sources.forEach { (c, a) ->
                    TransferSourceRow(
                        title = "${a.icon} ${c.name} • ${a.name}",
                        subtitle = "متاح: ${Money.fmt(a.realBalance)} $currency",
                        selected = selectedAcc?.second?.id == a.id,
                        onClick = { pickAcc(c to a) },
                    )
                    Spacer(Modifier.height(8.dp))
                }
                if (savings <= 0 && goals.isEmpty() && sources.isEmpty()) {
                    Text(
                        "لا توجد مصادر برصيد للتحويل منها",
                        style = MaterialTheme.typography.bodySmall,
                        color = LocalAppColors.current.red,
                    )
                }
            }
            Spacer(Modifier.height(10.dp))
            AmountField(amount, { amount = it }, "المبلغ للتحويل", currency)
            Spacer(Modifier.height(14.dp))
            Button(
                onClick = {
                    val amt = Money.parse(amount)
                    if (amt <= 0) return@Button
                    when {
                        selectedSavings -> onFromSavings(minOf(amt, savings))
                        selectedGoal != null -> {
                            val g = selectedGoal!!
                            onFromGoal(g.first, g.second, minOf(amt, g.third))
                        }
                        selectedAcc != null -> {
                            val (c, a) = selectedAcc!!
                            onSave(c.id, a.id, minOf(amt, a.realBalance))
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
            ) { Text("تنفيذ التحويل ✅") }
            Spacer(Modifier.height(8.dp))
        }
    }
}

@Composable
private fun TransferSourceRow(
    title: String,
    subtitle: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(
                if (selected) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.surfaceVariant,
            )
            .border(
                width = if (selected) 2.dp else 1.dp,
                color = if (selected) MaterialTheme.colorScheme.primary
                else LocalAppColors.current.border,
                shape = RoundedCornerShape(14.dp),
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(
                title,
                style = MaterialTheme.typography.labelMedium,
                color = if (selected) Color.White else MaterialTheme.colorScheme.onSurface,
            )
            Text(
                subtitle,
                style = MaterialTheme.typography.labelSmall,
                color = if (selected) Color.White.copy(alpha = 0.85f) else LocalAppColors.current.muted,
            )
        }
        if (selected) {
            Text("محدد ✓", color = Color.White, style = MaterialTheme.typography.labelMedium)
        }
    }
}

// =====================================================================
// سجل تحويلات الرصيد الحقيقي
// =====================================================================

@Composable
fun HistoryDialog(
    transfers: List<TransferDisplay>,
    currency: String,
    onDismiss: () -> Unit,
) {
    AppSheet(title = "📋 سجل تحويلات الرصيد الحقيقي", onDismiss = onDismiss) {
        Column(
            Modifier
                .padding(horizontal = 20.dp)
                .height(380.dp),
        ) {
            if (transfers.isEmpty()) {
                Text(
                    "لا توجد تحويلات بعد",
                    style = MaterialTheme.typography.bodySmall,
                    color = LocalAppColors.current.muted,
                    modifier = Modifier.padding(vertical = 20.dp),
                )
            } else {
                transfers.forEach { t ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                            .padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text(
                                "${t.fromClientName} (${t.fromAccountName}) → ${t.toClientName} (${t.toAccountName})",
                                style = MaterialTheme.typography.labelMedium,
                            )
                            Text(
                                Dates.dateTime(t.transfer.date),
                                style = MaterialTheme.typography.labelSmall,
                                color = LocalAppColors.current.muted,
                            )
                        }
                        Text(
                            "${Money.fmt(t.transfer.amount)} $currency",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.primary,
                        )
                    }
                    Spacer(Modifier.height(6.dp))
                }
            }
            Spacer(Modifier.height(10.dp))
            Button(
                onClick = onDismiss,
                modifier = Modifier
                    .fillMaxWidth()
                    .bounceClick(),
                shape = RoundedCornerShape(14.dp),
            ) { Text("إغلاق") }
            Spacer(Modifier.height(8.dp))
        }
    }
}

// =====================================================================
// نافذة الرصيد الحقيقي غير الكافي (مع اقتراحات التمويل)
// =====================================================================

@Composable
fun InsufficientRealSheet(
    data: InsufficientRealData,
    currency: String,
    onClose: () -> Unit,
    onFund: (Wallet, Double) -> Unit,
    onTransfer: (Long, Double) -> Unit,
    onFundSavings: (Double) -> Unit = {},
    onFundGoal: (Long, String, Double) -> Unit = { _, _, _ -> },
) {
    val shortage = (data.needed - data.have).coerceAtLeast(0.0)
    AppSheet(title = "❌ رصيد حقيقي غير كافٍ", onDismiss = onClose) {
        Column(Modifier.padding(horizontal = 20.dp)) {
            Text(
                "لا يمكن إتمام العملية لأن الرصيد الحقيقي لهذا الحساب (${Money.fmt(data.have)} $currency) أقل من المبلغ المطلوب (${Money.fmt(data.needed)} $currency).",
                style = MaterialTheme.typography.bodySmall,
                color = LocalAppColors.current.muted,
            )
            Spacer(Modifier.height(6.dp))
            Text(
                "بعد الشحن ستعود تلقائياً لنفس العملية بنفس البيان لإتمامها.",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary,
            )
            Spacer(Modifier.height(12.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .padding(12.dp),
                horizontalArrangement = Arrangement.SpaceAround,
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("الرصيد الحقيقي", style = MaterialTheme.typography.labelSmall, color = LocalAppColors.current.muted)
                    Text(Money.fmt(data.have), style = MaterialTheme.typography.labelLarge)
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("المطلوب", style = MaterialTheme.typography.labelSmall, color = LocalAppColors.current.muted)
                    Text(Money.fmt(data.needed), style = MaterialTheme.typography.labelLarge)
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("النقص", style = MaterialTheme.typography.labelSmall, color = LocalAppColors.current.muted)
                    Text(Money.fmt(shortage), style = MaterialTheme.typography.labelLarge, color = LocalAppColors.current.red)
                }
            }
            Spacer(Modifier.height(14.dp))
            Text("💡 مصادر الشحن: بنك أو كاش أو حساب فيه رصيد حقيقي", style = MaterialTheme.typography.labelMedium)
            Spacer(Modifier.height(8.dp))
            RealSuggestionRow("🏦 البنك", "متاح: ${Money.fmt(data.bank)} $currency", "شحن ${Money.fmt(minOf(data.bank, shortage))} من البنك") {
                onFund(Wallet.BANK, minOf(data.bank, shortage))
            }
            Spacer(Modifier.height(6.dp))
            RealSuggestionRow("💵 الكاش", "متاح: ${Money.fmt(data.cash)} $currency", "شحن ${Money.fmt(minOf(data.cash, shortage))} من الكاش") {
                onFund(Wallet.CASH, minOf(data.cash, shortage))
            }
            Spacer(Modifier.height(6.dp))
            if (data.savings > 0) {
                RealSuggestionRow("🐷 الادخار", "متاح: ${Money.fmt(data.savings)} $currency", "شحن ${Money.fmt(minOf(data.savings, shortage))} من الادخار") {
                    onFundSavings(minOf(data.savings, shortage))
                }
                Spacer(Modifier.height(6.dp))
            }
            data.goals.forEach { (id, title, available) ->
                RealSuggestionRow(title, "متاح: ${Money.fmt(available)} $currency", "شحن ${Money.fmt(minOf(available, shortage))}") {
                    onFundGoal(id, title, minOf(available, shortage))
                }
                Spacer(Modifier.height(6.dp))
            }
            data.transferSources.forEach { (title, accountId, available) ->
                RealSuggestionRow(title, "متاح: ${Money.fmt(available)} $currency", "تحويل ${Money.fmt(minOf(available, shortage))}") {
                    onTransfer(accountId, minOf(available, shortage))
                }
                Spacer(Modifier.height(6.dp))
            }
            if (data.bank <= 0 && data.cash <= 0 && data.transferSources.isEmpty() && data.savings <= 0 && data.goals.isEmpty()) {
                Text(
                    "لا توجد مصادر تمويل متاحة حالياً",
                    style = MaterialTheme.typography.bodySmall,
                    color = LocalAppColors.current.red,
                )
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
private fun RealSuggestionRow(title: String, subtitle: String, buttonText: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.labelMedium)
            Text(subtitle, style = MaterialTheme.typography.labelSmall, color = LocalAppColors.current.muted)
        }
        Button(
            onClick = onClick,
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
            shape = RoundedCornerShape(10.dp),
        ) { Text(buttonText, fontSize = 11.sp) }
    }
}

// =====================================================================
// تسوية الرصيد الحقيقي (إصلاح fin-4) — تصحيح محمي بسبب إلزامي يُسجَّل في التدقيق
// =====================================================================

@Composable
fun AdjustRealDialog(
    currency: String,
    currentReal: Double,
    onDismiss: () -> Unit,
    onSave: (Double, String) -> Unit,
) {
    var amount by remember { mutableStateOf(Money.input(currentReal)) }
    var reason by remember { mutableStateOf("") }
    var hint by remember { mutableStateOf<String?>(null) }

    AppSheet(title = "⚖️ تسوية الرصيد الحقيقي", onDismiss = onDismiss) {
        Column(Modifier.padding(horizontal = 20.dp)) {
            Text(
                "الرصيد الحالي: ${Money.fmt(currentReal)} $currency",
                style = MaterialTheme.typography.labelMedium,
                color = LocalAppColors.current.muted,
            )
            Spacer(Modifier.height(10.dp))
            AmountField(amount, { amount = it }, "الرصيد الصحيح", currency)
            Spacer(Modifier.height(10.dp))
            AppTextField(reason, { reason = it }, "سبب التسوية (إلزامي) — مثل: خطأ إدخال سابق", maxLength = 150)
            Spacer(Modifier.height(10.dp))
            Text(
                "تُنشر التسوية مباشرة في «سجل التعديلات» مع القيمة القديمة والجديدة والسبب.",
                style = MaterialTheme.typography.labelSmall,
                color = LocalAppColors.current.muted,
            )
            Spacer(Modifier.height(8.dp))
            if (hint != null) {
                Text(hint!!, color = LocalAppColors.current.red, style = MaterialTheme.typography.labelSmall)
                Spacer(Modifier.height(6.dp))
            }
            Button(
                onClick = {
                    val amt = Money.parse(amount)
                    when {
                        amt < 0 || amt.isNaN() -> hint = "أدخل رقماً صحيحاً (صفر أو أكبر)"
                        reason.isBlank() -> hint = "اكتب سبب التسوية — يُسجَّل في سجل التدقيق"
                        else -> onSave(amt, reason.trim())
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = LocalAppColors.current.red),
                shape = RoundedCornerShape(14.dp),
            ) { Text("تنفيذ التسوية ⚖️") }
            Spacer(Modifier.height(8.dp))
        }
    }
}

// =====================================================================
// خيارات مشاركة كشف الحساب (إصلاح act-4)
// =====================================================================

@Composable
fun StatementOptionsDialog(
    opCount: Int,
    onDismiss: () -> Unit,
    onShareLast10: () -> Unit,
    onShareFull: () -> Unit,
    onExportCsv: () -> Unit,
) {
    AppSheet(title = "🧾 كشف الحساب", onDismiss = onDismiss) {
        Column(Modifier.padding(horizontal = 20.dp)) {
            Button(
                onClick = onShareLast10,
                modifier = Modifier.fillMaxWidth().bounceClick(),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF25D366)),
                shape = RoundedCornerShape(14.dp),
            ) { Text("💬 واتساب — آخر 10 عمليات", fontSize = 13.sp) }
            Spacer(Modifier.height(8.dp))
            Button(
                onClick = onShareFull,
                modifier = Modifier.fillMaxWidth().bounceClick(),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF25D366)),
                shape = RoundedCornerShape(14.dp),
            ) { Text("💬 واتساب — الكشف الكامل ($opCount عملية)", fontSize = 13.sp) }
            Spacer(Modifier.height(8.dp))
            Button(
                onClick = onExportCsv,
                modifier = Modifier.fillMaxWidth().bounceClick(),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                shape = RoundedCornerShape(14.dp),
            ) { Text("📊 تصدير CSV — كشف كامل قابل للفتح في Excel", fontSize = 13.sp) }
            Spacer(Modifier.height(8.dp))
        }
    }
}

// =====================================================================
// سجل التعديلات (إصلاح sec-6) — من فعل ماذا ومتى
// =====================================================================

@Composable
fun AuditLogDialog(
    entries: List<com.mahfazty.smart.data.AuditLogEntry>,
    onDismiss: () -> Unit,
) {
    AppSheet(title = "📝 سجل التعديلات", onDismiss = onDismiss) {
        Column(
            Modifier
                .padding(horizontal = 20.dp)
                .height(380.dp),
        ) {
            if (entries.isEmpty()) {
                Text(
                    "لا توجد تعديلات مسجلة بعد.\nتُسجل هنا: التعديلات، الحذف، الشحن، السحب، التحويل، والتسويات.",
                    style = MaterialTheme.typography.bodySmall,
                    color = LocalAppColors.current.muted,
                    modifier = Modifier.padding(vertical = 20.dp),
                )
            } else {
                Column(
                    Modifier
                        .fillMaxWidth()
                        .height(300.dp)
                        .verticalScroll(rememberScrollState()),
                ) {
                    entries.forEach { e ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant)
                                .padding(10.dp),
                            verticalAlignment = Alignment.Top,
                        ) {
                            Column(Modifier.weight(1f)) {
                                Text(e.action, style = MaterialTheme.typography.labelMedium)
                                Text(e.details, style = MaterialTheme.typography.labelSmall, color = LocalAppColors.current.muted)
                            }
                            Text(
                                com.mahfazty.smart.domain.Dates.dateTime(e.ts),
                                style = MaterialTheme.typography.labelSmall,
                                color = LocalAppColors.current.muted,
                            )
                        }
                        Spacer(Modifier.height(6.dp))
                    }
                }
            }
            Spacer(Modifier.height(10.dp))
            Button(
                onClick = onDismiss,
                modifier = Modifier.fillMaxWidth().bounceClick(),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.onSurface),
                shape = RoundedCornerShape(14.dp),
            ) { Text("إغلاق") }
            Spacer(Modifier.height(8.dp))
        }
    }
}
