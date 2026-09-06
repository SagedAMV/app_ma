package com.mahfazty.smart.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.biometric.BiometricManager
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.animation.Crossfade
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.mahfazty.smart.data.ClientsRepository
import com.mahfazty.smart.data.SettingsRepository
import com.mahfazty.smart.data.WalletRepository
import com.mahfazty.smart.domain.Ids
import com.mahfazty.smart.domain.Money
import com.mahfazty.smart.domain.WalletEngine
import com.mahfazty.smart.domain.model.AppSettings
import com.mahfazty.smart.domain.model.Category
import com.mahfazty.smart.domain.model.CategoryKind
import com.mahfazty.smart.domain.model.ThemeMode
import com.mahfazty.smart.ui.components.AppCard
import com.mahfazty.smart.ui.components.AppTextField
import com.mahfazty.smart.ui.components.ConfirmDialog
import com.mahfazty.smart.ui.components.ElasticEntrance
import com.mahfazty.smart.ui.components.FloatingIcon
import com.mahfazty.smart.ui.components.StaggeredEntrance
import com.mahfazty.smart.ui.components.bounceClick
import com.mahfazty.smart.ui.theme.LocalAppColors
import com.mahfazty.smart.ui.theme.parseHex
import com.mahfazty.smart.ui.util.shareFile
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.io.File

// =====================================================================
// ViewModel الإعدادات
// =====================================================================

/** نتيجة فحص ذاتي */
data class CheckResult(val name: String, val ok: Boolean, val details: String = "")

class SettingsViewModel(
    private val walletRepo: WalletRepository,
    private val settingsRepo: SettingsRepository,
    private val clientsRepo: ClientsRepository,
) : ViewModel() {

    val settings: StateFlow<AppSettings> = settingsRepo.settings
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), AppSettings())

    /** وقت آخر نسخة احتياطية (null = لم تعمل قط) — أساس تذكير النسخ (إصلاح data-5) */
    val lastBackupTs: StateFlow<Long?> = settingsRepo.lastBackupTs
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    /** قفل تبويب العملاء (إصلاح sec-2) — التحقق الفعلي بالبصمة/رمز الجهاز في MainViewModel */
    fun setLockClients(enabled: Boolean) = viewModelScope.launch { settingsRepo.set("lock_clients", enabled.toString()) }

    private val _checks = MutableSharedFlow<List<CheckResult>>(extraBufferCapacity = 1)
    val checks: kotlinx.coroutines.flow.SharedFlow<List<CheckResult>> = _checks.asSharedFlow()

    private val _toast = MutableSharedFlow<String>(extraBufferCapacity = 8)
    val toast = _toast.asSharedFlow()

    fun updateSetting(key: String, value: String) = viewModelScope.launch { settingsRepo.set(key, value) }

    fun setBudget(catId: String, value: Double) = viewModelScope.launch { settingsRepo.setBudget(catId, value) }

    fun setSavingsGoal(value: Double) = viewModelScope.launch { walletRepo.setSavingsGoal(value) }

    fun addCustomCategory(kind: CategoryKind, name: String, icon: String) = viewModelScope.launch {
        val s = settings.value
        val current = if (kind == CategoryKind.EXPENSE) s.customExpense else s.customIncome
        val id = "custom_${Ids.next()}"
        val updated = current + Category(id, icon.ifBlank { "🏷️" }, name, kind)
        settingsRepo.setCustomCategories(kind, updated)
        _toast.emit("تمت إضافة الفئة ✅")
    }

    fun deleteCustomCategory(kind: CategoryKind, id: String) = viewModelScope.launch {
        val s = settings.value
        val current = if (kind == CategoryKind.EXPENSE) s.customExpense else s.customIncome
        settingsRepo.setCustomCategories(kind, current.filterNot { it.id == id })
        _toast.emit("تم حذف الفئة")
    }

    fun exportJson(context: android.content.Context) = viewModelScope.launch {
        runCatching {
            val json = walletRepo.exportBackupJson()
            val dir = File(context.cacheDir, "exports").apply { mkdirs() }
            val file = File(dir, "mahfazty-backup-${com.mahfazty.smart.domain.Dates.fileStamp(System.currentTimeMillis())}.json")
            file.writeText(json, Charsets.UTF_8)
            val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
            shareFile(context, uri, "application/json", "نسخة احتياطية محفظتي الذكية")
            // إصلاح data-5: تسجيل وقت النجاح ليزول شريط التذكير بعد أسبوع
            settingsRepo.setLastBackupTs(System.currentTimeMillis())
            _toast.emit("تم إنشاء النسخة الاحتياطية ✅")
        }.onFailure { _toast.emit("تعذر إنشاء النسخة الاحتياطية") }
    }

    fun exportCsv(context: android.content.Context) = viewModelScope.launch {
        runCatching {
            val csv = walletRepo.exportCsv()
            val dir = File(context.cacheDir, "exports").apply { mkdirs() }
            val file = File(dir, "mahfazty-${com.mahfazty.smart.domain.Dates.fileStamp(System.currentTimeMillis())}.csv")
            file.writeText(csv, Charsets.UTF_8)
            val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
            shareFile(context, uri, "text/csv", "تصدير CSV محفظتي الذكية")
            _toast.emit("تم تصدير CSV ✅")
        }.onFailure { _toast.emit("تعذر التصدير") }
    }

    fun importJson(text: String) = viewModelScope.launch {
        val result = walletRepo.importBackup(text)
        _toast.emit(result.message)
    }

    fun clearAll() = viewModelScope.launch {
        walletRepo.clearAllData()
        _toast.emit("تم مسح كل البيانات")
    }

    /** فحص ذاتي: التحقق من سلامة القواعد المالية */
    fun runSelfTest() = viewModelScope.launch {
        val txs = walletRepo.transactions.first()
        val bank = walletRepo.bankBalance.first()
        val cash = walletRepo.cashBalance.first()
        val savings = walletRepo.savingsTotal.first()
        val goals = walletRepo.goals.first()
        val clients = clientsRepo.clientsWithData.first()
        val results = buildList {
            add(CheckResult("رصيد البنك ليس سالباً", bank >= 0, "الرصيد: ${Money.fmtLat(bank)}"))
            add(CheckResult("رصيد الكاش ليس سالباً", cash >= 0, "الرصيد: ${Money.fmtLat(cash)}"))
            add(CheckResult("الادخار ليس سالباً", savings >= 0, "المدخر: ${Money.fmtLat(savings)}"))
            add(CheckResult("كل المبالغ أكبر من صفر", txs.all { it.amount > 0 }, "${txs.size} عملية"))
            add(CheckResult("أهداف صالحة", goals.all { WalletEngine.goalSaved(it.opening, it.id, txs) >= 0 }, "${goals.size} هدف"))
            add(
                CheckResult(
                    "أرصدة حقيقية سليمة",
                    clients.flatMap { it.accounts }.all { it.account.realBalance >= 0 },
                    "${clients.size} عميل",
                ),
            )
        }
        _checks.emit(results)
    }
}

// =====================================================================
// شاشة الإعدادات
// =====================================================================

@Composable
fun SettingsScreen(
    vm: SettingsViewModel,
) {
    val context = LocalContext.current
    val snackbar = remember { SnackbarHostState() }
    LaunchedEffect(Unit) { vm.toast.collect { snackbar.showSnackbar(it) } }

    val settings by vm.settings.collectAsStateWithLifecycle()
    var confirmClear by remember { mutableStateOf(false) }
    var checks by remember { mutableStateOf<List<CheckResult>>(emptyList()) }
    LaunchedEffect(Unit) { vm.checks.collect { checks = it } }
    val importPicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let {
            runCatching {
                context.contentResolver.openInputStream(it)?.use { input ->
                    vm.importJson(input.bufferedReader(Charsets.UTF_8).readText())
                }
            }
        }
    }

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
                "الإعدادات ⚙️",
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp),
            )
            }

            // ===== الملف الشخصي =====
            SettingsSectionTitle("👤 الملف الشخصي", 0)
            SettingField("اسمك", "يظهر في التحية أعلى التطبيق") {
                var name by remember(settings.name) { mutableStateOf(settings.name) }
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it; vm.updateSetting("name", it) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    singleLine = true,
                    colors = fieldColors(),
                )
            }
            SettingField("اسم البنك", "يظهر بجانب الرصيد") {
                var bankName by remember(settings.bankName) { mutableStateOf(settings.bankName) }
                OutlinedTextField(
                    value = bankName,
                    onValueChange = { bankName = it; vm.updateSetting("bank_name", it) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    singleLine = true,
                    colors = fieldColors(),
                )
            }

            // ===== العملة والمظهر =====
            SettingsSectionTitle("💱 العملة والمظهر", 1)
            SettingRow("العملة", "تظهر بجانب كل مبلغ") {
                CurrencyPicker(settings.currency) { vm.updateSetting("currency", it) }
            }
            SettingRow("المظهر", "فاتح / داكن / حسب النظام") {
                val modes = listOf(
                    ThemeMode.LIGHT to "☀️ فاتح",
                    ThemeMode.DARK to "🌙 داكن",
                    ThemeMode.SYSTEM to "⚙️ نظام",
                )
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    modes.forEach { (mode, label) ->
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(10.dp))
                                .background(
                                    if (settings.theme == mode) MaterialTheme.colorScheme.primary
                                    else MaterialTheme.colorScheme.surfaceVariant,
                                )
                                .clickable { vm.updateSetting("theme", when (mode) {
                                    ThemeMode.DARK -> "dark"; ThemeMode.SYSTEM -> "system"; else -> "light"
                                }) }
                                .padding(horizontal = 10.dp, vertical = 6.dp),
                        ) {
                            Text(
                                label,
                                style = MaterialTheme.typography.labelSmall,
                                color = if (settings.theme == mode) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            }
            SettingRow("إخفاء الأرصدة", "وضع الخصوصية - يخفي الأرقام") {
                Switch(
                    checked = settings.hideBalance,
                    onCheckedChange = { vm.updateSetting("hide_balance", it.toString()) },
                    colors = SwitchDefaults.colors(checkedTrackColor = MaterialTheme.colorScheme.primary),
                )
            }
            SettingRow("🫣 إخفاء الادخار من الرصيد الرئيسي", "يُخصم مبلغ الادخار من كارت الرصيد") {
                Switch(
                    checked = settings.hideSavings,
                    onCheckedChange = { vm.updateSetting("hide_savings", it.toString()) },
                    colors = SwitchDefaults.colors(checkedTrackColor = MaterialTheme.colorScheme.primary),
                )
            }
            Column(Modifier.padding(horizontal = 20.dp, vertical = 4.dp)) {
                Text("لون التطبيق", style = MaterialTheme.typography.labelMedium)
                Spacer(Modifier.height(8.dp))
                val colorPairs = listOf(
                    "#6C5CE7" to "#A29BFE",
                    "#0984E3" to "#74B9FF",
                    "#00B894" to "#55EFC4",
                    "#E17055" to "#FAB1A0",
                    "#E84393" to "#FD79A8",
                    "#2D3436" to "#636E72",
                )
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    colorPairs.forEach { (c1, c2) ->
                        val selected = settings.primaryColor == c1
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .clip(CircleShape)
                                .background(
                                    Brush.linearGradient(listOf(parseHex(c1), parseHex(c2))),
                                )
                                .clickable {
                                    vm.updateSetting("primary_color", c1)
                                    vm.updateSetting("primary_color2", c2)
                                }
                                .padding(if (selected) 4.dp else 0.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            Crossfade(targetState = selected, label = "colorCheck") { sel ->
                                if (sel) Text("✓", color = Color.White, fontSize = 16.sp)
                            }
                        }
                    }
                }
            }

            // ===== الخصوصية والأمان =====
            // إصلاح sec-2: قفل تبويب العملاء بالبصمة أو رمز الجهاز
            SettingsSectionTitle("🔐 الخصوصية والأمان", 2)
            SettingRow("🔒 قفل تبويب العملاء", "يتطلب بصمة أو رمز الجهاز قبل عرض بيانات العملاء") {
                Switch(
                    checked = settings.lockClients,
                    onCheckedChange = { enabled ->
                        if (enabled) {
                            // لا نفعّل القفل على جهاز بلا وسيلة تحقق حقيقية — فلا قفل بلا تفكيك
                            val authenticators = BiometricManager.Authenticators.BIOMETRIC_STRONG or
                                BiometricManager.Authenticators.DEVICE_CREDENTIAL
                            if (BiometricManager.from(context).canAuthenticate(authenticators)
                                == BiometricManager.BIOMETRIC_SUCCESS
                            ) {
                                vm.setLockClients(true)
                            } else {
                                snackbar.showSnackbar("هذا الجهاز لا يدعم البصمة أو رمز القفل — لا يمكن تفعيل القفل")
                            }
                        } else {
                            vm.setLockClients(false)
                        }
                    },
                    colors = SwitchDefaults.colors(checkedTrackColor = MaterialTheme.colorScheme.primary),
                )
            }

            // ===== الحدود الشهرية =====
            SettingsSectionTitle("🚧 الحدود الشهرية", 3)
            Text(
                "ضع حد لكل فئة وسينبهك التطبيق عند تجاوزه",
                style = MaterialTheme.typography.bodySmall,
                color = LocalAppColors.current.muted,
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 2.dp),
            )
            val budgetCats = listOf(
                "food" to "🍔 طعام", "transport" to "🚕 مواصلات", "shopping" to "🛍️ تسوق",
                "bills" to "💡 فواتير", "home" to "🏠 سكن", "health" to "💊 صحة",
                "fun" to "🎬 ترفيه", "other" to "📦 أخرى",
            )
            budgetCats.forEach { (id, label) ->
                SettingRow(label, "") {
                    BudgetInput(settings.budgets[id] ?: 0.0) { vm.setBudget(id, it) }
                }
            }
            SettingRow("هدف الادخار", "المبلغ الذي تريد الوصول إليه في صندوق الادخار") {
                BudgetInput(settings.savingsGoal) { vm.setSavingsGoal(it) }
            }

            // ===== الفئات المخصصة =====
            SettingsSectionTitle("🏷️ فئاتي المخصصة", 4)
            CustomCategoriesSection(
                kind = CategoryKind.EXPENSE,
                items = settings.customExpense,
                onAdd = { name, icon -> vm.addCustomCategory(CategoryKind.EXPENSE, name, icon) },
                onDelete = { id -> vm.deleteCustomCategory(CategoryKind.EXPENSE, id) },
            )
            CustomCategoriesSection(
                kind = CategoryKind.INCOME,
                items = settings.customIncome,
                onAdd = { name, icon -> vm.addCustomCategory(CategoryKind.INCOME, name, icon) },
                onDelete = { id -> vm.deleteCustomCategory(CategoryKind.INCOME, id) },
            )

            // ===== إدارة البيانات =====
            SettingsSectionTitle("💾 إدارة البيانات", 5)
            // إصلاح data-5: شريط تذكير — لم تعمل نسخة بعد أو آخرها قبل أسبوع
            val lastBackupTs by vm.lastBackupTs.collectAsStateWithLifecycle()
            val nowMs = System.currentTimeMillis()
            if (lastBackupTs == null || nowMs - lastBackupTs > 7 * 86_400_000L) {
                val daysAgo = lastBackupTs?.let { ((nowMs - it) / 86_400_000L).toInt() }
                AppCard(Modifier.padding(horizontal = 20.dp, vertical = 4.dp)) {
                    Row(
                        Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text("⚠️", fontSize = 18.sp)
                        Spacer(Modifier.width(10.dp))
                        Column {
                            Text(
                                if (lastBackupTs == null) "لم تعمل نسخة احتياطية بعد"
                                else "آخر نسخة احتياطية قبل ${daysAgo} يوم",
                                style = MaterialTheme.typography.labelMedium,
                                color = LocalAppColors.current.red,
                            )
                            Text(
                                "نسخة أسبوعية تحميك من فقدان بياناتك — خاصة قبل التحديثات أو تغيير الجهاز.",
                                style = MaterialTheme.typography.labelSmall,
                                color = LocalAppColors.current.muted,
                            )
                        }
                    }
                }
            }
            Column(Modifier.padding(horizontal = 20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                DataButton("💾 عمل نسخة احتياطية", MaterialTheme.colorScheme.primary) { vm.exportJson(context) }
                DataButton("📊 تصدير CSV", MaterialTheme.colorScheme.primary) { vm.exportCsv(context) }
                DataButton("📥 استيراد", MaterialTheme.colorScheme.primary) { importPicker.launch("application/json") }
                DataButton("🗑️ مسح الكل", LocalAppColors.current.red) { confirmClear = true }
                Text(
                    "💡 نصيحة: صدّر بياناتك أسبوعياً كنسخة احتياطية. ملف JSON يحتوي جميع بياناتك وعملائك.",
                    style = MaterialTheme.typography.bodySmall,
                    color = LocalAppColors.current.muted,
                )
            }

            // ===== حول التطبيق =====
            SettingsSectionTitle("ℹ️ حول التطبيق", 6)
            AppCard(Modifier.padding(horizontal = 20.dp)) {
                Column(
                    Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    FloatingIcon {
                        Box(
                            modifier = Modifier
                                .size(60.dp)
                                .clip(RoundedCornerShape(18.dp))
                                .background(
                                    Brush.linearGradient(
                                        listOf(MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.secondary),
                                    ),
                                ),
                            contentAlignment = Alignment.Center,
                        ) { Text("💰", fontSize = 30.sp) }
                    }
                    Spacer(Modifier.height(10.dp))
                    Text("محفظتي الذكية v2.7.0", style = MaterialTheme.typography.titleMedium)
                    Text(
                        "محاسبك الشخصي الذكي\nمصمم بعناية في اليمن 🇾🇪",
                        style = MaterialTheme.typography.bodySmall,
                        color = LocalAppColors.current.muted,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                    )
                    Spacer(Modifier.height(10.dp))
                    Button(
                        onClick = { vm.runSelfTest() },
                        colors = ButtonDefaults.buttonColors(containerColor = LocalAppColors.current.chipBg),
                        shape = RoundedCornerShape(20.dp),
                    ) { Text("🔍 فحص ذاتي", color = MaterialTheme.colorScheme.primary, fontSize = 11.sp) }
                    if (checks.isNotEmpty()) {
                        Spacer(Modifier.height(12.dp))
                        Column(horizontalAlignment = Alignment.Start) {
                            checks.forEachIndexed { index, c ->
                                StaggeredEntrance(index) {
                                    Text(
                                        "${if (c.ok) "✅" else "❌"} ${c.name}${if (c.details.isNotBlank()) " • ${c.details}" else ""}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = if (c.ok) LocalAppColors.current.green else LocalAppColors.current.red,
                                        modifier = Modifier.padding(vertical = 2.dp),
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (confirmClear) {
        ConfirmDialog(
            title = "مسح كل البيانات؟",
            message = "ستُحذف كل العمليات والأهداف والعملاء والإعدادات نهائياً. لا يمكن التراجع.",
            onConfirm = { confirmClear = false; vm.clearAll() },
            onDismiss = { confirmClear = false },
        )
    }
}

// ============ مكونات مساعدة ============

@Composable
private fun fieldColors() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = MaterialTheme.colorScheme.primary,
    unfocusedBorderColor = LocalAppColors.current.border,
)

@Composable
private fun SettingsSectionTitle(title: String, index: Int = 0) {
    ElasticEntrance(index) {
        Text(
            title,
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp),
        )
    }
}

@Composable
private fun SettingRow(label: String, hint: String, control: @Composable () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(label, style = MaterialTheme.typography.labelLarge)
            if (hint.isNotBlank()) {
                Text(hint, style = MaterialTheme.typography.bodySmall, color = LocalAppColors.current.muted)
            }
        }
        Spacer(Modifier.width(12.dp))
        control()
    }
}

@Composable
private fun SettingField(label: String, hint: String, field: @Composable () -> Unit) {
    Column(Modifier.padding(horizontal = 20.dp, vertical = 8.dp)) {
        Text(label, style = MaterialTheme.typography.labelLarge)
        if (hint.isNotBlank()) {
            Text(hint, style = MaterialTheme.typography.bodySmall, color = LocalAppColors.current.muted)
        }
        Spacer(Modifier.height(6.dp))
        field()
    }
}

@Composable
private fun CurrencyPicker(current: String, onSelect: (String) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    val currencies = listOf(
        "ر.ي" to "ر.ي يمني",
        "ر.س" to "ر.س سعودي",
        "$" to "دولار",
        "د.إ" to "د.إ إماراتي",
        "د.ك" to "د.ك كويتي",
        "ج.م" to "ج.م مصري",
        "ل.س" to "ل.س سوري",
    )
    Box {
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(12.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .clickable { expanded = true }
                .padding(horizontal = 14.dp, vertical = 10.dp),
        ) { Text(current, style = MaterialTheme.typography.labelLarge) }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            currencies.forEach { (code, name) ->
                DropdownMenuItem(
                    text = { Text("$name ($code)") },
                    onClick = { expanded = false; onSelect(code) },
                )
            }
        }
    }
}

@Composable
private fun BudgetInput(current: Double, onChange: (Double) -> Unit) {
    var text by remember { mutableStateOf(if (current > 0) Money.input(current) else "") }
    var focused by remember { mutableStateOf(false) }
    LaunchedEffect(current, focused) {
        if (!focused) text = if (current > 0) Money.input(current) else ""
    }
    OutlinedTextField(
        value = text,
        onValueChange = {
            val filtered = it.filter { c -> c.isDigit() || c == '.' }
            text = filtered
            onChange(Money.parse(filtered))
        },
        modifier = Modifier
            .width(130.dp)
            .onFocusChanged { focused = it.isFocused },
        placeholder = { Text("0") },
        shape = RoundedCornerShape(12.dp),
        singleLine = true,
        colors = fieldColors(),
    )
}

@Composable
private fun CustomCategoriesSection(
    kind: CategoryKind,
    items: List<Category>,
    onAdd: (String, String) -> Unit,
    onDelete: (String) -> Unit,
) {
    val kindLabel = if (kind == CategoryKind.EXPENSE) "مصروف" else "دخل"
    var icon by remember { mutableStateOf("") }
    var name by remember { mutableStateOf("") }
    Column(Modifier.padding(horizontal = 20.dp, vertical = 4.dp)) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            AppTextField(icon, { if (it.length <= 2) icon = it }, "أيقونة", Modifier.width(76.dp))
            AppTextField(name, { name = it }, "اسم فئة $kindLabel (مثلاً: مطعم)", Modifier.weight(1f))
            Button(
                onClick = {
                    if (name.isNotBlank()) { onAdd(name.trim(), icon); name = ""; icon = "" }
                },
                shape = RoundedCornerShape(12.dp),
            ) { Text("➕") }
        }
        if (items.isNotEmpty()) {
            Spacer(Modifier.height(6.dp))
            items.forEach { c ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 3.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text("${c.icon} ${c.name}", style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
                    Text(
                        "🗑️",
                        modifier = Modifier.clickable { onDelete(c.id) },
                    )
                }
            }
        }
    }
}

@Composable
private fun DataButton(label: String, color: Color, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .bounceClick(),
        colors = ButtonDefaults.buttonColors(containerColor = color),
        shape = RoundedCornerShape(14.dp),
    ) { Text(label) }
}
